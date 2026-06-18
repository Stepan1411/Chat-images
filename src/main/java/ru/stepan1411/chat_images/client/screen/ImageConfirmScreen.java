package ru.stepan1411.chat_images.client.screen;

import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import ru.stepan1411.chat_images.Chat_images;
import ru.stepan1411.chat_images.client.FileChatStorage;
import ru.stepan1411.chat_images.client.FileUtil;
import ru.stepan1411.chat_images.client.ImageChatStorage;
import ru.stepan1411.chat_images.networking.ImageDataC2SPayload;
import ru.stepan1411.chat_images.networking.ImageEndC2SPayload;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.UUID;

public class ImageConfirmScreen extends OverlayScreen {

    private static final WidgetSprites CONFIRM_SPRITES = new WidgetSprites(
            Identifier.fromNamespaceAndPath(Chat_images.MOD_ID, "confirm"),
            Identifier.fromNamespaceAndPath(Chat_images.MOD_ID, "confirm")
    );
    private static final WidgetSprites CANCEL_SPRITES = new WidgetSprites(
            Identifier.fromNamespaceAndPath(Chat_images.MOD_ID, "cancel"),
            Identifier.fromNamespaceAndPath(Chat_images.MOD_ID, "cancel")
    );

    private final byte[] sendBytes;
    private final byte[] previewBytes;
    private final String fileName;
    private final boolean isImage;
    private DynamicTexture previewTexture;
    private Identifier previewId;
    private int imageWidth;
    private int imageHeight;
    private double zoom = 1.0;
    private double offsetX = 0;
    private double offsetY = 0;
    private boolean dragging = false;
    private double dragStartX;
    private double dragStartY;
    private double dragOffsetX;
    private double dragOffsetY;
    private ImageButton confirmButton;
    private ImageButton cancelButton;
    private UUID previewUuid;
    private boolean confirmed = false;

    public ImageConfirmScreen(Screen parent, byte[] sendBytes, byte[] previewBytes, String fileName, boolean isImage) {
        super(Component.translatable("chat_images.confirm"), parent);
        this.sendBytes = sendBytes;
        this.previewBytes = previewBytes;
        this.fileName = fileName;
        this.isImage = isImage;
    }

    @Override
    protected void init() {
        super.init();
        if (isImage) loadPreview();

        int cx = width / 2;
        int cy = height / 2;

        confirmButton = addRenderableWidget(new ImageButton(
                cx - 60,
                cy + 60,
                20, 20,
                CONFIRM_SPRITES,
                button -> sendFile(),
                CommonComponents.EMPTY
        ));
        confirmButton.setTooltip(Tooltip.create(Component.translatable("chat_images.send")));

        cancelButton = addRenderableWidget(new ImageButton(
                cx + 40,
                cy + 60,
                20, 20,
                CANCEL_SPRITES,
                button -> onClose(),
                CommonComponents.EMPTY
        ));
        cancelButton.setTooltip(Tooltip.create(CommonComponents.GUI_CANCEL));
    }

    private void loadPreview() {
        UUID animUuid = ImageChatStorage.storeAnimated(sendBytes, Minecraft.getInstance().getUser().getName());
        if (animUuid != null) {
            previewUuid = animUuid;
            previewId = ImageChatStorage.getTextureId(animUuid);
            imageWidth = ImageChatStorage.getFrameWidth(animUuid);
            imageHeight = ImageChatStorage.getFrameHeight(animUuid);
        } else {
            try {
                NativeImage nativeImage = NativeImage.read(new ByteArrayInputStream(previewBytes));
                imageWidth = nativeImage.getWidth();
                imageHeight = nativeImage.getHeight();
                previewTexture = new DynamicTexture(() -> "chat_images_preview", nativeImage);
                previewId = Identifier.fromNamespaceAndPath(Chat_images.MOD_ID, "preview_" + UUID.randomUUID().toString().replace("-", ""));
                Minecraft.getInstance().getTextureManager().register(previewId, previewTexture);
                previewTexture.upload();
            } catch (IOException e) {
                Chat_images.LOGGER.error("Failed to load preview", e);
            }
        }
    }

    private void sendFile() {
        int offset = 0;
        while (offset < sendBytes.length) {
            int chunkSize = Math.min(ImageDataC2SPayload.MAX_CHUNK_SIZE, sendBytes.length - offset);
            byte[] chunk = new byte[chunkSize];
            System.arraycopy(sendBytes, offset, chunk, 0, chunkSize);
            offset += chunkSize;
            ClientPlayNetworking.send(new ImageDataC2SPayload(chunk));
        }
        ClientPlayNetworking.send(new ImageEndC2SPayload(sendBytes.length));

        if (isImage) {
            UUID imageId = previewUuid;
            if (imageId == null) {
                imageId = ImageChatStorage.storeAnimated(sendBytes, Minecraft.getInstance().getUser().getName());
                if (imageId == null) {
                    imageId = ImageChatStorage.store(previewBytes, Minecraft.getInstance().getUser().getName());
                }
            }
            if (imageId != null) {
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft.gui != null) {
                    minecraft.gui.getChat().addMessage(
                            Component.literal("<" + Minecraft.getInstance().getUser().getName() + ">"),
                            null,
                            new GuiMessageTag(0, null, null, "ChatImages#" + imageId)
                    );
                }
            }
        } else {
            Minecraft minecraft = Minecraft.getInstance();
            String ext = FileUtil.getExtension(fileName);
            UUID fileId = FileChatStorage.store(sendBytes, fileName, Minecraft.getInstance().getUser().getName());
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
            minecraft.getTextureManager().register(texId, tex);
            tex.upload();
            ImageChatStorage.storeTexture(fileId, tex, texId, Minecraft.getInstance().getUser().getName());
            if (minecraft.gui != null) {
                minecraft.gui.getChat().addMessage(
                        Component.literal("<" + Minecraft.getInstance().getUser().getName() + ">"),
                        null,
                        new GuiMessageTag(0, null, null, "ChatImages#" + fileId)
                );
            }
        }

