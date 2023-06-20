package mrbubblegum.fastcrystal.mixin;

import mrbubblegum.fastcrystal.FastCrystalMod;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.dragon.EnderDragonFight;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.EndCrystalItem;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.List;

import static mrbubblegum.fastcrystal.FastCrystalMod.mc;

@Mixin({EndCrystalItem.class})
public class EndCrystalItemMixin {

    /**
     * @author MrBubblegum
     * @reason why not
     */
    @Overwrite
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos blockPos = context.getBlockPos();
        BlockState blockState = world.getBlockState(blockPos);
        if (!blockState.isOf(Blocks.OBSIDIAN) && !blockState.isOf(Blocks.BEDROCK)) {
            return ActionResult.FAIL;
        } else {
            BlockPos blockPos2 = blockPos.up();
            if (!world.isAir(blockPos2)) {
                return ActionResult.FAIL;
            } else {
                double d = blockPos2.getX();
                double e = blockPos2.getY();
                double f = blockPos2.getZ();
                List<Entity> list = world.getOtherEntities(null, new Box(d, e, f, d + 1.0, e + 2.0, f + 1.0));
                if (!list.isEmpty()) {
                    return ActionResult.FAIL;
                } else {
                    if (world instanceof ServerWorld) {
                        EndCrystalEntity endCrystalEntity = new EndCrystalEntity(world, d + 0.5, e, f + 0.5);
                        endCrystalEntity.setShowBottom(false);
                        world.spawnEntity(endCrystalEntity);
                        world.emitGameEvent(context.getPlayer(), GameEvent.ENTITY_PLACE, blockPos2);
                        EnderDragonFight enderDragonFight = ((ServerWorld) world).getEnderDragonFight();
                        if (enderDragonFight != null) {
                            enderDragonFight.respawnDragon();
                        }
                    }

                    decrement(context);
                    return ActionResult.success(world.isClient);
                }
            }
        }
    }

    public void decrement(ItemUsageContext context) {
        mc.execute(() -> {
            while (!context.getStack().isEmpty() && FastCrystalMod.isLookingAtOrCloseToCrystal(context.getBlockPos(), context.getWorld()))
                context.getStack().decrement(1);
//    }

//    private BlockState getBlockState(BlockPos pos) {
//        if (mc.world != null) {
//            return mc.world.getBlockState(pos);
//        }
//        return null;
//    }

//    private boolean isLookingAt(Block block, BlockPos pos) {
//        return Objects.requireNonNull(getBlockState(pos)).getBlock() == block;
//    }
//
//    private BlockHitResult generalLookPos() {
//        if (mc.world != null && mc.player != null) {
//            Vec3d camPos = mc.player.getEyePos();
//            Vec3d clientLookVec = lookVec();
//            if (clientLookVec != null) {
//                return mc.world.raycast(new RaycastContext(camPos, camPos.add(clientLookVec.multiply(4.5)), RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player));
//            }
//        }
//        return null;
//    }

//    private Vec3d lookVec() {
//        if (mc.player != null) {
//            float f = (float) Math.PI / 180;
//            float pi = (float) Math.PI;
//            float f1 = MathHelper.cos(-mc.player.getYaw() * f - pi);
//            float f2 = MathHelper.sin(-mc.player.getYaw() * f - pi);
//            float f3 = -MathHelper.cos(-mc.player.getPitch() * f);
//            float f4 = MathHelper.sin(-mc.player.getPitch() * f);
//            return new Vec3d(f2 * f3, f4, f1 * f3).normalize();
//        }
//        return null;
//    }

//    private boolean canPlaceCrystalServer(BlockPos block) {
//        if (mc.world != null) {
//            BlockState blockState = mc.world.getBlockState(block);
//            if (!blockState.isOf(Blocks.OBSIDIAN) && !blockState.isOf(Blocks.BEDROCK))
//                return false;
//            BlockPos blockPos2 = block.up();
//            if (!mc.world.isAir(blockPos2))
//                return false;
//            double d = blockPos2.getX();
//            double e = blockPos2.getY();
//            double f = blockPos2.getZ();
//            List<Entity> list = mc.world.getOtherEntities(null, new Box(d, e, f, d + 1.0D, e + 2.0D, f + 1.0D));
//            return list.isEmpty();
//        }
//        return false;
        });
    }
}