package fr.lumavision.client.gui;

import fr.lumavision.blockentity.LedScreenBlockEntity;
import fr.lumavision.client.video.catalog.ClientVideoSourceCatalog;
import fr.lumavision.network.ModNetworking;
import fr.lumavision.network.SetScreenSourcePacket;
import fr.lumavision.video.VideoSourceDescriptor;
import fr.lumavision.video.provider.CatalogSourceEntry;
import fr.lumavision.video.provider.VideoSourceProvider;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

/**
 * In-game screen for choosing a media provider and source via {@link ClientVideoSourceCatalog}.
 */
@OnlyIn(Dist.CLIENT)
public final class ScreenConfigScreen extends AbstractContainerScreen<ScreenConfigMenu> {

    private static final int PANEL_WIDTH = 260;
    private static final int PANEL_HEIGHT = 220;

    private static final ClientVideoSourceCatalog CATALOG = ClientVideoSourceCatalog.INSTANCE;

    private final List<VideoSourceProvider> providers;

    private VideoSourceProvider selectedProvider;
    private CatalogSourceEntry selectedSource;
    private Component subtitle = Component.empty();

    private CycleButton<VideoSourceProvider> providerButton;
    private SourceSelectionList sourceList;
    private Component statusMessage = Component.empty();

    public ScreenConfigScreen(ScreenConfigMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = PANEL_WIDTH;
        this.imageHeight = PANEL_HEIGHT;
        this.inventoryLabelY = Integer.MAX_VALUE;
        this.titleLabelY = 8;
        this.providers = CATALOG.getProviders();
    }

    private void initializeSelection() {
        BlockEntity blockEntity = minecraft.level.getBlockEntity(menu.getGroupOrigin());
        if (blockEntity instanceof LedScreenBlockEntity screen) {
            if (screen.getGroupMembership().isMerged()) {
                subtitle = Component.translatable(
                        "gui.lumavision.screen_config.merged_wall",
                        screen.getGroupMembership().gridWidth(),
                        screen.getGroupMembership().gridHeight()
                );
            }

            VideoSourceDescriptor current = CATALOG.resolve(screen);
            selectedProvider = CATALOG.providerFor(current).orElseGet(this::testPatternProviderFallback);
            selectedSource = findMatchingSource(selectedProvider, current);
            return;
        }

        selectedProvider = testPatternProviderFallback();
        selectedSource = null;
    }

    private VideoSourceProvider testPatternProviderFallback() {
        return providers.stream()
                .filter(provider -> provider.providerId().equals("test"))
                .findFirst()
                .orElse(providers.get(providers.size() - 1));
    }

    private static CatalogSourceEntry findMatchingSource(VideoSourceProvider provider, VideoSourceDescriptor descriptor) {
        for (CatalogSourceEntry entry : provider.listSources()) {
            if (entry.descriptor().cacheKey().equals(descriptor.cacheKey())) {
                return entry;
            }
        }
        List<CatalogSourceEntry> sources = provider.listSources();
        return sources.isEmpty() ? null : sources.get(0);
    }

    @Override
    protected void init() {
        super.init();
        initializeSelection();

        int left = (width - imageWidth) / 2;
        int top = (height - imageHeight) / 2;

        providerButton = CycleButton.<VideoSourceProvider>builder(this::providerLabel)
                .withValues(providers)
                .withInitialValue(selectedProvider)
                .create(
                        left + 10,
                        top + 34,
                        imageWidth - 20,
                        20,
                        Component.translatable("gui.lumavision.screen_config.provider"),
                        this::onProviderChanged
                );
        addRenderableWidget(providerButton);

        sourceList = new SourceSelectionList(this, minecraft, imageWidth - 20, top + 78, 96);
        sourceList.setLeftPos(left + 10);
        addRenderableWidget(sourceList);

        addRenderableWidget(Button.builder(Component.translatable("gui.lumavision.screen_config.refresh"), button -> refreshSources())
                .bounds(left + 10, top + imageHeight - 28, 80, 20)
                .build());

        addRenderableWidget(Button.builder(Component.translatable("gui.lumavision.screen_config.apply"), button -> applySelection())
                .bounds(left + imageWidth - 90, top + imageHeight - 28, 80, 20)
                .build());

        rebuildSourceList();
    }

