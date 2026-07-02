package fr.lumavision.client.gui;

import fr.lumavision.camera.CameraParameters;
import fr.lumavision.blockentity.CameraBlockEntity;
import fr.lumavision.menu.CameraConfigMenu;
import fr.lumavision.network.ConfigureCameraPacket;
import fr.lumavision.network.ModNetworking;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/**
 * Client configuration screen for a camera block. Reads current {@link CameraParameters} from the
 * synced block entity, lets the player edit the static fields (NDI name, resolution, fps, FOV, enable,
 * DMX patch), and sends them back with {@link ConfigureCameraPacket}. Live DMX-driven values are not
 * edited here — they are authored on the server from Art-Net (M3).
 */
public final class CameraConfigScreen extends AbstractContainerScreen<CameraConfigMenu> {

    private static final int PANEL_W = 260;
    private static final int PANEL_H = 236;

    private EditBox nameBox;
    private EditBox resWBox;
    private EditBox resHBox;
    private EditBox fpsBox;
    private EditBox fovBox;
    private EditBox panBox;
    private EditBox tiltBox;
    private EditBox rollBox;
    private EditBox universeBox;
    private EditBox panChBox;
    private EditBox tiltChBox;
    private EditBox zoomChBox;
    private EditBox trackChBox;
    private EditBox enableChBox;

    private boolean enabled = true;
    private boolean sixteenBit;
    private Button enabledButton;
    private Button sixteenBitButton;

    public CameraConfigScreen(CameraConfigMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = PANEL_W;
        this.imageHeight = PANEL_H;
    }

