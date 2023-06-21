package mrbubblegum.fastcrystal.mixin;

import mrbubblegum.fastcrystal.FastCrystalMod;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.dragon.EnderDragonFight;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.EndCrystalItem;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
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
        BlockPos pos = context.getBlockPos();
        BlockPos crystalPos = pos.up();
        double x = crystalPos.getX();
        double y = crystalPos.getY();
        double z = crystalPos.getZ();
        World world = context.getWorld();
        BlockState blockState = world.getBlockState(pos);

        if (!mc.isIntegratedServerRunning())
            return ActionResult.FAIL;

        if (!blockState.isOf(Blocks.OBSIDIAN) && !blockState.isOf(Blocks.BEDROCK))
            return ActionResult.FAIL;

        if (!world.isAir(crystalPos))
            return ActionResult.FAIL;

        List<Entity> list = world.getOtherEntities(null, new Box(x, y, z, x + 1.0, y + 2.0, z + 1.0));
        if (!list.isEmpty())
            return ActionResult.FAIL;

        if (world instanceof ServerWorld serverWorld) {
            EndCrystalEntity endCrystalEntity = new EndCrystalEntity(world, x + 0.5, y, z + 0.5);
            endCrystalEntity.setShowBottom(false);
            world.spawnEntity(endCrystalEntity);
            world.emitGameEvent(context.getPlayer(), GameEvent.ENTITY_PLACE, crystalPos);
            EnderDragonFight enderDragonFight = serverWorld.getEnderDragonFight();
            if (enderDragonFight != null)
                enderDragonFight.respawnDragon();
        }
        decrement(context, 1);
        return ActionResult.success(world.isClient);
    }

    public void decrement(ItemUsageContext context, int amount) {
        if (context.getStack().getItem().equals(Items.END_CRYSTAL) && FastCrystalMod.isLookingAtOrCloseToCrystal(context.getBlockPos(), context.getWorld()))
            context.getStack().decrement(amount);
    }
}