package me.mrbubbles.fastcrystal.mixin;

import me.mrbubbles.fastcrystal.FastCrystal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Items;
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
    @Nullable
    public Entity crosshairPickEntity;
    @Shadow
    @Nullable
    public HitResult hitResult;
    @Shadow
    private int rightClickDelay;

    @Unique
    private boolean crystalHandled = false;

    @Unique
    private boolean useWasDown;

    @Unique
    private boolean attackWasDown;

    @Unique
    private boolean isHoldingCrystal() {
        return player != null && (player.getMainHandItem().is(Items.END_CRYSTAL) || player.getOffhandItem().is(Items.END_CRYSTAL));
    }

    @Inject(at = @At("HEAD"), method = "startUseItem")
    private void onStartUseItem(CallbackInfo ci) {
        doFastPlace();
    }

    @Unique
    private void doFastPlace() {
        if (!FastCrystal.isEnabled() || gameMode.isDestroying() || player.isPassenger() || !options.keyUse.isDown() || options.keyAttack.isDown())
            return;

        BlockHitResult blockHit = FastCrystal.getPlaceHit(FastCrystal.getLookedAtBlockHit());
        if (blockHit == null) return;

        BlockPos pos = blockHit.getBlockPos();
        for (InteractionHand hand : HANDS) {
            if (!player.getItemInHand(hand).isItemEnabled(player.level().enabledFeatures())) continue;
            if (FastCrystal.canPlaceCrystal(pos, hand)) {
                FastCrystal.doServerInteractBlock(hand, blockHit);
                crystalHandled = true;
                return;
            }
        }
    }

    @Inject(at = @At("TAIL"), method = "startUseItem")
    private void onStartUseItemTail(CallbackInfo ci) {
        if (crystalHandled || (FastCrystal.isEnabled() && isHoldingCrystal())) {
            crystalHandled = false;
            rightClickDelay = 0;
        }
    }

    @Inject(at = @At("HEAD"), method = "startAttack")
    private void onStartAttack(CallbackInfoReturnable<Boolean> cir) {
        doFastBreak();
    }

    @Unique
    private void doFastBreak() {
        if (!FastCrystal.isEnabled() || player.isPassenger() || !player.getMainHandItem().isItemEnabled(player.level().enabledFeatures()) || !FastCrystal.canBreakCrystal())
            return;

        Entity crystal = FastCrystal.getLookedAtCrystal();
        if (crystal != null) {
            crystalHandled = true;
            FastCrystal.FakeEndCrystalEntity fake = FastCrystal.getFakeCrystal(crystal);
            if (fake != null) {
                FastCrystal.attackFakeCrystal(fake);
            } else {
                FastCrystal.doServerAttack(crystal);
                FastCrystal.predictExplosion(crystal);
                crystal.discard();
            }
            crosshairPickEntity = null;
            hitResult = player.pick(player.blockInteractionRange(), 1.0F, false);
            return;
        }

        BlockPos predicted = FastCrystal.getPredictedHit();
        if (predicted != null) {
            crystalHandled = true;
            FastCrystal.queueAttack(predicted);
        }
    }

    @Inject(at = @At("TAIL"), method = "startAttack")
    private void onStartAttackTail(CallbackInfoReturnable<Boolean> cir) {
        if (crystalHandled || (FastCrystal.isEnabled() && isHoldingCrystal())) {
            crystalHandled = false;
            missTime = 0;
        }
    }

    @Inject(at = @At("HEAD"), method = "renderFrame(Z)V")
    private void onRenderFrame(CallbackInfo ci) {
        boolean useDown = options.keyUse.isDown();
        boolean attackDown = options.keyAttack.isDown();

        if (!FastCrystal.mc.mouseHandler.isMouseGrabbed() || player == null || gameMode == null) {
            useWasDown = useDown;
            attackWasDown = attackDown;
            return;
        }

        if (useDown && !useWasDown)
            doFastPlace();

        if (attackDown && !attackWasDown)
            doFastBreak();

        useWasDown = useDown;
        attackWasDown = attackDown;
    }
}
