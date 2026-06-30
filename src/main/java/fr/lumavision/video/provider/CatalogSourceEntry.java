package fr.lumavision.video.provider;

import fr.lumavision.video.VideoSourceDescriptor;

import java.util.Objects;

/**
 * A selectable media source entry returned by {@link VideoSourceProvider#listSources()}.
 */
public record CatalogSourceEntry(
        String providerId,
        VideoSourceDescriptor descriptor,
        String displayName,
        String detail,
        boolean selectable
) {
    public CatalogSourceEntry {
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(descriptor, "descriptor");
        Objects.requireNonNull(displayName, "displayName");
        detail = detail == null ? "" : detail;
    }
}
