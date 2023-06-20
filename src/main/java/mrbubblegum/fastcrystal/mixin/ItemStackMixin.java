package mrbubblegum.fastcrystal.mixin;

import mrbubblegum.fastcrystal.FastCrystalMod;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static mrbubblegum.fastcrystal.FastCrystalMod.mc;

@Mixin(ItemStack.class)
public class ItemStackMixin {

    @Inject(method = "getBobbingAnimationTime", at = @At("HEAD"), cancellable = true)
    private void stopBobbingAnimation(CallbackInfoReturnable<Integer> cir) {
        mc.execute(() -> {
            if (FastCrystalMod.fastCrystal.getValue())
                cir.setReturnValue(0);
        });
    }
}
