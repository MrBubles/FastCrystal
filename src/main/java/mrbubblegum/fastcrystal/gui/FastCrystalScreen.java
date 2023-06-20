package mrbubblegum.fastcrystal.gui;

import mrbubblegum.fastcrystal.FastCrystalMod;
import mrbubblegum.fastcrystal.gui.components.FastCrystalGuiObj;
import mrbubblegum.fastcrystal.gui.components.KeybindBox;
import mrbubblegum.fastcrystal.gui.components.Switch;
import mrbubblegum.fastcrystal.settings.BooleanSetting;
import mrbubblegum.fastcrystal.settings.KeybindSetting;
import mrbubblegum.fastcrystal.settings.Setting;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class FastCrystalScreen extends Screen {

    private final List<FastCrystalGuiObj> objs = new ArrayList<>();

    public FastCrystalScreen() {
        super(Text.of("FastCrystal"));
    }

//    public static float round(float value, int places) {
//        BigDecimal bd = new BigDecimal(value);
//        bd = bd.setScale(places, RoundingMode.HALF_UP);
//        return bd.floatValue();
//    }

    @Override
    public void init() {
        int settingCount = 0;
        for (Setting setting : FastCrystalMod.SETTINGS) {
            settingCount++;
            if (!setting.isHidden()) {
                if (setting instanceof BooleanSetting) {
                    objs.add(new Switch((BooleanSetting) setting, 80, 50 + (settingCount << 4), 12));
//            } else if (setting instanceof FloatSetting) {
//                objs.add(new Slider((FloatSetting) setting, 80, 50 + (settingCount << 4), 80, 12));
//            } else if (setting instanceof IntegerSetting) {
//                objs.add(new IntSlider((IntegerSetting) setting, 80, 50 + (settingCount << 4), 80, 12));
                } else if (setting instanceof KeybindSetting) {
                    objs.add(new KeybindBox((KeybindSetting) setting, 80, 50 + (settingCount << 4), 70, 12));
                }
            }
        }
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float partialTicks) {
        renderBackground(matrices);
        for (FastCrystalGuiObj obj : objs)
            obj.render(matrices, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (FastCrystalGuiObj obj : objs) {
            if (obj.isWithin(mouseX, mouseY))
                obj.mouseClicked(mouseX, mouseY);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double multiplier) {
        for (FastCrystalGuiObj obj : objs) {
            if (obj.isWithin(mouseX, mouseY))
                obj.mouseScrolled(mouseX, mouseY, (float) multiplier);
        }
        return super.mouseScrolled(mouseX, mouseY, multiplier);
    }
}