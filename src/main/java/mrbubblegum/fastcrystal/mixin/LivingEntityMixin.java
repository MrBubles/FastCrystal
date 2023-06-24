package mrbubblegum.fastcrystal.mixin;

import mrbubblegum.fastcrystal.FastCrystalMod;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static mrbubblegum.fastcrystal.FastCrystalMod.mc;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Inject(at = @At("RETURN"), method = "getHandSwingDuration", cancellable = true)
    private void getHandSwingDuration(CallbackInfoReturnable<Integer> cir) {
        mc.execute(() -> {
            if (FastCrystalMod.fastCrystal.getValue() && FastCrystalMod.fastSwing.getValue() && mc.player != null && mc.player.getMainHandStack().getItem().equals(Items.END_CRYSTAL))
                cir.setReturnValue(4);
        });
    }
}