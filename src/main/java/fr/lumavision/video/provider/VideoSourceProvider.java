package fr.lumavision.video.provider;

import fr.lumavision.video.VideoSource;
import fr.lumavision.video.VideoSourceDescriptor;
import fr.lumavision.video.VideoSourceType;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Pluggable media backend for LumaVision.
 * <p>
 * Each provider discovers its own sources, creates {@link VideoSource} instances,
 * exposes configuration metadata for the GUI, and serializes {@link VideoSourceDescriptor} ids.
 */
public interface VideoSourceProvider {

    VideoSourceType type();

    /** Stable short id, e.g. {@code ndi}, {@code file}. */
    String providerId();

    /** Human-readable label for GUI grouping. */
    String displayName();

    /**
     * Prefix used in persisted {@code sourceId} strings ({@code ndi:}, {@code file:}, …).
     */
    String sourceIdPrefix();

    /** Whether this provider is turned on in mod configuration. */
    boolean isEnabled();

    /**
     * Whether the runtime is ready to produce frames (libraries loaded, permissions granted, …).
     */
    boolean isAvailable();

    /**
     * Whether capture/playback is implemented. Stubs return {@code false} so the GUI can show
     * planned capabilities without wiring a backend yet.
     */
    boolean isImplemented();

    void start();

    void stop();

    boolean supports(VideoSourceDescriptor descriptor);

    VideoSourceDescriptor descriptorFromPayload(String payload);

    List<CatalogSourceEntry> listSources();

    /**
     * Fallback descriptor when a screen has no explicit binding. Return {@code null} to defer
     * to the next provider in catalog order.
     */
    @Nullable
    VideoSourceDescriptor defaultDescriptor();

    VideoSource create(VideoSourceDescriptor descriptor, int targetWidth, int targetHeight);

    List<ProviderConfigOption> getConfigOptions();

    /**
     * Requests a refresh of discoverable sources (e.g. after the user clicks Refresh in the GUI).
     * Default implementation is a no-op when discovery is already continuous.
     */
    default void refreshSources() {
    }
}
