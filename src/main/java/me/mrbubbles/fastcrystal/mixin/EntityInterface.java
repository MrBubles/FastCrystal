package me.mrbubbles.fastcrystal.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// Later mc versions remove getPos
@Mixin(Entity.class)
public interface EntityInterface {

    @Accessor("pos")
    Vec3d getPos();
}