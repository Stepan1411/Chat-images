package ru.stepan1411.chat_images.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import ru.stepan1411.chat_images.Chat_images;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ImageChatStorage {

    private static final Map<UUID, TextureEntry> TEXTURES = new ConcurrentHashMap<>();

    private record TextureEntry(DynamicTexture texture, Identifier id, String senderName) {}

    public static UUID store(byte[] pngData, String senderName) {
        try {
            NativeImage nativeImage = NativeImage.read(new ByteArrayInputStream(pngData));
            String suffix = UUID.randomUUID().toString().replace("-", "");
            DynamicTexture texture = new DynamicTexture(() -> "chat_image_" + suffix, nativeImage);
            UUID uuid = UUID.randomUUID();
            Identifier id = Identifier.fromNamespaceAndPath(Chat_images.MOD_ID, "dynamic/" + suffix);
            Minecraft.getInstance().getTextureManager().register(id, texture);
            texture.upload();
            TEXTURES.put(uuid, new TextureEntry(texture, id, senderName));
            Chat_images.LOGGER.info("Stored image {} from {}, texture={}", uuid, senderName, texture.getTexture());
            return uuid;
        } catch (IOException e) {
            Chat_imagesClient.LOGGER.error("Failed to decode image", e);
            return null;
        }
    }

    public static void storeTexture(UUID uuid, DynamicTexture texture, Identifier id, String senderName) {
        TEXTURES.put(uuid, new TextureEntry(texture, id, senderName));
    }

    public static Identifier getTextureId(UUID id) {
        TextureEntry entry = TEXTURES.get(id);
        return entry != null ? entry.id : null;
    }

    public static DynamicTexture getTexture(UUID id) {
        TextureEntry entry = TEXTURES.get(id);
        return entry != null ? entry.texture : null;
    }

    public static String getSenderName(UUID id) {
        TextureEntry entry = TEXTURES.get(id);
        return entry != null ? entry.senderName : "Unknown";
    }

    public static void remove(UUID id) {
        TextureEntry entry = TEXTURES.remove(id);
        if (entry != null) {
            Minecraft.getInstance().getTextureManager().release(entry.id);
            entry.texture.close();
        }
    }

    public static void clear() {
        Minecraft mc = Minecraft.getInstance();
        TEXTURES.values().forEach(entry -> {
            mc.getTextureManager().release(entry.id);
            entry.texture.close();
        });
        TEXTURES.clear();
    }

}
