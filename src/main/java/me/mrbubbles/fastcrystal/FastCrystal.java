package me.mrbubbles.fastcrystal;

import me.mrbubbles.fastcrystal.mixin.ClientPlayerInteractionManagerInterface;
import me.mrbubbles.fastcrystal.mixin.PlayerInventoryInterface;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.s2c.play.CommandSuggestionsS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

public class FastCrystal implements ClientModInitializer {

    public static final Map<BlockPos, Long> pendingExplosions = new HashMap<>();
    public static final MinecraftClient mc = MinecraftClient.getInstance();
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
    private static World lastWorld = null;

    public static boolean isEnabled() {
        return !serverDisabled;
    }

    public static BlockHitResult getLookedAtBlockHit() {
        Entity camera = mc.getCameraEntity();
        if (mc.world == null || mc.player == null || camera == null) return null;

        Vec3d camPos = camera.getCameraPosVec(0.0F);
        Vec3d rotationVec = camera.getRotationVec(0.0F);

        return mc.world.raycast(new RaycastContext(camPos, camPos.add(rotationVec.multiply(mc.player.getBlockInteractionRange())), RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, camera));
    }

    public static BlockHitResult getPlaceHit(BlockHitResult hit) {
        Entity camera = mc.getCameraEntity();
        if (mc.world == null || mc.player == null || hit == null || camera == null || hit.getType() != HitResult.Type.BLOCK)
            return null;

        double range = mc.player.getBlockInteractionRange();
        double rangeSq = range * range;
        Vec3d camPos = camera.getCameraPosVec(0.0F);

        BlockPos pos = hit.getBlockPos();
        BlockState state = mc.world.getBlockState(pos);
        boolean direct = state.isOf(Blocks.OBSIDIAN) || state.isOf(Blocks.BEDROCK);

        if (!direct) {
            BlockPos closest = null;
            double closestDist = Double.MAX_VALUE;

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos next = pos.add(dx, dy, dz);
                        BlockState nextState = mc.world.getBlockState(next);
                        if ((!nextState.isOf(Blocks.OBSIDIAN) && !nextState.isOf(Blocks.BEDROCK)) || pendingPlacements.containsKey(next))
                            continue;
                        Vec3d top = new Vec3d(next.getX() + 0.5, next.getY() + 1.0, next.getZ() + 0.5);
                        if (camPos.squaredDistanceTo(top) > rangeSq) continue;
                        double dist = hit.getPos().squaredDistanceTo(top);
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

        if (hit.getSide() == Direction.UP || !direct) {
            for (int i = 0; i < 3; i++) {
                BlockPos above = pos.up();
                BlockState aboveState = mc.world.getBlockState(above);
                if (!aboveState.isOf(Blocks.OBSIDIAN) && !aboveState.isOf(Blocks.BEDROCK)) break;
                pos = above;
            }
        }

        if (pendingPlacements.containsKey(pos) || !mc.world.isAir(pos.up()) || !mc.world.isAir(pos.up(2))) return null;

        BlockPos crystalPos = pos.up();
        if (!mc.world.getOtherEntities(null, new Box(crystalPos.getX(), crystalPos.getY(), crystalPos.getZ(), crystalPos.getX() + 1.0, crystalPos.getY() + 2.0, crystalPos.getZ() + 1.0)).isEmpty())
            return null;

        Vec3d placePos = new Vec3d(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
        if (camPos.squaredDistanceTo(placePos) > rangeSq) return null;
        return new BlockHitResult(placePos, Direction.UP, pos, false);
    }

    public static BlockPos baseOf(double x, double y, double z) {
        return new BlockPos((int) (x - 0.5), (int) y - 1, (int) (z - 0.5));
    }

    public static boolean isCrystal(Entity entity) {
        if (mc.player == null || mc.world == null || entity == null || entity.isRemoved() || !entity.canHit())
            return false;

        EntityType<?> type = entity.getType();
        if (type == EntityType.END_CRYSTAL) return true;
        if (type != EntityType.SLIME && type != EntityType.MAGMA_CUBE) return false;

        BlockState state = mc.world.getBlockState(baseOf(entity.getX(), entity.getY(), entity.getZ()));

        return state.isOf(Blocks.OBSIDIAN) || state.isOf(Blocks.BEDROCK);
    }

    public static Entity getLookedAtCrystal() {
        Entity camera = mc.getCameraEntity();
        if (mc.world == null || mc.player == null || camera == null) return null;

        double range = mc.player.getEntityInteractionRange();
        double rangeSq = range * range;
        Vec3d camPos = camera.getCameraPosVec(0.0F);
        Vec3d lookVec = camera.getRotationVec(0.0F);
        Vec3d endPos = camPos.add(lookVec.multiply(range));
        Box searchBox = camera.getBoundingBox().stretch(lookVec.multiply(range)).expand(1.0, 1.0, 1.0);

        Entity closest = null;
        double closestDist = rangeSq;

        for (Entity entity : mc.world.getOtherEntities(camera, searchBox, e -> EntityPredicates.EXCEPT_SPECTATOR.test(e) && isCrystal(e))) {
            Box expandedBox = entity.getBoundingBox().expand(entity.getTargetingMargin());
            Optional<Vec3d> optional = expandedBox.raycast(camPos, endPos);

            if (expandedBox.contains(camPos)) {
                closest = entity;
                closestDist = 0.0;
            } else if (optional.isPresent()) {
                Vec3d vec3d = optional.get();
                double dist = camPos.squaredDistanceTo(vec3d);
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
            BlockPos up = entry.getKey().up();
            Box box = new Box(up.getX() - 0.5, up.getY(), up.getZ() - 0.5, up.getX() + 1.5, up.getY() + 2.0, up.getZ() + 1.5);

            if (box.contains(camPos)) {
                predictedHitPos = entry.getKey();
                return null;
            }

            Optional<Vec3d> optional = box.raycast(camPos, endPos);
            if (optional.isPresent()) {
                Vec3d vec3d = optional.get();
                double dist = camPos.squaredDistanceTo(vec3d);
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

    public static boolean canPlaceCrystal(BlockPos pos, Hand hand) {
        if (mc.world == null || mc.player == null) return false;

        BlockState state = mc.world.getBlockState(pos);

        if (!mc.player.getStackInHand(hand).isOf(Items.END_CRYSTAL) || (!state.isOf(Blocks.OBSIDIAN) && !state.isOf(Blocks.BEDROCK)))
            return false;

        if (!mc.world.isAir(pos.up()) || !mc.world.isAir(pos.up(2)) || pendingPlacements.containsKey(pos)) return false;

        BlockPos crystalPos = pos.up();
        return mc.world.getOtherEntities(null, new Box(crystalPos.getX(), crystalPos.getY(), crystalPos.getZ(), crystalPos.getX() + 1.0, crystalPos.getY() + 2.0, crystalPos.getZ() + 1.0)).isEmpty();
    }

    public static boolean canBreakCrystal() {
        if (mc.world == null || getDamage() > 0.0) return true;
        return EnchantmentHelper.getLevel(mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS), mc.player.getMainHandStack()) > 0;
    }

    private static double getDamage() {
        StatusEffectInstance weakness = mc.player.getStatusEffect(StatusEffects.WEAKNESS);
        StatusEffectInstance strength = mc.player.getStatusEffect(StatusEffects.STRENGTH);
        double damage = mc.player.getAttributeValue(EntityAttributes.ATTACK_DAMAGE);
        if (strength != null) damage += 3.0 * (strength.getAmplifier() + 1);
        if (weakness != null) damage -= 4.0 * (weakness.getAmplifier() + 1);
        return Math.max(0.0, damage);
    }

    public static void sendPacket(Packet<?> packet) {
        if (mc.getNetworkHandler() == null || packet == null) return;
        mc.getNetworkHandler().sendPacket(packet);
    }

    public static void syncSelectedSlot() {
        if (mc.player == null || mc.getNetworkHandler() == null || mc.interactionManager == null) return;

        ClientPlayerInteractionManagerInterface interactionManager = ((ClientPlayerInteractionManagerInterface) mc.interactionManager);
        int slot = ((PlayerInventoryInterface) mc.player.getInventory()).getSelectedSlot();
        if (slot != interactionManager.getLastSelectedSlot()) {
            interactionManager.setLastSelectedSlot(slot);
            sendPacket(new UpdateSelectedSlotC2SPacket(interactionManager.getLastSelectedSlot()));
        }
    }

    public static void doServerInteractBlock(Hand hand, BlockHitResult blockHit) {
        if (mc.player == null || mc.interactionManager == null) return;

        BlockPos pos = blockHit.getBlockPos().toImmutable();
        syncSelectedSlot();
        pendingPlacements.put(pos, System.currentTimeMillis());
        sendPacket(new PlayerInteractBlockC2SPacket(hand, blockHit, 0));
        mc.player.swingHand(hand);
        spawnFakeCrystal(pos);
    }

    private static FakeEndCrystalEntity spawnFakeCrystal(BlockPos pos) {
        pos = pos.toImmutable();
        if (mc.world == null || fakeCrystals.containsKey(pos)) return null;

        FakeEndCrystalEntity fakeCrystal = new FakeEndCrystalEntity(mc.world, pos, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
        fakeCrystal.setId(nextFakeCrystalId--);
        fakeCrystals.put(pos, fakeCrystal);
        mc.world.addEntity(fakeCrystal);
        return fakeCrystal;
    }

    public static FakeEndCrystalEntity getFakeCrystal(Entity entity) {
        for (FakeEndCrystalEntity fakeCrystal : fakeCrystals.values()) {
            if (fakeCrystal == entity) return fakeCrystal;
        }
        return null;
    }

    public static void attackFakeCrystal(FakeEndCrystalEntity fakeCrystal) {
        if (fakeCrystal.real != null) {
            attackCrystal(fakeCrystal.getInteractPos(), fakeCrystal.real);
        } else {
            queueAttack(fakeCrystal.getInteractPos());
        }
        destroyFakeCrystal(fakeCrystal);
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
        if (mc.getNetworkHandler() == null || mc.player == null) return 0;

        PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        return entry != null ? entry.getLatency() : 0;
    }

    private static void tickPingSampler() {
        if (mc.getNetworkHandler() == null || mc.world == null) return;

        long now = System.currentTimeMillis();
        if (!pingPending) {
            if (now - pingSentAt < 10000L) return;
            pingPending = true;
        }
        if (now - pingSentAt < getNextPingInterval()) return;

        sendPacket(new RequestCommandCompletionsC2SPacket(1000, "w "));
        pingSentAt = now;
        pingPending = false;
    }

    private static long getNextPingInterval() {
        if (vanillaPingInterval <= 0L) return 5000L;
        return Math.min(5000L, Math.max(1000L, vanillaPingInterval / 2L));
    }

    public static void onPacketReceive(Packet<?> packet) {
        if (packet instanceof CommandSuggestionsS2CPacket suggestions && suggestions.id() == 1000) {
            ping = (int) (System.currentTimeMillis() - pingSentAt);
            pingPending = true;
        } else if (packet instanceof PlayerListS2CPacket playerList) trackVanillaPing(playerList);
    }

    private static void trackVanillaPing(PlayerListS2CPacket playerList) {
        if (mc.player == null) return;

        for (PlayerListS2CPacket.Entry entry : playerList.getEntries()) {
            if (!entry.profileId().equals(mc.player.getUuid())) continue;

            for (PlayerListS2CPacket.Action action : playerList.getActions()) {
                if (action != PlayerListS2CPacket.Action.UPDATE_LATENCY) continue;
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
        long fakeCrystalTtl = Math.max(300, eff * 7 / 4 + 150);
        long queueTtl = Math.max(300, eff * 29 / 20 + 150);
        long explodeTtl = Math.max(300, eff * 8 / 5 + 150);
        expire(pendingPlacements, fakeCrystalTtl, now);
        expire(attackQueue, queueTtl, now);
        expire(pendingExplosions, explodeTtl, now);

        Iterator<Map.Entry<BlockPos, PendingBreak>> bit = pendingBreaks.entrySet().iterator();
        while (bit.hasNext()) {
            Map.Entry<BlockPos, PendingBreak> entry = bit.next();
            if (now - entry.getValue().time() <= fakeCrystalTtl) continue;

            bit.remove();
            if (fakeCrystals.containsKey(entry.getKey())) continue;

            Entity real = entry.getValue().real();
            if (real == null || real.isRemoved()) continue;

            FakeEndCrystalEntity fakeCrystal = spawnFakeCrystal(entry.getKey());
            if (fakeCrystal != null) {
                fakeCrystal.real = real;
            }
        }

        Iterator<Map.Entry<BlockPos, FakeEndCrystalEntity>> it = fakeCrystals.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, FakeEndCrystalEntity> entry = it.next();
            FakeEndCrystalEntity fakeCrystal = entry.getValue();
            if (fakeCrystal.real != null && fakeCrystal.real.isRemoved()) {
                removeFakeCrystal(fakeCrystal);
                it.remove();
                continue;
            }
            if (fakeCrystal.real == null && !pendingPlacements.containsKey(entry.getKey())) {
                removeFakeCrystal(fakeCrystal);
                it.remove();
            }
        }
    }

    private static void checkWorldChange() {
        if (mc.world == lastWorld) return;
        lastWorld = mc.world;
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
        FakeEndCrystalEntity fakeCrystal = fakeCrystals.get(pos);
        boolean hadFake = fakeCrystal != null;

        if (hadFake) {
            fakeCrystal.real = entity;
        }
        pendingPlacements.remove(pos);
        pendingBreaks.remove(pos);
        Entity camera = mc.getCameraEntity();
        if (!attackQueue.containsKey(pos) || camera == null) return hadFake;

        Vec3d camPos = camera.getCameraPosVec(0.0F);
        Vec3d lookVec = camera.getRotationVec(0.0F);
        Vec3d endPos = camPos.add(lookVec.multiply(mc.player.getEntityInteractionRange()));
        Box expandedBox = entity.getBoundingBox().expand(entity.getTargetingMargin());
        if (expandedBox.raycast(camPos, endPos).isEmpty() && !expandedBox.contains(camPos)) {
            return hadFake;
        }

        attackQueue.remove(pos);
        attackCrystal(pos, entity);
        destroyFakeCrystal(fakeCrystal);
        return true;
    }

    public static void predictExplosion(Entity crystal) {
        if (mc.world == null) return;

        double x = crystal.getX();
        double y = crystal.getY();
        double z = crystal.getZ();
        BlockPos base = baseOf(x, y, z);
        pendingExplosions.remove(base);
        predicting = true;
        mc.world.playSound(x, y, z, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.BLOCKS, 4.0F, (1.0F + (mc.world.random.nextFloat() - mc.world.random.nextFloat()) * 0.2F) * 0.7F, false);
        predicting = false;
        mc.world.addParticle(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1.0, 0.0, 0.0);
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
        for (FakeEndCrystalEntity fakeCrystal : fakeCrystals.values().toArray(new FakeEndCrystalEntity[0])) {
            destroyFakeCrystal(fakeCrystal);
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

    private static void removeFakeCrystal(FakeEndCrystalEntity fakeCrystal) {
        if (fakeCrystal == null || fakeCrystal.isRemoved()) return;

        if (mc.world != null) mc.world.removeEntity(fakeCrystal.getId(), RemovalReason.DISCARDED);
    }

    private static void destroyFakeCrystal(FakeEndCrystalEntity fakeCrystal) {
        if (fakeCrystal == null || !fakeCrystals.values().remove(fakeCrystal)) return;
        removeFakeCrystal(fakeCrystal);
    }

    public static void doServerAttack(Entity entity) {
        if (mc.player == null || mc.interactionManager == null) return;

        syncSelectedSlot();
        sendPacket(PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking()));
        sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
    }

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            syncSelectedSlot();
            tickPingSampler();
            cleanup();
        });
        PayloadTypeRegistry.playS2C().register(DisableFastCrystalPayload.ID, DisableFastCrystalPayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(DisableFastCrystalPayload.ID, (payload, context) -> context.client().execute(() -> {
            serverDisabled = true;
            mc.inGameHud.getChatHud().addMessage(Text.literal("[FastCrystal] FastCrystal has been disabled on this server."));
        }));

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            serverDisabled = false;
            onDisconnect();
        });
    }

    private record PendingBreak(Entity real, long time) {
    }

    public static class FakeEndCrystalEntity extends EndCrystalEntity {
        private final BlockPos interactPos;
        @Nullable
        public Entity real;

        public FakeEndCrystalEntity(World world, BlockPos interactPos, double x, double y, double z) {
            super(world, x, y, z);
            this.interactPos = interactPos;
        }

        public BlockPos getInteractPos() {
            return interactPos;
        }

        @Override
        public boolean shouldShowBottom() {
            return mc.world.getRegistryKey() == World.END;
        }
    }
}
