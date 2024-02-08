package me.mrbubbles.fastcrystal.ui.components;

import me.mrbubbles.fastcrystal.settings.BooleanSetting;
import net.minecraft.client.gui.DrawContext;

import static me.mrbubbles.fastcrystal.FastCrystal.mc;

public class Switch implements FastCrystalGuiObj {

    private final BooleanSetting setting;
    private final int x;
    private final int y;
    private final int height;
    private final int width;
    private final int padding;

    public Switch(BooleanSetting setting, int x, int y, int height) {
        this.setting = setting;
        this.x = x;
        this.y = y;
        this.height = height;
        this.width = height * 2;
        this.padding = height / 4;
    }

    @Override
    public void mouseScrolled(double mx, double my, float inc) {

    }

    @Override
    public void mouseClicked(double mx, double my) {
        setting.setValue(!setting.getValue());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY) {

        if (isWithin(mouseX, mouseY)) {
            context.getMatrices().translate(0f, 0f, 1f);
            context.fill(mouseX + 5, mouseY - 1, mouseX + 6 + mc.textRenderer.getWidth(setting.getDescription()), mouseY + 9, 0xEF000000);
            context.drawTextWithShadow(textRenderer, setting.getDescription(), mouseX + 6, mouseY, -1);
            context.getMatrices().translate(0f, 0f, -1f);
        }

        context.drawTextWithShadow(textRenderer, setting.getName(), x - mc.textRenderer.getWidth(setting.getName()) - padding, y + height / 2 - mc.textRenderer.fontHeight / 2, -1);

        context.fill(x - 1, y - 1, x + width + 1, y + height + 1, -0x33333334);
        context.fill(x, y, x + width, y + height, -0x78EFEFF0);

        if (setting.getValue()) {
            context.fill(x + width - height - 1, y - 1, x + width + 1, y + height + 1, -0x33333334);
            context.fill(x + width - height, y, x + width, y + height, -1);
        } else {
            context.fill(x - 1, y - 1, x + height + 1, y + height + 1, -0x33333334);
            context.fill(x, y, x + height, y + height, -1);
        }

        context.drawTextWithShadow(textRenderer, setting.getValue().toString(), x + width + padding, y + height / 2 - mc.textRenderer.fontHeight / 2, -1);
    }

    @Override
    public boolean isWithin(double mouseX, double mouseY) {
        return mouseX > x && mouseY > y && mouseX < x + width && mouseY < y + height;
    }
}