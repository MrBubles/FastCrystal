package mrbubblegum.fastcrystal.gui.components;

import mrbubblegum.fastcrystal.settings.BooleanSetting;
import net.minecraft.client.util.math.MatrixStack;

import static mrbubblegum.fastcrystal.FastCrystalMod.mc;
import static net.minecraft.client.gui.DrawableHelper.fill;

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
        mc.execute(() -> {
            setting.setValue(!setting.getValue());
        });
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY) {

        mc.execute(() -> {

            mc.textRenderer.drawWithShadow(matrices, setting.getName(), x - mc.textRenderer.getWidth(setting.getName()) - padding, y + height / 2f - mc.textRenderer.fontHeight / 2f, -1);

            fill(matrices, x - 1, y - 1, x + width + 1, y + height + 1, -0x33333334);
            fill(matrices, x, y, x + width, y + height, -0x78EFEFF0);

            if (setting.getValue()) {
                fill(matrices, x + width - height - 1, y - 1, x + width + 1, y + height + 1, -0x33333334);
                fill(matrices, x + width - height, y, x + width, y + height, -1);
            } else {
                fill(matrices, x - 1, y - 1, x + height + 1, y + height + 1, -0x33333334);
                fill(matrices, x, y, x + height, y + height, -1);
            }

            mc.textRenderer.drawWithShadow(matrices, setting.getValue().toString(), x + width + padding, y + height / 2f - mc.textRenderer.fontHeight / 2f, -1);

            if (isWithin(mouseX, mouseY)) {
                matrices.translate(0.0f, 0.0f, 1.0f);
                fill(matrices, mouseX + 5, mouseY - 1, mouseX + 6 + mc.textRenderer.getWidth(setting.getDescription()), mouseY + 9, 0xEF000000);
                mc.textRenderer.drawWithShadow(matrices, setting.getDescription(), mouseX + 6, mouseY, -1);
                matrices.translate(0.0f, 0.0f, -1.0f);
            }
        });
    }

    @Override
    public boolean isWithin(double mouseX, double mouseY) {
        return mouseX > x && mouseY > y && mouseX < x + width && mouseY < y + height;
    }
}