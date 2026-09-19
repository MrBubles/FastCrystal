package me.mrbubbles.fastcrystal;

import me.mrbubbles.fastcrystal.mixin.InventoryAccessor;
import me.mrbubbles.fastcrystal.mixin.MultiPlayerGameModeAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

public class FastCrystal implements ClientModInitializer {

    public static final Map<BlockPos, Long> pendingExplosions = new HashMap<>();
    public static final Minecraft mc = Minecraft.getInstance();
    private static final Map<BlockPos, Long> pendingPlacements = new HashMap<>();
    private static final Map<BlockPos, Long> attackQueue = new HashMap<>();
    private static final Map<BlockPos, FakeEndCrystalEntity> fakeCrystals = new HashMap<>();
    private static final Map<BlockPos, PendingBreak> pendingBreaks = new HashMap<>();
    public static boolean predicting = false;
    private static boolean serverDisabled = false;
    private static BlockPos predictedHitPos = null;
    private static int nextFakeCrystalId = -2;
    private static long lastEffectivePing = 250L;
    private static volatile long pingSentAt = 0L;
    private static volatile boolean pingPending = true;
    private static volatile int ping = -1;
    private static volatile long lastVanillaPingAt = 0L;
    private static volatile long vanillaPingInterval = 0L;

    private static Level lastWorld = null;

    public static boolean isEnabled() {
        return !serverDisabled;
    }

