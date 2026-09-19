package me.mrbubbles.fastcrystal;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public class DisableFastCrystalPayload implements CustomPacketPayload {

    public static final DisableFastCrystalPayload INSTANCE = new DisableFastCrystalPayload();
    public static final Type<DisableFastCrystalPayload> ID = new Type<>(Identifier.parse("fastcrystal:disable_fast_crystal"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DisableFastCrystalPayload> CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
