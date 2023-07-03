//package mrbubblegum.fastcrystal.mixin;
//
//import mrbubblegum.fastcrystal.FastCrystalMod;
//import net.minecraft.client.main.Main;
//import net.minecraft.obfuscate.DontObfuscate;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//
//@Mixin({Main.class})
//public class MainMixin {
//
//    @DontObfuscate
//    @Inject(method = "main([Ljava/lang/String;Z)V", at = @At("HEAD"))
//    private static void preInit(String[] args, boolean optimizeDataFixer, CallbackInfo ci) {
//        FastCrystalMod.execute(() -> FastCrystalMod.INSTANCE.onPreInitializeClient());
//    }
//}