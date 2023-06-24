package mrbubblegum.fastcrystal.mixin;

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
        BlockPos pos = context.getBlockPos();
        BlockPos crystalPos = pos.up();
        BlockState state = world.getBlockState(pos);

        if (!state.isOf(Blocks.OBSIDIAN) && !state.isOf(Blocks.BEDROCK))
            return ActionResult.FAIL;

        if (!world.isAir(crystalPos))
            return ActionResult.FAIL;

        List<Entity> list = world.getOtherEntities(null, new Box(crystalPos.getX(), crystalPos.getY(), crystalPos.getZ(), crystalPos.getX() + 1.0, crystalPos.getY() + 2.0, crystalPos.getZ() + 1.0));
        if (!list.isEmpty())
            return ActionResult.FAIL;

        if (world instanceof ServerWorld serverWorld && mc.isIntegratedServerRunning()) {
            EndCrystalEntity endCrystalEntity = new EndCrystalEntity(world, crystalPos.getX() + 0.5, crystalPos.getY(), crystalPos.getZ() + 0.5);
            endCrystalEntity.setShowBottom(false);
            world.spawnEntity(endCrystalEntity);
            world.emitGameEvent(context.getPlayer(), GameEvent.ENTITY_PLACE, crystalPos);
            EnderDragonFight enderDragonFight = serverWorld.getEnderDragonFight();
            if (enderDragonFight != null)
                enderDragonFight.respawnDragon();
            context.getStack().decrement(1);
        }
        return ActionResult.success(world.isClient);
    }
}