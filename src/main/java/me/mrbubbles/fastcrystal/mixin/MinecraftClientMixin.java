package me.mrbubbles.fastcrystal.mixin;

import me.mrbubbles.fastcrystal.FastCrystal;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.option.GameOptions;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
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
    @Nullable
    public ClientPlayerEntity player;

    @Shadow
    @Nullable
    public ClientPlayerInteractionManager interactionManager;
    @Shadow
    @Final
    public GameOptions options;

    @Shadow
    @Nullable
    public abstract ClientPlayNetworkHandler getNetworkHandler();

    @Inject(at = @At("HEAD"), method = "doItemUse")
    private void itemUse(CallbackInfo ci) {
        if (!FastCrystal.instantPlace.getValue()) return;

        BlockHitResult blockHit = FastCrystal.getLookedAtBlockHit();

        for (Hand hand : Hand.values()) {

            if (!options.attackKey.isPressed() && FastCrystal.canPlaceCrystal(blockHit.getBlockPos(), hand)) {
                ActionResult action = interactionManager.interactBlock(player, hand, blockHit);
                if (action.isAccepted() && action.shouldSwingHand())
                    player.swingHand(hand);
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "doAttack")
    private void attack(CallbackInfoReturnable<Boolean> cir) {
        Entity crystal = FastCrystal.getLookedAtCrystal();
        if (crystal == null || !FastCrystal.removeCrystal.getValue()) return;

        crystal.discard();

        interactionManager.attackEntity(player, crystal);
        getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
    }
}