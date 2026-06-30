package fr.lumavision.client.gui.components;

import fr.lumavision.client.gui.ScreenConfigScreen;
import fr.lumavision.video.provider.VideoSourceProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import java.util.Objects;

/**
 * Left sidebar list for selecting a media provider.
 */
@OnlyIn(Dist.CLIENT)
public final class ProviderSelectionList extends ObjectSelectionList<ProviderSelectionList.Entry> {

    private final ScreenConfigScreen screen;

    public ProviderSelectionList(
            ScreenConfigScreen screen,
            Minecraft minecraft,
            int width,
            int listTop,
            int listBottom
    ) {
        super(minecraft, width, listBottom - listTop, listTop, listBottom, 24);
        this.screen = screen;
    }

    public void setProviders(List<VideoSourceProvider> providers, VideoSourceProvider selected) {
        clearEntries();
        for (VideoSourceProvider provider : providers) {
            addEntry(new Entry(provider));
        }
        if (selected != null) {
            for (int index = 0; index < getItemCount(); index++) {
                Entry entry = getEntry(index);
                if (entry.provider.providerId().equals(selected.providerId())) {
                    setSelected(entry);
                    break;
                }
            }
        }
    }

    @Override
    public int getRowWidth() {
        return width - 8;
    }

    @Override
    protected int getScrollbarPosition() {
        return getLeft() + width - 6;
    }

    public final class Entry extends ObjectSelectionList.Entry<Entry> {

        private final VideoSourceProvider provider;

        private Entry(VideoSourceProvider provider) {
            this.provider = Objects.requireNonNull(provider);
        }

        @Override
        public Component getNarration() {
            return Component.literal(provider.displayName());
        }

        @Override
        public void render(
                GuiGraphics graphics,
                int index,
                int top,
                int left,
                int entryWidth,
                int entryHeight,
                int mouseX,
                int mouseY,
                boolean hovered,
                float partialTick
        ) {
            boolean selected = screen.isProviderSelected(provider);
            boolean selectable = screen.isProviderSelectable(provider);

            if (selected) {
                graphics.fill(left, top, left + entryWidth, top + entryHeight, GuiTheme.SELECTION_BG);
                GuiTheme.drawSelectionBar(graphics, left, top, entryHeight);
            } else if (hovered && selectable) {
                graphics.fill(left, top, left + entryWidth, top + entryHeight, GuiTheme.ROW_HOVER);
            }

            int nameColor = selectable ? GuiTheme.TEXT_PRIMARY : GuiTheme.TEXT_MUTED;
            graphics.drawString(
                    Minecraft.getInstance().font,
                    provider.displayName(),
                    left + 8,
                    top + 4,
                    nameColor,
                    false
            );

            Component badge = badgeFor(provider);
            int badgeColor = badgeColorFor(provider);
            graphics.drawString(
                    Minecraft.getInstance().font,
                    badge,
                    left + 8,
                    top + 14,
                    badgeColor,
                    false
            );
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!screen.isProviderSelectable(provider)) {
                return false;
            }
            screen.selectProvider(provider);
            ProviderSelectionList.this.setSelected(this);
            return true;
        }
    }

    private static Component badgeFor(VideoSourceProvider provider) {
        if (!provider.isImplemented()) {
            return Component.translatable("gui.lumavision.screen_config.badge_soon");
        }
        if (!provider.isEnabled()) {
            return Component.translatable("gui.lumavision.screen_config.badge_disabled");
        }
        if (!provider.isAvailable()) {
            return Component.translatable("gui.lumavision.screen_config.badge_unavailable");
        }
        return Component.translatable("gui.lumavision.screen_config.badge_live");
    }

    private static int badgeColorFor(VideoSourceProvider provider) {
        if (!provider.isImplemented()) {
            return GuiTheme.BADGE_SOON;
        }
        if (!provider.isEnabled() || !provider.isAvailable()) {
            return GuiTheme.BADGE_DISABLED;
        }
        return GuiTheme.BADGE_LIVE;
    }
}
