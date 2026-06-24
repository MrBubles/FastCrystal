package me.mrbubbles.fastcrystal;

import me.mrbubbles.fastcrystal.mixin.ClientPlayerInteractionManagerInterface;
import me.mrbubbles.fastcrystal.mixin.PlayerInventoryInterface;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

public class FastCrystal implements ClientModInitializer {

    public static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final Map<BlockPos, Long> pendingPlacements = new HashMap<>();
    private static final Map<BlockPos, Long> attackQueue = new HashMap<>();
    private static boolean serverDisabled = false;
    private static BlockPos predictedHitPos = null;

    public static boolean isEnabled() {
        return !serverDisabled;
    }

    public static BlockHitResult getLookedAtBlockHit() {
        if (mc.world == null || mc.player == null) return null;

        Entity camera = mc.getCameraEntity();
        if (camera == null) return null;

        Vec3d camPos = camera.getCameraPosVec(0f);
        Vec3d rotationVec = camera.getRotationVec(0f);

        return mc.world.raycast(new RaycastContext(camPos, camPos.add(rotationVec.multiply(mc.player.getBlockInteractionRange())), RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, camera));
    }

    public static boolean isCrystal(Entity entity) {
        if (mc.player == null || mc.world == null || entity == null || entity.isRemoved() || !entity.canHit())
            return false;

        EntityType<?> type = entity.getType();
        if (type.equals(EntityType.END_CRYSTAL)) return true;
        if (!type.equals(EntityType.SLIME) && !type.equals(EntityType.MAGMA_CUBE)) return false;

        BlockPos pos = new BlockPos((int) (entity.getX() - 0.5), (int) (entity.getY()) - 1, (int) (entity.getZ() - 0.5));
        BlockState state = mc.world.getBlockState(pos);

        if (!state.isOf(Blocks.OBSIDIAN) && !state.isOf(Blocks.BEDROCK)) return false;

        return !mc.player.getMainHandStack().getComponents().contains(DataComponentTypes.TOOL) && !mc.player.getOffHandStack().getComponents().contains(DataComponentTypes.TOOL);
    }

