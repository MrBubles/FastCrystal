package mrbubblegum.fastcrystal.mixin;

import mrbubblegum.fastcrystal.FastCrystalMod;
import net.minecraft.entity.Entity;
import net.minecraft.item.EndCrystalItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static mrbubblegum.fastcrystal.FastCrystalMod.mc;

@Mixin(Entity.class)
public class EntityMixin {

    @Inject(at = @At("HEAD"), method = "tick")
    private void onTick(CallbackInfo ci) {
        mc.execute(() -> {
            if (mc.world == null | mc.player == null | !FastCrystalMod.fastCrystal.getValue()) {
                return;
            }

            if (FastCrystalMod.fastAttack.getValue() && mc.player.getMainHandStack().getItem() instanceof EndCrystalItem crystalItem) {
                mc.player.getItemCooldownManager().set(crystalItem, 0);
                ((LivingEntityInterface) mc.player).setLastAttackedTicks(69);
                ((MinecraftClientInterface) mc).setAttackCooldown(0);
            }
        });
    }
}