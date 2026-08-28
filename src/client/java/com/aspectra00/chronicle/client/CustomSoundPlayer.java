package com.aspectra00.chronicle.client;

import com.aspectra00.chronicle.client.config.ReminderConfig;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.JOrbisAudioStream;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public final class CustomSoundPlayer {
    private static final long MAX_FILE_BYTES = 16L * 1024L * 1024L;
    private static final long MAX_DURATION_MICROS = 30_000_000L;
    private static final Set<String> SUPPORTED = Set.of(
            "mp3", "ogg", "wav", "wave", "aif", "aiff", "aifc", "au", "snd");
    private static final AtomicBoolean WORKER_RUNNING = new AtomicBoolean();
    private static final AtomicLong REQUEST_GENERATION = new AtomicLong();
    private static final AtomicReference<SoundRequest> PENDING_REQUEST = new AtomicReference<>();
    private static final Object CLIP_LOCK = new Object();
    private static final ExecutorService AUDIO_WORKER = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "Chronicle custom sound");
        thread.setDaemon(true);
        return thread;
    });

    private static Clip cachedClip;
    private static Path cachedPath;
    private static long cachedModified;
    private static long cachedSize;
    private static volatile String lastError;
    private static volatile boolean shuttingDown;

    private record SoundRequest(Path path, float volume, long generation) {}

    private CustomSoundPlayer() {}

    public static void playConfigured(Minecraft client, ReminderConfig config) {
        if (config == null) return;
        String mode = config.notificationSoundMode == null ? "VANILLA" : config.notificationSoundMode;
        switch (mode) {
            case "OFF" -> stopCustom();
            case "CUSTOM" -> playCustom(client, config.customSoundPath, config.notificationSoundVolume);
            default -> playVanilla(client, config.notificationSoundVolume);
        }
    }

    public static void playVanilla(Minecraft client, float volume) {
        if (client == null) return;
        cancelCustom(true);
        client.getSoundManager().play(SimpleSoundInstance.forUI(
                SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, clampVolume(volume)));
    }

    public static void playCustom(Minecraft client, String configuredPath, float volume) {
        lastError = null;
        if (shuttingDown) return;
        if (configuredPath == null || configuredPath.isBlank()) {
            cancelCustom(false);
            lastError = ChronicleI18n.tr("sound.error.no_file");
            return;
        }
        final Path path;
        try {
            path = Path.of(configuredPath).toAbsolutePath().normalize();
        } catch (RuntimeException ex) {
            cancelCustom(false);
            lastError = ChronicleI18n.tr("sound.error.invalid_path");
            return;
        }
        float effectiveVolume = clampVolume(volume);
        if (client != null && client.options != null) {
            effectiveVolume *= clampVolume(client.options.getSoundSourceVolume(SoundSource.MASTER));
        }
        if (effectiveVolume <= 0.0001f) {
            cancelCustom(false);
            return;
        }
        long requestGeneration = REQUEST_GENERATION.incrementAndGet();
        stopCachedClip(false);
        PENDING_REQUEST.set(new SoundRequest(path, effectiveVolume, requestGeneration));
        ensureWorkerRunning();
    }

    public static String getLastError() {
        return lastError;
    }

    public static void clearLastError() {
        cancelCustom(false);
    }

    public static void stopCustom() {
        cancelCustom(true);
    }

    public static void shutdown() {
        shuttingDown = true;
        REQUEST_GENERATION.incrementAndGet();
        PENDING_REQUEST.set(null);
        stopCachedClip(true);
        AUDIO_WORKER.shutdownNow();
    }

    public static String validateCustomFile(String configuredPath) {
        if (configuredPath == null || configuredPath.isBlank()) {
            return ChronicleI18n.tr("sound.error.no_file");
        }
        try {
            Path path = Path.of(configuredPath).toAbsolutePath().normalize();
            if (!Files.isRegularFile(path)) return ChronicleI18n.tr("sound.error.not_found");
            if (Files.size(path) > MAX_FILE_BYTES) return ChronicleI18n.tr("sound.error.too_large");
            String name = path.getFileName().toString();
            int dot = name.lastIndexOf('.');
            String extension = dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
            return SUPPORTED.contains(extension) ? null : ChronicleI18n.tr("sound.error.unsupported");
        } catch (Exception ex) {
            return ChronicleI18n.tr("sound.error.invalid_path");
        }
    }

    public static String supportedFormats() {
        return "MP3, OGG, WAV, AIFF/AIF/AIFC, AU/SND";
    }

    public static String[] supportedFilePatterns() {
        return new String[] {
                "*.mp3", "*.ogg", "*.wav", "*.wave", "*.aif", "*.aiff", "*.aifc", "*.au", "*.snd"
        };
    }

    private static void ensureWorkerRunning() {
        if (shuttingDown || !WORKER_RUNNING.compareAndSet(false, true)) return;
        try {
            AUDIO_WORKER.execute(CustomSoundPlayer::drainPendingRequests);
        } catch (RuntimeException schedulingFailure) {
            WORKER_RUNNING.set(false);
            SoundRequest pending = PENDING_REQUEST.get();
            if (pending != null && REQUEST_GENERATION.get() == pending.generation()) {
                lastError = ChronicleI18n.tr("sound.error.could_not_play",
                        schedulingFailure.getClass().getSimpleName());
            }
        }
    }

    private static void drainPendingRequests() {
        try {
            SoundRequest request;
            while (!shuttingDown && (request = PENDING_REQUEST.getAndSet(null)) != null) {
                try {
                    playOnWorker(request.path(), request.volume(), request.generation());
                    if (REQUEST_GENERATION.get() == request.generation()) lastError = null;
                } catch (Exception ex) {
                    if (REQUEST_GENERATION.get() == request.generation()) {
                        String detail = ex.getMessage();
                        lastError = ChronicleI18n.tr("sound.error.could_not_play",
                                detail == null || detail.isBlank()
                                        ? ex.getClass().getSimpleName() : detail);
                    }
                }
            }
        } finally {
            WORKER_RUNNING.set(false);
            if (!shuttingDown && PENDING_REQUEST.get() != null) ensureWorkerRunning();
        }
    }

    private static void playOnWorker(Path path, float volume, long requestGeneration) throws Exception {
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException(ChronicleI18n.tr("sound.error.not_found"));
        }
        long fileSize = Files.size(path);
        if (fileSize > MAX_FILE_BYTES) {
            throw new IllegalArgumentException(ChronicleI18n.tr("sound.error.too_large"));
        }
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String extension = dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!SUPPORTED.contains(extension)) {
            throw new IllegalArgumentException(ChronicleI18n.tr("sound.error.unsupported"));
        }

        if (REQUEST_GENERATION.get() != requestGeneration) return;
        long modified = Files.getLastModifiedTime(path).toMillis();
        synchronized (CLIP_LOCK) {
            if (cachedClip != null && cachedClip.isOpen() && path.equals(cachedPath)
                    && modified == cachedModified && fileSize == cachedSize) {
                if (REQUEST_GENERATION.get() == requestGeneration) {
                    restartClipLocked(cachedClip, volume);
                }
                return;
            }
        }

        Clip decoded = switch (extension) {
            case "mp3" -> loadMp3(path);
            case "ogg" -> loadOgg(path);
            default -> loadJavaSound(path);
        };
        if (decoded.getMicrosecondLength() > MAX_DURATION_MICROS) {
            decoded.close();
            throw new IllegalArgumentException(ChronicleI18n.tr("sound.error.too_long"));
        }
        if (REQUEST_GENERATION.get() != requestGeneration) {
            decoded.close();
            return;
        }
        synchronized (CLIP_LOCK) {
            if (REQUEST_GENERATION.get() != requestGeneration) {
                decoded.close();
                return;
            }
            closeCachedClipLocked();
            cachedClip = decoded;
            cachedPath = path;
            cachedModified = modified;
            cachedSize = fileSize;
            restartClipLocked(decoded, volume);
        }
    }

    private static Clip loadJavaSound(Path path) throws Exception {
        try (AudioInputStream source = AudioSystem.getAudioInputStream(path.toFile())) {
            AudioFormat sourceFormat = source.getFormat();
            validateFormat(sourceFormat);
            if (source.getFrameLength() > 0
                    && source.getFrameLength() / sourceFormat.getSampleRate() > 30.0) {
                throw new IllegalArgumentException(ChronicleI18n.tr("sound.error.too_long"));
            }
            int channels = sourceFormat.getChannels();
            float sampleRate = sourceFormat.getSampleRate();
            AudioFormat target = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                    sampleRate, 16, channels, channels * 2, sampleRate, false);
            try (AudioInputStream decoded = AudioSystem.getAudioInputStream(target, source)) {
                Clip clip = AudioSystem.getClip();
                try {
                    clip.open(decoded);
                    return clip;
                } catch (Exception ex) {
                    clip.close();
                    throw ex;
                }
            }
        }
    }

    private static Clip loadOgg(Path path) throws Exception {
        try (InputStream input = Files.newInputStream(path);
             JOrbisAudioStream stream = new JOrbisAudioStream(input)) {
            AudioFormat format = stream.getFormat();
            validateFormat(format);
            int frameSize = Math.max(1, format.getFrameSize());
            long allowed = Math.round(format.getSampleRate() * frameSize * 30.0);
            int maxBytes = (int) Math.min(Integer.MAX_VALUE - frameSize, allowed);
            ByteBuffer decoded = stream.read(maxBytes + frameSize);
            if (decoded.remaining() > maxBytes) {
                throw new IllegalArgumentException(ChronicleI18n.tr("sound.error.too_long"));
            }
            byte[] pcm = new byte[decoded.remaining()];
            decoded.get(pcm);
            Clip clip = AudioSystem.getClip();
            try {
                clip.open(format, pcm, 0, pcm.length);
                return clip;
            } catch (Exception ex) {
                clip.close();
                throw ex;
            }
        }
    }

    private static Clip loadMp3(Path path) throws Exception {
        try (InputStream input = Files.newInputStream(path);
             ByteArrayOutputStream pcm = new ByteArrayOutputStream()) {
            Bitstream bitstream = new Bitstream(input);
            Decoder decoder = new Decoder();
            AudioFormat format = null;
            long maxDecodedBytes = Long.MAX_VALUE;
            try {
                Header header;
                while ((header = bitstream.readFrame()) != null) {
                    try {
                        SampleBuffer samples = (SampleBuffer) decoder.decodeFrame(header, bitstream);
                        int channels = samples.getChannelCount();
                        float sampleRate = samples.getSampleFrequency();
                        AudioFormat frameFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                                sampleRate, 16, channels, channels * 2, sampleRate, false);
                        validateFormat(frameFormat);
                        if (format == null) {
                            format = frameFormat;
                            maxDecodedBytes = Math.min(Integer.MAX_VALUE - frameFormat.getFrameSize(),
                                    Math.round(frameFormat.getSampleRate()
                                            * frameFormat.getFrameSize() * 30.0));
                        } else if (format.getChannels() != channels
                                || format.getSampleRate() != sampleRate) {
                            throw new IllegalArgumentException(
                                    ChronicleI18n.tr("sound.error.invalid_audio"));
                        }

                        short[] buffer = samples.getBuffer();
                        int length = samples.getBufferLength();
                        if (pcm.size() + length * 2L > maxDecodedBytes) {
                            throw new IllegalArgumentException(
                                    ChronicleI18n.tr("sound.error.too_long"));
                        }
                        for (int i = 0; i < length; i++) {
                            int sample = buffer[i];
                            pcm.write(sample & 0xFF);
                            pcm.write((sample >>> 8) & 0xFF);
                        }
                    } finally {
                        bitstream.closeFrame();
                    }
                }
            } finally {
                bitstream.close();
            }

            if (format == null || pcm.size() == 0) {
                throw new IllegalArgumentException(ChronicleI18n.tr("sound.error.invalid_audio"));
            }
            byte[] decoded = pcm.toByteArray();
            Clip clip = AudioSystem.getClip();
            try {
                clip.open(format, decoded, 0, decoded.length);
                return clip;
            } catch (Exception ex) {
                clip.close();
                throw ex;
            }
        }
    }

    private static void validateFormat(AudioFormat format) {
        int channels = format.getChannels();
        float sampleRate = format.getSampleRate();
        if (channels < 1 || channels > 2 || sampleRate <= 0.0f || sampleRate > 192_000.0f
                || !Float.isFinite(sampleRate)) {
            throw new IllegalArgumentException(ChronicleI18n.tr("sound.error.invalid_audio"));
        }
    }

    private static void applyVolume(Clip clip, float volume) {
        if (!clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) return;
        FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        float decibels = volume <= 0.0001f ? gain.getMinimum() : (float) (20.0 * Math.log10(volume));
        gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), decibels)));
    }

    private static void restartClipLocked(Clip clip, float volume) {
        if (clip.isRunning()) clip.stop();
        clip.setFramePosition(0);
        applyVolume(clip, volume);
        clip.start();
    }

    private static void cancelCustom(boolean closeCached) {
        REQUEST_GENERATION.incrementAndGet();
        PENDING_REQUEST.set(null);
        stopCachedClip(closeCached);
        lastError = null;
    }

    private static void stopCachedClip(boolean close) {
        synchronized (CLIP_LOCK) {
            if (cachedClip != null && cachedClip.isRunning()) cachedClip.stop();
            if (close) closeCachedClipLocked();
        }
    }

    private static void closeCachedClipLocked() {
        if (cachedClip != null) {
            cachedClip.stop();
            cachedClip.close();
        }
        cachedClip = null;
        cachedPath = null;
        cachedModified = 0L;
        cachedSize = 0L;
    }

    private static float clampVolume(float volume) {
        if (!Float.isFinite(volume)) return 0.75f;
        return Math.max(0.0f, Math.min(1.0f, volume));
    }
}
