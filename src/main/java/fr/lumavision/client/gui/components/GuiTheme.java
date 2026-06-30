package fr.lumavision.client.gui.components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Shared colors and drawing helpers for LumaVision GUI screens.
 */
@OnlyIn(Dist.CLIENT)
public final class GuiTheme {

    public static final int OVERLAY = 0xAA000000;
    public static final int PANEL_BG = 0xFF12141A;
    public static final int PANEL_BORDER = 0xFF2A3040;
    public static final int PANEL_HEADER = 0xFF181C24;
    public static final int DIVIDER = 0xFF2A3040;
    public static final int ACCENT = 0xFF00C8B4;
    public static final int ACCENT_DIM = 0xFF007A6E;
    public static final int SELECTION_BG = 0xFF1A2D45;
    public static final int ROW_HOVER = 0xFF1E2430;
    public static final int TEXT_PRIMARY = 0xFFE8EAED;
    public static final int TEXT_SECONDARY = 0xFF9AA3B2;
    public static final int TEXT_MUTED = 0xFF6B7280;
    public static final int TEXT_ERROR = 0xFFFF8080;
    public static final int BADGE_LIVE = 0xFF00C8B4;
    public static final int BADGE_SOON = 0xFF8B7355;
    public static final int BADGE_DISABLED = 0xFF6B7280;
    public static final int BUTTON_PRIMARY_BG = 0xFF00A896;
    public static final int BUTTON_PRIMARY_HOVER = 0xFF00C8B4;
    public static final int BUTTON_SECONDARY_BG = 0xFF1E2430;
    public static final int BUTTON_SECONDARY_HOVER = 0xFF2A3040;
    public static final int BUTTON_SECONDARY_BORDER = 0xFF3D4654;
    public static final int BUTTON_DISABLED_BG = 0xFF2A2E36;
    public static final int BUTTON_DISABLED_TEXT = 0xFF6B7280;

    private GuiTheme() {
    }

    public static void drawOverlay(GuiGraphics graphics, int screenWidth, int screenHeight) {
        graphics.fill(0, 0, screenWidth, screenHeight, OVERLAY);
    }

    public static void drawPanel(GuiGraphics graphics, int left, int top, int width, int height) {
        graphics.fill(left, top, left + width, top + height, PANEL_BORDER);
        graphics.fill(left + 1, top + 1, left + width - 1, top + height - 1, PANEL_BG);
    }

    public static void drawHeader(GuiGraphics graphics, int left, int top, int width, int height) {
        graphics.fill(left + 1, top + 1, left + width - 1, top + height, PANEL_HEADER);
        graphics.fill(left + 1, top + height - 1, left + width - 1, top + height, ACCENT_DIM);
    }

    public static void drawDividerVertical(GuiGraphics graphics, int x, int top, int bottom) {
        graphics.fill(x, top, x + 1, bottom, DIVIDER);
    }

    public static void drawDividerHorizontal(GuiGraphics graphics, int left, int right, int y) {
        graphics.fill(left, y, right, y + 1, DIVIDER);
    }

    public static void drawListBackground(GuiGraphics graphics, int left, int top, int width, int height) {
        graphics.fill(left, top, left + width, top + height, 0xFF0E1016);
        graphics.fill(left, top, left + width, top + 1, DIVIDER);
        graphics.fill(left, top + height - 1, left + width, top + height, DIVIDER);
        graphics.fill(left, top, left + 1, top + height, DIVIDER);
        graphics.fill(left + width - 1, top, left + width, top + height, DIVIDER);
    }

    public static void drawSelectionBar(GuiGraphics graphics, int left, int top, int height) {
        graphics.fill(left, top, left + 3, top + height, ACCENT);
    }
}
