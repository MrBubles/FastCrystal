package mrbubblegum.fastcrystal;

import mrbubblegum.fastcrystal.config.LoadConfig;
import mrbubblegum.fastcrystal.config.SaveConfig;
import mrbubblegum.fastcrystal.settings.BooleanSetting;
import mrbubblegum.fastcrystal.settings.KeybindSetting;
import mrbubblegum.fastcrystal.settings.Setting;
import mrbubblegum.fastcrystal.utils.RenderUtil;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.mob.MagmaCubeEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class FastCrystalMod implements ClientModInitializer {

    public static final BooleanSetting fastCrystal = new BooleanSetting("FastCrystal", true);
    public static final KeybindSetting guiBind = new KeybindSetting("UiBind", "backslash");
    public static final BooleanSetting removeCrystal = new BooleanSetting("RemoveCrystal", true);
    public static final BooleanSetting fastUse = new BooleanSetting("FastUse", true);
    public static final BooleanSetting fastAttack = new BooleanSetting("FastAttack", true);
    public static final BooleanSetting noPickupAnim = new BooleanSetting("NoPickupAnim", false);
    public static final BooleanSetting fastSwing = new BooleanSetting("FastSwing", false);
    public static final BooleanSetting openedGui = new BooleanSetting("OpenedGui", false, true);
    public static final List<Setting<?>> SETTINGS = Arrays.asList(fastCrystal, guiBind, removeCrystal, fastUse, fastAttack, /*moreCps,*/ noPickupAnim, openedGui);
    public static FastCrystalMod INSTANCE = new FastCrystalMod();
    public static MinecraftClient mc;
    public static int hitCount;
    public static int breakingBlockTick;

    public static void useOwnTicks() {
        if (mc.world == null | mc.player == null | mc.interactionManager == null) {
            return;
        }

        ItemStack mainHandStack = mc.player.getMainHandStack();

        if (mc.interactionManager.isBreakingBlock() && isLookingAt(Blocks.OBSIDIAN, Objects.requireNonNull(getLookedAtBlockHitResult()).getBlockPos()) | isLookingAt(Blocks.BEDROCK, Objects.requireNonNull(getLookedAtBlockHitResult()).getBlockPos())) {
            breakingBlockTick++;
        } else breakingBlockTick = 0;

        if (breakingBlockTick > 5)
            return;

        if (!mc.options.useKey.isPressed())
            hitCount = 0;

        if (hitCount == limitPackets())
            return;

        if (isLookingAtCrystal()) {
            if (mc.options.attackKey.isPressed())
                hitCount++;
        }
        if (!mainHandStack.isOf(Items.END_CRYSTAL)) {
            return;
        }
        if (mc.options.useKey.isPressed() && (isLookingAt(Blocks.OBSIDIAN, Objects.requireNonNull(getLookedAtBlockHitResult()).getBlockPos()) | isLookingAt(Blocks.BEDROCK, Objects.requireNonNull(getLookedAtBlockHitResult()).getBlockPos()))) {
            ActionResult result = sendInteractBlockPacket(Objects.requireNonNull(getLookedAtBlockHitResult()).getBlockPos(), Objects.requireNonNull(getLookedAtBlockHitResult()).getSide());
            if (canPlaceCrystal(Objects.requireNonNull(getLookedAtBlockHitResult()).getBlockPos()) && result.isAccepted() && result.shouldSwingHand())
                mc.player.swingHand(mc.player.getActiveHand());
        }
    }

//    public static void attack(Entity entity, boolean serverAttack) {
//        if (mc.getNetworkHandler() != null && mc.player != null && mc.interactionManager != null) {
//            if (serverAttack) {
//                ((ClientPlayerInteractionManagerInterface) mc.interactionManager).syncSelectedSlot();
//                mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking()));
//                mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
//            } else {
//                ((ClientPlayerInteractionManagerInterface) mc.interactionManager).syncSelectedSlot();
//                mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking()));
//                if (((ClientPlayerInteractionManagerInterface) mc.interactionManager).getGameMode() != GameMode.SPECTATOR) {
//                    mc.player.attack(entity);
//                    mc.player.resetLastAttackedTicks();
//                }
//            }
//            if (entity instanceof EndCrystalEntity | entity instanceof SlimeEntity | entity instanceof MagmaCubeEntity) {
//                entity.kill();
//                entity.remove(Entity.RemovalReason.KILLED);
//                entity.onRemoved();
//            }
//        }
//    }

    public static BlockState getBlockState(BlockPos pos) {
        if (mc.world != null) {
            return mc.world.getBlockState(pos);
        }
        return null;
    }

    public static boolean isLookingAt(Block block, BlockPos pos) {
        return Objects.requireNonNull(getBlockState(pos)).getBlock() == block;
    }

    public static BlockHitResult getLookedAtBlockHitResult() {
        if (mc.world != null && mc.player != null) {
            Vec3d camPos = mc.player.getEyePos();
            Vec3d clientLookVec = getLookVec();
            if (clientLookVec != null)
                return mc.world.raycast(new RaycastContext(camPos, camPos.add(clientLookVec.multiply(4.5)), RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player));
        }
        return null;
    }

    public static boolean isCrystal(Entity entity) {
        return entity instanceof EndCrystalEntity | entity instanceof MagmaCubeEntity | entity instanceof SlimeEntity;
    }

    public static Vec3d getLookVec() {
        if (mc.player != null) {
            float f = (float) Math.PI / 180;
            float pi = (float) Math.PI;
            float f1 = MathHelper.cos(-mc.player.getYaw() * f - pi);
            float f2 = MathHelper.sin(-mc.player.getYaw() * f - pi);
            float f3 = -MathHelper.cos(-mc.player.getPitch() * f);
            float f4 = MathHelper.sin(-mc.player.getPitch() * f);
            return new Vec3d(f2 * f3, f4, f1 * f3).normalize();
        }
        return null;
    }

    public static ActionResult sendInteractBlockPacket(BlockPos pos, Direction dir) {
        Vec3d vec = new Vec3d(pos.getX(), pos.getY(), pos.getZ());
        return setPacket(vec, dir);
    }

    public static ActionResult setPacket(Vec3d vec3d, Direction dir) {
        if (mc.world != null && mc.player != null && mc.interactionManager != null) {
            Vec3i vec3i = new Vec3i((int) vec3d.x, (int) vec3d.y, (int) vec3d.z);
            BlockPos pos = new BlockPos(vec3i);
            BlockHitResult result = new BlockHitResult(vec3d, dir, pos, false);
            return mc.interactionManager.interactBlock(mc.player, mc.player.getActiveHand(), result);
        }
        return null;
    }

    public static int limitPackets() {
        int stop = 2;
        if (getPing() > 50) stop = 2;
        if (getPing() < 50) stop = 1;
        return stop;
    }

    public static int getPing() {
        if (mc.getNetworkHandler() == null | mc.player == null) return 0;

        PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        if (playerListEntry == null) return 0;
        return playerListEntry.getLatency();
    }

    public static boolean canPlaceCrystal(BlockPos block) {
        if (mc.world != null) {
            BlockState blockState = mc.world.getBlockState(block);
            if (!blockState.isOf(Blocks.OBSIDIAN) && !blockState.isOf(Blocks.BEDROCK))
                return false;
            BlockPos blockPos2 = block.up();
            if (!mc.world.isAir(blockPos2))
                return false;
            double d = blockPos2.getX();
            double e = blockPos2.getY();
            double f = blockPos2.getZ();
            List<Entity> list = mc.world.getOtherEntities(null, new Box(d, e, f, d + 1.0D, e + 2.0D, f + 1.0D));
            return list.isEmpty();
        }
        return false;
    }

    public static Block getCurrentBlock() {
        BlockHitResult hit = getLookedAtBlockHitResult();
        if (mc.world != null && mc.player != null && hit != null) {
            Block block = mc.world.getBlockState(hit.getBlockPos()).getBlock();
            if (block != Blocks.AIR) {
                return block;
            }
        }
        return null;
    }

    public static boolean isCloseToCrystal(BlockPos blockPos, World world) {
        List<EndCrystalEntity> list = world.getEntitiesByType(EntityType.END_CRYSTAL, new Box(blockPos.getX(), blockPos.getY(), blockPos.getZ(), blockPos.getX() + 1, blockPos.getY() + 2, blockPos.getZ() + 1), e -> !e.isRemoved() && RenderUtil.isEntityRendered(e));
        return !list.isEmpty();
    }

    public static boolean isLookingAtCrystal() {
        return mc.crosshairTarget instanceof EntityHitResult result && result.getEntity() instanceof EndCrystalEntity crystal && !crystal.isRemoved() && RenderUtil.isEntityRendered(crystal);
    }

    public static boolean isLookingAtOrCloseToCrystal(BlockPos blockPos, World world) {
        return isLookingAtCrystal() | isCloseToCrystal(blockPos, world);
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

    public boolean isModLoaded(String modId, String modName) {
        for (ModContainer modContainer : FabricLoader.getInstance().getAllMods()) {
            ModMetadata metadata = modContainer.getMetadata();
            String id = metadata.getId();
            String name = metadata.getName();
            if (id.equalsIgnoreCase(modId) | name.equalsIgnoreCase(modName))
                return true;
        }
        return false;
    }

    @Override
    public void onInitializeClient() {
        if (isModLoaded("walksycrystaloptimizer", "Walksy Optimizer"))
            displayMessage("WalksyCrystalOptimizer is not needed for FastCrystal, please disable it", "Warning!");
        if (isModLoaded("marlows-crystal-optimizer", "Marlow's Crystal Optimizer"))
            displayMessage("MarlowCrystalOptimizer is not needed for FastCrystal, please disable it", "Warning!");
        if (isModLoaded("crystaloptimizer", "CrystalOptimizer"))
            displayMessage("CrystalOptimizer is not needed for FastCrystal, please disable it", "Warning!");

        mc = MinecraftClient.getInstance();
        new LoadConfig();
        new SaveConfig();
        Runtime.getRuntime().addShutdownHook(new Thread(SaveConfig::saveAllSettings));

        if (!openedGui.getValue())
            displayMessage("The fastcrystal gui bind is " + guiBind.getStringValue(), "");
    }
}