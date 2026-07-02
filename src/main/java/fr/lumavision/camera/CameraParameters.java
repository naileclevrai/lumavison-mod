package fr.lumavision.camera;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;

/**
 * Authoritative, serializable state of a single virtual camera.
 *
 * <p>These are the values a camera block renders from. Angles are expressed relative to the camera
 * block's facing: {@code pan}/{@code tilt} compose on top of the block's base orientation so a pan of
 * 0 means "looking straight out of the block". State is server-authoritative (persisted in the block
 * entity NBT) and synced to the clients that render the camera and emit its NDI feed.
 */
public final class CameraParameters {

    // Sensible bounds for the configurable values.
    public static final float MIN_FOV = 10.0F;
    public static final float MAX_FOV = 150.0F;
    public static final float MIN_ZOOM = 1.0F;
    public static final float MAX_ZOOM = 20.0F;
    public static final int MIN_RESOLUTION = 64;
    public static final int MAX_RESOLUTION = 3840;
    public static final int MIN_FPS = 1;
    public static final int MAX_FPS = 60;
    public static final int MAX_NAME_LENGTH = 128;

    private float pan;      // degrees, [-180, 180], added to block facing yaw
    private float tilt;     // degrees, [-90, 90]
    private float roll;     // degrees, [-180, 180]
    private float fov = 70.0F;
    private float zoom = 1.0F;
    private int resolutionWidth = 1280;
    private int resolutionHeight = 720;
    private int fps = 30;
    private boolean enabled = true;
    private String ndiSourceName = "";
    /** Normalized position along the mounted rail track, [0,1]. 0 = track origin when unmounted. */
    private float trackPosition;

    // Jib/crane arm: the camera renders from the end of an arm pivoting at the boom.
    private float boomSwing;   // degrees, arm yaw relative to the block facing
    private float boomPitch;   // degrees, arm elevation (positive = up)
    private float boomLength;  // blocks of reach; 0 = no arm (camera at the block)

    public CameraParameters() {
    }

    public float pan() {
        return pan;
    }

    public void setPan(float degrees) {
        this.pan = Mth.wrapDegrees(degrees);
    }

    public float tilt() {
        return tilt;
    }

    public void setTilt(float degrees) {
        this.tilt = Mth.clamp(degrees, -90.0F, 90.0F);
    }

    public float roll() {
        return roll;
    }

    public void setRoll(float degrees) {
        this.roll = Mth.wrapDegrees(degrees);
    }

    public float fov() {
        return fov;
    }

    public void setFov(float degrees) {
        this.fov = Mth.clamp(degrees, MIN_FOV, MAX_FOV);
    }

    public float zoom() {
        return zoom;
    }

    public void setZoom(float factor) {
        this.zoom = Mth.clamp(factor, MIN_ZOOM, MAX_ZOOM);
    }

    /** Effective vertical FOV after applying zoom (zoom narrows the field of view). */
    public float effectiveFov() {
        return Mth.clamp(fov / zoom, MIN_FOV, MAX_FOV);
    }

    public int resolutionWidth() {
        return resolutionWidth;
    }

    public int resolutionHeight() {
        return resolutionHeight;
    }

    public void setResolution(int width, int height) {
        this.resolutionWidth = Mth.clamp(width, MIN_RESOLUTION, MAX_RESOLUTION);
        this.resolutionHeight = Mth.clamp(height, MIN_RESOLUTION, MAX_RESOLUTION);
    }

    public int fps() {
        return fps;
    }

    public void setFps(int fps) {
        this.fps = Mth.clamp(fps, MIN_FPS, MAX_FPS);
    }

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String ndiSourceName() {
        return ndiSourceName;
    }

    public void setNdiSourceName(String name) {
        if (name == null) {
            this.ndiSourceName = "";
        } else {
            this.ndiSourceName = name.length() > MAX_NAME_LENGTH ? name.substring(0, MAX_NAME_LENGTH) : name;
        }
    }

    public float trackPosition() {
        return trackPosition;
    }

    public void setTrackPosition(float normalized) {
        this.trackPosition = Mth.clamp(normalized, 0.0F, 1.0F);
    }

    public float boomSwing() {
        return boomSwing;
    }

