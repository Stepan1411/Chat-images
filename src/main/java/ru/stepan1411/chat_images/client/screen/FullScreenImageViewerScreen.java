package ru.stepan1411.chat_images.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import ru.stepan1411.chat_images.client.ImageChatStorage;

import java.util.UUID;

public class FullScreenImageViewerScreen extends OverlayScreen {

    private final UUID imageId;
    private double zoom = 1.0;
    private double offsetX = 0;
    private double offsetY = 0;
    private boolean dragging = false;
    private double dragStartX;
    private double dragStartY;
    private double dragOffsetX;
    private double dragOffsetY;
    private long handCursor;
    private boolean cursorIsHand;

    public FullScreenImageViewerScreen(Screen parent, UUID imageId) {
        super(Component.translatable("chat_images.viewer"), parent);
        this.imageId = imageId;
        this.handCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_POINTING_HAND_CURSOR);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isHovered) {
        if (event.button() == 0) {
            dragging = true;
            dragStartX = event.x();
            dragStartY = event.y();
            dragOffsetX = offsetX;
            dragOffsetY = offsetY;
            return true;
        }
        if (event.button() == 1) {
            onClose();
            return true;
        }
        return super.mouseClicked(event, isHovered);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragging = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (dragging && event.button() == 0) {
            offsetX = dragOffsetX + (event.x() - dragStartX);
            offsetY = dragOffsetY + (event.y() - dragStartY);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        double factor = scrollY > 0 ? 1.1 : 1.0 / 1.1;
        double newZoom = Math.max(0.025, Math.min(zoom * factor, 40.0));
        if (newZoom == zoom) return true;

        var nativeImage = ImageChatStorage.getTexture(imageId).getPixels();
        if (nativeImage == null) return true;

        int texWidth = nativeImage.getWidth();
        int texHeight = nativeImage.getHeight();
        double fitScale = Math.min((double) width / texWidth, (double) height / texHeight) * 0.85;
        int cx = width / 2;
        int cy = height / 2;

        double fracX = (mouseX - (cx - texWidth * fitScale * zoom / 2 + offsetX)) / (texWidth * fitScale * zoom);
        double fracY = (mouseY - (cy - texHeight * fitScale * zoom / 2 + offsetY)) / (texHeight * fitScale * zoom);

        zoom = newZoom;

        offsetX = mouseX - cx + texWidth * fitScale * zoom * (0.5 - fracX);
        offsetY = mouseY - cy + texHeight * fitScale * zoom * (0.5 - fracY);

        return true;
    }

    @Override
    public void onClose() {
        if (cursorIsHand) {
            GLFW.glfwSetCursor(Minecraft.getInstance().getWindow().handle(), 0);
            cursorIsHand = false;
        }
        if (handCursor != 0) {
            GLFW.glfwDestroyCursor(handCursor);
            handCursor = 0;
        }
        super.onClose();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    protected void actualRender(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        super.actualRender(guiGraphics, mouseX, mouseY, delta);

        Identifier texId = ImageChatStorage.getTextureId(imageId);
        if (texId == null) {
            guiGraphics.drawString(font, Component.translatable("chat_images.image_removed"),
                    width / 2 - font.width(Component.translatable("chat_images.image_removed")) / 2,
                    height / 2, 0xFFFF5555);
            return;
        }

        var nativeImage = ImageChatStorage.getTexture(imageId).getPixels();
        if (nativeImage == null) {
            guiGraphics.drawString(font, Component.translatable("chat_images.image_removed"),
                    width / 2 - font.width(Component.translatable("chat_images.image_removed")) / 2,
                    height / 2, 0xFFFF5555);
            return;
        }

        int texWidth = nativeImage.getWidth();
        int texHeight = nativeImage.getHeight();

        double fitScale = Math.min((double) width / texWidth, (double) height / texHeight) * 0.85;

        int imgW = (int) (texWidth * fitScale * zoom);
        int imgH = (int) (texHeight * fitScale * zoom);

        int centerX = width / 2;
        int centerY = height / 2;

        int x = centerX - imgW / 2 + (int) offsetX;
        int y = centerY - imgH / 2 + (int) offsetY;

        guiGraphics.blit(texId, x, y, x + imgW, y + imgH, 0.0f, 1.0f, 0.0f, 1.0f);

        String info = texWidth + "x" + texHeight + "  " + String.format("%.0f", zoom * 100) + "%";
        guiGraphics.drawString(font, info, 10, height - 20, 0xFFFFFFFF);

        boolean overImage = mouseX >= x && mouseX <= x + imgW && mouseY >= y && mouseY <= y + imgH;
        boolean wantHand = overImage || dragging;
        if (wantHand != cursorIsHand) {
            cursorIsHand = wantHand;
            long window = Minecraft.getInstance().getWindow().handle();
            GLFW.glfwSetCursor(window, wantHand ? handCursor : 0);
        }
    }

}