    public static Entity getLookedAtCrystal() {
        if (mc.world == null || mc.player == null) return null;

        Entity camera = mc.getCameraEntity();
        if (camera == null) return null;

        double range = mc.player.getEntityInteractionRange();
        double rangeSq = range * range;
        Vec3d camPos = camera.getCameraPosVec(0f);
        Vec3d lookVec = camera.getRotationVec(0f);
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
                if (dist < closestDist || closestDist == 0.0) {
                    if (entity.getRootVehicle() == camera.getRootVehicle()) {
                        if (closestDist == 0.0) {
                            closest = entity;
                        }
                    } else {
                        closest = entity;
                        closestDist = dist;
                    }
                }
            }
        }

        if (closest != null) return closest;

        predictedHitPos = null;
        double predictedClosestDist = Double.MAX_VALUE;

        for (Map.Entry<BlockPos, Long> entry : pendingPlacements.entrySet()) {
            Box box = crystalBox(entry.getKey());

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

        if (!mc.world.isAir(pos.up())) return false;

        BlockPos crystalPos = pos.up();
        return mc.world.getOtherEntities(null, new Box(crystalPos.getX(), crystalPos.getY(), crystalPos.getZ(), crystalPos.getX() + 1.0, crystalPos.getY() + 2.0, crystalPos.getZ() + 1.0)).isEmpty();
    }

    public static boolean canBreakCrystal() {
        StatusEffectInstance weakness = mc.player.getStatusEffect(StatusEffects.WEAKNESS);
        if (weakness == null) return true;

        if (mc.player.getAttributeValue(EntityAttributes.ATTACK_DAMAGE) > 4.0 * (weakness.getAmplifier() + 1.0) + 5.0)
            return true;

        return calculateDamage(weakness) > 0.0;
    }

    private static double calculateDamage(StatusEffectInstance weakness) {
        StatusEffectInstance strength = mc.player.getStatusEffect(StatusEffects.STRENGTH);
        double strengthBonus = (strength != null) ? 3.0 * (strength.getAmplifier() + 1) : 0.0;

        double weaknessPenalty = (weakness != null) ? 4.0 * (weakness.getAmplifier() + 1) : 0.0;

        return Math.max(0.0, mc.player.getAttributeValue(EntityAttributes.ATTACK_DAMAGE) + getWeaponDamage(mc.player.getMainHandStack()) + strengthBonus - weaknessPenalty);
    }

    private static double getWeaponDamage(ItemStack item) {
        if (item.isEmpty()) return 0.0D;

        double[] damage = {0.0};
        applyAttributeModifier(item, AttributeModifierSlot.MAINHAND, (attribute, modifier) -> {
            if (attribute.equals(EntityAttributes.ATTACK_DAMAGE)) damage[0] += modifier.value();
        });
        return damage[0];
    }

    private static void applyAttributeModifier(ItemStack item, AttributeModifierSlot slot, BiConsumer<RegistryEntry<EntityAttribute>, EntityAttributeModifier> attributeModifierConsumer) {
        AttributeModifiersComponent attributeModifiersComponent = item.getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT);
        attributeModifiersComponent.applyModifiers(slot, attributeModifierConsumer);
        EnchantmentHelper.applyAttributeModifiers(item, slot, attributeModifierConsumer);
    }

    public static void sendPacket(Packet<?> packet) {
        if (mc.getNetworkHandler() == null || packet == null) return;
        mc.getNetworkHandler().sendPacket(packet);
    }

    public static void sendSequencedPacket(SequencedPacketCreator creator) {
        if (mc.world == null || mc.interactionManager == null) return;
        ((ClientPlayerInteractionManagerInterface) mc.interactionManager).sendSequencedPacket(mc.world, creator);
    }

    public static void syncSelectedSlot() {
        if (mc.player == null || mc.getNetworkHandler() == null || mc.interactionManager == null) return;

        ClientPlayerInteractionManagerInterface interactionManager = ((ClientPlayerInteractionManagerInterface) mc.interactionManager);
        int i = ((PlayerInventoryInterface) mc.player.getInventory()).getSelectedSlot();
        if (i != interactionManager.getLastSelectedSlot()) {
            interactionManager.setLastSelectedSlot(i);
            sendPacket(new UpdateSelectedSlotC2SPacket(interactionManager.getLastSelectedSlot()));
        }
    }

    public static void doServerInteractBlock(Hand hand, BlockHitResult blockHit) {
        if (mc.player == null || mc.interactionManager == null) return;

        syncSelectedSlot();
        sendSequencedPacket((sequence) -> new PlayerInteractBlockC2SPacket(hand, blockHit, sequence));
        sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        addPendingPlacement(blockHit.getBlockPos());
    }

    private static void addPendingPlacement(BlockPos pos) {
        cleanup();
        pendingPlacements.put(pos, System.currentTimeMillis());
    }

    public static void queueAttack(BlockPos pos) {
        cleanup();
        attackQueue.put(pos, System.currentTimeMillis());
    }

    private static Box crystalBox(BlockPos base) {
        BlockPos pos = base.up();
        return new Box(pos.getX() - 0.5, pos.getY(), pos.getZ() - 0.5, pos.getX() + 1.5, pos.getY() + 2.0, pos.getZ() + 1.5);
    }

    private static void cleanup() {
        long now = System.currentTimeMillis();
        long ping = 0;
        if (mc.getNetworkHandler() != null && mc.player != null) {
            var entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (entry != null) ping = entry.getLatency();
        }
        long ttl = Math.max(50, ping * 2L + ping * ping / 200L);
        pendingPlacements.values().removeIf(time -> now - time > ttl);
        attackQueue.values().removeIf(time -> now - time > ttl);
    }

    public static void onEntitySpawn(Entity entity) {
        if (!isCrystal(entity)) return;
        cleanup();
        if (attackQueue.isEmpty() || mc.player == null || mc.world == null) return;

        Entity camera = mc.getCameraEntity();
        if (camera == null) return;

        BlockPos pos = new BlockPos((int) (entity.getX() - 0.5), (int) (entity.getY()) - 1, (int) (entity.getZ() - 0.5));
        if (!attackQueue.containsKey(pos)) return;

        Vec3d camPos = camera.getCameraPosVec(0f);
        Vec3d lookVec = camera.getRotationVec(0f);
        Vec3d endPos = camPos.add(lookVec.multiply(mc.player.getEntityInteractionRange()));
        Box expandedBox = entity.getBoundingBox().expand(entity.getTargetingMargin());
        if (expandedBox.raycast(camPos, endPos).isEmpty() && !expandedBox.contains(camPos)) return;

        attackQueue.remove(pos);
        pendingPlacements.remove(pos);
        doServerAttack(entity);
    }

    public static void doServerAttack(Entity entity) {
        if (mc.player == null || mc.interactionManager == null) return;

        syncSelectedSlot();
        sendPacket(PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking()));
        sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
    }

    @Override
    public void onInitializeClient() {
        PayloadTypeRegistry.playS2C().register(DisableFastCrystalPayload.ID, DisableFastCrystalPayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(DisableFastCrystalPayload.ID, (payload, context) -> context.client().execute(() -> {
            serverDisabled = true;
            mc.inGameHud.getChatHud().addMessage(Text.literal("[FastCrystal] FastCrystal has been disabled on this server."));
        }));

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> serverDisabled = false);
    }
}
