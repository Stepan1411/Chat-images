package ru.stepan1411.chat_images.client;

import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

public final class ClickableImageTracker {

    public record ClickableImage(UUID imageId, int x, int y, int w, int h) {}

    private static final List<ClickableImage> CLICKABLE = new ArrayList<>();

    public static void clear() {
        CLICKABLE.clear();
    }

    public static void add(UUID imageId, int x, int y, int w, int h) {
        CLICKABLE.add(new ClickableImage(imageId, x, y, w, h));
    }

    public static List<ClickableImage> getClickable() {
        return CLICKABLE;
    }
}
