package ru.stepan1411.chat_images.networking;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ru.stepan1411.chat_images.Chat_images;

public record TestS2CPayload() implements CustomPacketPayload {

    public static final Type<TestS2CPayload> TYPE = new Type<>(Chat_images.id("test_s2c"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TestS2CPayload> STREAM_CODEC = StreamCodec.unit(new TestS2CPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
