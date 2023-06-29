package mrbubblegum.fastcrystal.mixin;

import mrbubblegum.fastcrystal.FastCrystalMod;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static mrbubblegum.fastcrystal.FastCrystalMod.mc;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {

    //    public Entity crystal;
    @Shadow
    private int lastSelectedSlot;

    /**
     * @author MrBubblegum
     * @reason cuz i didn't like the original code
     */
    @Overwrite
    private void syncSelectedSlot() {
        mc.execute(() -> {
            if (mc.player != null && mc.getNetworkHandler() != null) {
                int selectedSlot = mc.player.getInventory().selectedSlot;
                if (lastSelectedSlot != selectedSlot) {
                    lastSelectedSlot = selectedSlot;
                    mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(selectedSlot));
                }
            }
        });
    }

    @Inject(at = @At("HEAD"), method = "attackEntity")
    private void onAttackEntity(PlayerEntity player, Entity entity, CallbackInfo ci) {
        if (mc.world != null && mc.getNetworkHandler() != null && FastCrystalMod.fastCrystal.getValue() && FastCrystalMod.removeCrystal.getValue() && player.equals(mc.player) && FastCrystalMod.isCrystal(entity) && FastCrystalMod.isEntityExisting(entity)) {
            syncSelectedSlot();
            mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking()));
            mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            entity.kill();
            entity.remove(Entity.RemovalReason.KILLED);
            entity.onRemoved();
//        }
//
//        if (FastCrystalMod.fastCrystal.getValue() && FastCrystalMod.placeOptimize.getValue() && player.equals(mc.player) && FastCrystalMod.isCrystal(entity) && FastCrystalMod.attackedCrystals.contains(entity) && !mc.player.isSpectator()) {
//            mc.player.attack(entity);
//            mc.player.resetLastAttackedTicks();
//            ci.cancel();
        } /*else FastCrystalMod.attackedCrystals.add(entity);*/
    }
}