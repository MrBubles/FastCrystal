package me.mrbubbles.fastcrystal.ui.components;//package me.mrbubbles.fastcrystal.ui.components;
//
//import me.mrbubbles.fastcrystal.settings.IntegerSetting;
//import net.minecraft.client.util.math.MatrixStack;
//import net.minecraft.util.math.MathHelper;
//
//import static me.mrbubbles.fastcrystal.FastCrystal.mc;
//import static net.minecraft.client.ui.DrawableHelper.fill;
//
//public class IntSlider implements FastCrystalGuiObj {
//
//    private final IntegerSetting setting;
//    private final int x, y, width, height;
//    private final int min, max;
//
//    public IntSlider(IntegerSetting setting, int x, int y, int width, int height) {
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
//        setting.setValue((int) MathHelper.clamp(setting.getValue() + inc * 0.05F, min, max));
//    }
//
//    @Override
//    public void mouseClicked(double mx, double my) {
//        setting.setValue((int) (min + ((max - min) / width * (mx - x))));
//    }
//
//    @Override
//    public void render(MatrixStack matrices, int mouseX, int mouseY) {
//        mc.textRenderer.drawWithShadow(matrices, setting.getName(), x - mc.textRenderer.getWidth(setting.getName()) - 1, y + height / 2f - mc.textRenderer.fontHeight / 2f, -1);
//        fill(matrices, x, y, x + ((setting.getValue() - min) / (max - min)) * width, y + height, -1);
//        mc.textRenderer.drawWithShadow(matrices, String.valueOf(setting.getValue()), x + width + 1, y + height / 2f - mc.textRenderer.fontHeight / 2f, -1);
//    }
//
//    @Override
//    public boolean isWithin(double mouseX, double mouseY) {
//        return mouseX > x && mouseY > y && mouseX < x + width && mouseY < y + height;
//    }
//}
////package me.mrbubbles.fastcrystal.ui.components;
////
////        import me.mrbubbles.fastcrystal.settings.IntegerSetting;
////        import net.minecraft.client.util.math.MatrixStack;
////        import net.minecraft.util.math.MathHelper;
////        import static me.mrbubbles.fastcrystal.FastCrystal.mc;
////        import static net.minecraft.client.ui.DrawableHelper.fill;
////
////public class IntSlider implements FastCrystalGuiObj {
////    private final IntegerSetting setting;
////    private final int x, y, width, height;
////    private final int min, max;
////
////    public IntSlider(IntegerSetting setting, int x, int y, int width, int height) {
////        this.setting = setting;
////        this.x = x;
////        this.y = y;
////        this.width = width;
////        this.height = height;
////        min = setting.min;
////        max = setting.max;
////    }
////
////    @Override
////    public void mouseScrolled(double mx, double my, float inc) {
////        setting.setValue((int) MathHelper.clamp(setting.getValue() + inc * 0.05F, min, max));
////    }
////
////    @Override
////    public void mouseClicked(double mx, double my) {
////        setting.setValue((int) (min + ((max - min) / width * (mx - x))));
////    }
////
////    @Override
////    public void render(MatrixStack matrices, int mouseX, int mouseY) {
////        // draw the setting name to the left of the slider
////        mc.textRenderer.drawWithShadow(matrices, setting.getName(), x - mc.textRenderer.getWidth(setting.getName()) - 1,
////                y + height / 2f - mc.textRenderer.fontHeight / 2f, -1);
////
////        // draw the slider background with rounded corners
////        fill(matrices, x + 1, y + 1, x + width - 1, y + height - 1, 0xFF000000); // black fill
////        fill(matrices, x + 1, y + 1, x + width - 1, y + 2, 0xFF808080); // top border
////        fill(matrices, x + 1, y + height - 2, x + width - 1, y + height - 1, 0xFF808080); // bottom border
////        fill(matrices, x + 1, y + 2, x + 2 , y + height - 2 , 0xFF808080); // left border
////        fill(matrices, x + width - 2 , y + 2 , x + width - 1 , y + height - 2 , 0xFF808080); // right border
////
////        // draw the slider handle with rounded corners
////        int handleX = x + ((setting.getValue() - min) / (max - min)) * width; // calculate the handle position based on the setting value
////        fill(matrices, handleX - height / 2 , y , handleX + height / 2 , y + height , -1); // white fill
////        fill(matrices, handleX - height / 2 , y , handleX - height / 2 + 1 , y + height , 0xFF808080); // left border
////        fill(matrices, handleX + height / 2 - 1 , y , handleX + height / 2 , y + height , 0xFF808080); // right border
////        fill(matrices, handleX - height / 2 , y , handleX - height / 2 + height /4 , y + height /4 ,0xFF808080); // top left corner
////        fill(matrices, handleX - height /4 , y , handleX + height /4 , y+height/4 ,0xFF808080); // top border
////        fill(matrices, handleX+height/4,y+height/4-1,x+height/4,y+height/4,-1); // fix top right corner pixel
////        fill(matrices, handleX+height/4,y+height/4,x+height/4,y+height/4+height/4-1,-1); // fix right border pixel
////        fill(matrices, handleX+height/4,y+height/4,x+height/4+height/4,y+height/4+height/4,0xFF808080); // top right corner
////        fill(matrices, handleX-height/2,y+height-height/4,handleX-height/2+height/4,y+height,0xFF808080); // bottom left corner
////        fill(matrices, handleX-height/2+height/4,y+height-height/4,handleX+height/2-height/4,y+height,0xFF808080); // bottom border
////        fill(matrices, handleX+height/2-height/4,y+height-height/4,handleX+height/2,y+height,0xFF808080); // bottom right corner
////
////        // draw the setting value to the right of the slider
////        mc.textRenderer.drawWithShadow(matrices, String.valueOf(setting.getValue()), x + width + 1,
////                y + height / 2f - mc.textRenderer.fontHeight / 2f, -1);
////    }
////
////    @Override
////    public boolean isWithin(double mouseX, double mouseY) {
////        return mouseX > x && mouseY > y && mouseX < x + width && mouseY < y + height;
////    }
////}