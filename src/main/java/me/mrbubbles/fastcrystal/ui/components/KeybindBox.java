package me.mrbubbles.fastcrystal.ui.components;

import me.mrbubbles.fastcrystal.settings.KeybindSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import static me.mrbubbles.fastcrystal.FastCrystal.mc;

public class KeybindBox implements FastCrystalGuiObj {

    private final KeybindSetting setting;
    private final int x;
    private final int y;
    private final int padding;
    private final int boxWidth;
    private final int boxHeight;
    private boolean listening;
    private int keybind;

    public KeybindBox(KeybindSetting setting, int x, int y, int boxHeight) {
        this.setting = setting;
        this.x = x;
        this.y = y;
        listening = false;
        keybind = setting.getValue();
        this.padding = boxHeight / 4;
        this.boxWidth = boxHeight * 2;
        this.boxHeight = boxHeight;
    }

    @Override
    public void mouseScrolled(double mx, double my, float inc) {
    }

    @Override
    public void mouseClicked(double mx, double my) {
        if (isWithin(mx, my)) {
            listening = true;
        }
    }

    private String getKeyName(int key) {
        if (key == GLFW.GLFW_KEY_UNKNOWN) return "none";
        String str = InputUtil.fromKeyCode(key, 0).getTranslationKey().toLowerCase();
        str = str.replace("KEY.KEYBOARD", "").replace(".", "");
        if (str.charAt(0) == ' ') str = str.substring(1);
        return str;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY) {

        if (isWithin(mouseX, mouseY)) {
            context.getMatrices().translate(0f, 0f, 1f);
            context.fill(mouseX + 5, mouseY - 1, mouseX + 6 + mc.textRenderer.getWidth(setting.getDescription()), mouseY + 9, 0xEF000000);
            context.drawTextWithShadow(textRenderer, setting.getDescription(), mouseX + 6, mouseY, -1);
            context.getMatrices().translate(0f, 0f, -1f);
        }

        context.drawTextWithShadow(textRenderer, setting.getName(), x - mc.textRenderer.getWidth(setting.getName()) - padding, this.y + boxHeight / 2 - mc.textRenderer.fontHeight / 2, -1);

        context.fill(x - 1, this.y - 1, x + boxWidth + 1, this.y + boxHeight + 1, -0x33333334);
        context.fill(x, this.y, x + boxWidth, this.y + boxHeight, -0x78EFEFF0);

        if (listening) {

            context.drawTextWithShadow(textRenderer, "Press any key", x + boxWidth + padding, this.y + boxHeight / 2 - mc.textRenderer.fontHeight / 2, -1);

            for (int i = GLFW.GLFW_KEY_SPACE; i <= GLFW.GLFW_KEY_LAST; i++) {
                if (GLFW.glfwGetKey(mc.getWindow().getHandle(), i) == GLFW.GLFW_PRESS) {
                    if (i == GLFW.GLFW_KEY_ESCAPE) {
                        keybind = -1;
                        setting.setValue(-1);
                    } else {
                        keybind = i;
                        setting.setValue(i);
                    }
                    listening = false;
                    break;
                }
            }
        } else {
            String keyName = GLFW.glfwGetKeyName(keybind, 0);

            if (keyName == null || keyName.isEmpty()) {
                context.drawTextWithShadow(textRenderer, getKeyName(keybind), x + boxWidth / 2 - mc.textRenderer.getWidth(String.valueOf(keybind)) / 2, this.y + boxHeight / 2 - mc.textRenderer.fontHeight / 2, -1);
            } else if (keybind == GLFW.GLFW_KEY_UNKNOWN) {
                context.drawTextWithShadow(textRenderer, "None", x + boxWidth / 2 - mc.textRenderer.getWidth("None") / 2, this.y + boxHeight / 2 - mc.textRenderer.fontHeight / 2, -1);
            } else {
                context.drawTextWithShadow(textRenderer, keyName, x + boxWidth / 2 - mc.textRenderer.getWidth(keyName) / 2, this.y + boxHeight / 2 - mc.textRenderer.fontHeight / 2, -1);
            }
        }
    }

    @Override
    public boolean isWithin(double mouseX, double mouseY) {
        return mouseX > x && mouseY > y && mouseX < x + boxWidth && mouseY < y + boxHeight;
    }
}