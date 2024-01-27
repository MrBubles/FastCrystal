package me.mrbubbles.fastcrystal.mixin;

import me.mrbubbles.fastcrystal.FastCrystal;
import net.minecraft.entity.Entity;
import net.minecraft.item.EndCrystalItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static me.mrbubbles.fastcrystal.FastCrystal.mc;

@Mixin(Entity.class)
public class EntityMixin {


    @Inject(at = @At("HEAD"), method = "tick")
    private void onTick(CallbackInfo ci) {
        if (mc == null || mc.player == null | mc.world == null || !FastCrystal.fastCrystal.getValue() || !FastCrystal.fastAttack.getValue())
            return;

        if (mc.player.getMainHandStack().getItem() instanceof EndCrystalItem crystalItem) {
            if (((MinecraftClientInterface) mc).getAttackCooldown() != 0)
                ((MinecraftClientInterface) mc).setAttackCooldown(0);
            mc.player.getItemCooldownManager().set(crystalItem, 0);
            ((LivingEntityInterface) mc.player).setLastAttackedTicks(69);
            if (((HeldItemRendererInterface) mc.gameRenderer.firstPersonRenderer).getPrevEquipProgressMainHand() >= 0.9) {
                ((HeldItemRendererInterface) mc.gameRenderer.firstPersonRenderer).setEquipProgressMainHand(1.0f);
                ((HeldItemRendererInterface) mc.gameRenderer.firstPersonRenderer).setMainhandStack(mc.player.getMainHandStack());
            }
        }
    }
}