    public static BlockHitResult getLookedAtBlockHit() {
        if (mc.level == null || mc.player == null) return null;

        Entity camera = mc.getCameraEntity();
        if (camera == null) return null;

        Vec3 eyePos = camera.getEyePosition(0.0F);
        Vec3 viewVec = camera.getViewVector(0.0F);

        return mc.level.clip(new ClipContext(eyePos, eyePos.add(viewVec.scale(mc.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE))), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, camera));
    }

    public static BlockHitResult getPlaceHit(BlockHitResult hit) {
        Entity camera = mc.getCameraEntity();
        if (mc.level == null || mc.player == null || hit == null || camera == null || hit.getType() != HitResult.Type.BLOCK)
            return null;

        double range = mc.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
        double rangeSq = range * range;
        Vec3 eyePos = camera.getEyePosition(0.0F);

        BlockPos pos = hit.getBlockPos();
        BlockState state = mc.level.getBlockState(pos);
        boolean direct = state.is(Blocks.OBSIDIAN) || state.is(Blocks.BEDROCK);

        if (!direct) {
            BlockPos closest = null;
            double closestDist = Double.MAX_VALUE;

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos next = pos.offset(dx, dy, dz);
                        BlockState nextState = mc.level.getBlockState(next);
                        if ((!nextState.is(Blocks.OBSIDIAN) && !nextState.is(Blocks.BEDROCK)) || pendingPlacements.containsKey(next))
                            continue;
                        Vec3 top = new Vec3(next.getX() + 0.5, next.getY() + 1.0, next.getZ() + 0.5);
                        if (eyePos.distanceToSqr(top) > rangeSq) continue;
                        double dist = hit.getLocation().distanceToSqr(top);
                        if (dist < closestDist) {
                            closestDist = dist;
                            closest = next;
                        }
                    }
                }
            }

            if (closest == null) return null;
            pos = closest;
        }

        if (hit.getDirection() == Direction.UP || !direct) {
            for (int i = 0; i < 3; i++) {
                BlockPos above = pos.above();
                BlockState aboveState = mc.level.getBlockState(above);
                if (!aboveState.is(Blocks.OBSIDIAN) && !aboveState.is(Blocks.BEDROCK)) break;
                pos = above;
            }
        }

        if (pendingPlacements.containsKey(pos) || !mc.level.isEmptyBlock(pos.above()) || !mc.level.isEmptyBlock(pos.above(2)))
            return null;

        BlockPos crystalPos = pos.above();
        if (!mc.level.getEntities(null, new AABB(crystalPos.getX(), crystalPos.getY(), crystalPos.getZ(), crystalPos.getX() + 1.0, crystalPos.getY() + 2.0, crystalPos.getZ() + 1.0)).isEmpty())
            return null;

        Vec3 placePos = new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
        if (eyePos.distanceToSqr(placePos) > rangeSq) return null;
        return new BlockHitResult(placePos, Direction.UP, pos, false);
    }

    public static BlockPos baseOf(double x, double y, double z) {
        return new BlockPos((int) (x - 0.5), (int) y - 1, (int) (z - 0.5));
    }

    public static boolean isCrystal(Entity entity) {
        if (mc.player == null || mc.level == null || entity == null || entity.isRemoved() || !entity.isPickable())
            return false;

        //? if >=26.3 {
        if (entity.getType() == EntityTypes.END_CRYSTAL) return true;
        if (entity.getType() != EntityTypes.SLIME && entity.getType() != EntityTypes.MAGMA_CUBE) return false;
        //?} else {
        /*String type = entity.getType().toShortString();
        if (type.equals("end_crystal")) return true;
        if (!type.equals("slime") && !type.equals("magma_cube")) return false;
        *///?}

        BlockState state = mc.level.getBlockState(baseOf(entity.getX(), entity.getY(), entity.getZ()));

        return state.is(Blocks.OBSIDIAN) || state.is(Blocks.BEDROCK);
    }

    public static Entity getLookedAtCrystal() {
        if (mc.level == null || mc.player == null) return null;

        Entity camera = mc.getCameraEntity();
        if (camera == null) return null;

        double range = mc.player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
        double rangeSq = range * range;
        Vec3 eyePos = camera.getEyePosition(0.0F);
        Vec3 viewVec = camera.getViewVector(0.0F);
        Vec3 reachEnd = eyePos.add(viewVec.scale(range));
        AABB searchBox = camera.getBoundingBox().expandTowards(viewVec.scale(range)).inflate(1.0, 1.0, 1.0);

        Entity closest = null;
        double closestDist = rangeSq;

        for (Entity entity : mc.level.getEntities(camera, searchBox, e -> !e.isSpectator() && isCrystal(e))) {
            AABB expandedBox = entity.getBoundingBox().inflate(entity.getPickRadius());
            Optional<Vec3> optional = expandedBox.clip(eyePos, reachEnd);

            if (expandedBox.contains(eyePos)) {
                closest = entity;
                closestDist = 0.0;
            } else if (optional.isPresent()) {
                Vec3 vec3 = optional.get();
                double dist = eyePos.distanceToSqr(vec3);
                boolean isCameraVehicle = entity.getRootVehicle() == camera.getRootVehicle();
                if ((dist < closestDist || closestDist == 0.0) && (!isCameraVehicle || closestDist == 0.0)) {
                    closest = entity;
                    if (!isCameraVehicle) closestDist = dist;
                }
            }
        }

        if (closest != null) return closest;

        predictedHitPos = null;
        double predictedClosestDist = Double.MAX_VALUE;

        for (Map.Entry<BlockPos, Long> entry : pendingPlacements.entrySet()) {
            BlockPos up = entry.getKey().above();
            AABB box = new AABB(up.getX() - 0.5, up.getY(), up.getZ() - 0.5, up.getX() + 1.5, up.getY() + 2.0, up.getZ() + 1.5);

            if (box.contains(eyePos)) {
                predictedHitPos = entry.getKey();
                return null;
            }

            Optional<Vec3> optional = box.clip(eyePos, reachEnd);
            if (optional.isPresent()) {
                Vec3 vec3 = optional.get();
                double dist = eyePos.distanceToSqr(vec3);
                if (dist < predictedClosestDist && dist < rangeSq) {
                    predictedClosestDist = dist;
                    predictedHitPos = entry.getKey();
                }
            }
        }

        return closest;
    }

    public static BlockPos getPredictedHit() {
        BlockPos pos = predictedHitPos;
        predictedHitPos = null;
        return pos;
    }

    public static boolean canPlaceCrystal(BlockPos pos, InteractionHand hand) {
        if (mc.level == null || mc.player == null) return false;

        BlockState state = mc.level.getBlockState(pos);

        if (!mc.player.getItemInHand(hand).is(Items.END_CRYSTAL) || (!state.is(Blocks.OBSIDIAN) && !state.is(Blocks.BEDROCK)))
            return false;

        if (!mc.level.isEmptyBlock(pos.above()) || !mc.level.isEmptyBlock(pos.above(2)) || pendingPlacements.containsKey(pos))
            return false;

        BlockPos crystalPos = pos.above();
        return mc.level.getEntities(null, new AABB(crystalPos.getX(), crystalPos.getY(), crystalPos.getZ(), crystalPos.getX() + 1.0, crystalPos.getY() + 2.0, crystalPos.getZ() + 1.0)).isEmpty();
    }

    public static boolean canBreakCrystal() {
        if (mc.level == null || getDamage() > 0.0) return true;
        return mc.player.getMainHandItem().getEnchantments().getLevel(mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS)) > 0;
    }

    private static double getDamage() {
        MobEffectInstance weakness = mc.player.getEffect(MobEffects.WEAKNESS);
        MobEffectInstance strength = mc.player.getEffect(MobEffects.STRENGTH);
        double damage = mc.player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        if (strength != null) damage += 3.0 * (strength.getAmplifier() + 1);
        if (weakness != null) damage -= 4.0 * (weakness.getAmplifier() + 1);
        return Math.max(0.0, damage);
    }

    public static void sendPacket(Packet<?> packet) {
        if (mc.getConnection() == null || packet == null) return;
        mc.getConnection().send(packet);
    }

    public static void syncSelectedSlot() {
        if (mc.player == null || mc.getConnection() == null || mc.gameMode == null) return;

        MultiPlayerGameModeAccessor gameMode = ((MultiPlayerGameModeAccessor) mc.gameMode);
        InventoryAccessor inventory = ((InventoryAccessor) mc.player.getInventory());
        int selectedSlot = inventory.getSelected();
        if (selectedSlot != gameMode.getCarriedIndex()) {
            gameMode.setCarriedIndex(selectedSlot);
            sendPacket(new ServerboundSetCarriedItemPacket(gameMode.getCarriedIndex()));
        }
    }

    public static void doServerInteractBlock(InteractionHand hand, BlockHitResult blockHit) {
        if (mc.player == null || mc.gameMode == null) return;

        BlockPos pos = blockHit.getBlockPos().immutable();
        syncSelectedSlot();
        pendingPlacements.put(pos, System.currentTimeMillis());
        sendPacket(new ServerboundUseItemOnPacket(hand, blockHit, 0));
        //? if >=26.3 {
        mc.player.swing(hand, mc.player.getItemInHand(hand).getInteractAnimation(), false);
        //?} else {
        /*mc.player.swing(hand);
         *///?}
        spawnFakeCrystal(pos);
    }

    private static FakeEndCrystalEntity spawnFakeCrystal(BlockPos pos) {
        pos = pos.immutable();
        if (mc.level == null || fakeCrystals.containsKey(pos)) return null;

        FakeEndCrystalEntity fake = new FakeEndCrystalEntity(mc.level, pos, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
        fake.setId(nextFakeCrystalId--);
        fakeCrystals.put(pos, fake);
        mc.level.addEntity(fake);
        return fake;
    }

    public static FakeEndCrystalEntity getFakeCrystal(Entity entity) {
        for (FakeEndCrystalEntity fake : fakeCrystals.values()) {
            if (fake == entity) return fake;
        }
        return null;
    }

    public static void attackFakeCrystal(FakeEndCrystalEntity fake) {
        if (fake.real != null) {
            attackCrystal(fake.getInteractPos(), fake.real);
        } else {
            queueAttack(fake.getInteractPos());
        }
        destroyFakeCrystal(fake);
    }

    private static void attackCrystal(BlockPos pos, Entity real) {
        if (pendingBreaks.containsKey(pos)) return;
        doServerAttack(real);
        predictExplosion(real);
        pendingBreaks.put(pos, new PendingBreak(real, System.currentTimeMillis()));
    }

    public static void queueAttack(BlockPos pos) {
        attackQueue.put(pos, System.currentTimeMillis());
    }

    private static long getEffectivePing() {
        long raw = ping >= 0 ? ping : getPing();
        lastEffectivePing = Math.max(raw, (long) (lastEffectivePing * 0.999));
        return lastEffectivePing;
    }

    private static long getPing() {
        if (mc.getConnection() == null || mc.player == null) return 0;

        PlayerInfo entry = mc.getConnection().getPlayerInfo(mc.player.getUUID());
        return entry != null ? entry.getLatency() : 0;
    }

    private static void tickPingSampler() {
        if (mc.getConnection() == null || mc.level == null) return;

        long now = System.currentTimeMillis();
        if (!pingPending) {
            if (now - pingSentAt < 10000L) return;
            pingPending = true;
        }
        if (now - pingSentAt < getNextPingInterval()) return;

        sendPacket(new ServerboundCommandSuggestionPacket(1000, "w "));
        pingSentAt = now;
        pingPending = false;
    }

    private static long getNextPingInterval() {
        if (vanillaPingInterval <= 0L) return 5000L;
        return Math.min(5000L, Math.max(1000L, vanillaPingInterval / 2L));
    }

    public static void onPacketReceive(Packet<?> packet) {
        if (packet instanceof ClientboundCommandSuggestionsPacket suggestions && suggestions.id() == 1000) {
            ping = (int) (System.currentTimeMillis() - pingSentAt);
            pingPending = true;
        } else if (packet instanceof ClientboundPlayerInfoUpdatePacket playerList) trackVanillaPing(playerList);
    }

    private static void trackVanillaPing(ClientboundPlayerInfoUpdatePacket playerList) {
        if (mc.player == null) return;

        for (ClientboundPlayerInfoUpdatePacket.Entry entry : playerList.entries()) {
            if (!entry.profileId().equals(mc.player.getUUID())) continue;

            for (ClientboundPlayerInfoUpdatePacket.Action action : playerList.actions()) {
                if (action != ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY) continue;
                long now = System.currentTimeMillis();
                if (lastVanillaPingAt != 0L) vanillaPingInterval = now - lastVanillaPingAt;
                lastVanillaPingAt = now;
                return;
            }
        }
    }

    public static void cleanup() {
        checkWorldChange();
        long now = System.currentTimeMillis();
        long eff = getEffectivePing();
        long fakeTtl = Math.max(300, eff * 7 / 4 + 150);
        long queueTtl = Math.max(300, eff * 29 / 20 + 150);
        long explodeTtl = Math.max(300, eff * 8 / 5 + 150);
        expire(pendingPlacements, fakeTtl, now);
        expire(attackQueue, queueTtl, now);
        expire(pendingExplosions, explodeTtl, now);

        Iterator<Map.Entry<BlockPos, PendingBreak>> bit = pendingBreaks.entrySet().iterator();
        while (bit.hasNext()) {
            Map.Entry<BlockPos, PendingBreak> entry = bit.next();
            if (now - entry.getValue().time() <= fakeTtl) continue;

            bit.remove();
            if (fakeCrystals.containsKey(entry.getKey())) continue;

            Entity real = entry.getValue().real();
            if (real == null || real.isRemoved()) continue;

            FakeEndCrystalEntity fake = spawnFakeCrystal(entry.getKey());
            if (fake != null) fake.real = real;
        }

        Iterator<Map.Entry<BlockPos, FakeEndCrystalEntity>> it = fakeCrystals.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, FakeEndCrystalEntity> entry = it.next();
            FakeEndCrystalEntity fake = entry.getValue();
            if (fake.real != null && fake.real.isRemoved()) {
                removeFakeCrystal(fake);
                it.remove();
                continue;
            }
            if (fake.real == null && !pendingPlacements.containsKey(entry.getKey())) {
                removeFakeCrystal(fake);
                it.remove();
            }
        }
    }

    private static void checkWorldChange() {
        if (mc.level == lastWorld) return;
        lastWorld = mc.level;
        fakeCrystals.clear();
        pendingPlacements.clear();
        attackQueue.clear();
        pendingExplosions.clear();
        pendingBreaks.clear();
        predictedHitPos = null;
    }

    private static void expire(Map<BlockPos, Long> map, long ttl, long now) {
        Iterator<Map.Entry<BlockPos, Long>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, Long> entry = it.next();
            if (now - entry.getValue() <= ttl) continue;
            it.remove();
        }
    }

    public static boolean onEntitySpawn(Entity entity) {
        if (entity.getId() < 0 || !isCrystal(entity)) return false;

        BlockPos pos = baseOf(entity.getX(), entity.getY(), entity.getZ());
        FakeEndCrystalEntity fake = fakeCrystals.get(pos);
        boolean hadFake = fake != null;

        if (hadFake) {
            fake.real = entity;
        }
        pendingPlacements.remove(pos);
        pendingBreaks.remove(pos);
        Entity camera = mc.getCameraEntity();
        if (!attackQueue.containsKey(pos) || camera == null) return hadFake;

        Vec3 eyePos = camera.getEyePosition(0.0F);
        Vec3 viewVec = camera.getViewVector(0.0F);
        Vec3 reachEnd = eyePos.add(viewVec.scale(mc.player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE)));
        AABB expandedBox = entity.getBoundingBox().inflate(entity.getPickRadius());
        if (expandedBox.clip(eyePos, reachEnd).isEmpty() && !expandedBox.contains(eyePos)) return hadFake;

        attackQueue.remove(pos);
        attackCrystal(pos, entity);
        destroyFakeCrystal(fake);
        return true;
    }

    public static void predictExplosion(Entity crystal) {
        if (mc.level == null) return;

        double x = crystal.getX();
        double y = crystal.getY();
        double z = crystal.getZ();
        BlockPos base = baseOf(x, y, z);
        pendingExplosions.remove(base);
        predicting = true;
        mc.level.playLocalSound(x, y, z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 4.0F, (1.0F + (mc.level.getRandom().nextFloat() - mc.level.getRandom().nextFloat()) * 0.2F) * 0.7F, false);
        predicting = false;
        mc.level.addParticle(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1.0, 0.0, 0.0);
        pendingExplosions.put(base, System.currentTimeMillis());
    }

    public static void onPredictedExplosionSound(BlockPos pos) {
        pendingExplosions.remove(pos);
        pendingBreaks.remove(pos);
    }

    public static void onCrystalExploded(BlockPos pos) {
        pendingBreaks.remove(pos);
        destroyFakeCrystal(fakeCrystals.get(pos));
    }

    public static void onDisconnect() {
        for (FakeEndCrystalEntity fake : fakeCrystals.values().toArray(new FakeEndCrystalEntity[0])) {
            destroyFakeCrystal(fake);
        }
        fakeCrystals.clear();
        pendingPlacements.clear();
        attackQueue.clear();
        pendingExplosions.clear();
        pendingBreaks.clear();
        predictedHitPos = null;
        lastEffectivePing = 250L;
        pingSentAt = 0L;
        pingPending = true;
        ping = -1;
        lastVanillaPingAt = 0L;
        vanillaPingInterval = 0L;
    }

    private static void removeFakeCrystal(FakeEndCrystalEntity fake) {
        if (fake == null || fake.isRemoved()) return;

        if (mc.level != null) mc.level.removeEntity(fake.getId(), Entity.RemovalReason.DISCARDED);
    }

    private static void destroyFakeCrystal(FakeEndCrystalEntity fake) {
        if (fake == null || !fakeCrystals.values().remove(fake)) return;
        removeFakeCrystal(fake);
    }

    public static void doServerAttack(Entity entity) {
        if (mc.player == null || mc.gameMode == null) return;

        syncSelectedSlot();
        sendPacket(new ServerboundAttackPacket(entity.getId()));
        //? if <26.3 {
        /*sendPacket(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
         *///?}
    }

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(_ -> {
            syncSelectedSlot();
            tickPingSampler();
            cleanup();
        });
        PayloadTypeRegistry.clientboundPlay().register(DisableFastCrystalPayload.ID, DisableFastCrystalPayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(DisableFastCrystalPayload.ID, (_, context) -> context.client().execute(() -> {
            serverDisabled = true;
            //? if >=26.3 {
            mc.gui.chatListener().handleSystemMessage(Component.literal("[FastCrystal] FastCrystal has been disabled on this server."), false);
            //?} else {
            /*mc.gui.getChat().addClientSystemMessage(Component.literal("[FastCrystal] FastCrystal has been disabled on this server."));
             *///?}
        }));

        ClientPlayConnectionEvents.DISCONNECT.register((_, _) -> {
            serverDisabled = false;
            onDisconnect();
        });
    }

    private record PendingBreak(Entity real, long time) {
    }

    public static class FakeEndCrystalEntity extends EndCrystal {
        private final BlockPos interactPos;
        @Nullable
        public Entity real;

        public FakeEndCrystalEntity(Level level, BlockPos interactPos, double x, double y, double z) {
            super(level, x, y, z);
            this.interactPos = interactPos;
        }

        public BlockPos getInteractPos() {
            return interactPos;
        }

        @Override
        public boolean showsBottom() {
            return mc.level.dimension() == Level.END;
        }
    }
}