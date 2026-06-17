package ru.stepan1411.chat_images.client;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FileChatStorage {

    public record FileEntry(byte[] data, String fileName, String senderName) {}

    private static final Map<UUID, FileEntry> FILES = new ConcurrentHashMap<>();

    public static UUID store(byte[] data, String fileName, String senderName) {
        UUID uuid = UUID.randomUUID();
        FILES.put(uuid, new FileEntry(data, fileName, senderName));
        return uuid;
    }

    public static FileEntry get(UUID id) {
        return FILES.get(id);
    }

    public static void remove(UUID id) {
        FILES.remove(id);
    }

    public static void clear() {
        FILES.clear();
    }
}
