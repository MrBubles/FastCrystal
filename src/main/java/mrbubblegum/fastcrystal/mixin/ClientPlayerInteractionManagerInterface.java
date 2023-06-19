package mrbubblegum.fastcrystal.mixin;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientPlayerInteractionManager.class)
public interface ClientPlayerInteractionManagerInterface {

    @Invoker("syncSelectedSlot")
    void syncSelectedSlot();

    @Accessor("gameMode")
    GameMode getGameMode();
}