package me.mrbubbles.fastcrystal;

import me.mrbubbles.fastcrystal.config.LoadConfig;
import me.mrbubbles.fastcrystal.config.SaveConfig;
import me.mrbubbles.fastcrystal.mixin.ClientPlayerInteractionManagerInterface;
import me.mrbubbles.fastcrystal.settings.BooleanSetting;
import me.mrbubbles.fastcrystal.settings.KeybindSetting;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Items;
import net.minecraft.item.ToolItem;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Path;

public class FastCrystal implements ClientModInitializer {

    public static final BooleanSetting fastCrystal = new BooleanSetting("FastCrystal", true, "The entire mod");
    public static final BooleanSetting removeCrystal = new BooleanSetting("RemoveCrystal", true, "Removes the crystal you hit (marlow's crystal optimizer)");
    public static final BooleanSetting fastAttack = new BooleanSetting("FastAttack", true, "Makes it so whenever you hold a crystal you attack like you're in 1.7");
    public static final BooleanSetting noPickupAnim = new BooleanSetting("NoPickupAnim", false, "Disables the pickup item packet");
    public static final BooleanSetting instantPlace = new BooleanSetting("InstantPlace", false, "Makes you instant place crystals");
    public static final KeybindSetting uiBind = new KeybindSetting("UIBind", GLFW.GLFW_KEY_BACKSLASH, "The FastCrystal ui bind");
    public static final MinecraftClient mc = MinecraftClient.getInstance();
    public static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("fastcrystal.json");
    public static boolean openedUI = false;

    public static BlockHitResult getLookedAtBlockHit() {
        if (mc.world == null || mc.player == null) return null;
        Vec3d camPos = mc.player.getEyePos();
        Vec3d rotationVec = mc.player.getRotationVecClient();
        return mc.world.raycast(new RaycastContext(camPos, camPos.add(rotationVec.multiply(4.5)), RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player));
    }

    public static boolean isCrystal(Entity entity) {
        if (mc.player == null || mc.world == null || entity == null || entity.isRemoved()) return false;

        if (mc.player.getMainHandStack().getItem() instanceof ToolItem || mc.player.getOffHandStack().getItem() instanceof ToolItem)
            return entity.getType().equals(EntityType.END_CRYSTAL);

        return entity.getType().equals(EntityType.END_CRYSTAL) || entity.getType().equals(EntityType.SLIME) || entity.getType().equals(EntityType.MAGMA_CUBE);
    }

    public static Entity getLookedAtCrystal() {
        if (fastCrystal.getValue() && mc.crosshairTarget instanceof EntityHitResult entityHit && isCrystal(entityHit.getEntity())) {
            return entityHit.getEntity();
        }
        return null;
    }

    public static boolean canPlaceCrystal(BlockPos pos, Hand hand) {
        if (mc.world == null || mc.player == null) return false;

        BlockPos crystalPos = pos.up();
        BlockState state = mc.world.getBlockState(pos);

        if (!mc.player.getStackInHand(hand).isOf(Items.END_CRYSTAL) || (!state.isOf(Blocks.OBSIDIAN) && !state.isOf(Blocks.BEDROCK)))
            return false;

        if (!mc.world.isAir(crystalPos))
            return false;

        return mc.world.getOtherEntities(null, new Box(crystalPos.getX(), crystalPos.getY(), crystalPos.getZ(), crystalPos.getX() + 1.0, crystalPos.getY() + 2.0, crystalPos.getZ() + 1.0)).isEmpty();
    }

    public static void sendPacket(Packet<?> packet) {
        if (mc.getNetworkHandler() == null || packet == null) return;
        mc.getNetworkHandler().sendPacket(packet);
    }

    public static void syncSelectedSlot() {
        if (mc.player == null || mc.getNetworkHandler() == null || mc.interactionManager == null) return;

        ClientPlayerInteractionManagerInterface interactionManager = ((ClientPlayerInteractionManagerInterface) mc.interactionManager);
        int i = mc.player.getInventory().selectedSlot;
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
            sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        } else {
            ActionResult result = mc.interactionManager.interactBlock(mc.player, hand, blockHit);
            if (result.isAccepted() && result.shouldSwingHand())
                mc.player.swingHand(hand);
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
        System.setProperty("java.awt.headless", "false");

        LoadConfig.loadConfig(CONFIG_FILE);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> SaveConfig.saveConfig(CONFIG_FILE)));
    }
}