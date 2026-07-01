package fr.lumavision.video;

import com.mojang.blaze3d.platform.NativeImage;
import org.lwjgl.system.MemoryUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.IntBuffer;

/**
 * Fast path for copying an entire {@link VideoFrame} into Minecraft's native image memory.
 */
final class NativeImageAccess {

    private static final Field PIXELS_FIELD = findPixelsField();

    private NativeImageAccess() {
    }

    static boolean copyNativeRgbaTo(NativeImage target, int[] nativeRgbaPixels) {
        if (PIXELS_FIELD == null) {
            return false;
        }
        try {
            long address = PIXELS_FIELD.getLong(target);
            if (address == 0L) {
                return false;
            }
            IntBuffer buffer = MemoryUtil.memIntBuffer(address, nativeRgbaPixels.length);
            buffer.put(nativeRgbaPixels, 0, nativeRgbaPixels.length);
            return true;
        } catch (IllegalAccessException | RuntimeException ignored) {
            return false;
        }
    }

    private static Field findPixelsField() {
        try {
            Field field = NativeImage.class.getDeclaredField("pixels");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Production Forge may expose obfuscated private field names. In NativeImage,
            // the native pixel pointer is the only non-final long field.
            for (Field field : NativeImage.class.getDeclaredFields()) {
                int modifiers = field.getModifiers();
                if (field.getType() == long.class && !Modifier.isFinal(modifiers)) {
                    try {
                        field.setAccessible(true);
                        return field;
                    } catch (RuntimeException ignoredAgain) {
                        return null;
                    }
                }
            }
            return null;
        }
    }
}
