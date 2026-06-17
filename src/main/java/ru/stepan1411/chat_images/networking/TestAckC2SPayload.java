package ru.stepan1411.chat_images.networking;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ru.stepan1411.chat_images.Chat_images;

public record TestAckC2SPayload() implements CustomPacketPayload {

    public static final Type<TestAckC2SPayload> TYPE = new Type<>(Chat_images.id("test_ack_c2s"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TestAckC2SPayload> STREAM_CODEC = StreamCodec.unit(new TestAckC2SPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
