package fr.lumavision.video;

/**
 * Placeholder {@link VideoSource} for future file, URL, or NDI implementations.
 * <p>
 * Not wired to any screen yet — exists to anchor the pipeline architecture.
 */
public final class StubVideoSource implements VideoSource {

    private final int width;
    private final int height;
    private final VideoFrame frame;

    public StubVideoSource(int width, int height) {
        this.width = width;
        this.height = height;
        this.frame = new VideoFrame(width, height);
        this.frame.fill(0xFF000000);
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
    }

    @Override
    public VideoFrame getCurrentFrame() {
        return frame;
    }

    @Override
    public void dispose() {
    }
}
