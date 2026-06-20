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
import org.jetbrains.annotations.Nullable;
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
    private int itemUseCooldown;

    @Shadow
    @Nullable
    public HitResult crosshairTarget;

    @Shadow
    @Nullable
    public Entity targetedEntity;

    @Inject(at = @At("HEAD"), method = "doItemUse")
    private void itemUse(CallbackInfo ci) {
        if (!FastCrystal.isEnabled() || interactionManager.isBreakingBlock() || player.isRiding()) return;

        for (Hand hand : Hand.values()) {
            if (!player.getStackInHand(hand).isItemEnabled(world.getEnabledFeatures())) continue;
            BlockHitResult blockHit = FastCrystal.getLookedAtBlockHit();
            if (blockHit != null && FastCrystal.canPlaceCrystal(blockHit.getBlockPos(), hand) && options.useKey.isPressed() && !options.attackKey.isPressed()) {
                FastCrystal.doInteractBlock(hand, blockHit, true);
                itemUseCooldown = 0;
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "doAttack")
    private void attack(CallbackInfoReturnable<Boolean> cir) {
        if (!FastCrystal.isEnabled() || player.isRiding() || !player.getStackInHand(Hand.MAIN_HAND).isItemEnabled(world.getEnabledFeatures()))
            return;

        Entity crystal = FastCrystal.getLookedAtCrystal();
        if (crystal == null) return;


        if (FastCrystal.canBreakCrystal()) {
            FastCrystal.doServerAttack(crystal);
            crystal.discard();
            targetedEntity = null;
            crosshairTarget = player.raycast(player.getBlockInteractionRange(), 1.0F, false);

            attackCooldown = 0;
        }
    }
}