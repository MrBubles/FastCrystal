package me.mrbubbles.fastcrystal.mixin;

import me.mrbubbles.fastcrystal.FastCrystal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Shadow
    @Final
    public Options options;
    @Shadow
    @Nullable
    public LocalPlayer player;
    @Shadow
    @Nullable
    public MultiPlayerGameMode gameMode;
    @Shadow
    public int missTime;
    @Shadow
    @Nullable
    public HitResult crosshairTarget;
    @Shadow
    @Nullable
    public Entity targetedEntity;
    @Shadow
    private int rightClickDelay;

    @Inject(at = @At("HEAD"), method = "startUseItem")
    private void onStartUseItem(CallbackInfo ci) {
        if (!FastCrystal.isEnabled() || gameMode.isDestroying() || player.isPassenger()) return;

        for (InteractionHand hand : InteractionHand.values()) {
            if (!player.getItemInHand(hand).isEmpty()) {
                BlockHitResult hitResult = FastCrystal.getLookedAtBlockHit();
                if (hitResult != null && FastCrystal.canPlaceCrystal(hitResult.getBlockPos(), hand) && options.keyUse.isDown() && !options.keyAttack.isDown()) {
                    FastCrystal.doInteractBlock(hand, hitResult, true);
                    rightClickDelay = 0;
                }
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "startAttack")
    private void onStartAttack(CallbackInfoReturnable<Boolean> cir) {
        if (!FastCrystal.isEnabled() || player.isPassenger() || player.getMainHandItem().isEmpty())
            return;

        Entity crystal = FastCrystal.getLookedAtCrystal();
        if (crystal == null) return;


        if (FastCrystal.canBreakCrystal()) {
            FastCrystal.doServerAttack(crystal);
            crystal.discard();
            targetedEntity = null;
            crosshairTarget = player.raycast(player.getBlockInteractionRange(), 1.0F, false);

            missTime = 0;
        }
    }
}
