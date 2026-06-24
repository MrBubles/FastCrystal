package me.mrbubbles.fastcrystal;

import me.mrbubbles.fastcrystal.mixin.InventoryAccessor;
import me.mrbubbles.fastcrystal.mixin.MultiPlayerGameModeAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class FastCrystal implements ClientModInitializer {

    public static final Minecraft mc = Minecraft.getInstance();

    private static final Map<BlockPos, Long> pendingPlacements = new HashMap<>();
    private static final Map<BlockPos, Long> attackQueue = new HashMap<>();
    private static boolean serverDisabled = false;
    private static BlockPos predictedHitPos = null;

    public static boolean isEnabled() {
        return !serverDisabled;
    }

    public static BlockHitResult getLookedAtBlockHit() {
        if (mc.level == null || mc.player == null) return null;

        Entity camera = mc.getCameraEntity();
        if (camera == null) return null;

        Vec3 eyePos = camera.getEyePosition(0f);
        Vec3 viewVec = camera.getViewVector(0f);

        return mc.level.clip(new ClipContext(eyePos, eyePos.add(viewVec.scale(mc.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE))), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, camera));
    }

    public static boolean isCrystal(Entity entity) {
        if (mc.player == null || mc.level == null || entity == null || entity.isRemoved() || !entity.isPickable())
            return false;

        String type = entity.getType().toShortString();
        if (type.equals("end_crystal")) return true;
        if (!type.equals("slime") && !type.equals("magma_cube")) return false;

        BlockPos belowPos = BlockPos.containing(entity.position().add(-0.5, -1.0, -0.5));
        BlockState blockState = mc.level.getBlockState(belowPos);

        if (!blockState.is(Blocks.OBSIDIAN) && !blockState.is(Blocks.BEDROCK)) return false;

        return !mc.player.getMainHandItem().getComponents().has(DataComponents.TOOL) && !mc.player.getOffhandItem().getComponents().has(DataComponents.TOOL);
    }

    public static Entity getLookedAtCrystal() {
        if (mc.level == null || mc.player == null) return null;

        Entity camera = mc.getCameraEntity();
        if (camera == null) return null;

        double range = mc.player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
        double rangeSq = range * range;
        Vec3 eyePos = camera.getEyePosition(0f);
        Vec3 viewVec = camera.getViewVector(0f);
        Vec3 reachEnd = eyePos.add(viewVec.scale(range));
        AABB searchBox = camera.getBoundingBox().expandTowards(viewVec.scale(range)).inflate(1.0, 1.0, 1.0);

        EntityHitResult hitResult = ProjectileUtil.getEntityHitResult(camera, eyePos, reachEnd, searchBox, entity -> !entity.isSpectator() && entity.isPickable() && isCrystal(entity), rangeSq);
        if (hitResult != null) return hitResult.getEntity();

        predictedHitPos = null;
        double predictedClosestDist = Double.MAX_VALUE;

        for (Map.Entry<BlockPos, Long> entry : pendingPlacements.entrySet()) {
            AABB box = crystalBox(entry.getKey());

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

        return null;
    }

    public static BlockPos getPredictedHit() {
        BlockPos pos = predictedHitPos;
        predictedHitPos = null;
        return pos;
    }

    public static boolean canPlaceCrystal(BlockPos blockPos, InteractionHand hand) {
        if (mc.level == null || mc.player == null) return false;

        BlockPos abovePos = blockPos.above();
        BlockState blockState = mc.level.getBlockState(blockPos);

        if (!mc.player.getItemInHand(hand).is(Items.END_CRYSTAL) || (!blockState.is(Blocks.OBSIDIAN) && !blockState.is(Blocks.BEDROCK)))
            return false;

        if (!mc.level.isEmptyBlock(abovePos)) return false;

        return mc.level.getEntities(null, AABB.ofSize(Vec3.atCenterOf(abovePos), 1.0, 2.0, 1.0)).isEmpty();
    }

    public static boolean canBreakCrystal() {
        MobEffectInstance weakness = mc.player.getEffect(MobEffects.WEAKNESS);
        return calculateDamage(weakness) > 0.0;
    }

    private static double calculateDamage(MobEffectInstance weakness) {
        MobEffectInstance strength = mc.player.getEffect(MobEffects.STRENGTH);
        double strengthBonus = (strength != null) ? 3.0 * (strength.getAmplifier() + 1) : 0.0;
        double weaknessPenalty = (weakness != null) ? 4.0 * (weakness.getAmplifier() + 1) : 0.0;
        return Math.max(0.0, mc.player.getAttributeValue(Attributes.ATTACK_DAMAGE) + getWeaponDamage(mc.player.getMainHandItem()) + strengthBonus - weaknessPenalty);
    }

    private static double getWeaponDamage(ItemStack stack) {
        if (stack.isEmpty()) return 0.0;
        final double[] damageSum = {0.0};
        stack.forEachModifier(EquipmentSlot.MAINHAND, (attributeHolder, attributeModifier) -> {
            if (Attributes.ATTACK_DAMAGE.equals(attributeHolder)) {
                damageSum[0] += attributeModifier.amount();
            }
        });
        return damageSum[0];
    }

    public static void sendPacket(Packet<?> packet) {
        if (mc.getConnection() == null || packet == null) return;
        mc.getConnection().send(packet);
    }

    public static void ensureHasSentCarriedItem() {
        if (mc.player == null || mc.getConnection() == null || mc.gameMode == null) return;

        MultiPlayerGameModeAccessor gameMode = ((MultiPlayerGameModeAccessor) mc.gameMode);
        InventoryAccessor inventory = ((InventoryAccessor) mc.player.getInventory());
        int selectedSlot = inventory.getSelected();
        if (selectedSlot != gameMode.getCarriedIndex()) {
            gameMode.setCarriedIndex(selectedSlot);
            mc.getConnection().send(new ServerboundSetCarriedItemPacket(gameMode.getCarriedIndex()));
        }
    }

    public static void doServerInteractBlock(InteractionHand hand, BlockHitResult hitResult) {
        if (mc.player == null || mc.gameMode == null) return;

        ensureHasSentCarriedItem();
        sendPacket(new ServerboundUseItemOnPacket(hand, hitResult, 0));
        mc.player.swing(hand);
        addPendingPlacement(hitResult.getBlockPos());
    }

    public static void doServerAttack(Entity entity) {
        if (mc.player == null || mc.gameMode == null) return;

        ensureHasSentCarriedItem();
        sendPacket(new ServerboundAttackPacket(entity.getId()));
        sendPacket(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
    }

    private static void addPendingPlacement(BlockPos pos) {
        cleanup();
        pendingPlacements.put(pos, System.currentTimeMillis());
    }

    public static void queueAttack(BlockPos pos) {
        cleanup();
        attackQueue.put(pos, System.currentTimeMillis());
    }

    private static AABB crystalBox(BlockPos base) {
        BlockPos pos = base.above();
        return new AABB(pos.getX() - 0.5, pos.getY(), pos.getZ() - 0.5, pos.getX() + 1.5, pos.getY() + 2.0, pos.getZ() + 1.5);
    }

    private static void cleanup() {
        long now = System.currentTimeMillis();
        long ping = 0;
        if (mc.getConnection() != null && mc.player != null) {
            var entry = mc.getConnection().getPlayerInfo(mc.player.getUUID());
            if (entry != null) ping = entry.getLatency();
        }
        long ttl = Math.max(50, ping * 2L + ping * ping / 200L);
        pendingPlacements.values().removeIf(time -> now - time > ttl);
        attackQueue.values().removeIf(time -> now - time > ttl);
    }

    public static void onEntitySpawn(Entity entity) {
        if (!isCrystal(entity)) return;
        cleanup();
        if (attackQueue.isEmpty() || mc.player == null || mc.level == null) return;

        Entity camera = mc.getCameraEntity();
        if (camera == null) return;

        BlockPos pos = new BlockPos((int) (entity.getX() - 0.5), (int) (entity.getY()) - 1, (int) (entity.getZ() - 0.5));
        if (!attackQueue.containsKey(pos)) return;

        Vec3 eyePos = camera.getEyePosition(0f);
        Vec3 viewVec = camera.getViewVector(0f);
        Vec3 reachEnd = eyePos.add(viewVec.scale(mc.player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE)));
        AABB expandedBox = entity.getBoundingBox().inflate(entity.getPickRadius());
        if (expandedBox.clip(eyePos, reachEnd).isEmpty() && !expandedBox.contains(eyePos)) return;

        attackQueue.remove(pos);
        pendingPlacements.remove(pos);
        doServerAttack(entity);
    }

    @Override
    public void onInitializeClient() {
        PayloadTypeRegistry.clientboundPlay().register(DisableFastCrystalPayload.ID, DisableFastCrystalPayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(DisableFastCrystalPayload.ID, (_, context) -> context.client().execute(() -> {
            serverDisabled = true;
            SystemToast.add(mc.getToastManager(), SystemToast.SystemToastId.PERIODIC_NOTIFICATION, Component.literal("FastCrystal").withStyle(ChatFormatting.DARK_PURPLE), Component.literal("FastCrystal has been disabled on this server.").withStyle(ChatFormatting.RED));
        }));

        ClientPlayConnectionEvents.DISCONNECT.register((_, _) -> serverDisabled = false);
    }
}
