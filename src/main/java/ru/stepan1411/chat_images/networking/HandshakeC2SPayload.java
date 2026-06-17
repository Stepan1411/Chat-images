package ru.stepan1411.chat_images.networking;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ru.stepan1411.chat_images.Chat_images;

public record HandshakeC2SPayload() implements CustomPacketPayload {

    public static final Type<HandshakeC2SPayload> TYPE = new Type<>(Chat_images.id("handshake"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HandshakeC2SPayload> STREAM_CODEC = StreamCodec.unit(new HandshakeC2SPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}
