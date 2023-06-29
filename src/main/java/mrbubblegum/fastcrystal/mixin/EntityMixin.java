package mrbubblegum.fastcrystal.mixin;

import mrbubblegum.fastcrystal.FastCrystalMod;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.item.EndCrystalItem;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

import static mrbubblegum.fastcrystal.FastCrystalMod.mc;

@Mixin(Entity.class)
public class EntityMixin {

    @Inject(at = @At("HEAD"), method = "tick")
    private void onTick(CallbackInfo ci) {
        if (mc.world == null | mc.player == null | !FastCrystalMod.fastCrystal.getValue()) {
            return;
        }

        if (FastCrystalMod.fastUse.getValue() && mc.player.getMainHandStack().isOf(Items.END_CRYSTAL) && Objects.equals(FastCrystalMod.getLookedAtBlock(), Blocks.OBSIDIAN) | Objects.equals(FastCrystalMod.getLookedAtBlock(), Blocks.BEDROCK))
            ((MinecraftClientInterface) mc).setItemUseCooldown(0);

        if (FastCrystalMod.fastAttack.getValue() && mc.player.getMainHandStack().getItem() instanceof EndCrystalItem crystalItem) {
            mc.player.getItemCooldownManager().set(crystalItem, 0);
            ((LivingEntityInterface) mc.player).setLastAttackedTicks(69);
            ((MinecraftClientInterface) mc).setAttackCooldown(0);
            if (((HeldItemRendererInterface) mc.gameRenderer.firstPersonRenderer).getPrevEquipProgressMainHand() >= 0.9) {
                ((HeldItemRendererInterface) mc.gameRenderer.firstPersonRenderer).setEquipProgressMainHand(1.0f);
                ((HeldItemRendererInterface) mc.gameRenderer.firstPersonRenderer).setMainhandStack(mc.player.getMainHandStack());
            }
        }

        mc.execute(() -> {
            if (!FastCrystalMod.fastCrystal.getValue() | !FastCrystalMod.instantPlace.getValue())
                return;

            BlockHitResult hit = FastCrystalMod.getLookedAtBlockHitResult();
            if (hit == null || mc.interactionManager == null || mc.currentScreen != null || !mc.options.useKey.isPressed() || !mc.player.getMainHandStack().isOf(Items.END_CRYSTAL) || !(Objects.equals(FastCrystalMod.getLookedAtBlock(), Blocks.OBSIDIAN) | Objects.equals(FastCrystalMod.getLookedAtBlock(), Blocks.BEDROCK)))
                return;

            BlockPos pos = hit.getBlockPos();
            if (pos == null || FastCrystalMod.isLookingAtOrCloseToCrystal(pos, mc.world) || !FastCrystalMod.canPlaceCrystal(pos))
                return;

            ActionResult interactBlock = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
            if (interactBlock.isAccepted() && interactBlock.shouldSwingHand())
                mc.player.swingHand(Hand.MAIN_HAND);
//        }
//
//        EndCrystalEntity crystal = FastCrystalMod.getLookedAtCrystal();
//
//        if (mc.interactionManager != null && crystal != null && mc.currentScreen == null && mc.options.attackKey.isPressed() && mc.player.getMainHandStack().isOf(Items.END_CRYSTAL) && FastCrystalMod.isEntityExisting(crystal))
//            mc.interactionManager.attackEntity(mc.player, crystal);
        });
    }
}