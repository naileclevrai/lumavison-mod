package fr.lumavision.client.display;

import fr.lumavision.screen.DisplayMode;
import fr.lumavision.screen.ScreenDisplaySettings;
import fr.lumavision.screen.ScreenGroupMembership;

/**
 * Maps wall cell UV coordinates through display transforms (fit/fill, rotation, mirror).
 */
public final class DisplayUvMapper {

    public record MappedUv(
            float u0, float v0, float u1, float v1,
            float quadX0, float quadY0, float quadX1, float quadY1
    ) {
        public static MappedUv identity(ScreenGroupMembership group) {
            return new MappedUv(
                    group.uvMinU(), group.uvMinV(), group.uvMaxU(), group.uvMaxV(),
                    0.0F, 0.0F, 1.0F, 1.0F
            );
        }
    }

    private DisplayUvMapper() {
    }

    public static MappedUv map(
            ScreenGroupMembership group,
            ScreenDisplaySettings settings,
            int frameWidth,
            int frameHeight
    ) {
        if (frameWidth <= 0 || frameHeight <= 0) {
            return MappedUv.identity(group);
        }

        float cellU0 = group.uvMinU();
        float cellV0 = group.uvMinV();
        float cellU1 = group.uvMaxU();
        float cellV1 = group.uvMaxV();

        float[] content = contentRegion(settings.mode(), frameWidth, frameHeight, group.gridWidth(), group.gridHeight());

        float u0 = lerp(content[0], content[2], cellU0);
        float u1 = lerp(content[0], content[2], cellU1);
        float v0 = lerp(content[1], content[3], cellV0);
        float v1 = lerp(content[1], content[3], cellV1);

        float[] transformed = transformCorners(u0, v0, u1, v1, settings);

        return new MappedUv(
                transformed[0], transformed[1], transformed[2], transformed[3],
                0.0F, 0.0F, 1.0F, 1.0F
        );
    }

    /**
     * Normalized texture UV bounds [u0, v0, u1, v1] of the video content for the whole wall
     * (before per-cell rotation/mirror — matches what players see in FIT/FILL/STRETCH).
     */
    public static float[] visibleContentUv(
            ScreenDisplaySettings settings,
            int frameWidth,
            int frameHeight,
            int gridWidth,
            int gridHeight
    ) {
        if (frameWidth <= 0 || frameHeight <= 0) {
            return new float[]{0.0F, 0.0F, 1.0F, 1.0F};
        }
        return contentRegion(settings.mode(), frameWidth, frameHeight, gridWidth, gridHeight);
    }

    private static float[] contentRegion(DisplayMode mode, int frameW, int frameH, int gridW, int gridH) {
        float frameAspect = (float) frameW / frameH;
        float wallAspect = (float) gridW / gridH;

        return switch (mode) {
            case STRETCH -> new float[]{0.0F, 0.0F, 1.0F, 1.0F};
            case FIT -> {
                if (frameAspect > wallAspect) {
                    float usedHeight = wallAspect / frameAspect;
                    float pad = (1.0F - usedHeight) * 0.5F;
                    yield new float[]{0.0F, pad, 1.0F, 1.0F - pad};
                } else {
                    float usedWidth = frameAspect / wallAspect;
                    float pad = (1.0F - usedWidth) * 0.5F;
                    yield new float[]{pad, 0.0F, 1.0F - pad, 1.0F};
                }
            }
            case FILL -> {
                if (frameAspect > wallAspect) {
                    float usedWidth = wallAspect / frameAspect;
                    float pad = (1.0F - usedWidth) * 0.5F;
                    yield new float[]{pad, 0.0F, 1.0F - pad, 1.0F};
                } else {
                    float usedHeight = frameAspect / wallAspect;
                    float pad = (1.0F - usedHeight) * 0.5F;
                    yield new float[]{0.0F, pad, 1.0F, 1.0F - pad};
                }
            }
        };
    }

    /**
     * Transforms the four corners of a cell UV rectangle through rotation and mirror in normalized space.
     */
    private static float[] transformCorners(float u0, float v0, float u1, float v1, ScreenDisplaySettings settings) {
        float[][] corners = {
                {u0, v0}, {u1, v0}, {u1, v1}, {u0, v1}
        };

        float minU = 1.0F;
        float minV = 1.0F;
        float maxU = 0.0F;
        float maxV = 0.0F;

        for (float[] corner : corners) {
            float u = corner[0];
            float v = corner[1];
            if (settings.mirrorH()) {
                u = 1.0F - u;
            }
            if (settings.mirrorV()) {
                v = 1.0F - v;
            }
            float[] rotated = rotateUv(u, v, settings.rotation());
            minU = Math.min(minU, rotated[0]);
            minV = Math.min(minV, rotated[1]);
            maxU = Math.max(maxU, rotated[0]);
            maxV = Math.max(maxV, rotated[1]);
        }

        return new float[]{minU, minV, maxU, maxV};
    }

    private static float[] rotateUv(float u, float v, int rotation) {
        return switch (rotation) {
            case 90 -> new float[]{v, 1.0F - u};
            case 180 -> new float[]{1.0F - u, 1.0F - v};
            case 270 -> new float[]{1.0F - v, u};
            default -> new float[]{u, v};
        };
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