    public void setBoomSwing(float degrees) {
        this.boomSwing = Mth.wrapDegrees(degrees);
    }

    public float boomPitch() {
        return boomPitch;
    }

    public void setBoomPitch(float degrees) {
        // Arm elevation only rises from horizontal — never dips below (would bury the tip underground).
        this.boomPitch = Mth.clamp(degrees, 0.0F, 85.0F);
    }

    public float boomLength() {
        return boomLength;
    }

    public void setBoomLength(float blocks) {
        this.boomLength = Mth.clamp(blocks, 0.0F, 16.0F);
    }

    public CameraParameters copy() {
        CameraParameters c = new CameraParameters();
        c.pan = pan;
        c.tilt = tilt;
        c.roll = roll;
        c.fov = fov;
        c.zoom = zoom;
        c.resolutionWidth = resolutionWidth;
        c.resolutionHeight = resolutionHeight;
        c.fps = fps;
        c.enabled = enabled;
        c.ndiSourceName = ndiSourceName;
        c.trackPosition = trackPosition;
        c.boomSwing = boomSwing;
        c.boomPitch = boomPitch;
        c.boomLength = boomLength;
        return c;
    }

    // --- NBT ---------------------------------------------------------------

    public CompoundTag save(CompoundTag tag) {
        tag.putFloat("Pan", pan);
        tag.putFloat("Tilt", tilt);
        tag.putFloat("Roll", roll);
        tag.putFloat("Fov", fov);
        tag.putFloat("Zoom", zoom);
        tag.putInt("ResW", resolutionWidth);
        tag.putInt("ResH", resolutionHeight);
        tag.putInt("Fps", fps);
        tag.putBoolean("Enabled", enabled);
        tag.putString("NdiName", ndiSourceName);
        tag.putFloat("TrackPos", trackPosition);
        tag.putFloat("BoomSwing", boomSwing);
        tag.putFloat("BoomPitch", boomPitch);
        tag.putFloat("BoomLength", boomLength);
        return tag;
    }

    public void load(CompoundTag tag) {
        setPan(tag.getFloat("Pan"));
        setTilt(tag.getFloat("Tilt"));
        setRoll(tag.getFloat("Roll"));
        setFov(tag.contains("Fov") ? tag.getFloat("Fov") : 70.0F);
        setZoom(tag.contains("Zoom") ? tag.getFloat("Zoom") : 1.0F);
        setResolution(
                tag.contains("ResW") ? tag.getInt("ResW") : 1280,
                tag.contains("ResH") ? tag.getInt("ResH") : 720);
        setFps(tag.contains("Fps") ? tag.getInt("Fps") : 30);
        this.enabled = !tag.contains("Enabled") || tag.getBoolean("Enabled");
        this.ndiSourceName = tag.getString("NdiName");
        setTrackPosition(tag.getFloat("TrackPos"));
        setBoomSwing(tag.getFloat("BoomSwing"));
        setBoomPitch(tag.getFloat("BoomPitch"));
        setBoomLength(tag.getFloat("BoomLength"));
    }

    // --- Network (config edits: static fields only; live motion uses a separate lightweight packet) ---

    public void writeConfig(FriendlyByteBuf buf) {
        buf.writeUtf(ndiSourceName, MAX_NAME_LENGTH);
        buf.writeVarInt(resolutionWidth);
        buf.writeVarInt(resolutionHeight);
        buf.writeVarInt(fps);
        buf.writeFloat(fov);
        buf.writeFloat(pan);
        buf.writeFloat(tilt);
        buf.writeFloat(roll);
        buf.writeBoolean(enabled);
    }

    /** Applies GUI-authored config to this (server-side) instance from a validated buffer. */
    public void readConfig(FriendlyByteBuf buf) {
        setNdiSourceName(buf.readUtf(MAX_NAME_LENGTH));
        int w = buf.readVarInt();
        int h = buf.readVarInt();
        setResolution(w, h);
        setFps(buf.readVarInt());
        setFov(buf.readFloat());
        setPan(buf.readFloat());
        setTilt(buf.readFloat());
        setRoll(buf.readFloat());
        setEnabled(buf.readBoolean());
    }
}
