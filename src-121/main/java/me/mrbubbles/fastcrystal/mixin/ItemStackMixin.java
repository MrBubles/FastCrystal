package me.mrbubbles.fastcrystal.mixin;

import me.mrbubbles.fastcrystal.FastCrystal;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class ItemStackMixin {

    @Inject(method = "getBobbingAnimationTime", at = @At("HEAD"), cancellable = true)
    private void getBobbingAnimationTime(CallbackInfoReturnable<Integer> ci) {
        if (FastCrystal.isEnabled()) ci.setReturnValue(0);
    }
}
