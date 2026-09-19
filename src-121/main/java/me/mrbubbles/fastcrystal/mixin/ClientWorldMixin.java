package me.mrbubbles.fastcrystal.mixin;

import me.mrbubbles.fastcrystal.FastCrystal;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientWorld.class)
public class ClientWorldMixin {

    @Inject(at = @At("HEAD"), method = "addEntity", cancellable = true)
    private void addEntity(Entity entity, CallbackInfo ci) {
        if (FastCrystal.onEntitySpawn(entity)) ci.cancel();
    }

    @Inject(at = @At("HEAD"), method = "playSound(DDDLnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FFZ)V", cancellable = true)
    private void playSound(double x, double y, double z, SoundEvent sound, SoundCategory category, float volume, float pitch, boolean useDistance, CallbackInfo ci) {
        if (category != SoundCategory.BLOCKS || !sound.equals(SoundEvents.ENTITY_GENERIC_EXPLODE.value()) || FastCrystal.predicting)
            return;

        BlockPos pos = FastCrystal.baseOf(x, y, z);
        if (FastCrystal.pendingExplosions.containsKey(pos)) {
            ci.cancel();
            FastCrystal.onPredictedExplosionSound(pos);
        } else FastCrystal.onCrystalExploded(pos);
    }

    @Inject(at = @At("HEAD"), method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)V", cancellable = true)
    private void addParticle(ParticleEffect parameters, double x, double y, double z, double velocityX, double velocityY, double velocityZ, CallbackInfo ci) {
        if (parameters.getType() == ParticleTypes.EXPLOSION_EMITTER && FastCrystal.pendingExplosions.containsKey(FastCrystal.baseOf(x, y, z)))
            ci.cancel();
    }
}
