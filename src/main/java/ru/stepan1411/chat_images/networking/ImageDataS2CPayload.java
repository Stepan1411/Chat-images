package ru.stepan1411.chat_images.networking;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ru.stepan1411.chat_images.Chat_images;

import java.util.UUID;

public record ImageDataS2CPayload(UUID sender, byte[] data) implements CustomPacketPayload {

    public static final int MAX_CHUNK_SIZE = 100000;
    public static final Type<ImageDataS2CPayload> TYPE = new Type<>(Chat_images.id("image_data_s2c"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ImageDataS2CPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC,
            ImageDataS2CPayload::sender,
            ByteBufCodecs.byteArray(MAX_CHUNK_SIZE),
            ImageDataS2CPayload::data,
            ImageDataS2CPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}
