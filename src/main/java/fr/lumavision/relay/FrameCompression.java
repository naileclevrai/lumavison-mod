package fr.lumavision.relay;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * Zlib compression for relayed RGBA frame payloads.
 */
public final class FrameCompression {

    private FrameCompression() {
    }

    public static byte[] compress(byte[] rgba) {
        Deflater deflater = new Deflater(Deflater.BEST_SPEED);
        deflater.setInput(rgba);
        deflater.finish();
        byte[] buffer = new byte[Math.max(256, rgba.length / 4)];
        ByteArrayOutputStream out = new ByteArrayOutputStream(rgba.length / 2);
        try {
            while (!deflater.finished()) {
                int count = deflater.deflate(buffer);
                if (count > 0) {
                    out.write(buffer, 0, count);
                }
            }
            return out.toByteArray();
        } finally {
            deflater.end();
        }
    }

    public static byte[] decompress(byte[] compressed, int expectedSize) {
        try (InflaterInputStream inflater = new InflaterInputStream(new ByteArrayInputStream(compressed))) {
            byte[] out = inflater.readAllBytes();
            if (out.length != expectedSize) {
                throw new IllegalStateException("Decompressed size mismatch: expected " + expectedSize + ", got " + out.length);
            }
            return out;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decompress relay frame", e);
        }
    }
}
