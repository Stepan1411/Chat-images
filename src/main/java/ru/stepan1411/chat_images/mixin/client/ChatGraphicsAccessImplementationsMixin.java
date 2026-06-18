package ru.stepan1411.chat_images.mixin.client;

import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.stepan1411.chat_images.client.ChatGraphicsHolder;
import ru.stepan1411.chat_images.client.Chat_imagesClient;
import ru.stepan1411.chat_images.client.ClickableImageTracker;
import ru.stepan1411.chat_images.client.ImageChatStorage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mixin(targets = {
        "net.minecraft.client.gui.components.ChatComponent$DrawingBackgroundGraphicsAccess",
        "net.minecraft.client.gui.components.ChatComponent$DrawingFocusedGraphicsAccess"
})
abstract class ChatGraphicsAccessImplementationsMixin {

    @Unique
    private FormattedCharSequence chatImages_lastText;
    @Unique
    private final Map<UUID, Integer> chatImages_imageTextWidths = new HashMap<>();

    @Unique
    private static final int MAX_IMAGE_WIDTH = 80;
    @Unique
    private static final int MAX_IMAGE_HEIGHT = 80;

    @Inject(method = "handleMessage", at = @At("TAIL"))
    private void chatImages_handleMessageTail(int i, float f, FormattedCharSequence text, CallbackInfoReturnable<Boolean> cir) {
        if (text != FormattedCharSequence.EMPTY) {
            chatImages_lastText = text;
        }
    }

    @Inject(method = "handleTag", at = @At("HEAD"))
    private void chatImages_handleTagHead(int x1, int y1, int x2, int y2, float alpha, GuiMessageTag tag, CallbackInfo ci) {
        if (chatImages_lastText == null) return;
        String logTag = tag.logTag();
        if (logTag == null || !logTag.startsWith("ChatImages#")) return;
        String[] parts = logTag.split("#");
        if (parts.length < 2) return;
        UUID mainUuid;
        try {
            mainUuid = UUID.fromString(parts[1]);
        } catch (IllegalArgumentException e) {
            return;
        }
        chatImages_imageTextWidths.put(mainUuid, Minecraft.getInstance().font.width(chatImages_lastText));
    }

    @Inject(method = "handleTag", at = @At("TAIL"))
    private void chatImages_handleTagTail(int x1, int y1, int x2, int y2, float alpha, GuiMessageTag tag, CallbackInfo ci) {
        String logTag = tag.logTag();
        if (logTag == null || !logTag.startsWith("ChatImages#")) return;

        String[] parts = logTag.split("#");
        if (parts.length < 2) return;

        UUID mainUuid;
        try {
            mainUuid = UUID.fromString(parts[1]);
        } catch (IllegalArgumentException e) {
            return;
        }

        Identifier texId = ImageChatStorage.getTextureId(mainUuid);
        if (texId == null) return;

        GuiGraphics guiGraphics = ChatGraphicsHolder.get();

        var tex = ImageChatStorage.getTexture(mainUuid);
        if (tex == null || tex.getPixels() == null) return;

        int texW = tex.getPixels().getWidth();
        int texH = ImageChatStorage.getFrameHeight(mainUuid);
        if (texH < 0) texH = tex.getPixels().getHeight();

        float aspect = (float) texW / texH;
        int drawWidth, drawHeight;
        if (aspect > 1.0f) {
            drawWidth = MAX_IMAGE_WIDTH;
            drawHeight = Math.max(1, (int) (MAX_IMAGE_WIDTH / aspect));
        } else {
            drawHeight = MAX_IMAGE_HEIGHT;
            drawWidth = Math.max(1, (int) (MAX_IMAGE_HEIGHT * aspect));
        }

        int frameCount = ImageChatStorage.getFrameCount(mainUuid);
        int currentFrame = ImageChatStorage.getCurrentFrame(mainUuid);
        float frameUnit = 1.0f / frameCount;
        float frameV = frameUnit * currentFrame;

        int textWidth = chatImages_imageTextWidths.getOrDefault(mainUuid, 60);
        int baseX = x2 + 2 + textWidth + 8;

        if (parts.length >= 4) {
            int index = Integer.parseInt(parts[2]);
            int spacerCount = Integer.parseInt(parts[3]);

            float vMin = frameV + frameUnit * index / spacerCount;
            float vMax = frameV + frameUnit * (index + 1) / spacerCount;

            if (guiGraphics != null) {
                guiGraphics.blit(texId, baseX, y1 - 1,
                        baseX + drawWidth, y1 + 8,
                        0.0f, 1.0f, vMin, vMax);
            }

            ClickableImageTracker.add(mainUuid, baseX, y1 + 1, drawWidth, 9);
        } else {
            if (drawHeight <= 9 && guiGraphics != null) {
                guiGraphics.blit(texId, baseX, y1 - 1,
                        baseX + drawWidth, y1 - 1 + drawHeight,
                        0.0f, 1.0f, frameV, frameV + frameUnit);
            }

            ClickableImageTracker.add(mainUuid, baseX, y1 + 1, drawWidth, 9);
        }
    }

}
