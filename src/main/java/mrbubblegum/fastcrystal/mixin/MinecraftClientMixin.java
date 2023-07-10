package mrbubblegum.fastcrystal.mixin;

import mrbubblegum.fastcrystal.FastCrystalMod;
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
        mc.execute(() -> {
            if (mc == null || mc.player == null | mc.world == null)
                return;

//        if (itemUseCooldown != FastCrystalMod.itemUseCooldown)
//            itemUseCooldown = FastCrystalMod.itemUseCooldown;
//        if (attackCooldown != FastCrystalMod.attackCooldown)
//            attackCooldown = FastCrystalMod.attackCooldown;

            BlockHitResult hit = FastCrystalMod.getLookedAtBlockHitResult();
            BlockPos pos = Objects.requireNonNull(hit).getBlockPos();
            if (FastCrystalMod.fastCrystal.getValue() && FastCrystalMod.instantPlace.getValue() && mc.interactionManager != null && pos != null && mc.options.useKey.isPressed() && mc.currentScreen == null && FastCrystalMod.canPlaceCrystal(pos)) {
                ActionResult interactBlock = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
                if (interactBlock.isAccepted() && interactBlock.shouldSwingHand())
                    mc.player.swingHand(Hand.MAIN_HAND);
            }
        });
    }
}