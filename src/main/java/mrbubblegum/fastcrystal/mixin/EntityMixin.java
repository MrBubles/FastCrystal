package mrbubblegum.fastcrystal.mixin;

import mrbubblegum.fastcrystal.FastCrystalMod;
import mrbubblegum.fastcrystal.utils.RenderUtil;
import net.minecraft.entity.Entity;
import net.minecraft.item.EndCrystalItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static mrbubblegum.fastcrystal.FastCrystalMod.mc;

@Mixin(Entity.class)
public class EntityMixin {

    @Inject(at = @At("HEAD"), method = "remove")
    private void onRemove(CallbackInfo ci) {
        mc.execute(() -> {
            RenderUtil.renderedEntities.remove((Entity) (Object) this);
            RenderUtil.unrenderedEntities.add((Entity) (Object) this);
        });
    }

    @Inject(at = @At("HEAD"), method = "kill")
    private void onKill(CallbackInfo ci) {
        mc.execute(() -> {
            RenderUtil.renderedEntities.remove((Entity) (Object) this);
            RenderUtil.unrenderedEntities.add((Entity) (Object) this);
        });
    }

    @Inject(at = @At("HEAD"), method = "tick")
    private void onTick(CallbackInfo ci) {
        mc.execute(() -> {
            if (mc == null || mc.player == null | mc.world == null)
                return;

//            if (FastCrystalMod.fastUse.getValue() && ((MinecraftClientInterface) mc).getItemUseCooldown() != 0 && mc.player.getMainHandStack().isOf(Items.END_CRYSTAL) && Objects.equals(FastCrystalMod.getLookedAtBlock(), Blocks.OBSIDIAN) | Objects.equals(FastCrystalMod.getLookedAtBlock(), Blocks.BEDROCK))
//                ((MinecraftClientInterface) mc).setItemUseCooldown(0);

            if (FastCrystalMod.fastAttack.getValue() && mc.player.getMainHandStack().getItem() instanceof EndCrystalItem crystalItem) {
                if (((MinecraftClientInterface) mc).getAttackCooldown() != 0)
                    ((MinecraftClientInterface) mc).setAttackCooldown(0);
                mc.player.getItemCooldownManager().set(crystalItem, 0);
                ((LivingEntityInterface) mc.player).setLastAttackedTicks(69);
                if (((HeldItemRendererInterface) mc.gameRenderer.firstPersonRenderer).getPrevEquipProgressMainHand() >= 0.9) {
                    ((HeldItemRendererInterface) mc.gameRenderer.firstPersonRenderer).setEquipProgressMainHand(1.0f);
                    ((HeldItemRendererInterface) mc.gameRenderer.firstPersonRenderer).setMainhandStack(mc.player.getMainHandStack());
                }
            }
        });
    }
}