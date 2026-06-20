<div style="text-align: center;">
  <h1>FastCrystal</h1>
  <h3>Crystal fast</h3>
</div>

## Overview

FastCrystal is a ClientSided Minecraft mod that makes end crystals go faster by removing all the delays for placing and
breaking end crystals and removes them from the clientside too.

## Opt-Out

Servers can send out an opt-out packet which disables FastCrystal for the player it was sent to.

## Server Opt-Out Implementation

#### Fabric Server

Simply installing FastCrystal on a Fabric server will automatically disable it for all players.

Alternatively, you can implement the opt-out packet:

##### Pre-26.1

```java

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
```

```java

@Override
public void onInitializeServer() {
    PayloadTypeRegistry.playS2C().register(DisableFastCrystalPayload.ID, DisableFastCrystalPayload.CODEC);
    ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> ServerPlayNetworking.send(handler.player, DisableFastCrystalPayload.INSTANCE));
}
```

##### 26.1+

```java
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
```

```java

@Override
public void onInitializeServer() {
    PayloadTypeRegistry.clientboundPlay().register(DisableFastCrystalPayload.ID, DisableFastCrystalPayload.CODEC);
    ServerPlayConnectionEvents.JOIN.register((handler, _, server) -> ServerPlayNetworking.send(handler.getPlayer(), DisableFastCrystalPayload.INSTANCE));
}
```

#### Paper / Spigot

```java
private static final String CHANNEL = "clientcrystal:disable_fast_crystal";

@EventHandler
public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    if (player.getListeningPluginChannels().contains(CHANNEL)) player.sendPluginMessage(this, CHANNEL, new byte[0]);
}
```