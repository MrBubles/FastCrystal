package me.mrbubbles.fastcrystal.mixin;

import me.mrbubbles.fastcrystal.FastCrystal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Unique
    private static final InteractionHand[] HANDS = InteractionHand.values();
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
    @org.jspecify.annotations.Nullable
    public Entity crosshairPickEntity;
    @Shadow
    @org.jspecify.annotations.Nullable
    public HitResult hitResult;
    @Shadow
    private int rightClickDelay;

    @Inject(at = @At("HEAD"), method = "startUseItem")
    private void onStartUseItem(CallbackInfo ci) {
        if (!FastCrystal.isEnabled() || gameMode.isDestroying() || player.isPassenger()) return;

        BlockHitResult blockHit = FastCrystal.getLookedAtBlockHit();
        if (blockHit == null) return;

        for (InteractionHand hand : HANDS) {
            if (!player.getItemInHand(hand).isItemEnabled(player.level().enabledFeatures())) continue;
            if (options.keyUse.isDown() && !options.keyAttack.isDown() && FastCrystal.canPlaceCrystal(blockHit.getBlockPos(), hand)) {
                FastCrystal.doServerInteractBlock(hand, blockHit);
                rightClickDelay = 0;
                return;
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "startAttack")
    private void onStartAttack(CallbackInfoReturnable<Boolean> cir) {
        if (!FastCrystal.isEnabled() || player.isPassenger() || player.getMainHandItem().isEmpty())
            return;

        Entity crystal = FastCrystal.getLookedAtCrystal();
        if (crystal != null) {
            if (FastCrystal.canBreakCrystal()) {
                FastCrystal.doServerAttack(crystal);
                crystal.discard();
                crosshairPickEntity = null;
                hitResult = player.pick(player.blockInteractionRange(), 1.0F, false);
                missTime = 0;
            }
            return;
        }

        BlockPos predicted = FastCrystal.getPredictedHit();
        if (predicted != null && FastCrystal.canBreakCrystal()) {
            FastCrystal.queueAttack(predicted);
            missTime = 0;
        }
    }
}
