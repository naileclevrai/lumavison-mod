package fr.lumavision.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import fr.lumavision.blockentity.LedScreenBlockEntity;
import fr.lumavision.client.gui.components.GuiTheme;
import fr.lumavision.client.gui.components.LumaButton;
import fr.lumavision.client.gui.components.ProviderSelectionList;
import fr.lumavision.client.gui.components.SourceSelectionList;
import fr.lumavision.client.video.catalog.ClientVideoSourceCatalog;
import fr.lumavision.network.ModNetworking;
import fr.lumavision.network.SetScreenSourcePacket;
import fr.lumavision.video.VideoSourceDescriptor;
import fr.lumavision.video.provider.CatalogSourceEntry;
import fr.lumavision.video.provider.VideoSourceProvider;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * In-game screen for choosing a media provider and source via {@link ClientVideoSourceCatalog}.
 */
@OnlyIn(Dist.CLIENT)
public final class ScreenConfigScreen extends Screen implements MenuAccess<ScreenConfigMenu> {

    private static final int PANEL_WIDTH = 380;
    private static final int PANEL_HEIGHT = 300;
    private static final int HEADER_HEIGHT = 44;
    private static final int FOOTER_HEIGHT = 36;
    private static final int PROVIDER_COLUMN_WIDTH = 130;
    private static final int PADDING = 10;
    private static final int STATUS_HEIGHT = 14;

    private static final ClientVideoSourceCatalog CATALOG = ClientVideoSourceCatalog.INSTANCE;

    private final ScreenConfigMenu menu;
    private final List<VideoSourceProvider> providers;

    private int panelLeft;
    private int panelTop;

    private VideoSourceProvider selectedProvider;
    private CatalogSourceEntry selectedSource;
    private Component subtitle = Component.empty();
    private Component statusMessage = Component.empty();

    private ProviderSelectionList providerList;
    private SourceSelectionList sourceList;
    private LumaButton applyButton;

    public ScreenConfigScreen(ScreenConfigMenu menu, Inventory playerInventory, Component title) {
        super(title);
        this.menu = menu;
        this.providers = CATALOG.getProviders();
    }

    @Override
    public ScreenConfigMenu getMenu() {
        return menu;
    }

    @Override
    protected void init() {
        super.init();
        computePanelBounds();
        initializeSelection();
        buildWidgets();
        rebuildSourceList();
        updateApplyButton();
    }

    private void computePanelBounds() {
        panelLeft = (width - PANEL_WIDTH) / 2;
        panelTop = (height - PANEL_HEIGHT) / 2;
    }

