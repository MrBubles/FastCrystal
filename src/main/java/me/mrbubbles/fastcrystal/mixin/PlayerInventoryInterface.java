package me.mrbubbles.fastcrystal.mixin;

import net.minecraft.entity.player.PlayerInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// Later mc versions make it private
@Mixin(PlayerInventory.class)
public interface PlayerInventoryInterface {

    @Accessor("selectedSlot")
    int getSelectedSlot();
}