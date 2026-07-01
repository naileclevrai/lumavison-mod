package fr.lumavision.client.video;

import fr.lumavision.video.VideoFrame;
import fr.lumavision.video.VideoSource;

/**
 * Client-side test pattern: animated checkerboard with a horizontal color gradient.
 * Used to validate the dynamic texture pipeline before real media sources exist.
 */
public final class TestPatternVideoSource implements VideoSource {

    private static final int CELL_SIZE = 16;

    private final int width;
    private final int height;
    private final VideoFrame frame;
    private int tick;

    public TestPatternVideoSource(int width, int height) {
        this.width = width;
        this.height = height;
        this.frame = new VideoFrame(width, height);
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void tick() {
        tick++;
        renderPattern();
    }

    @Override
    public VideoFrame getCurrentFrame() {
        return frame;
    }

    @Override
    public void dispose() {
    }

    private void renderPattern() {
        int offset = tick % CELL_SIZE;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int cellX = (x + offset) / CELL_SIZE;
                int cellY = (y + offset / 2) / CELL_SIZE;
                boolean lightCell = (cellX + cellY) % 2 == 0;

                int gradient = (x * 255) / Math.max(1, width - 1);
                int red = lightCell ? 40 + gradient / 2 : 10 + gradient / 4;
                int green = lightCell ? 180 - gradient / 3 : 40;
                int blue = lightCell ? 255 - gradient : 80 + gradient / 2;

                frame.setArgb(x, y, 0xFF000000 | (red << 16) | (green << 8) | blue);
            }
        }
        frame.markDirty();
    }
}
