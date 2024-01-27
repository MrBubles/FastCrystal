package me.mrbubbles.fastcrystal;

import me.mrbubbles.fastcrystal.config.LoadConfig;
import me.mrbubbles.fastcrystal.config.SaveConfig;
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
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.lwjgl.glfw.GLFW;

import javax.swing.*;
import java.awt.*;
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

    public static Entity getLookedAtCrystal() {
        if (fastCrystal.getValue() && mc.crosshairTarget instanceof EntityHitResult entity &&
                entity.getEntity().getType().equals(EntityType.END_CRYSTAL)
                        | entity.getEntity().getType().equals(EntityType.SLIME)
                        | entity.getEntity().getType().equals(EntityType.MAGMA_CUBE)) {
            return entity.getEntity();
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

    public static void displayMessage(String message, String title) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setAlwaysOnTop(true);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel messageLabel = new JLabel(message);
        panel.add(messageLabel, BorderLayout.CENTER);

        JOptionPane.showMessageDialog(frame, panel, title, JOptionPane.PLAIN_MESSAGE);
        frame.dispose();
    }

    @Override
    public void onInitializeClient() {
        System.setProperty("java.awt.headless", "false"); // I don't know why i added this

        LoadConfig.loadConfig(CONFIG_FILE);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> SaveConfig.saveConfig(CONFIG_FILE)));
    }
}