package me.mrbubbles.fastcrystal.mixin;

import me.mrbubbles.fastcrystal.FastCrystal;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @Unique
    private static final Hand[] HANDS = Hand.values();
    @Shadow
    @Final
    public GameOptions options;
    @Shadow
    @Nullable
    public ClientPlayerEntity player;
    @Shadow
    @Nullable
    public ClientWorld world;
    @Shadow
    @Nullable
    public ClientPlayerInteractionManager interactionManager;
    @Shadow
    public int attackCooldown;
    @Shadow
    @Nullable
    public HitResult crosshairTarget;
    @Shadow
    @Nullable
    public Entity targetedEntity;
    @Shadow
    private int itemUseCooldown;

    @Inject(at = @At("HEAD"), method = "doItemUse")
    private void doItemUse(CallbackInfo ci) {
        if (!FastCrystal.isEnabled() || interactionManager.isBreakingBlock() || player.isRiding()) return;

        BlockHitResult blockHit = FastCrystal.getLookedAtBlockHit();
        if (blockHit == null) return;

        BlockPos pos = blockHit.getBlockPos();
        for (Hand hand : HANDS) {
            if (!player.getStackInHand(hand).isItemEnabled(world.getEnabledFeatures())) continue;
            if (options.useKey.isPressed() && !options.attackKey.isPressed() && FastCrystal.canPlaceCrystal(pos, hand)) {
                FastCrystal.doServerInteractBlock(hand, blockHit);
                itemUseCooldown = 0;
                return;
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "doAttack")
    private void doAttack(CallbackInfoReturnable<Boolean> cir) {
        if (!FastCrystal.isEnabled() || player.isRiding() || !player.getStackInHand(Hand.MAIN_HAND).isItemEnabled(world.getEnabledFeatures()) || !FastCrystal.canBreakCrystal())
            return;

        Entity crystal = FastCrystal.getLookedAtCrystal();
        if (crystal != null) {
            FastCrystal.GhostCrystalEntity ghost = FastCrystal.getGhost(crystal);
            if (ghost != null) {
                FastCrystal.attackGhost(ghost);
            } else {
                FastCrystal.doServerAttack(crystal);
                FastCrystal.predictExplosion(crystal);
                crystal.discard();
            }
            targetedEntity = null;
            crosshairTarget = player.raycast(player.getBlockInteractionRange(), 1.0F, false);
            attackCooldown = 0;
            return;
        }

        BlockPos predictedPos = FastCrystal.getPredictedHit();
        if (predictedPos != null) {
            attackCooldown = 0;
            FastCrystal.queueAttack(predictedPos);
        }
    }
}
