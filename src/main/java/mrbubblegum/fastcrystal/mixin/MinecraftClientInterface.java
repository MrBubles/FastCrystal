package mrbubblegum.fastcrystal.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({MinecraftClient.class})
public interface MinecraftClientInterface {

    @Accessor("attackCooldown")
    int getAttackCooldown();

    @Accessor("attackCooldown")
    void setAttackCooldown(int cooldown);

    @Accessor("itemUseCooldown")
    int getItemUseCooldown();

    @Accessor("itemUseCooldown")
    void setItemUseCooldown(int cooldown);
}