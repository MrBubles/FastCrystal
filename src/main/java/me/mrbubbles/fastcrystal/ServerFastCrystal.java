package me.mrbubbles.fastcrystal;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class ServerFastCrystal implements DedicatedServerModInitializer {

    @Override
    public void onInitializeServer() {
        PayloadTypeRegistry.playS2C().register(DisableFastCrystalPayload.ID, DisableFastCrystalPayload.CODEC);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> ServerPlayNetworking.send(handler.player, DisableFastCrystalPayload.INSTANCE));
    }
}