        confirmed = true;
        onClose();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isHovered) {
        if (super.mouseClicked(event, isHovered)) return true;
        if (!isImage) return true;
        if (event.button() == 0 && previewId != null) {
            dragging = true;
            dragStartX = event.x();
            dragStartY = event.y();
            dragOffsetX = offsetX;
            dragOffsetY = offsetY;
        }
        if (event.button() == 1) {
            onClose();
        }
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragging = false;
        if (super.mouseReleased(event)) return true;
        if (event.button() == 1) {
            onClose();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (super.mouseDragged(event, dragX, dragY)) return true;
        if (dragging && event.button() == 0) {
            offsetX = dragOffsetX + (event.x() - dragStartX);
            offsetY = dragOffsetY + (event.y() - dragStartY);
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!isImage || previewId == null) return true;
        double factor = scrollY > 0 ? 1.1 : 1.0 / 1.1;
        double newZoom = Math.max(0.025, Math.min(zoom * factor, 40.0));
        if (newZoom == zoom) return true;

        double fitScale = Math.min((double) width / imageWidth, (double) height / imageHeight) * 0.85;
        int cx = width / 2;
        int cy = height / 2;
        double fracX = (mouseX - (cx - imageWidth * fitScale * zoom / 2 + offsetX)) / (imageWidth * fitScale * zoom);
        double fracY = (mouseY - (cy - imageHeight * fitScale * zoom / 2 + offsetY)) / (imageHeight * fitScale * zoom);
        zoom = newZoom;
        offsetX = mouseX - cx + imageWidth * fitScale * zoom * (0.5 - fracX);
        offsetY = mouseY - cy + imageHeight * fitScale * zoom * (0.5 - fracY);

        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
            sendFile();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
    }

    @Override
    protected void actualRender(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        super.actualRender(guiGraphics, mouseX, mouseY, delta);

        if (isImage) {
            renderImagePreview(guiGraphics, mouseX, mouseY, delta);
        } else {
            renderFileInfo(guiGraphics);
        }
    }

    private void renderImagePreview(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        if (previewId == null) {
            guiGraphics.drawString(font, Component.translatable("chat_images.load_error"),
                    width / 2 - font.width(Component.translatable("chat_images.load_error")) / 2,
                    height / 2, 0xFFFF5555);
            return;
        }

        double fitScale = Math.min((double) width / imageWidth, (double) height / imageHeight) * 0.85;
        int imgW = (int) (imageWidth * fitScale * zoom);
        int imgH = (int) (imageHeight * fitScale * zoom);
        int cx = width / 2;
        int cy = height / 2;
        int x = cx - imgW / 2 + (int) offsetX;
        int y = cy - imgH / 2 + (int) offsetY;

        if (previewUuid != null && ImageChatStorage.getFrameCount(previewUuid) > 1) {
            int currentFrame = ImageChatStorage.getCurrentFrame(previewUuid);
            int frameCount = ImageChatStorage.getFrameCount(previewUuid);
            float vMin = (float) currentFrame / frameCount;
            float vMax = (float) (currentFrame + 1) / frameCount;
            guiGraphics.blit(previewId, x, y, x + imgW, y + imgH, 0.0f, 1.0f, vMin, vMax);
        } else {
            guiGraphics.blit(previewId, x, y, x + imgW, y + imgH, 0.0f, 1.0f, 0.0f, 1.0f);
        }

        String info = imageWidth + "x" + imageHeight + "  " + String.format("%.0f", zoom * 100) + "%";
        guiGraphics.drawString(font, info, 10, height - 20, 0xFFFFFFFF);
    }

    private void renderFileInfo(GuiGraphics guiGraphics) {
        String ext = FileUtil.getExtension(fileName);
        String type = FileUtil.isVideo(ext) ? "Video" : FileUtil.isAudio(ext) ? "Audio" : "File";

        int cx = width / 2;
        int cy = height / 2 - 20;

        guiGraphics.drawString(font, Component.literal("§l" + fileName), cx - font.width(fileName) / 2, cy - 20, 0xFFFFFFFF);
        guiGraphics.drawString(font, Component.literal("Type: " + type + " (." + ext + ")"), cx - font.width("Type: " + type + " (." + ext + ")") / 2, cy + 5, 0xFFAAAAAA);
        guiGraphics.drawString(font, Component.literal("Size: " + FileUtil.formatFileSize(sendBytes.length)), cx - font.width("Size: " + FileUtil.formatFileSize(sendBytes.length)) / 2, cy + 20, 0xFFAAAAAA);
        guiGraphics.drawString(font, Component.translatable("chat_images.confirm_hint"), cx - font.width(Component.translatable("chat_images.confirm_hint")) / 2, cy + 45, 0xFF666666);
    }

    @Override
    public void removed() {
        super.removed();
        if (previewUuid != null) {
            if (!confirmed) {
                ImageChatStorage.remove(previewUuid);
            }
        } else {
            if (previewId != null) {
                Minecraft.getInstance().getTextureManager().release(previewId);
            }
            if (previewTexture != null) {
                previewTexture.close();
            }
        }
    }
}
