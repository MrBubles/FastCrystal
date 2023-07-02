package mrbubblegum.fastcrystal.mixin;

import mrbubblegum.fastcrystal.FastCrystalMod;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
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

import static mrbubblegum.fastcrystal.FastCrystalMod.limitPackets;
import static mrbubblegum.fastcrystal.FastCrystalMod.mc;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

//    @Shadow
//    protected int attackCooldown;
//
//    @Shadow
//    private int itemUseCooldown;

    @Inject(at = @At("HEAD"), method = "doItemUse", cancellable = true)
    private void onDoItemUse(CallbackInfo ci) {
        if (FastCrystalMod.fastCrystal.getValue() && mc.player != null) {
            ItemStack mainHand = mc.player.getMainHandStack();
            if (mainHand.isOf(Items.END_CRYSTAL))
                if (FastCrystalMod.hitCount != limitPackets())
                    ci.cancel();
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void tick(CallbackInfo info) {
        if (mc == null || mc.player == null | mc.world == null)
            return;

//        if (itemUseCooldown != FastCrystalMod.itemUseCooldown)
//            itemUseCooldown = FastCrystalMod.itemUseCooldown;
//        if (attackCooldown != FastCrystalMod.attackCooldown)
//            attackCooldown = FastCrystalMod.attackCooldown;
//
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
//    }
//
//    @Inject(method = "doItemUse", at = @At("HEAD"))
//    private void itemUse(CallbackInfo ci) {
//        if (mc == null || mc.player == null | mc.world == null)
//            return;
//
//        if (FastCrystalMod.fastUse.getValue() && mc.player.getMainHandStack().isOf(Items.END_CRYSTAL) && Objects.equals(FastCrystalMod.getLookedAtBlock(), Blocks.OBSIDIAN) | Objects.equals(FastCrystalMod.getLookedAtBlock(), Blocks.BEDROCK))
//            itemUseCooldown = 0;
//
//        if (FastCrystalMod.fastAttack.getValue() && mc.player.getMainHandStack().getItem() instanceof EndCrystalItem crystalItem) {
//            attackCooldown = 0;
//            mc.player.getItemCooldownManager().set(crystalItem, 0);
//            ((LivingEntityInterface) mc.player).setLastAttackedTicks(69);
//            if (((HeldItemRendererInterface) mc.gameRenderer.firstPersonRenderer).getPrevEquipProgressMainHand() >= 0.9) {
//                ((HeldItemRendererInterface) mc.gameRenderer.firstPersonRenderer).setEquipProgressMainHand(1.0f);
//                ((HeldItemRendererInterface) mc.gameRenderer.firstPersonRenderer).setMainhandStack(mc.player.getMainHandStack());
//            }
//        }
    }
}