package mrbubblegum.fastcrystal.mixin;

import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({HeldItemRenderer.class})
public interface HeldItemRendererInterface {

    @Accessor("prevEquipProgressMainHand")
    float getPrevEquipProgressMainHand();

    @Accessor("equipProgressMainHand")
    void setEquipProgressMainHand(float progress);

    @Accessor("mainHand")
    void setMainhandStack(ItemStack stack);
}