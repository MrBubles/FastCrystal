//package mrbubblegum.fastcrystal.mixin;
//
//import net.minecraft.client.model.ModelPart;
//import net.minecraft.client.render.OverlayTexture;
//import net.minecraft.client.render.RenderLayer;
//import net.minecraft.client.render.VertexConsumer;
//import net.minecraft.client.render.VertexConsumerProvider;
//import net.minecraft.client.render.entity.EndCrystalEntityRenderer;
//import net.minecraft.client.render.entity.EnderDragonEntityRenderer;
//import net.minecraft.client.render.entity.EntityRenderer;
//import net.minecraft.client.render.entity.EntityRendererFactory;
//import net.minecraft.client.util.math.MatrixStack;
//import net.minecraft.entity.decoration.EndCrystalEntity;
//import net.minecraft.util.math.BlockPos;
//import net.minecraft.util.math.MathHelper;
//import net.minecraft.util.math.RotationAxis;
//import org.jetbrains.annotations.NotNull;
//import org.joml.Quaternionf;
//import org.spongepowered.asm.mixin.Final;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.Overwrite;
//import org.spongepowered.asm.mixin.Shadow;
//
//import static mrbubblegum.fastcrystal.FastCrystalMod.mc;
//
//@Mixin({EndCrystalEntityRenderer.class})
//public abstract class EndCrystalRendererMixin extends EntityRenderer<EndCrystalEntity> {
//
//    @Final
//    @Shadow
//    private static RenderLayer END_CRYSTAL;
//
//    @Final
//    @Shadow
//    private static float SINE_45_DEGREES;
//
//    @Final
//    @Shadow
//    private ModelPart core;
//
//    @Final
//    @Shadow
//    private ModelPart frame;
//
//    @Final
//    @Shadow
//    private ModelPart bottom;
//
//    protected EndCrystalRendererMixin(EntityRendererFactory.Context ctx) {
//        super(ctx);
//    }
//
//    private static float getYOffset(EndCrystalEntity crystal, float tickDelta) {
//        float f = (float) crystal.endCrystalAge + tickDelta;
//        float g = MathHelper.sin(f * 0.2F) / 2.0F + 0.5F;
//        g = (g * g + g) * 0.4F;
//        return g - 1.4F;
//    }
//
//    /**
//     * @author MrBubblegum
//     * @reason why not
//     */
//    @Overwrite
//    public void render(EndCrystalEntity crystal, float yaw, float tickDelta, @NotNull MatrixStack matrices, @NotNull VertexConsumerProvider vertexConsumers, int light) {
//        mc.execute(() -> {
//            matrices.push();
//
//            float yOffset = getYOffset(crystal, tickDelta);
//            float rotationSpeed = ((float) crystal.endCrystalAge + tickDelta) * 3.0F;
//            VertexConsumer vertexConsumer = vertexConsumers.getBuffer(END_CRYSTAL);
//
//            matrices.push();
//            matrices.scale(1.5F, 1.5F, 1.5F);
//            matrices.translate(0.0, -0.5, 0.0);
//
//            int overlayUV = OverlayTexture.DEFAULT_UV;
//
//            if (crystal.shouldShowBottom())
//                this.bottom.render(matrices, vertexConsumer, light, overlayUV);
//
//            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotationSpeed * 1.7F));
//            matrices.translate(0.0, 1.5F + yOffset / 2.0F, 0.0);
//            matrices.multiply((new Quaternionf()).setAngleAxis(1.0471976F, SINE_45_DEGREES, 0.0F, SINE_45_DEGREES));
//            this.frame.render(matrices, vertexConsumer, light, overlayUV);
//
//            matrices.scale(0.875F, 0.875F, 0.875F);
//            matrices.multiply((new Quaternionf()).setAngleAxis(1.0471976F, SINE_45_DEGREES, 0.0F, SINE_45_DEGREES));
//            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotationSpeed * 1.7F));
//            this.frame.render(matrices, vertexConsumer, light, overlayUV);
//
////            matrices.scale(0.875F, 0.875F, 0.875F);
////            matrices.multiply((new Quaternionf()).setAngleAxis(-1.0471976F, SINE_45_DEGREES, 0.0F, SINE_45_DEGREES));
////            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotationSpeed * 1.7F));
////            this.frame.render(matrices, vertexConsumer, light, overlayUV);
//
//            matrices.scale(0.875F, 0.875F, 0.875F);
//            matrices.multiply((new Quaternionf()).setAngleAxis(1.0471976F, SINE_45_DEGREES, 0.0F, SINE_45_DEGREES));
//            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotationSpeed * 1.7F));
//            this.core.render(matrices, vertexConsumer, light, overlayUV);
//            matrices.pop();
//
//            matrices.pop();
//
//            if (crystal.getBeamTarget() != null) {
//                BlockPos blockPos = crystal.getBeamTarget();
//                float targetX = (float) blockPos.getX() + 0.5F;
//                float targetY = (float) blockPos.getY() + 0.5F;
//                float targetZ = (float) blockPos.getZ() + 0.5F;
//                float deltaX = (float) (targetX - crystal.getX());
//                float deltaY = (float) (targetY - crystal.getY());
//                float deltaZ = (float) (targetZ - crystal.getZ());
//                matrices.translate(deltaX, deltaY, deltaZ);
//                EnderDragonEntityRenderer.renderCrystalBeam(-deltaX, -deltaY + yOffset, -deltaZ, tickDelta, crystal.endCrystalAge, matrices, vertexConsumers, light);
//            }
//
//            super.render(crystal, yaw, tickDelta, matrices, vertexConsumers, light);
//        });
//    }
//}