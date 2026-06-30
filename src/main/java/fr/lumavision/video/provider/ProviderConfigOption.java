package fr.lumavision.video.provider;

import java.util.Objects;

/**
 * Describes a single configuration knob owned by a {@link VideoSourceProvider}.
 * <p>
 * The screen configuration GUI reads these entries without knowing the underlying backend.
 */
public record ProviderConfigOption(
        String key,
        String label,
        String description,
        ProviderConfigOptionType type,
        String currentValue,
        boolean readOnly
) {
    public ProviderConfigOption {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(type, "type");
        currentValue = currentValue == null ? "" : currentValue;
    }
}
