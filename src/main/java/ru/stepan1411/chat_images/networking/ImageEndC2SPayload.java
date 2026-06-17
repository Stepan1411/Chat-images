package ru.stepan1411.chat_images.networking;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ru.stepan1411.chat_images.Chat_images;

public record ImageEndC2SPayload(int totalSize) implements CustomPacketPayload {

    public static final Type<ImageEndC2SPayload> TYPE = new Type<>(Chat_images.id("image_end_c2s"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ImageEndC2SPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            ImageEndC2SPayload::totalSize,
            ImageEndC2SPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}
