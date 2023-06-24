package mrbubblegum.fastcrystal.mixin;

import mrbubblegum.fastcrystal.FastCrystalMod;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

import static mrbubblegum.fastcrystal.FastCrystalMod.mc;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    @Inject(method = "sendPacket*", at = @At("HEAD"), cancellable = true)
    public void onPacketSend(Packet<?> packet, CallbackInfo ci) {
        if (FastCrystalMod.fastCrystal.getValue() && FastCrystalMod.placeOptimize.getValue() && packet instanceof PlayerInteractBlockC2SPacket placePacket && mc.world != null) {
            BlockPos pos = placePacket.getBlockHitResult().getBlockPos();
            BlockPos currentLookedPos = Objects.requireNonNull(FastCrystalMod.getLookedAtBlockHitResult()).getBlockPos();
            if (FastCrystalMod.isCloseToCrystal(pos, mc.world) | !Objects.equals(pos, currentLookedPos))
                ci.cancel();
        }
    }
}