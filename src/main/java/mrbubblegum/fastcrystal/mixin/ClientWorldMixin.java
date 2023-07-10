package mrbubblegum.fastcrystal.mixin;

import mrbubblegum.fastcrystal.utils.RenderUtil;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static mrbubblegum.fastcrystal.FastCrystalMod.mc;

@Mixin({ClientWorld.class})
public class ClientWorldMixin {

    @Inject(method = {"removeEntity"}, at = {@At("HEAD")})
    private void removeEntity(int entityId, Entity.RemovalReason removalReason, CallbackInfo ci) {
        mc.execute(() -> {
            if (mc.world != null)
                RenderUtil.renderedEntities.remove(mc.world.getEntityById(entityId));
            RenderUtil.unrenderedEntities.add(mc.world.getEntityById(entityId));
        });
    }
}