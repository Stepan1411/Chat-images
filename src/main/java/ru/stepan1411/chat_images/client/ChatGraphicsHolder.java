package ru.stepan1411.chat_images.client;

import net.minecraft.client.gui.GuiGraphics;

public final class ChatGraphicsHolder {
    private static GuiGraphics current;

    public static void set(GuiGraphics g) {
        current = g;
    }

    public static GuiGraphics get() {
        return current;
    }

    private ChatGraphicsHolder() {}
}
