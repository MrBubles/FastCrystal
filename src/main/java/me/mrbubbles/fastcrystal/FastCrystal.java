package me.mrbubbles.fastcrystal;

import com.google.common.util.concurrent.AtomicDouble;
import me.mrbubbles.fastcrystal.mixin.ClientPlayerInteractionManagerInterface;
import me.mrbubbles.fastcrystal.mixin.PlayerInventoryInterface;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public class FastCrystal implements ClientModInitializer {

    public static final MinecraftClient mc = MinecraftClient.getInstance();

    private static boolean serverDisabled = false;

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
        if (mc.player == null || mc.world == null || entity == null || entity.isRemoved()) return false;

        if (mc.player.getMainHandStack().getComponents().contains(DataComponentTypes.TOOL) || mc.player.getOffHandStack().getComponents().contains(DataComponentTypes.TOOL))
            return entity.getType().equals(EntityType.END_CRYSTAL);

        return entity.getType().equals(EntityType.END_CRYSTAL) || entity.getType().equals(EntityType.SLIME) || entity.getType().equals(EntityType.MAGMA_CUBE);
    }

    public static EntityHitResult getLookedAtEntityHit() {
        if (mc.world == null || mc.player == null) return null;

        Entity camera = mc.getCameraEntity();
        if (camera == null) return null;

        double range = mc.player.getEntityInteractionRange();

        Vec3d camPos = camera.getCameraPosVec(0f);
        Vec3d lookVec = camera.getRotationVec(0f);
        Vec3d endPos = camPos.add(lookVec.multiply(range));

        Box box = camera.getBoundingBox().stretch(lookVec.multiply(range)).expand(1.0, 1.0, 1.0);

        return ProjectileUtil.raycast(camera, camPos, endPos, box, EntityPredicates.EXCEPT_SPECTATOR.and(Entity::canHit), range * range);
    }

    public static Entity getLookedAtCrystal() {
        EntityHitResult entityHit = getLookedAtEntityHit();
        if (entityHit != null && isCrystal(entityHit.getEntity())) return entityHit.getEntity();
        return null;
    }

    public static boolean canPlaceCrystal(BlockPos pos, Hand hand) {
        if (mc.world == null || mc.player == null) return false;

        BlockPos crystalPos = pos.up();
        BlockState state = mc.world.getBlockState(pos);

        if (!mc.player.getStackInHand(hand).isOf(Items.END_CRYSTAL) || (!state.isOf(Blocks.OBSIDIAN) && !state.isOf(Blocks.BEDROCK)))
            return false;

        if (!mc.world.isAir(crystalPos)) return false;

        return mc.world.getOtherEntities(null, new Box(crystalPos.getX(), crystalPos.getY(), crystalPos.getZ(), crystalPos.getX() + 1.0, crystalPos.getY() + 2.0, crystalPos.getZ() + 1.0)).isEmpty();
    }

    public static boolean canBreakCrystal() {
        StatusEffectInstance weakness = mc.player.getStatusEffect(StatusEffects.WEAKNESS);
        if (weakness == null) return true;

        if (mc.player.getAttributeValue(EntityAttributes.ATTACK_DAMAGE) > 4.0 * (weakness.getAmplifier() + 1.0) + 5.0)
            return true;

        return calculateDamage() > 0.0;
    }

    private static double calculateDamage() {
        StatusEffectInstance strength = mc.player.getStatusEffect(StatusEffects.STRENGTH);
        double strengthBonus = (strength != null) ? 3.0 * (strength.getAmplifier() + 1) : 0.0;

        StatusEffectInstance weakness = mc.player.getStatusEffect(StatusEffects.WEAKNESS);
        double weaknessPenalty = (weakness != null) ? 4.0 * (weakness.getAmplifier() + 1) : 0.0;

        return Math.max(0.0, mc.player.getAttributeValue(EntityAttributes.ATTACK_DAMAGE) + getWeaponDamage(mc.player.getMainHandStack()) + strengthBonus - weaknessPenalty);
    }

    private static double getWeaponDamage(ItemStack item) {
        if (item.isEmpty()) return 0.0D;

        AtomicDouble damage = new AtomicDouble();
        item.applyAttributeModifier(AttributeModifierSlot.MAINHAND, (attribute, modifier) -> {
            if (attribute.equals(EntityAttributes.ATTACK_DAMAGE)) damage.addAndGet(modifier.value());
        });
        return damage.get();
    }

    public static void sendPacket(Packet<?> packet) {
        if (mc.getNetworkHandler() == null || packet == null) return;
        mc.getNetworkHandler().sendPacket(packet);
    }

    public static void syncSelectedSlot() {
        if (mc.player == null || mc.getNetworkHandler() == null || mc.interactionManager == null) return;

        ClientPlayerInteractionManagerInterface interactionManager = ((ClientPlayerInteractionManagerInterface) mc.interactionManager);
        int i = ((PlayerInventoryInterface) mc.player.getInventory()).getSelectedSlot();
        if (i != interactionManager.getLastSelectedSlot()) {
            interactionManager.setLastSelectedSlot(i);
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(interactionManager.getLastSelectedSlot()));
        }
    }

    public static void doInteractBlock(Hand hand, BlockHitResult blockHit, boolean serverSided) {
        if (mc.player == null || mc.interactionManager == null) return;

        if (serverSided) {
            syncSelectedSlot();
            sendPacket(new PlayerInteractBlockC2SPacket(hand, blockHit, 0));
            mc.player.swingHand(hand);
        } else {
            ActionResult result = mc.interactionManager.interactBlock(mc.player, hand, blockHit);
            if (result.isAccepted()) mc.player.swingHand(hand);
        }
    }

    public static void doAttack(Entity entity, boolean serverSided) {
        if (mc.player == null || mc.interactionManager == null) return;

        if (serverSided) {
            syncSelectedSlot();
            sendPacket(PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking()));
            sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        } else {
            mc.interactionManager.attackEntity(mc.player, entity);
            mc.player.swingHand(Hand.MAIN_HAND);
        }
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