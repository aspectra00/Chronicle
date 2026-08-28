package com.aspectra00.chronicle.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class CustomToastBackground {
    private static final long MAX_FILE_BYTES = 8L * 1024L * 1024L;
    private static final long MAX_PIXELS = 4_194_304L;
    private static final int MAX_EDGE = 4_096;
    private static final int MAX_PATH_LENGTH = 1_024;
    private static final int MAX_CACHED_TEXTURES = 2;
    private static final Map<String, LoadedTexture> TEXTURES = new LinkedHashMap<>(8, 0.75f, true);
    private static String attemptedPath = "";
    private static String lastError;
    private static int textureGeneration;

    private CustomToastBackground() {
    }

    public static String validateFile(String rawPath) {
        try {
            inspect(normalize(rawPath));
            return null;
        } catch (ImageFailure failure) {
            return failure.getMessage();
        } catch (RuntimeException failure) {
            return ChronicleI18n.tr("error.image_load", failureDetail(failure));
        }
    }

    public static boolean prepare(Minecraft minecraft, String rawPath) {
        return prepare(minecraft, rawPath, false);
    }

    public static boolean prepare(Minecraft minecraft, String rawPath, boolean force) {
        String path;
        try {
            path = normalize(rawPath);
        } catch (ImageFailure failure) {
            attemptedPath = rawPath == null ? "" : rawPath;
            lastError = failure.getMessage();
            return false;
        }
        if (path.isEmpty()) {
            attemptedPath = "";
            lastError = null;
            return true;
        }
        LoadedTexture cached = TEXTURES.get(path);
        if (!force && cached != null) {
            attemptedPath = path;
            lastError = null;
            return true;
        }
        if (!force && path.equals(attemptedPath)) {
            return false;
        }
        attemptedPath = path;
        if (minecraft == null || minecraft.getTextureManager() == null) {
            lastError = ChronicleI18n.tr("error.image_load", "Minecraft");
            return false;
        }
        NativeImage image = null;
        DynamicTexture texture = null;
        try {
            ImageData data = inspect(path);
            image = NativeImage.read(data.bytes());
            if (image.getWidth() != data.width() || image.getHeight() != data.height()) {
                throw new ImageFailure(ChronicleI18n.tr("error.image_unsupported"));
            }
            texture = new DynamicTexture(() -> "Chronicle notification background", image);
            image = null;
            Identifier nextId = Identifier.fromNamespaceAndPath("chronicle",
                    "custom_toast_background/" + Integer.toUnsignedString(path.hashCode(), 16)
                            + "_" + Integer.toUnsignedString(textureGeneration++, 16));
            minecraft.getTextureManager().register(nextId, texture);
            texture = null;
            LoadedTexture previous = TEXTURES.put(path,
                    new LoadedTexture(nextId, data.width(), data.height()));
            if (previous != null) minecraft.getTextureManager().release(previous.id());
            trimCache(minecraft);
            lastError = null;
            return true;
        } catch (ImageFailure failure) {
            lastError = failure.getMessage();
            return false;
        } catch (IOException | RuntimeException failure) {
            lastError = ChronicleI18n.tr("error.image_load", failureDetail(failure));
            return false;
        } finally {
            if (texture != null) texture.close();
            if (image != null) image.close();
        }
    }

    public static boolean draw(GuiGraphics graphics, String rawPath,
                               int x, int y, int width, int height) {
        if (graphics == null || rawPath == null || rawPath.isBlank()
                || width <= 0 || height <= 0) {
            return false;
        }
        String path;
        try {
            path = normalize(rawPath);
        } catch (ImageFailure ignored) {
            return false;
        }
        LoadedTexture texture = TEXTURES.get(path);
        if (texture == null || texture.width() <= 0 || texture.height() <= 0) return false;
        float destinationAspect = width / (float) height;
        float sourceAspect = texture.width() / (float) texture.height();
        int sourceWidth = texture.width();
        int sourceHeight = texture.height();
        int sourceX = 0;
        int sourceY = 0;
        if (sourceAspect > destinationAspect) {
            sourceWidth = Math.max(1, Math.round(texture.height() * destinationAspect));
            sourceX = Math.max(0, (texture.width() - sourceWidth) / 2);
        } else if (sourceAspect < destinationAspect) {
            sourceHeight = Math.max(1, Math.round(texture.width() / destinationAspect));
            sourceY = Math.max(0, (texture.height() - sourceHeight) / 2);
        }
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture.id(),
                x, y, sourceX, sourceY, width, height,
                sourceWidth, sourceHeight, texture.width(), texture.height());
        return true;
    }

    public static String getLastError() {
        return lastError;
    }

    public static void retain(Minecraft minecraft, String rawPath) {
        String retained = "";
        try {
            retained = normalize(rawPath);
        } catch (ImageFailure ignored) {
        }
        Iterator<Map.Entry<String, LoadedTexture>> entries = TEXTURES.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<String, LoadedTexture> entry = entries.next();
            if (!entry.getKey().equals(retained)) {
                if (minecraft != null && minecraft.getTextureManager() != null) {
                    minecraft.getTextureManager().release(entry.getValue().id());
                }
                entries.remove();
            }
        }
        attemptedPath = retained;
        lastError = null;
    }

    public static void close(Minecraft minecraft) {
        if (minecraft != null && minecraft.getTextureManager() != null) {
            for (LoadedTexture texture : TEXTURES.values()) {
                minecraft.getTextureManager().release(texture.id());
            }
        }
        TEXTURES.clear();
        attemptedPath = "";
        lastError = null;
    }

    private static void trimCache(Minecraft minecraft) {
        Iterator<Map.Entry<String, LoadedTexture>> entries = TEXTURES.entrySet().iterator();
        while (TEXTURES.size() > MAX_CACHED_TEXTURES && entries.hasNext()) {
            LoadedTexture texture = entries.next().getValue();
            minecraft.getTextureManager().release(texture.id());
            entries.remove();
        }
    }

    private static ImageData inspect(String pathText) throws ImageFailure {
        if (pathText.isEmpty()) throw new ImageFailure(ChronicleI18n.tr("error.image_missing"));
        Path path = Path.of(pathText);
        String name = path.getFileName() == null
                ? "" : path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (!(name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg"))) {
            throw new ImageFailure(ChronicleI18n.tr("error.image_unsupported"));
        }
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new ImageFailure(ChronicleI18n.tr("error.image_missing"));
        }
        byte[] bytes;
        try (InputStream input = Files.newInputStream(path)) {
            bytes = input.readNBytes((int) MAX_FILE_BYTES + 1);
        } catch (IOException failure) {
            throw new ImageFailure(ChronicleI18n.tr("error.image_load", failureDetail(failure)));
        }
        if (bytes.length > MAX_FILE_BYTES) {
            throw new ImageFailure(ChronicleI18n.tr("error.image_too_large"));
        }
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (input == null) throw new ImageFailure(ChronicleI18n.tr("error.image_unsupported"));
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new ImageFailure(ChronicleI18n.tr("error.image_unsupported"));
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width > MAX_EDGE || height > MAX_EDGE
                        || (long) width * height > MAX_PIXELS) {
                    throw new ImageFailure(ChronicleI18n.tr("error.image_dimensions"));
                }
                return new ImageData(bytes, width, height);
            } finally {
                reader.dispose();
            }
        } catch (ImageFailure failure) {
            throw failure;
        } catch (IOException | RuntimeException failure) {
            throw new ImageFailure(ChronicleI18n.tr("error.image_unsupported"));
        }
    }

    private static String normalize(String rawPath) throws ImageFailure {
        if (rawPath == null || rawPath.isBlank()) return "";
        try {
            String normalized = Path.of(rawPath.trim()).toAbsolutePath().normalize().toString();
            if (normalized.length() > MAX_PATH_LENGTH) {
                throw new ImageFailure(ChronicleI18n.tr("error.image_path_too_long"));
            }
            return normalized;
        } catch (ImageFailure failure) {
            throw failure;
        } catch (RuntimeException failure) {
            throw new ImageFailure(ChronicleI18n.tr("error.image_missing"));
        }
    }

    private static String failureDetail(Throwable failure) {
        String detail = failure == null ? null : failure.getMessage();
        return detail == null || detail.isBlank()
                ? failure == null ? "Unknown" : failure.getClass().getSimpleName()
                : detail;
    }

    private record ImageData(byte[] bytes, int width, int height) {
    }

    private record LoadedTexture(Identifier id, int width, int height) {
    }

    private static final class ImageFailure extends Exception {
        private static final long serialVersionUID = 1L;

        private ImageFailure(String message) {
            super(message == null || message.isBlank()
                    ? ChronicleI18n.tr("error.image_unsupported") : message);
        }
    }
}