    private CameraParameters currentParameters() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            BlockEntity be = mc.level.getBlockEntity(menu.getCameraPos());
            if (be instanceof CameraBlockEntity camera) {
                return camera.parameters();
            }
        }
        return new CameraParameters();
    }

    @Override
    protected void init() {
        super.init();
        CameraParameters p = currentParameters();
        this.enabled = p.enabled();
        this.sixteenBit = p.dmx().sixteenBit();

        int left = this.leftPos + 12;
        int col2 = this.leftPos + 140;
        int y = this.topPos + 22;
        int rowH = 20;

        nameBox = addEdit(left, y, 236, p.ndiSourceName());
        y += rowH + 4;

        resWBox = addEdit(left, y, 48, Integer.toString(p.resolutionWidth()));
        resHBox = addEdit(left + 56, y, 48, Integer.toString(p.resolutionHeight()));
        fpsBox = addEdit(left + 120, y, 36, Integer.toString(p.fps()));
        fovBox = addEdit(left + 176, y, 48, Integer.toString(Math.round(p.fov())));
        y += rowH + 4;

        // Manual aim: point the camera lens (degrees), independent of the player. DMX overrides these when patched.
        panBox = addEdit(left, y, 60, fmt(p.pan()));
        tiltBox = addEdit(left + 68, y, 60, fmt(p.tilt()));
        rollBox = addEdit(left + 136, y, 60, fmt(p.roll()));
        y += rowH + 4;

        enabledButton = Button.builder(enabledLabel(), b -> {
            enabled = !enabled;
            b.setMessage(enabledLabel());
        }).bounds(left, y, 110, 18).build();
        addRenderableWidget(enabledButton);
        sixteenBitButton = Button.builder(sixteenBitLabel(), b -> {
            sixteenBit = !sixteenBit;
            b.setMessage(sixteenBitLabel());
        }).bounds(left + 118, y, 118, 18).build();
        addRenderableWidget(sixteenBitButton);
        y += rowH + 8;

        universeBox = addEdit(left, y, 60, Integer.toString(p.dmx().universe()));
        y += rowH + 4;

        panChBox = addEdit(left, y, 40, Integer.toString(p.dmx().panChannel()));
        tiltChBox = addEdit(left + 48, y, 40, Integer.toString(p.dmx().tiltChannel()));
        zoomChBox = addEdit(left + 96, y, 40, Integer.toString(p.dmx().zoomChannel()));
        trackChBox = addEdit(left + 144, y, 40, Integer.toString(p.dmx().trackChannel()));
        enableChBox = addEdit(left + 192, y, 40, Integer.toString(p.dmx().enableChannel()));
        y += rowH + 10;

        addRenderableWidget(Button.builder(Component.translatable("gui.lumavision.camera_config.apply"),
                b -> apply()).bounds(left, y, 114, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.lumavision.camera_config.cancel"),
                b -> onClose()).bounds(col2, y, 108, 20).build());
    }

    private EditBox addEdit(int x, int y, int w, String value) {
        EditBox box = new EditBox(this.font, x, y, w, 16, Component.empty());
        box.setMaxLength(CameraParameters.MAX_NAME_LENGTH);
        box.setValue(value);
        addRenderableWidget(box);
        return box;
    }

    private Component enabledLabel() {
        return Component.translatable("gui.lumavision.camera_config.enabled",
                Component.translatable(enabled ? "gui.lumavision.camera_config.on" : "gui.lumavision.camera_config.off"));
    }

    private Component sixteenBitLabel() {
        return Component.translatable("gui.lumavision.camera_config.sixteen_bit",
                Component.translatable(sixteenBit ? "gui.lumavision.camera_config.on" : "gui.lumavision.camera_config.off"));
    }

    private void apply() {
        CameraParameters edited = new CameraParameters();
        edited.setNdiSourceName(nameBox.getValue());
        edited.setResolution(parseInt(resWBox, 1280), parseInt(resHBox, 720));
        edited.setFps(parseInt(fpsBox, 30));
        edited.setFov(parseFloat(fovBox, 70.0F));
        edited.setPan(parseFloat(panBox, 0.0F));
        edited.setTilt(parseFloat(tiltBox, 0.0F));
        edited.setRoll(parseFloat(rollBox, 0.0F));
        edited.setEnabled(enabled);
        edited.dmx().setUniverse(parseInt(universeBox, 0));
        edited.dmx().setSixteenBit(sixteenBit);
        edited.dmx().setPanChannel(parseInt(panChBox, 0));
        edited.dmx().setTiltChannel(parseInt(tiltChBox, 0));
        edited.dmx().setZoomChannel(parseInt(zoomChBox, 0));
        edited.dmx().setTrackChannel(parseInt(trackChBox, 0));
        edited.dmx().setEnableChannel(parseInt(enableChBox, 0));

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        edited.writeConfig(buf);
        ModNetworking.CHANNEL.sendToServer(new ConfigureCameraPacket(menu.getCameraPos(), buf));
        onClose();
    }

    private static int parseInt(EditBox box, int fallback) {
        try {
            return Integer.parseInt(box.getValue().trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static float parseFloat(EditBox box, float fallback) {
        try {
            return Float.parseFloat(box.getValue().trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static String fmt(float value) {
        return Integer.toString(Math.round(value));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xE0101014);
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + 16, 0xFF1E1E2A);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        int white = 0xFFFFFF;
        int grey = 0xA0A0B0;
        graphics.drawString(this.font, this.title, 8, 5, white, false);
        graphics.drawString(this.font, Component.translatable("gui.lumavision.camera_config.ndi_name"), 12, 12, grey, false);
        graphics.drawString(this.font, Component.translatable("gui.lumavision.camera_config.res_fps_fov"), 12, 39, grey, false);
        graphics.drawString(this.font, Component.translatable("gui.lumavision.camera_config.aim"), 12, 63, grey, false);
        graphics.drawString(this.font, Component.translatable("gui.lumavision.camera_config.dmx_universe"), 12, 115, grey, false);
        graphics.drawString(this.font, Component.translatable("gui.lumavision.camera_config.dmx_channels"), 12, 139, grey, false);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // ESC
            onClose();
            return true;
        }
        if (getFocused() instanceof EditBox box) {
            box.keyPressed(keyCode, scanCode, modifiers);
            return true; // swallow so inventory key ('e') doesn't close the screen while typing
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
