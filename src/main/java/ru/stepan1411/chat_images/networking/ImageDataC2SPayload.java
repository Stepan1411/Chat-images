package ru.stepan1411.chat_images.networking;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ru.stepan1411.chat_images.Chat_images;

public record ImageDataC2SPayload(byte[] data) implements CustomPacketPayload {

    public static final int MAX_CHUNK_SIZE = 30000;
    public static final Type<ImageDataC2SPayload> TYPE = new Type<>(Chat_images.id("image_data_c2s"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ImageDataC2SPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.byteArray(MAX_CHUNK_SIZE),
            ImageDataC2SPayload::data,
            ImageDataC2SPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}
