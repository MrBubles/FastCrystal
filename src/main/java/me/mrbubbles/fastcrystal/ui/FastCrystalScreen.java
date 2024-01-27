package me.mrbubbles.fastcrystal.ui;

import me.mrbubbles.fastcrystal.FastCrystal;
import me.mrbubbles.fastcrystal.settings.BooleanSetting;
import me.mrbubbles.fastcrystal.settings.KeybindSetting;
import me.mrbubbles.fastcrystal.settings.Setting;
import me.mrbubbles.fastcrystal.ui.components.FastCrystalGuiObj;
import me.mrbubbles.fastcrystal.ui.components.KeybindBox;
import me.mrbubbles.fastcrystal.ui.components.Switch;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.util.LinkedHashSet;
import java.util.Set;

public class FastCrystalScreen extends Screen {

    private final Set<FastCrystalGuiObj> objs = new LinkedHashSet<>();

    public FastCrystalScreen() {
        super(Text.of("FastCrystal"));
    }

    @Override
    public void init() {
        int settingCount = 0;
        for (Setting<?> setting : Setting.settings) {
            if (setting.isHidden()) return;
            settingCount++;
            if (setting instanceof BooleanSetting booleanSetting) {
                objs.add(new Switch(booleanSetting, 80, 50 + (settingCount << 4), 12));
            } else if (setting instanceof KeybindSetting keySetting) {
                objs.add(new KeybindBox(keySetting, 80, 50 + (settingCount << 4), 12));
            }
        }
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float partialTicks) {
        renderBackground(matrices);
        for (FastCrystalGuiObj obj : objs) obj.render(matrices, mouseX, mouseY);

        if (!FastCrystal.openedUI) FastCrystal.openedUI = true;
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