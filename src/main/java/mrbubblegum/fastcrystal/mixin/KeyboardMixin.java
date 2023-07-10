package mrbubblegum.fastcrystal.mixin;

import mrbubblegum.fastcrystal.FastCrystalMod;
import mrbubblegum.fastcrystal.gui.FastCrystalScreen;
import net.minecraft.client.Keyboard;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static mrbubblegum.fastcrystal.FastCrystalMod.mc;

@Mixin(Keyboard.class)
public class KeyboardMixin {

    @Inject(at = @At("HEAD"), method = "onKey(JIIII)V")
    private void onKey(long windowHandle, int keyCode, int scanCode, int action, int modifiers, CallbackInfo ci) {
        mc.execute(() -> {
            if (mc.world != null && mc.player != null && mc.currentScreen == null && windowHandle == mc.getWindow().getHandle() && action == GLFW.GLFW_PRESS && keyCode == FastCrystalMod.guiBind.getValue()) {
                mc.setScreen(new FastCrystalScreen());
                if (!FastCrystalMod.openedGui.getValue())
                    FastCrystalMod.openedGui.setValue(true);
            }
        });
    }
}