package me.mrbubbles.fastcrystal.mixin;

import me.mrbubbles.fastcrystal.FastCrystal;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.item.Items;
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
    @Nullable
    public Screen currentScreen;
    @Shadow
    private int itemUseCooldown;

    @Unique
    private boolean crystalHandled = false;

    @Unique
    private boolean useWasDown;

    @Unique
    private boolean attackWasDown;

    @Unique
    private boolean isHoldingCrystal() {
        return player != null && (player.getStackInHand(Hand.MAIN_HAND).isOf(Items.END_CRYSTAL) || player.getStackInHand(Hand.OFF_HAND).isOf(Items.END_CRYSTAL));
    }

    @Inject(at = @At("HEAD"), method = "doItemUse")
    private void doItemUse(CallbackInfo ci) {
        doFastPlace();
    }

    @Unique
    private void doFastPlace() {
        if (!FastCrystal.isEnabled() || interactionManager.isBreakingBlock() || player.isRiding() || !options.useKey.isPressed() || options.attackKey.isPressed())
            return;

        BlockHitResult blockHit = FastCrystal.getPlaceHit(FastCrystal.getLookedAtBlockHit());
        if (blockHit == null) return;

        BlockPos pos = blockHit.getBlockPos();
        for (Hand hand : HANDS) {
            if (!player.getStackInHand(hand).isItemEnabled(world.getEnabledFeatures())) continue;
            if (FastCrystal.canPlaceCrystal(pos, hand)) {
                FastCrystal.doServerInteractBlock(hand, blockHit);
                crystalHandled = true;
                return;
            }
        }
    }

    @Inject(at = @At("TAIL"), method = "doItemUse")
    private void doItemUseTail(CallbackInfo ci) {
        boolean holdingCrystal = isHoldingCrystal();
        if (crystalHandled || (FastCrystal.isEnabled() && holdingCrystal)) {
            crystalHandled = false;
            itemUseCooldown = 0;
        }
    }

    @Inject(at = @At("HEAD"), method = "doAttack")
    private void doAttack(CallbackInfoReturnable<Boolean> cir) {
        doFastBreak();
    }

    @Unique
    private void doFastBreak() {
        if (!FastCrystal.isEnabled() || player.isRiding() || !player.getStackInHand(Hand.MAIN_HAND).isItemEnabled(world.getEnabledFeatures()) || !FastCrystal.canBreakCrystal())
            return;

        Entity crystal = FastCrystal.getLookedAtCrystal();
        if (crystal != null) {
            crystalHandled = true;
            FastCrystal.FakeEndCrystalEntity fakeCrystal = FastCrystal.getFakeCrystal(crystal);
            if (fakeCrystal != null) {
                FastCrystal.attackFakeCrystal(fakeCrystal);
            } else {
                FastCrystal.doServerAttack(crystal);
                FastCrystal.predictExplosion(crystal);
                crystal.discard();
            }
            targetedEntity = null;
            crosshairTarget = player.raycast(player.getBlockInteractionRange(), 1.0F, false);
            return;
        }

        BlockPos predictedPos = FastCrystal.getPredictedHit();
        if (predictedPos != null) {
            crystalHandled = true;
            FastCrystal.queueAttack(predictedPos);
        }
    }

    @Inject(at = @At("TAIL"), method = "doAttack")
    private void doAttackTail(CallbackInfoReturnable<Boolean> cir) {
        if (crystalHandled || (FastCrystal.isEnabled() && isHoldingCrystal())) {
            crystalHandled = false;
            attackCooldown = 0;
        }
    }

    @Inject(at = @At("HEAD"), method = "render(Z)V")
    private void render(CallbackInfo ci) {
        boolean useDown = options.useKey.isPressed();
        boolean attackDown = options.attackKey.isPressed();

        if (currentScreen != null || player == null || world == null || interactionManager == null) {
            useWasDown = useDown;
            attackWasDown = attackDown;
            return;
        }

        if (useDown && !useWasDown) {
            doFastPlace();
        }

        if (attackDown && !attackWasDown) {
            doFastBreak();
        }

        useWasDown = useDown;
        attackWasDown = attackDown;
    }
}
