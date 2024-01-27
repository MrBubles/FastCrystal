package me.mrbubbles.fastcrystal.mixin;

import net.minecraft.item.EndCrystalItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EndCrystalItem.class)
public class EndCrystalItemMixin {

    @Redirect(method = "useOnBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;decrement(I)V"))
    private void fixDecrement(ItemStack stack, int amount, ItemUsageContext context) {
        if (!context.getWorld().isClient)
            stack.decrement(1);
    }
}