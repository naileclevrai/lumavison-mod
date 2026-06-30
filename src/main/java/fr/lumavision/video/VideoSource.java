package fr.lumavision.video;

/**
 * Abstraction for any pixel source feeding an LED screen.
 * <p>
 * Implementations may wrap video files, NDI streams, GIFs, static images, or test patterns.
 * The renderer only consumes {@link VideoFrame} instances and does not know the origin.
 */
public interface VideoSource {

    int getWidth();

    int getHeight();

    /**
     * Advances the source by one client tick (animation, decoding, network receive, etc.).
     */
    void tick();

    /**
     * Returns the frame that should be displayed right now.
     * The returned instance may be reused across ticks; callers must not retain it.
     */
    VideoFrame getCurrentFrame();

    /**
     * Releases native or GPU resources held by this source.
     */
    void dispose();
}
