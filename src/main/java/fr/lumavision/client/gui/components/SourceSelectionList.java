package fr.lumavision.client.gui.components;

import fr.lumavision.client.gui.ScreenConfigScreen;
import fr.lumavision.video.provider.CatalogSourceEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Objects;

/**
 * Scrollable single-select list of catalog sources.
 */
@OnlyIn(Dist.CLIENT)
public final class SourceSelectionList extends ObjectSelectionList<SourceSelectionList.Entry> {

    private final ScreenConfigScreen screen;

    public SourceSelectionList(
            ScreenConfigScreen screen,
            Minecraft minecraft,
            int width,
            int listTop,
            int listBottom
    ) {
        super(minecraft, width, listBottom - listTop, listTop, listBottom, 22);
        this.screen = screen;
    }

    public void setSources(Iterable<CatalogSourceEntry> sources, CatalogSourceEntry selected) {
        clearEntries();
        for (CatalogSourceEntry source : sources) {
            addEntry(new Entry(source));
        }
        if (selected != null) {
            for (int index = 0; index < getItemCount(); index++) {
                Entry entry = getEntry(index);
                if (entry.source.descriptor().cacheKey().equals(selected.descriptor().cacheKey())) {
                    setSelected(entry);
                    break;
                }
            }
        }
    }

    public CatalogSourceEntry getSelectedSource() {
        Entry entry = getSelected();
        return entry == null ? null : entry.source;
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

        private final CatalogSourceEntry source;

        private Entry(CatalogSourceEntry source) {
            this.source = Objects.requireNonNull(source);
        }

        @Override
        public Component getNarration() {
            return Component.literal(source.displayName());
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
            boolean selected = screen.isSourceSelected(source);
            boolean selectable = source.selectable();

            if (selected) {
                graphics.fill(left, top, left + entryWidth, top + entryHeight, GuiTheme.SELECTION_BG);
                GuiTheme.drawSelectionBar(graphics, left, top, entryHeight);
            } else if (hovered && selectable) {
                graphics.fill(left, top, left + entryWidth, top + entryHeight, GuiTheme.ROW_HOVER);
            }

            int nameColor = selectable ? GuiTheme.TEXT_PRIMARY : GuiTheme.TEXT_MUTED;
            graphics.drawString(
                    Minecraft.getInstance().font,
                    source.displayName(),
                    left + 10,
                    top + 4,
                    nameColor,
                    false
            );

            if (!source.detail().isBlank()) {
                graphics.drawString(
                        Minecraft.getInstance().font,
                        source.detail(),
                        left + 10,
                        top + 13,
                        GuiTheme.TEXT_SECONDARY,
                        false
                );
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!source.selectable()) {
                return false;
            }
            screen.selectSource(source);
            SourceSelectionList.this.setSelected(this);
            return true;
        }
    }
}
