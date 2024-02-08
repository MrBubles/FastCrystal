package me.mrbubbles.fastcrystal.mixin;

import me.mrbubbles.fastcrystal.FastCrystal;
import me.mrbubbles.fastcrystal.ui.FastCrystalScreen;
import net.minecraft.client.Keyboard;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static me.mrbubbles.fastcrystal.FastCrystal.mc;

@Mixin(Keyboard.class)
public class KeyboardMixin {

    @Inject(at = @At("HEAD"), method = "onKey(JIIII)V")
    private void onKey(long windowHandle, int keyCode, int scanCode, int action, int modifiers, CallbackInfo ci) {
        if (mc.world == null || mc.player == null || mc.currentScreen != null || windowHandle != mc.getWindow().getHandle() || action != GLFW.GLFW_PRESS || keyCode != FastCrystal.uiBind.getValue())
            return;

        mc.setScreen(FastCrystalScreen.UI);
    }
}