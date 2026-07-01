package fr.lumavision.camera;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

/**
 * Art-Net / DMX patch for a single camera: which universe and channels drive its live parameters.
 *
 * <p>Channel fields are 1-based DMX addresses inside {@link #universe()} (1..512). A value of 0 means
 * "not patched" — that function is left under manual/GUI control and ignored by the Art-Net apply loop.
 * When {@link #sixteenBit()} is set, each patched function consumes two consecutive channels
 * (coarse at the address, fine at address+1) for smooth 16-bit control. Actual DMX→parameter mapping
 * lives in the server-side Art-Net apply loop (milestone M3); this class is only the persisted patch.
 */
public final class DmxPatch {

    public static final int MAX_UNIVERSE = 32767;
    public static final int MAX_ADDRESS = 512;

    private int universe;
    private boolean sixteenBit;

    // 1-based DMX addresses; 0 = unpatched.
    private int panChannel;
    private int tiltChannel;
    private int zoomChannel;
    private int trackChannel;
    private int enableChannel;

    public DmxPatch() {
    }

    public int universe() {
        return universe;
    }

    public void setUniverse(int universe) {
        this.universe = Mth.clamp(universe, 0, MAX_UNIVERSE);
    }

    public boolean sixteenBit() {
        return sixteenBit;
    }

    public void setSixteenBit(boolean sixteenBit) {
        this.sixteenBit = sixteenBit;
    }

    public int panChannel() {
        return panChannel;
    }

    public void setPanChannel(int channel) {
        this.panChannel = clampChannel(channel);
    }

    public int tiltChannel() {
        return tiltChannel;
    }

    public void setTiltChannel(int channel) {
        this.tiltChannel = clampChannel(channel);
    }

    public int zoomChannel() {
        return zoomChannel;
    }

    public void setZoomChannel(int channel) {
        this.zoomChannel = clampChannel(channel);
    }

    public int trackChannel() {
        return trackChannel;
    }

    public void setTrackChannel(int channel) {
        this.trackChannel = clampChannel(channel);
    }

    public int enableChannel() {
        return enableChannel;
    }

    public void setEnableChannel(int channel) {
        this.enableChannel = clampChannel(channel);
    }

    /** True if any function is patched to a DMX address. */
    public boolean isActive() {
        return panChannel > 0 || tiltChannel > 0 || zoomChannel > 0 || trackChannel > 0 || enableChannel > 0;
    }

    private static int clampChannel(int channel) {
        return channel <= 0 ? 0 : Mth.clamp(channel, 1, MAX_ADDRESS);
    }

    public DmxPatch copy() {
        DmxPatch c = new DmxPatch();
        c.universe = universe;
        c.sixteenBit = sixteenBit;
        c.panChannel = panChannel;
        c.tiltChannel = tiltChannel;
        c.zoomChannel = zoomChannel;
        c.trackChannel = trackChannel;
        c.enableChannel = enableChannel;
        return c;
    }

    public CompoundTag save(CompoundTag tag) {
        CompoundTag dmx = new CompoundTag();
        dmx.putInt("Universe", universe);
        dmx.putBoolean("SixteenBit", sixteenBit);
        dmx.putInt("Pan", panChannel);
        dmx.putInt("Tilt", tiltChannel);
        dmx.putInt("Zoom", zoomChannel);
        dmx.putInt("Track", trackChannel);
        dmx.putInt("Enable", enableChannel);
        tag.put("Dmx", dmx);
        return tag;
    }

    public void load(CompoundTag tag) {
        if (!tag.contains("Dmx")) {
            return;
        }
        CompoundTag dmx = tag.getCompound("Dmx");
        setUniverse(dmx.getInt("Universe"));
        setSixteenBit(dmx.getBoolean("SixteenBit"));
        setPanChannel(dmx.getInt("Pan"));
        setTiltChannel(dmx.getInt("Tilt"));
        setZoomChannel(dmx.getInt("Zoom"));
        setTrackChannel(dmx.getInt("Track"));
        setEnableChannel(dmx.getInt("Enable"));
    }
}
