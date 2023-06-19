package mrbubblegum.fastcrystal.mixin;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface LivingEntityInterface {

    @Accessor("lastAttackedTicks")
    void setLastAttackedTicks(int ticks);
}