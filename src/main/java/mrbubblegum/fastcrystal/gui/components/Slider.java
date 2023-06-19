//package mrbubblegum.fastcrystal.gui.components;
//
//import mrbubblegum.fastcrystal.settings.FloatSetting;
//import net.minecraft.client.util.math.MatrixStack;
//import net.minecraft.util.math.MathHelper;
//
//import static mrbubblegum.fastcrystal.FastCrystalMod.mc;
//import static mrbubblegum.fastcrystal.gui.FastCrystalScreen.round;
//import static net.minecraft.client.gui.DrawableHelper.fill;
//
//public class Slider implements FastCrystalGuiObj {
//
//    private final FloatSetting setting;
//    private final int x, y, width, height;
//    private final float min, max;
//
//    public Slider(FloatSetting setting, int x, int y, int width, int height) {
//        this.setting = setting;
//        this.x = x;
//        this.y = y;
//        this.width = width;
//        this.height = height;
//        min = setting.min;
//        max = setting.max;
//    }
//
//    @Override
//    public void mouseScrolled(double mx, double my, float inc) {
//        setting.setValue(MathHelper.clamp(setting.getValue() + inc * 0.1F, min, max));
//    }
//
//    @Override
//    public void mouseClicked(double mx, double my) {
//        setting.setValue(min + (float) ((max - min) / width * (mx - x)));
//    }
//
//    @Override
//    public void render(MatrixStack matrices, int mouseX, int mouseY) {
//        mc.textRenderer.drawWithShadow(matrices, setting.getName(), x - mc.textRenderer.getWidth(setting.getName()) - 1, y + height / 2f - mc.textRenderer.fontHeight / 2f, -1);
//        fill(matrices, x, y, (int) (x + ((setting.getValue() - min) / (max - min)) * width), y + height, -1);
//        mc.textRenderer.drawWithShadow(matrices, String.valueOf(round(setting.getValue(), 1)), x + width + 1, y + height / 2f - mc.textRenderer.fontHeight / 2f, -1);
//    }
//
//    @Override
//    public boolean isWithin(double mouseX, double mouseY) {
//        return mouseX > x && mouseY > y && mouseX < x + width && mouseY < y + height;
//    }
//}