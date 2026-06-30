package fr.lumavision.client.shimmer;

import fr.lumavision.LumaVisionMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModList;
import org.joml.Vector3f;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Optional Shimmer integration via reflection — no compile-time dependency on Shimmer.
 */
@OnlyIn(Dist.CLIENT)
public final class ShimmerBridge {

    private static final String LIGHT_MANAGER_CLASS = "com.lowdragmc.shimmer.client.light.LightManager";
    private static final String COLOR_POINT_LIGHT_CLASS = "com.lowdragmc.shimmer.client.light.ColorPointLight";

    private static boolean initAttempted;
    private static boolean available;
    private static boolean loggedFailure;

    private static Object lightManager;
    private static Method addLight;
    private static Method setColor;
    private static Method setPos;
    private static Method setEnable;
    private static Method remove;
    private static Method isRemoved;
    private static Method update;

    private ShimmerBridge() {
    }

    public static boolean isAvailable() {
        if (!initAttempted) {
            init();
        }
        return available;
    }

    public static Object addLight(Vector3f pos, int color, float radius) {
        if (!isAvailable()) {
            return null;
        }
        try {
            Object light = addLight.invoke(lightManager, pos, color, radius);
            if (light != null) {
                setEnable(light, true);
            }
            return light;
        } catch (Throwable throwable) {
            logFailure("Failed to add Shimmer light", throwable);
            return null;
        }
    }

    public static void setLightColor(Object light, int color) {
        if (!isAvailable() || light == null) {
            return;
        }
        try {
            setColor.invoke(light, color);
        } catch (Throwable throwable) {
            logFailure("Failed to update Shimmer light color", throwable);
        }
    }

    public static void setLightPos(Object light, Vector3f pos) {
        if (!isAvailable() || light == null) {
            return;
        }
        try {
            setPos.invoke(light, pos.x, pos.y, pos.z);
        } catch (Throwable throwable) {
            logFailure("Failed to update Shimmer light position", throwable);
        }
    }

    public static void setLightRadius(Object light, float radius) {
        if (!isAvailable() || light == null) {
            return;
        }
        try {
            java.lang.reflect.Field radiusField = light.getClass().getField("radius");
            radiusField.setFloat(light, radius);
        } catch (Throwable throwable) {
            logFailure("Failed to update Shimmer light radius", throwable);
        }
    }

    public static void setEnable(Object light, boolean enable) {
        if (!isAvailable() || light == null) {
            return;
        }
        try {
            setEnable.invoke(light, enable);
        } catch (Throwable throwable) {
            logFailure("Failed to enable Shimmer light", throwable);
        }
    }

    public static void updateLight(Object light) {
        if (!isAvailable() || light == null) {
            return;
        }
        try {
            update.invoke(light);
        } catch (Throwable throwable) {
            logFailure("Failed to update Shimmer light", throwable);
        }
    }

    public static void removeLight(Object light) {
        if (!isAvailable() || light == null) {
            return;
        }
        try {
            remove.invoke(light);
        } catch (Throwable throwable) {
            logFailure("Failed to remove Shimmer light", throwable);
        }
    }

    public static boolean isRemoved(Object light) {
        if (!isAvailable() || light == null) {
            return false;
        }
        try {
            return (boolean) isRemoved.invoke(light);
        } catch (Throwable throwable) {
            logFailure("Failed to check Shimmer light state", throwable);
            return false;
        }
    }

    private static synchronized void init() {
        if (initAttempted) {
            return;
        }
        initAttempted = true;

        if (!ModList.get().isLoaded("shimmer")) {
            available = false;
            return;
        }

        try {
            Class<?> lightManagerClass = Class.forName(LIGHT_MANAGER_CLASS);
            Field instanceField = lightManagerClass.getField("INSTANCE");
            lightManager = instanceField.get(null);
            addLight = lightManagerClass.getMethod("addLight", Vector3f.class, int.class, float.class);

            Class<?> lightClass = Class.forName(COLOR_POINT_LIGHT_CLASS);
            setColor = lightClass.getMethod("setColor", int.class);
            setPos = lightClass.getMethod("setPos", float.class, float.class, float.class);
            setEnable = lightClass.getMethod("setEnable", boolean.class);
            remove = lightClass.getMethod("remove");
            isRemoved = lightClass.getMethod("isRemoved");
            update = lightClass.getMethod("update");

            available = true;
            LumaVisionMod.LOGGER.debug("Shimmer ambilight bridge initialized");
        } catch (Throwable throwable) {
            available = false;
            logFailure("Shimmer ambilight initialization failed", throwable);
        }
    }

    private static void logFailure(String message, Throwable throwable) {
        if (loggedFailure) {
            return;
        }
        loggedFailure = true;
        LumaVisionMod.LOGGER.warn(message, throwable);
    }
}
