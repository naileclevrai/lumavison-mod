package fr.lumavision.client.gui;

import fr.lumavision.video.provider.CatalogSourceEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;

import java.util.Objects;

/**
 * Scrollable single-select list of catalog sources for {@link ScreenConfigScreen}.
 */
final class SourceSelectionList extends ObjectSelectionList<SourceSelectionList.Entry> {

    private final ScreenConfigScreen screen;

    SourceSelectionList(ScreenConfigScreen screen, Minecraft minecraft, int width, int top, int height) {
        super(minecraft, width, height, top, top + height, 20);
        this.screen = screen;
    }

    void setSources(Iterable<CatalogSourceEntry> sources, CatalogSourceEntry selected) {
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

    CatalogSourceEntry getSelectedSource() {
        Entry entry = getSelected();
        return entry == null ? null : entry.source;
    }

    @Override
    public int getRowWidth() {
        return width - 12;
    }

    final class Entry extends ObjectSelectionList.Entry<Entry> {

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
            String marker = selected ? "● " : "○ ";
            int color = source.selectable() ? 0xFFFFFF : 0x808080;
            graphics.drawString(
                    Minecraft.getInstance().font,
                    marker + source.displayName(),
                    left + 4,
                    top + 6,
                    color,
                    false
            );
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
