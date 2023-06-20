package mrbubblegum.fastcrystal.mixin;

import mrbubblegum.fastcrystal.FastCrystalMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static mrbubblegum.fastcrystal.FastCrystalMod.limitPackets;
import static mrbubblegum.fastcrystal.FastCrystalMod.mc;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {


    @Inject(at = @At("HEAD"), method = "doItemUse", cancellable = true)
    private void onDoItemUse(CallbackInfo ci) {
        if (FastCrystalMod.fastCrystal.getValue() && mc.player != null) {
            ItemStack mainHand = mc.player.getMainHandStack();
            if (mainHand.isOf(Items.END_CRYSTAL))
                if (FastCrystalMod.hitCount != limitPackets())
                    ci.cancel();
        }
    }
}