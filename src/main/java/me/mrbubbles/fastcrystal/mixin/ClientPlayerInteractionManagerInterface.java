package me.mrbubbles.fastcrystal.mixin;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientPlayerInteractionManager.class)
public interface ClientPlayerInteractionManagerInterface {

    @Accessor("lastSelectedSlot")
    int getLastSelectedSlot();

    @Accessor("lastSelectedSlot")
    void setLastSelectedSlot(int slot);

    @Invoker("sendSequencedPacket")
    void sendSequencedPacket(ClientWorld world, SequencedPacketCreator packetCreator);
}