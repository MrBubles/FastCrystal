package me.mrbubbles.fastcrystal.mixin;

import me.mrbubbles.fastcrystal.FastCrystal;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientWorld.class)
public class ClientWorldMixin {
    @Inject(at = @At("TAIL"), method = "addEntity")
    private void onAddEntity(Entity entity, CallbackInfo ci) {
        FastCrystal.onEntitySpawn(entity);
    }
}
