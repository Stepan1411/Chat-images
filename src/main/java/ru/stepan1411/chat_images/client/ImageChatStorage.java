package ru.stepan1411.chat_images.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import ru.stepan1411.chat_images.Chat_images;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ImageChatStorage {

    private static final Map<UUID, TextureEntry> TEXTURES = new ConcurrentHashMap<>();
    private static final Map<UUID, GifData> GIF_DATA = new ConcurrentHashMap<>();
    private static final int MIN_FRAME_DELAY = 20;

    private record TextureEntry(DynamicTexture texture, Identifier id, String senderName) {}

    private record GifData(int frameCount, int frameWidth, int frameHeight, int[] delaysMs, long startTime) {
        int totalDuration() {
            int total = 0;
            for (int d : delaysMs) total += Math.max(d, MIN_FRAME_DELAY);
            return total;
        }

        int currentFrame() {
            int total = totalDuration();
            if (total <= 0) return 0;
            long elapsed = (System.currentTimeMillis() - startTime) % total;
            int accum = 0;
            for (int i = 0; i < delaysMs.length; i++) {
                accum += Math.max(delaysMs[i], MIN_FRAME_DELAY);
                if (elapsed < accum) return i;
            }
            return delaysMs.length - 1;
        }
    }

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
            return uuid;
        } catch (IOException e) {
            Chat_imagesClient.LOGGER.error("Failed to decode image", e);
            return null;
        }
    }

    public static UUID storeAnimated(byte[] data, String senderName) {
        ImageReader reader = null;
        ImageInputStream stream = null;
        try {
            stream = new javax.imageio.stream.MemoryCacheImageInputStream(new ByteArrayInputStream(data));
            reader = ImageIO.getImageReadersByFormatName("gif").next();
            reader.setInput(stream);

            int frameCount = reader.getNumImages(true);
            if (frameCount <= 1) return null;

            BufferedImage firstFrame = reader.read(0);
            int frameW = firstFrame.getWidth();
            int frameH = firstFrame.getHeight();

            NativeImage combined = new NativeImage(frameW, frameH * frameCount, false);
            int[] delays = new int[frameCount];

            for (int i = 0; i < frameCount; i++) {
                BufferedImage frame = reader.read(i);
                if (frame.getWidth() != frameW || frame.getHeight() != frameH) {
                    BufferedImage fullFrame = new BufferedImage(frameW, frameH, BufferedImage.TYPE_INT_ARGB);
                    java.awt.Graphics2D g = fullFrame.createGraphics();
                    try {
                        IIOMetadataNode root = (IIOMetadataNode) reader.getImageMetadata(i).getAsTree("javax_imageio_gif_image_1.0");
                        int xOff = 0, yOff = 0;
                        for (int j = 0; j < root.getLength(); j++) {
                            if (root.item(j).getNodeName().equals("ImageDescriptor")) {
                                xOff = Integer.parseInt(((IIOMetadataNode) root.item(j)).getAttribute("imageLeftPosition"));
                                yOff = Integer.parseInt(((IIOMetadataNode) root.item(j)).getAttribute("imageTopPosition"));
                                break;
                            }
                        }
                        g.drawImage(frame, xOff, yOff, null);
                    } finally {
                        g.dispose();
                    }
                    frame = fullFrame;
                }
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(frame, "PNG", baos);
                NativeImage frameNative = NativeImage.read(new ByteArrayInputStream(baos.toByteArray()));
                for (int y = 0; y < frameH; y++) {
                    for (int x = 0; x < frameW; x++) {
                        combined.setPixel(x, y + i * frameH, frameNative.getPixel(x, y));
                    }
                }
                frameNative.close();

                delays[i] = 100;
                try {
                    IIOMetadataNode root = (IIOMetadataNode) reader.getImageMetadata(i).getAsTree("javax_imageio_gif_image_1.0");
                    for (int j = 0; j < root.getLength(); j++) {
                        if (root.item(j).getNodeName().equals("GraphicControlExtension")) {
                            int delay = Integer.parseInt(((IIOMetadataNode) root.item(j)).getAttribute("delayTime"));
                            delays[i] = delay * 10;
                            break;
                        }
                    }
                } catch (Exception e) {
                    // keep default
                }
                if (delays[i] <= 0) delays[i] = 100;
            }

            reader.dispose();
            stream.close();

            String suffix = UUID.randomUUID().toString().replace("-", "");
            DynamicTexture texture = new DynamicTexture(() -> "chat_anim_" + suffix, combined);
            UUID uuid = UUID.randomUUID();
            Identifier id = Identifier.fromNamespaceAndPath(Chat_images.MOD_ID, "dynamic/" + suffix);
            Minecraft.getInstance().getTextureManager().register(id, texture);
            texture.upload();
            TEXTURES.put(uuid, new TextureEntry(texture, id, senderName));
            GIF_DATA.put(uuid, new GifData(frameCount, frameW, frameH, delays, System.currentTimeMillis()));
            return uuid;
        } catch (Exception e) {
            return null;
        } finally {
            if (reader != null) reader.dispose();
            if (stream != null) try { stream.close(); } catch (IOException ignored) {}
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

    public static int getFrameCount(UUID id) {
        GifData gif = GIF_DATA.get(id);
        return gif != null ? gif.frameCount : 1;
    }

    public static int getFrameHeight(UUID id) {
        GifData gif = GIF_DATA.get(id);
        return gif != null ? gif.frameHeight : -1;
    }

    public static int getFrameWidth(UUID id) {
        GifData gif = GIF_DATA.get(id);
        return gif != null ? gif.frameWidth : -1;
    }

    public static int getCurrentFrame(UUID id) {
        GifData gif = GIF_DATA.get(id);
        return gif != null ? gif.currentFrame() : 0;
    }

    public static void remove(UUID id) {
        TextureEntry entry = TEXTURES.remove(id);
        if (entry != null) {
            Minecraft.getInstance().getTextureManager().release(entry.id);
            entry.texture.close();
        }
        GIF_DATA.remove(id);
    }

    public static void clear() {
        Minecraft mc = Minecraft.getInstance();
        TEXTURES.values().forEach(entry -> {
            mc.getTextureManager().release(entry.id);
            entry.texture.close();
        });
        TEXTURES.clear();
        GIF_DATA.clear();
    }

}
