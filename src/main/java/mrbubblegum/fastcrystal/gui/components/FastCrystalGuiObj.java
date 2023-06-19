package mrbubblegum.fastcrystal.gui.components;

import net.minecraft.client.util.math.MatrixStack;

public interface FastCrystalGuiObj {

    void mouseScrolled(double mx, double my, float inc);

    void mouseClicked(double mx, double my);

    void render(MatrixStack matrices, int mouseX, int mouseY);

    boolean isWithin(double mouseX, double mouseY);
}