package fr.lumavision.screen;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Per-wall display properties stored on the group origin block entity.
 * Applied at render time — never mutates {@link fr.lumavision.video.VideoSource}.
 */
public record ScreenDisplaySettings(
        int rotation,
        boolean mirrorH,
        boolean mirrorV,
        DisplayMode mode,
        float brightness,
        float contrast,
        float gamma,
        float colorTemp
) {

    public static final ScreenDisplaySettings DEFAULT = new ScreenDisplaySettings(
            0, false, false, DisplayMode.STRETCH, 1.0F, 1.0F, 1.0F, 0.0F
    );

    public ScreenDisplaySettings {
        rotation = normalizeRotation(rotation);
        brightness = clamp(brightness, 0.0F, 2.0F);
        contrast = clamp(contrast, 0.0F, 2.0F);
        gamma = clamp(gamma, 0.5F, 2.5F);
        colorTemp = clamp(colorTemp, -1.0F, 1.0F);
        if (mode == null) {
            mode = DisplayMode.STRETCH;
        }
    }

    public int rotationIndex() {
        return rotation / 90;
    }

    public ScreenDisplaySettings withRotation(int degrees) {
        return new ScreenDisplaySettings(degrees, mirrorH, mirrorV, mode, brightness, contrast, gamma, colorTemp);
    }

    public ScreenDisplaySettings withMirrorH(boolean value) {
        return new ScreenDisplaySettings(rotation, value, mirrorV, mode, brightness, contrast, gamma, colorTemp);
    }

    public ScreenDisplaySettings withMirrorV(boolean value) {
        return new ScreenDisplaySettings(rotation, mirrorH, value, mode, brightness, contrast, gamma, colorTemp);
    }

    public ScreenDisplaySettings withMode(DisplayMode value) {
        return new ScreenDisplaySettings(rotation, mirrorH, mirrorV, value, brightness, contrast, gamma, colorTemp);
    }

    public ScreenDisplaySettings withBrightness(float value) {
        return new ScreenDisplaySettings(rotation, mirrorH, mirrorV, mode, value, contrast, gamma, colorTemp);
    }

    public ScreenDisplaySettings withContrast(float value) {
        return new ScreenDisplaySettings(rotation, mirrorH, mirrorV, mode, brightness, value, gamma, colorTemp);
    }

    public ScreenDisplaySettings withGamma(float value) {
        return new ScreenDisplaySettings(rotation, mirrorH, mirrorV, mode, brightness, contrast, value, colorTemp);
    }

    public ScreenDisplaySettings withColorTemp(float value) {
        return new ScreenDisplaySettings(rotation, mirrorH, mirrorV, mode, brightness, contrast, gamma, value);
    }

    public boolean needsTextureColorGrading() {
        return contrast != 1.0F || gamma != 1.0F;
    }

    public String textureColorGradingKey() {
        if (!needsTextureColorGrading()) {
            return "";
        }
        return contrast + "|" + gamma;
    }

    public String cacheKey() {
        return rotation + "|" + mirrorH + "|" + mirrorV + "|" + mode.name() + "|"
                + brightness + "|" + contrast + "|" + gamma + "|" + colorTemp;
    }

    public void write(CompoundTag tag) {
        tag.putInt("DisplayRotation", rotation);
        tag.putBoolean("DisplayMirrorH", mirrorH);
        tag.putBoolean("DisplayMirrorV", mirrorV);
        tag.putString("DisplayMode", mode.name());
        tag.putFloat("DisplayBrightness", brightness);
        tag.putFloat("DisplayContrast", contrast);
        tag.putFloat("DisplayGamma", gamma);
        tag.putFloat("DisplayColorTemp", colorTemp);
    }

    public static ScreenDisplaySettings read(CompoundTag tag) {
        if (!tag.contains("DisplayMode")) {
            return DEFAULT;
        }
        DisplayMode mode;
        try {
            mode = DisplayMode.valueOf(tag.getString("DisplayMode"));
        } catch (IllegalArgumentException ignored) {
            mode = DisplayMode.STRETCH;
        }
        return new ScreenDisplaySettings(
                tag.getInt("DisplayRotation"),
                tag.getBoolean("DisplayMirrorH"),
                tag.getBoolean("DisplayMirrorV"),
                mode,
                tag.getFloat("DisplayBrightness"),
                tag.getFloat("DisplayContrast"),
                tag.getFloat("DisplayGamma"),
                tag.getFloat("DisplayColorTemp")
        );
    }

    public static void encode(ScreenDisplaySettings settings, FriendlyByteBuf buffer) {
        buffer.writeVarInt(settings.rotation);
        buffer.writeBoolean(settings.mirrorH);
        buffer.writeBoolean(settings.mirrorV);
        buffer.writeEnum(settings.mode);
        buffer.writeFloat(settings.brightness);
        buffer.writeFloat(settings.contrast);
        buffer.writeFloat(settings.gamma);
        buffer.writeFloat(settings.colorTemp);
    }

    public static ScreenDisplaySettings decode(FriendlyByteBuf buffer) {
        return new ScreenDisplaySettings(
                buffer.readVarInt(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readEnum(DisplayMode.class),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat()
        );
    }

    private static int normalizeRotation(int degrees) {
        int normalized = ((degrees % 360) + 360) % 360;
        return switch (normalized) {
            case 90 -> 90;
            case 180 -> 180;
            case 270 -> 270;
            default -> 0;
        };
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
