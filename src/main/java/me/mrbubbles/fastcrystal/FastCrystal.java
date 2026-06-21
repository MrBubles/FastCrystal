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
import net.minecraft.world.InteractionResult;
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

public class FastCrystal implements ClientModInitializer {

    public static final Minecraft mc = Minecraft.getInstance();

    private static boolean serverDisabled = false;

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
        if (mc.player == null || mc.level == null || entity == null || entity.isRemoved()) return false;
        System.out.println(entity.getType().toShortString());

        BlockPos belowPos = BlockPos.containing(entity.position().add(-0.5, -1.0, -0.5));

        BlockState blockState = mc.level.getBlockState(belowPos);

        if (!blockState.is(Blocks.OBSIDIAN) && !blockState.is(Blocks.BEDROCK))
            return entity.getType().toShortString().equals("end_crystal");

        if (mc.player.getMainHandItem().getComponents().has(DataComponents.TOOL) || mc.player.getOffhandItem().getComponents().has(DataComponents.TOOL))
            return entity.getType().toShortString().equals("end_crystal");

        return entity.getType().toShortString().equals("end_crystal") || entity.getType().toShortString().equals("slime") || entity.getType().toShortString().equals("magma_cube");
    }

    public static EntityHitResult getLookedAtEntityHit() {
        if (mc.level == null || mc.player == null) return null;

        Entity camera = mc.getCameraEntity();
        if (camera == null) return null;

        double range = mc.player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);

        Vec3 eyePos = camera.getEyePosition(0f);
        Vec3 viewVec = camera.getViewVector(0f);
        Vec3 reachEnd = eyePos.add(viewVec.scale(range));

        AABB searchBox = camera.getBoundingBox().expandTowards(viewVec.scale(range)).inflate(1.0, 1.0, 1.0);

        return ProjectileUtil.getEntityHitResult(camera, eyePos, reachEnd, searchBox, entity -> !entity.isSpectator() && entity.isPickable(), range * range);
    }

    public static Entity getLookedAtCrystal() {
        EntityHitResult hitResult = getLookedAtEntityHit();
        if (hitResult != null && isCrystal(hitResult.getEntity())) return hitResult.getEntity();
        return null;
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
        double attackDamage = mc.player.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
        attackDamage += getWeaponDamage(mc.player.getMainHandItem());

        MobEffectInstance strengthEffect = mc.player.getEffect(MobEffects.STRENGTH);
        if (strengthEffect != null) attackDamage += 3.0 * (strengthEffect.getAmplifier() + 1);

        MobEffectInstance weaknessEffect = mc.player.getEffect(MobEffects.WEAKNESS);
        if (weaknessEffect != null) attackDamage -= 4.0 * (weaknessEffect.getAmplifier() + 1);

        return attackDamage > 0.0;
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

    public static void doInteractBlock(InteractionHand hand, BlockHitResult hitResult, boolean serverSide) {
        if (mc.player == null || mc.gameMode == null) return;

        if (serverSide) {
            ensureHasSentCarriedItem();
            sendPacket(new ServerboundUseItemOnPacket(hand, hitResult, 0));
            mc.player.swing(hand);
        } else {
            InteractionResult interactionResult = mc.gameMode.useItemOn(mc.player, hand, hitResult);
            if (interactionResult.consumesAction()) mc.player.swing(hand);
        }
    }

    public static void doServerAttack(Entity entity) {
        if (mc.player == null || mc.gameMode == null) return;

        ensureHasSentCarriedItem();
        sendPacket(new ServerboundAttackPacket(entity.getId()));
        sendPacket(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
    }

    @Override
    public void onInitializeClient() {
        PayloadTypeRegistry.clientboundPlay().register(DisableFastCrystalPayload.ID, DisableFastCrystalPayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(DisableFastCrystalPayload.ID, (_, context) -> context.client().execute(() -> {
            serverDisabled = true;
            // 26.2 compatiblity
            SystemToast.add(mc.getToastManager(), SystemToast.SystemToastId.PERIODIC_NOTIFICATION, Component.literal("FastCrystal").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), Component.literal("FastCrystal has been disabled on this server.").withStyle(ChatFormatting.RED));
        }));

        ClientPlayConnectionEvents.DISCONNECT.register((_, _) -> serverDisabled = false);
    }
}
