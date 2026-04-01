package me.mrbubbles.fastcrystal;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public class DisableFastCrystalPayload implements CustomPayload {

    public static final DisableFastCrystalPayload INSTANCE = new DisableFastCrystalPayload();
    public static final Id<DisableFastCrystalPayload> ID = new Id<>(Identifier.of("fastcrystal", "disable_fast_crystal"));
    public static final PacketCodec<RegistryByteBuf, DisableFastCrystalPayload> CODEC = PacketCodec.unit(INSTANCE);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}