    private Component providerLabel(VideoSourceProvider provider) {
        if (!provider.isImplemented()) {
            return Component.literal(provider.displayName() + " ("
                    + Component.translatable("gui.lumavision.screen_config.coming_soon").getString() + ")");
        }
        if (!provider.isEnabled()) {
            return Component.literal(provider.displayName() + " ("
                    + Component.translatable("gui.lumavision.screen_config.disabled").getString() + ")");
        }
        if (!provider.isAvailable()) {
            return Component.literal(provider.displayName() + " ("
                    + Component.translatable("gui.lumavision.screen_config.unavailable").getString() + ")");
        }
        return Component.literal(provider.displayName());
    }

    private void onProviderChanged(CycleButton<VideoSourceProvider> button, VideoSourceProvider provider) {
        selectedProvider = provider;
        selectedSource = null;
        rebuildSourceList();
    }

    private void refreshSources() {
        CATALOG.refreshProvider(selectedProvider.providerId());
        rebuildSourceList();
    }

    private void rebuildSourceList() {
        if (sourceList == null) {
            return;
        }

        if (!selectedProvider.isImplemented()) {
            statusMessage = Component.translatable("gui.lumavision.screen_config.provider_unavailable");
            sourceList.setSources(List.of(), null);
            return;
        }

        if (!selectedProvider.isEnabled()) {
            statusMessage = Component.translatable("gui.lumavision.screen_config.provider_disabled");
            sourceList.setSources(List.of(), null);
            return;
        }

        if (!selectedProvider.isAvailable()) {
            statusMessage = Component.translatable("gui.lumavision.screen_config.provider_unavailable");
            sourceList.setSources(List.of(), null);
            return;
        }

        List<CatalogSourceEntry> sources = CATALOG.listSourcesForProvider(selectedProvider.providerId());
        if (sources.isEmpty()) {
            statusMessage = Component.translatable("gui.lumavision.screen_config.no_sources");
        } else {
            statusMessage = Component.empty();
        }

        if (selectedSource == null && !sources.isEmpty()) {
            selectedSource = sources.get(0);
        } else if (selectedSource != null) {
            selectedSource = sources.stream()
                    .filter(entry -> entry.descriptor().cacheKey().equals(selectedSource.descriptor().cacheKey()))
                    .findFirst()
                    .orElse(sources.isEmpty() ? null : sources.get(0));
        }

        sourceList.setSources(sources, selectedSource);
    }

    void selectSource(CatalogSourceEntry source) {
        selectedSource = source;
    }

    boolean isSourceSelected(CatalogSourceEntry source) {
        return selectedSource != null
                && selectedSource.descriptor().cacheKey().equals(source.descriptor().cacheKey());
    }

    private void applySelection() {
        if (selectedSource == null || !selectedSource.selectable()) {
            return;
        }

        ModNetworking.CHANNEL.sendToServer(new SetScreenSourcePacket(
                menu.getGroupOrigin(),
                selectedSource.descriptor().toSourceId()
        ));
        onClose();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left = (width - imageWidth) / 2;
        int top = (height - imageHeight) / 2;
        graphics.fill(left, top, left + imageWidth, top + imageHeight, 0xC0101010);
        graphics.fill(left + 1, top + 1, left + imageWidth - 1, top + imageHeight - 1, 0xF0202020);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        int left = (width - imageWidth) / 2;
        int top = (height - imageHeight) / 2;

        if (!subtitle.getString().isEmpty()) {
            graphics.drawCenteredString(font, subtitle, width / 2, top + 20, 0xA0A0A0);
        }

        graphics.drawString(font, title, left + 10, top + 8, 0xFFFFFF, false);
        graphics.drawString(font, Component.translatable("gui.lumavision.screen_config.sources"), left + 10, top + 64, 0xFFFFFF, false);

        if (!statusMessage.getString().isEmpty()) {
            graphics.drawCenteredString(font, statusMessage, width / 2, top + 186, 0xFF8080);
        }
    }
}
