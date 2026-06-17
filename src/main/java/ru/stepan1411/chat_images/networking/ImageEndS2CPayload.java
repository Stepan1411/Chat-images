package ru.stepan1411.chat_images.networking;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ru.stepan1411.chat_images.Chat_images;

import java.util.UUID;

public record ImageEndS2CPayload(UUID sender, String senderName, int totalSize) implements CustomPacketPayload {

    public static final Type<ImageEndS2CPayload> TYPE = new Type<>(Chat_images.id("image_end_s2c"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ImageEndS2CPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC,
            ImageEndS2CPayload::sender,
            ByteBufCodecs.stringUtf8(64),
            ImageEndS2CPayload::senderName,
            ByteBufCodecs.VAR_INT,
            ImageEndS2CPayload::totalSize,
            ImageEndS2CPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}
