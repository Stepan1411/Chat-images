package ru.stepan1411.chat_images.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.stepan1411.chat_images.Chat_images;
import ru.stepan1411.chat_images.client.Chat_imagesClient;
import ru.stepan1411.chat_images.client.ClickableImageTracker;
import ru.stepan1411.chat_images.client.FileChatStorage;
import ru.stepan1411.chat_images.client.FileUtil;
import ru.stepan1411.chat_images.client.screen.FileViewerScreen;
import ru.stepan1411.chat_images.client.screen.FullScreenImageViewerScreen;
import ru.stepan1411.chat_images.client.screen.ImageConfirmScreen;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.lwjgl.system.MemoryStack;

@Mixin(ChatScreen.class)
abstract class ChatScreenMixin extends Screen {

    @Shadow
    protected EditBox input;

    @Unique
    private WidgetSprites chatImages_getButtonSprites() {
        if (Chat_imagesClient.serverHasMod) {
            return new WidgetSprites(
                    Identifier.fromNamespaceAndPath(Chat_images.MOD_ID, "chat_button"),
                    Identifier.fromNamespaceAndPath(Chat_images.MOD_ID, "chat_button_hovered")
            );
        }
        Identifier error = Identifier.fromNamespaceAndPath(Chat_images.MOD_ID, "chat_button_error");
        return new WidgetSprites(error, error, error);
    }

    @Unique
    private ImageButton chatImages_button;

    @Unique
    private static Long chatImages_handCursor;

    private ChatScreenMixin() {
        super(null);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void chatImages_initTail(CallbackInfo ci) {
        int x = input.getX();
        int y = input.getY();
        chatImages_button = addRenderableWidget(new ImageButton(
                x - 3,
                y - 3,
                14,
                14,
                chatImages_getButtonSprites(),
                button -> openFileChooser()
        ));
        chatImages_button.setTooltip(Tooltip.create(Component.translatable(
                Chat_imagesClient.serverHasMod ? "chat_images.send_image" : "chat_images.server_not_supported"
        )));
        chatImages_button.active = Chat_imagesClient.serverHasMod;
        input.setWidth(input.getWidth() - 14);
        input.setX(x + 14);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void chatImages_renderHead(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        boolean onImage = false;
        for (var area : ClickableImageTracker.getClickable()) {
            if (mouseX >= area.x() && mouseX <= area.x() + area.w()
                    && mouseY >= area.y() && mouseY <= area.y() + area.h()) {
                onImage = true;
                break;
            }
        }
        long window = Minecraft.getInstance().getWindow().handle();
        if (onImage) {
            if (chatImages_handCursor == null) {
                chatImages_handCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_HAND_CURSOR);
            }
            GLFW.glfwSetCursor(window, chatImages_handCursor);
        } else {
            GLFW.glfwSetCursor(window, 0);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void chatImages_mouseClicked(MouseButtonEvent event, boolean isHovered, CallbackInfoReturnable<Boolean> cir) {
        if (event.button() == 0) {
            for (var area : ClickableImageTracker.getClickable()) {
                if (event.x() >= area.x() && event.x() <= area.x() + area.w()
                        && event.y() >= area.y() && event.y() <= area.y() + area.h()) {
                    Minecraft mc = Minecraft.getInstance();
                    if (FileChatStorage.get(area.imageId()) != null) {
                        mc.setScreen(new FileViewerScreen(mc.screen, area.imageId()));
                    } else {
                        mc.setScreen(new FullScreenImageViewerScreen(mc.screen, area.imageId()));
                    }
                    cir.setReturnValue(true);
                    return;
                }
            }
        }
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void chatImages_removed(CallbackInfo ci) {
        if (chatImages_handCursor != null) {
            GLFW.glfwSetCursor(Minecraft.getInstance().getWindow().handle(), 0);
        }
    }

    @Unique
    private void openFileChooser() {
        if (!Chat_imagesClient.serverHasMod) return;
        new Thread(() -> {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                PointerBuffer filterPatterns = stack.mallocPointer(3);
                filterPatterns.put(stack.UTF8("*.png;*.jpg;*.jpeg;*.webp;*.jfif;*.pjpeg;*.gif"));
                filterPatterns.put(stack.UTF8("*.mp3;*.mkv;*.mov"));
                filterPatterns.put(stack.UTF8("*.png;*.jpg;*.jpeg;*.webp;*.jfif;*.pjpeg;*.gif;*.mp3;*.mkv;*.mov"));
                filterPatterns.flip();
                String result = TinyFileDialogs.tinyfd_openFileDialog(
                        Component.translatable("chat_images.select_file").getString(),
                        null,
                        filterPatterns,
                        "Multimedia files",
                        false
                );
                if (result != null) {
                    String fileName = FileUtil.fileNameFromPath(result);
                    byte[] fileBytes = Files.readAllBytes(new File(result).toPath());
                    String ext = FileUtil.getExtension(fileName);

                    if (FileUtil.isImage(ext)) {
                        byte[] pngBytes = FileUtil.convertToPng(fileBytes);
                        if (pngBytes == null) {
                            minecraft.execute(() -> minecraft.player.displayClientMessage(Component.literal("§c[ChatImages] Unsupported image format"), true));
                            return;
                        }
                        final byte[] finalBytes = pngBytes;
                        minecraft.execute(() ->
                                minecraft.setScreen(new ImageConfirmScreen(this, finalBytes, fileName, true)));
                    } else {
                        minecraft.execute(() ->
                                minecraft.player.displayClientMessage(Component.literal("§e[ChatImages] Video/audio support is under development"), true));
                    }
                }
            } catch (IOException e) {
                Chat_images.LOGGER.error("Failed to read file", e);
            }
        }).start();
    }

}
