package me.mrbubbles.fastcrystal.mixin;

import me.mrbubbles.fastcrystal.FastCrystal;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.entity.Entity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @Shadow
    @Final
    public GameOptions options;

    @Inject(at = @At("HEAD"), method = "doItemUse")
    private void itemUse(CallbackInfo ci) {
        if (!FastCrystal.fastCrystal.getValue() || !FastCrystal.instantPlace.getValue()) return;

        for (Hand hand : Hand.values()) {
            BlockHitResult blockHit = FastCrystal.getLookedAtBlockHit();
            if (blockHit != null && FastCrystal.canPlaceCrystal(blockHit.getBlockPos(), hand) && options.useKey.isPressed() && !options.attackKey.isPressed()) {
                FastCrystal.doInteractBlock(hand, blockHit, true);
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "doAttack")
    private void attack(CallbackInfoReturnable<Boolean> cir) {
        Entity crystal = FastCrystal.getLookedAtCrystal();
        if (crystal == null || !FastCrystal.removeCrystal.getValue()) return;

        FastCrystal.doAttack(crystal, true);

        crystal.discard();
    }
}