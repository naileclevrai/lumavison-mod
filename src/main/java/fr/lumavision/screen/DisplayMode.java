package fr.lumavision.screen;

/**
 * How video content is mapped onto a merged LED wall texture.
 */
public enum DisplayMode {
    FIT,
    FILL,
    STRETCH;

    public DisplayMode next() {
        return switch (this) {
            case FIT -> FILL;
            case FILL -> STRETCH;
            case STRETCH -> FIT;
        };
    }
}
