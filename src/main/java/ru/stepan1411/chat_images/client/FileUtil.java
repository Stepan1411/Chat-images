package ru.stepan1411.chat_images.client;

import ru.stepan1411.chat_images.Chat_images;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public final class FileUtil {

    public static final List<String> IMAGE_EXTENSIONS = Arrays.asList("png", "jpg", "jpeg", "webp", "jfif", "pjpeg", "gif");
    public static final List<String> VIDEO_EXTENSIONS = Arrays.asList("mkv", "mov");
    public static final List<String> AUDIO_EXTENSIONS = Arrays.asList("mp3");

    private FileUtil() {
    }

    public static String getExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot >= fileName.length() - 1) return "";
        return fileName.substring(dot + 1).toLowerCase();
    }

    public static boolean isImage(String ext) {
        return IMAGE_EXTENSIONS.contains(ext);
    }

    public static boolean isVideo(String ext) {
        return VIDEO_EXTENSIONS.contains(ext);
    }

    public static boolean isAudio(String ext) {
        return AUDIO_EXTENSIONS.contains(ext);
    }

    public static String fileNameFromPath(String path) {
        int sep = Math.max(path.lastIndexOf('\\'), path.lastIndexOf('/'));
        return sep >= 0 ? path.substring(sep + 1) : path;
    }

    public static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    public static byte[] convertToPng(byte[] input) {
        if (input.length < 8) return null;
        if (input[0] == (byte) 0x89 && input[1] == (byte) 0x50 && input[2] == (byte) 0x4E && input[3] == (byte) 0x47) {
            return input;
        }
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(input));
            if (image == null) return null;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            Chat_images.LOGGER.error("Failed to convert image to PNG", e);
            return null;
        }
    }
}
