package me.mrbubbles.fastcrystal;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class ServerFastCrystal implements DedicatedServerModInitializer {

    @Override
    public void onInitializeServer() {
        PayloadTypeRegistry.clientboundPlay().register(DisableFastCrystalPayload.ID, DisableFastCrystalPayload.CODEC);
        ServerPlayConnectionEvents.JOIN.register((handler, _, server) -> {
            if (server.isSingleplayer()) return;
            ServerPlayNetworking.send(handler.getPlayer(), DisableFastCrystalPayload.INSTANCE);
        });
    }
}
