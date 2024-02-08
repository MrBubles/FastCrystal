package me.mrbubbles.fastcrystal.ui.components;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import static me.mrbubbles.fastcrystal.FastCrystal.mc;

public interface FastCrystalGuiObj {

    TextRenderer textRenderer = mc.textRenderer;

    void mouseScrolled(double mx, double my, float inc);

    void mouseClicked(double mx, double my);

    void render(DrawContext context, int mouseX, int mouseY);

    boolean isWithin(double mouseX, double mouseY);
}