    private void buildWidgets() {
        clearWidgets();

        int contentTop = panelTop + HEADER_HEIGHT + 16;
        int footerTop = panelTop + PANEL_HEIGHT - FOOTER_HEIGHT;
        int listBottom = footerTop - STATUS_HEIGHT - PADDING - 4;

        int providerListLeft = panelLeft + PADDING;
        int providerListWidth = PROVIDER_COLUMN_WIDTH - PADDING;
        providerList = new ProviderSelectionList(this, minecraft, providerListWidth, contentTop, listBottom);
        providerList.setLeftPos(providerListLeft);
        providerList.setProviders(providers, selectedProvider);
        addRenderableWidget(providerList);

        int sourceListLeft = panelLeft + PROVIDER_COLUMN_WIDTH + PADDING;
        int sourceListWidth = PANEL_WIDTH - PROVIDER_COLUMN_WIDTH - PADDING * 2;
        sourceList = new SourceSelectionList(this, minecraft, sourceListWidth, contentTop, listBottom);
        sourceList.setLeftPos(sourceListLeft);
        addRenderableWidget(sourceList);

        int buttonY = panelTop + PANEL_HEIGHT - FOOTER_HEIGHT + 8;
        int buttonH = 20;

        addRenderableWidget(LumaButton.secondary(
                panelLeft + PADDING,
                buttonY,
                72,
                buttonH,
                Component.translatable("gui.lumavision.screen_config.cancel"),
                button -> onClose()
        ));

        addRenderableWidget(LumaButton.secondary(
                panelLeft + PANEL_WIDTH / 2 - 40,
                buttonY,
                80,
                buttonH,
                Component.translatable("gui.lumavision.screen_config.refresh"),
                button -> refreshSources()
        ));

        applyButton = addRenderableWidget(LumaButton.primary(
                panelLeft + PANEL_WIDTH - PADDING - 80,
                buttonY,
                80,
                buttonH,
                Component.translatable("gui.lumavision.screen_config.apply"),
                button -> applySelection()
        ));
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

    public void selectProvider(VideoSourceProvider provider) {
        selectedProvider = provider;
        selectedSource = null;
        if (providerList != null) {
            providerList.setProviders(providers, selectedProvider);
        }
        rebuildSourceList();
        updateApplyButton();
    }

    public boolean isProviderSelected(VideoSourceProvider provider) {
        return selectedProvider != null
                && selectedProvider.providerId().equals(provider.providerId());
    }

    public boolean isProviderSelectable(VideoSourceProvider provider) {
        return provider.isImplemented();
    }

    public void selectSource(CatalogSourceEntry source) {
        selectedSource = source;
        updateApplyButton();
    }

    public boolean isSourceSelected(CatalogSourceEntry source) {
        return selectedSource != null
                && selectedSource.descriptor().cacheKey().equals(source.descriptor().cacheKey());
    }

    private void refreshSources() {
        CATALOG.refreshProvider(selectedProvider.providerId());
        rebuildSourceList();
        updateApplyButton();
    }

    private void rebuildSourceList() {
        if (sourceList == null || selectedProvider == null) {
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
            String reason = Component.translatable("gui.lumavision.screen_config.provider_unavailable").getString();
            String detail = selectedProvider.unavailableReason();
            if (detail != null && !detail.isBlank()) {
                reason = detail;
            }
            statusMessage = Component.literal(reason);
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

    private void updateApplyButton() {
        if (applyButton != null) {
            applyButton.active = selectedSource != null && selectedSource.selectable();
        }
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
    public void tick() {
        super.tick();
        if (minecraft.player != null && !menu.stillValid(minecraft.player)) {
            onClose();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            if (applyButton != null && applyButton.active) {
                applySelection();
                return true;
            }
        }
        if (minecraft.options.keyInventory.isActiveAndMatches(InputConstants.getKey(keyCode, scanCode))) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        GuiTheme.drawOverlay(graphics, width, height);
        GuiTheme.drawPanel(graphics, panelLeft, panelTop, PANEL_WIDTH, PANEL_HEIGHT);
        GuiTheme.drawHeader(graphics, panelLeft, panelTop, PANEL_WIDTH, HEADER_HEIGHT);

        graphics.drawString(font, title, panelLeft + PADDING, panelTop + 10, GuiTheme.TEXT_PRIMARY, false);

        if (!subtitle.getString().isEmpty()) {
            graphics.drawString(font, subtitle, panelLeft + PADDING, panelTop + 24, GuiTheme.TEXT_SECONDARY, false);
        }

        int columnTop = panelTop + HEADER_HEIGHT;
        int footerTop = panelTop + PANEL_HEIGHT - FOOTER_HEIGHT;
        GuiTheme.drawDividerHorizontal(graphics, panelLeft + 1, panelLeft + PANEL_WIDTH - 1, columnTop);
        GuiTheme.drawDividerVertical(
                graphics,
                panelLeft + PROVIDER_COLUMN_WIDTH,
                columnTop + 1,
                footerTop
        );
        GuiTheme.drawDividerHorizontal(graphics, panelLeft + 1, panelLeft + PANEL_WIDTH - 1, footerTop);

        int labelY = panelTop + HEADER_HEIGHT + 2;
        graphics.drawString(
                font,
                Component.translatable("gui.lumavision.screen_config.provider"),
                panelLeft + PADDING,
                labelY,
                GuiTheme.TEXT_SECONDARY,
                false
        );
        graphics.drawString(
                font,
                Component.translatable("gui.lumavision.screen_config.sources"),
                panelLeft + PROVIDER_COLUMN_WIDTH + PADDING,
                labelY,
                GuiTheme.TEXT_SECONDARY,
                false
        );

        int statusY = footerTop - STATUS_HEIGHT - 2;
        if (!statusMessage.getString().isEmpty()) {
            graphics.drawString(
                    font,
                    statusMessage,
                    panelLeft + PROVIDER_COLUMN_WIDTH + PADDING,
                    statusY,
                    GuiTheme.TEXT_ERROR,
                    false
            );
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
