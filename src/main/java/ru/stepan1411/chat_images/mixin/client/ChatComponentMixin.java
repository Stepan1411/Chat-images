package ru.stepan1411.chat_images.mixin.client;

import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.stepan1411.chat_images.client.ChatGraphicsHolder;
import ru.stepan1411.chat_images.client.ClickableImageTracker;
import ru.stepan1411.chat_images.client.ImageChatStorage;

import java.util.List;
import java.util.UUID;

@Mixin(ChatComponent.class)
abstract class ChatComponentMixin {

    @Shadow @Final private List<GuiMessage.Line> trimmedMessages;

    @Unique
    private static UUID chatImages_getImageId(GuiMessageTag tag) {
        if (tag == null) return null;
        String logTag = tag.logTag();
        if (logTag == null || !logTag.startsWith("ChatImages#")) return null;
        String[] parts = logTag.split("#");
        if (parts.length < 2) return null;
        try {
            return UUID.fromString(parts[1]);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void chatImages_renderHead(GuiGraphics guiGraphics, Font font, int i, int j, int k, boolean bl, boolean bl2, CallbackInfo ci) {
        ClickableImageTracker.clear();
        ChatGraphicsHolder.set(guiGraphics);
    }

    @Unique
    private static final int MAX_IMAGE_WIDTH = 80;
    @Unique
    private static final int MAX_IMAGE_HEIGHT = 80;

    @Inject(method = "addMessageToDisplayQueue", at = @At("RETURN"))
    private void chatImages_addSpacers(GuiMessage message, CallbackInfo ci) {
        GuiMessageTag tag = message.tag();
        if (tag == null) return;
        UUID mainId = chatImages_getImageId(tag);
        if (mainId == null) return;

        var mainTexture = ImageChatStorage.getTexture(mainId);
        if (mainTexture == null || mainTexture.getPixels() == null) return;

        int texWidth = mainTexture.getPixels().getWidth();
        int texHeight = mainTexture.getPixels().getHeight();

        float aspect = (float) texWidth / texHeight;
        int drawWidth, drawHeight;
        if (aspect > 1.0f) {
            drawWidth = MAX_IMAGE_WIDTH;
            drawHeight = Math.max(1, (int) (MAX_IMAGE_WIDTH / aspect));
        } else {
            drawHeight = MAX_IMAGE_HEIGHT;
            drawWidth = Math.max(1, (int) (MAX_IMAGE_HEIGHT * aspect));
        }

        int spacerCount = Math.max(1, (drawHeight + 9 - 1) / 9);

        for (int i = 0; i < spacerCount; i++) {
            trimmedMessages.add(0, new GuiMessage.Line(
                    message.addedTime(),
                    FormattedCharSequence.EMPTY,
                    new GuiMessageTag(0, null, null, "ChatImages#" + mainId + "#" + i + "#" + spacerCount),
                    i == spacerCount - 1
            ));
        }

        while (trimmedMessages.size() > 100) {
            trimmedMessages.removeLast();
        }
    }

    @Redirect(
            method = "addMessageToDisplayQueue",
            at = @At(value = "INVOKE", target = "Ljava/util/List;removeLast()Ljava/lang/Object;")
    )
    private Object chatImages_redirectRemoveLast(List<?> instance) {
        Object removed = instance.removeLast();
        if (removed instanceof GuiMessage.Line line) {
            UUID id = chatImages_getImageId(line.tag());
            if (id != null) {
                ImageChatStorage.remove(id);
            }
        }
        return removed;
    }

}
