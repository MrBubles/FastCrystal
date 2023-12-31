package mrbubblegum.fastcrystal.mixin;

import mrbubblegum.fastcrystal.utils.RenderUtil;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static mrbubblegum.fastcrystal.FastCrystalMod.mc;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private <T extends Entity> void onRender(T entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        mc.execute(() -> {
            if (!entity.isRemoved())
                RenderUtil.renderedEntities.add(entity);
        });
        if (RenderUtil.unrenderedEntities.contains(entity))
            ci.cancel();
    }
}