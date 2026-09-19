package me.mrbubbles.fastcrystal.mixin;

import me.mrbubbles.fastcrystal.FastCrystal;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public class ClientLevelMixin {

    @Inject(at = @At("HEAD"), method = "addEntity", cancellable = true)
    private void onAddEntity(Entity entity, CallbackInfo ci) {
        if (FastCrystal.onEntitySpawn(entity)) ci.cancel();
    }

    @Inject(at = @At("HEAD"), method = "playLocalSound(DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZ)V", cancellable = true)
    private void onPlayLocalSound(double x, double y, double z, SoundEvent sound, SoundSource source, float volume, float pitch, boolean distanceDelay, CallbackInfo ci) {
        if (FastCrystal.predicting) return;
        if (source != SoundSource.BLOCKS || !sound.equals(SoundEvents.GENERIC_EXPLODE.value())) return;

        BlockPos pos = FastCrystal.baseOf(x, y, z);
        if (FastCrystal.pendingExplosions.containsKey(pos)) {
            ci.cancel();
            FastCrystal.onPredictedExplosionSound(pos);
        } else FastCrystal.onCrystalExploded(pos);
    }

    @Inject(at = @At("HEAD"), method = "addParticle(Lnet/minecraft/core/particles/ParticleOptions;ZZDDDDDD)V", cancellable = true)
    private void onAddParticle(ParticleOptions options, boolean force, boolean reduce, double x, double y, double z, double dx, double dy, double dz, CallbackInfo ci) {
        if (options.getType() == ParticleTypes.EXPLOSION_EMITTER && FastCrystal.pendingExplosions.containsKey(FastCrystal.baseOf(x, y, z)))
            ci.cancel();
    }
}