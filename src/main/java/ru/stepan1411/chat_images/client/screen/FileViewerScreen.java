package ru.stepan1411.chat_images.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import ru.stepan1411.chat_images.Chat_images;
import ru.stepan1411.chat_images.client.FileChatStorage;
import ru.stepan1411.chat_images.client.FileUtil;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class FileViewerScreen extends OverlayScreen {

    private final UUID fileId;
    private final FileChatStorage.FileEntry entry;

    public FileViewerScreen(Screen parent, UUID fileId) {
        super(Component.literal("File Viewer"), parent);
        this.fileId = fileId;
        this.entry = FileChatStorage.get(fileId);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isHovered) {
        if (event.button() == 1) {
            onClose();
            return true;
        }

        int bx = width / 2 - 60;
        int by = height / 2 + 30;
        int bw = 120;
        int bh = 20;

        if (event.button() == 0 && event.x() >= bx && event.x() <= bx + bw && event.y() >= by && event.y() <= by + bh) {
            openExternally();
            return true;
        }

        return super.mouseClicked(event, isHovered);
    }

    private void openExternally() {
        if (entry == null) return;
        try {
            String ext = FileUtil.getExtension(entry.fileName());
            Path tempDir = Files.createTempDirectory("chatimages_");
            File tempFile = tempDir.resolve(entry.fileName()).toFile();
            tempFile.deleteOnExit();
            tempDir.toFile().deleteOnExit();
            Files.write(tempFile.toPath(), entry.data());
            Desktop.getDesktop().open(tempFile);
        } catch (IOException e) {
            Chat_images.LOGGER.error("Failed to open file externally", e);
        }
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

        if (entry == null) {
            guiGraphics.drawString(font, Component.literal("File not found"),
                    width / 2 - font.width("File not found") / 2, height / 2, 0xFFFF5555);
            return;
        }

        int cx = width / 2;
        int cy = height / 2 - 30;

        guiGraphics.drawString(font, Component.literal("§l" + entry.fileName()), cx - font.width(entry.fileName()) / 2, cy, 0xFFFFFFFF);
        guiGraphics.drawString(font, Component.literal("Sender: " + entry.senderName()), cx - font.width("Sender: " + entry.senderName()) / 2, cy + 15, 0xFFAAAAAA);
        guiGraphics.drawString(font, Component.literal("Size: " + FileUtil.formatFileSize(entry.data().length)), cx - font.width("Size: " + FileUtil.formatFileSize(entry.data().length)) / 2, cy + 30, 0xFFAAAAAA);

        int bx = cx - 60;
        int by = cy + 60;
        boolean hovered = mouseX >= bx && mouseX <= bx + 120 && mouseY >= by && mouseY <= by + 20;
        guiGraphics.fill(bx, by, bx + 120, by + 20, hovered ? 0xFF555555 : 0xFF333333);
        guiGraphics.drawString(font, Component.literal("Open externally"), cx - font.width("Open externally") / 2, by + 6, 0xFFFFFFFF);
    }
}
