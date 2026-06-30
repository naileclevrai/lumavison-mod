package fr.lumavision.client.gui.components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Styled button for LumaVision configuration screens.
 */
@OnlyIn(Dist.CLIENT)
public final class LumaButton extends Button {

    public enum Style {
        PRIMARY,
        SECONDARY
    }

    private final Style style;

    private LumaButton(int x, int y, int width, int height, Component message, OnPress onPress, Style style) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.style = style;
    }

    public static LumaButton primary(int x, int y, int width, int height, Component message, OnPress onPress) {
        return new LumaButton(x, y, width, height, message, onPress, Style.PRIMARY);
    }

    public static LumaButton secondary(int x, int y, int width, int height, Component message, OnPress onPress) {
        return new LumaButton(x, y, width, height, message, onPress, Style.SECONDARY);
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean hovered = isHoveredOrFocused();
        int bg;
        int border = 0;
        int textColor;

        if (!active) {
            bg = GuiTheme.BUTTON_DISABLED_BG;
            textColor = GuiTheme.BUTTON_DISABLED_TEXT;
        } else if (style == Style.PRIMARY) {
            bg = hovered ? GuiTheme.BUTTON_PRIMARY_HOVER : GuiTheme.BUTTON_PRIMARY_BG;
            textColor = 0xFF0A0E12;
        } else {
            bg = hovered ? GuiTheme.BUTTON_SECONDARY_HOVER : GuiTheme.BUTTON_SECONDARY_BG;
            border = GuiTheme.BUTTON_SECONDARY_BORDER;
            textColor = GuiTheme.TEXT_PRIMARY;
        }

        if (border != 0) {
            graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), border);
            graphics.fill(getX() + 1, getY() + 1, getX() + getWidth() - 1, getY() + getHeight() - 1, bg);
        } else {
            graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), bg);
        }

        graphics.drawCenteredString(
                net.minecraft.client.Minecraft.getInstance().font,
                getMessage(),
                getX() + getWidth() / 2,
                getY() + (getHeight() - 8) / 2,
                textColor
        );
    }
}
