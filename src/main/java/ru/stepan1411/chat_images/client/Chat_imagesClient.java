package ru.stepan1411.chat_images.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.stepan1411.chat_images.Chat_images;
import ru.stepan1411.chat_images.networking.*;

import javax.imageio.ImageIO;
import java.util.*;

public class Chat_imagesClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("ChatImagesClient");

    public static boolean serverHasMod = false;

    private static final Map<UUID, List<byte[]>> PENDING_IMAGES = new HashMap<>();

    @Override
    public void onInitializeClient() {
        ImageIO.scanForPlugins();

        ClientPlayNetworking.registerGlobalReceiver(ImageDataS2CPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                PENDING_IMAGES.computeIfAbsent(payload.sender(), k -> new ArrayList<>()).add(payload.data());
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(ImageEndS2CPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                UUID senderUuid = payload.sender();
                String senderName = payload.senderName();
                int totalSize = payload.totalSize();

                List<byte[]> chunks = PENDING_IMAGES.remove(senderUuid);
                if (chunks == null || chunks.isEmpty()) return;

                byte[] fileData = new byte[totalSize];
                int offset = 0;
                for (byte[] chunk : chunks) {
                    System.arraycopy(chunk, 0, fileData, offset, chunk.length);
                    offset += chunk.length;
                }

                Minecraft client = context.client();
                client.execute(() -> {
                    byte[] pngBytes = FileUtil.convertToPng(fileData);
                    if (pngBytes != null) {
                        UUID imageId = ImageChatStorage.store(pngBytes, senderName);
                        if (imageId != null) {
                            if (client.gui != null) {
                                client.gui.getChat().addMessage(
                                        Component.literal("<" + senderName + ">"),
                                        null,
                                        new GuiMessageTag(0, null, null, "ChatImages#" + imageId)
                                );
                            }
                        }
                    } else {
                        String ext = FileUtil.getExtension(senderName.contains(".") ? senderName : "");
                        if (ext.isEmpty()) ext = "file";

                        UUID fileId = FileChatStorage.store(fileData, senderName, senderName);
                        int iconColor = FileUtil.isVideo(ext) ? 0xFF4488FF : FileUtil.isAudio(ext) ? 0xFF44FF44 : 0xFF888888;
                        NativeImage icon = new NativeImage(16, 16, false);
                        for (int y = 0; y < 16; y++) {
                            for (int x = 0; x < 16; x++) {
                                icon.setPixel(x, y, iconColor);
                            }
                        }
                        String suffix = fileId.toString().replace("-", "");
                        DynamicTexture tex = new DynamicTexture(() -> "chat_file_" + suffix, icon);
                        Identifier texId = Identifier.fromNamespaceAndPath(Chat_images.MOD_ID, "file/" + suffix);
                        Minecraft.getInstance().getTextureManager().register(texId, tex);
                        tex.upload();
                        ImageChatStorage.storeTexture(fileId, tex, texId, senderName);

                        if (client.gui != null) {
                            client.gui.getChat().addMessage(
                                    Component.literal("<" + senderName + ">"),
                                    null,
                                    new GuiMessageTag(0, null, null, "ChatImages#" + fileId)
                            );
                        }
                    }
                });
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(TestS2CPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                ClientPlayNetworking.send(new TestAckC2SPayload());
            });
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            PENDING_IMAGES.clear();
            serverHasMod = ClientPlayNetworking.canSend(ImageDataC2SPayload.TYPE.id());
            ClientPlayNetworking.send(new HandshakeC2SPayload());
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            serverHasMod = false;
            PENDING_IMAGES.clear();
        });
    }

}
