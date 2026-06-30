package fr.lumavision.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import fr.lumavision.blockentity.LedScreenBlockEntity;
import fr.lumavision.client.gui.components.GuiTheme;
import fr.lumavision.client.gui.components.LumaButton;
import fr.lumavision.client.gui.components.ProviderSelectionList;
import fr.lumavision.client.gui.components.SourceSelectionList;
import fr.lumavision.client.video.catalog.ClientVideoSourceCatalog;
import fr.lumavision.network.ModNetworking;
import fr.lumavision.network.SetScreenDisplayPacket;
import fr.lumavision.network.SetScreenSourcePacket;
import fr.lumavision.screen.DisplayMode;
import fr.lumavision.screen.ScreenDisplaySettings;
import fr.lumavision.screen.ScreenWallPermissions;
import fr.lumavision.video.VideoSourceDescriptor;
import fr.lumavision.video.provider.CatalogSourceEntry;
import fr.lumavision.video.provider.VideoSourceProvider;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.UUID;

/**
 * In-game screen for choosing media sources and display properties.
 */
@OnlyIn(Dist.CLIENT)
public final class ScreenConfigScreen extends Screen implements MenuAccess<ScreenConfigMenu> {

    private enum ConfigTab {
        SOURCE,
        DISPLAY
    }

    private static final int PANEL_WIDTH = 380;
    private static final int PANEL_HEIGHT_SOURCE = 300;
    private static final int PANEL_HEIGHT_DISPLAY = 340;
    private static final int HEADER_HEIGHT = 44;
    private static final int TAB_HEIGHT = 22;
    private static final int FOOTER_HEIGHT = 36;
    private static final int PROVIDER_COLUMN_WIDTH = 130;
    private static final int PADDING = 10;
    private static final int STATUS_HEIGHT = 14;

    private static final ClientVideoSourceCatalog CATALOG = ClientVideoSourceCatalog.INSTANCE;

    private final ScreenConfigMenu menu;
    private final List<VideoSourceProvider> providers;

    private int panelLeft;
    private int panelTop;
    private int panelHeight;

    private ConfigTab activeTab = ConfigTab.SOURCE;
    private boolean canConfigure = true;

    private VideoSourceProvider selectedProvider;
    private CatalogSourceEntry selectedSource;
    private ScreenDisplaySettings displaySettings = ScreenDisplaySettings.DEFAULT;
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

    /** Exposed for selection lists ({@code AbstractSelectionList} needs screen height). */
    public int guiHeight() {
        return height;
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
        panelHeight = activeTab == ConfigTab.SOURCE ? PANEL_HEIGHT_SOURCE : PANEL_HEIGHT_DISPLAY;
        panelLeft = (width - PANEL_WIDTH) / 2;
        panelTop = (height - panelHeight) / 2;
    }

    private void buildWidgets() {
        clearWidgets();

        int tabY = panelTop + HEADER_HEIGHT + 4;
        addRenderableWidget(LumaButton.secondary(
                panelLeft + PADDING,
                tabY,
                80,
                TAB_HEIGHT,
                Component.translatable("gui.lumavision.screen_config.tab_source"),
                button -> switchTab(ConfigTab.SOURCE)
        ));
        addRenderableWidget(LumaButton.secondary(
                panelLeft + PADDING + 86,
                tabY,
                80,
                TAB_HEIGHT,
                Component.translatable("gui.lumavision.screen_config.tab_display"),
                button -> switchTab(ConfigTab.DISPLAY)
        ));

        if (activeTab == ConfigTab.SOURCE) {
            buildSourceWidgets();
        } else {
            buildDisplayWidgets();
        }

        int buttonY = panelTop + panelHeight - FOOTER_HEIGHT + 8;
        int buttonH = 20;

        addRenderableWidget(LumaButton.secondary(
                panelLeft + PADDING,
                buttonY,
                72,
                buttonH,
                Component.translatable("gui.lumavision.screen_config.cancel"),
                button -> onClose()
        ));

        if (activeTab == ConfigTab.SOURCE) {
            addRenderableWidget(LumaButton.secondary(
                    panelLeft + PANEL_WIDTH / 2 - 40,
                    buttonY,
                    80,
                    buttonH,
                    Component.translatable("gui.lumavision.screen_config.refresh"),
                    button -> refreshSources()
            ));
        }

        applyButton = addRenderableWidget(LumaButton.primary(
                panelLeft + PANEL_WIDTH - PADDING - 80,
                buttonY,
                80,
                buttonH,
                Component.translatable("gui.lumavision.screen_config.apply"),
                button -> applySelection()
        ));
        applyButton.active = canConfigure;
        updateApplyButton();
    }

    private void switchTab(ConfigTab tab) {
        activeTab = tab;
        computePanelBounds();
        buildWidgets();
        if (activeTab == ConfigTab.SOURCE) {
            rebuildSourceList();
        }
        updateApplyButton();
    }

    private void buildSourceWidgets() {
        int contentTop = panelTop + HEADER_HEIGHT + TAB_HEIGHT + 16;
        int footerTop = panelTop + panelHeight - FOOTER_HEIGHT;
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
    }

    private void buildDisplayWidgets() {
        int left = panelLeft + PADDING;
        int right = panelLeft + PANEL_WIDTH - PADDING;
        int colWidth = (right - left - 8) / 2;
        int y = panelTop + HEADER_HEIGHT + TAB_HEIGHT + 20;

        LumaButton rotationButton = addRenderableWidget(LumaButton.secondary(left, y, colWidth, 20,
                Component.translatable("gui.lumavision.screen_config.rotation", displaySettings.rotation()),
                button -> {
                    int next = (displaySettings.rotation() + 90) % 360;
                    displaySettings = displaySettings.withRotation(next);
                    buildWidgets();
                }));
        rotationButton.active = canConfigure;

        LumaButton modeButton = addRenderableWidget(LumaButton.secondary(left + colWidth + 8, y, colWidth, 20,
                Component.translatable("gui.lumavision.screen_config.display_mode",
                        Component.translatable("gui.lumavision.screen_config.mode." + displaySettings.mode().name().toLowerCase())),
                button -> {
                    displaySettings = displaySettings.withMode(displaySettings.mode().next());
                    buildWidgets();
                }));
        modeButton.active = canConfigure;

        y += 28;
        LumaButton mirrorHButton = addRenderableWidget(LumaButton.secondary(left, y, colWidth, 20,
                Component.translatable("gui.lumavision.screen_config.mirror_h",
                        mirrorLabel(displaySettings.mirrorH())),
                button -> {
                    displaySettings = displaySettings.withMirrorH(!displaySettings.mirrorH());
                    buildWidgets();
                }));
        mirrorHButton.active = canConfigure;

        LumaButton mirrorVButton = addRenderableWidget(LumaButton.secondary(left + colWidth + 8, y, colWidth, 20,
                Component.translatable("gui.lumavision.screen_config.mirror_v",
                        mirrorLabel(displaySettings.mirrorV())),
                button -> {
                    displaySettings = displaySettings.withMirrorV(!displaySettings.mirrorV());
                    buildWidgets();
                }));
        mirrorVButton.active = canConfigure;

        y += 32;
        y = addSlider(left, right, y, "brightness", 0.0F, 2.0F, displaySettings.brightness(),
                value -> displaySettings = displaySettings.withBrightness(value));
        y = addSlider(left, right, y, "contrast", 0.0F, 2.0F, displaySettings.contrast(),
                value -> displaySettings = displaySettings.withContrast(value));
        y = addSlider(left, right, y, "gamma", 0.5F, 2.5F, displaySettings.gamma(),
                value -> displaySettings = displaySettings.withGamma(value));
        addSlider(left, right, y, "color_temp", -1.0F, 1.0F, displaySettings.colorTemp(),
                value -> displaySettings = displaySettings.withColorTemp(value));
    }

    private Component mirrorLabel(boolean enabled) {
        return Component.translatable(enabled
                ? "gui.lumavision.screen_config.on"
                : "gui.lumavision.screen_config.off");
    }

    private int addSlider(int left, int right, int y, String key, float min, float max, float current,
                          java.util.function.Consumer<Float> onChange) {
        float normalized = (current - min) / (max - min);
        AbstractSliderButton slider = new AbstractSliderButton(
                left, y, right - left, 18,
                sliderLabel(key, current),
                normalized
        ) {
            @Override
            protected void updateMessage() {
                float value = (float) (min + (max - min) * this.value);
                setMessage(sliderLabel(key, value));
            }

            @Override
            protected void applyValue() {
                float value = (float) (min + (max - min) * this.value);
                onChange.accept(value);
            }
        };
        slider.active = canConfigure;
        addRenderableWidget(slider);
        return y + 24;
    }

    private Component sliderLabel(String key, float value) {
        return Component.translatable("gui.lumavision.screen_config." + key, String.format("%.2f", value));
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

            displaySettings = LedScreenBlockEntity.resolveDisplaySettings(minecraft.level, screen.getGroupMembership());

            UUID owner = screen.getOwnerUuid();
            if (minecraft.player != null && owner != null) {
                canConfigure = ScreenWallPermissions.canConfigure(minecraft.player, minecraft.level, menu.getGroupOrigin());
                if (!canConfigure) {
                    statusMessage = Component.translatable("gui.lumavision.screen_config.read_only");
                }
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
        if (!canConfigure) {
            return;
        }
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
        return canConfigure && provider.isImplemented();
    }

    public void selectSource(CatalogSourceEntry source) {
        if (!canConfigure) {
            return;
        }
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

        if (!canConfigure && !statusMessage.getString().isEmpty()) {
            // keep read-only message
        } else if (!selectedProvider.isImplemented()) {
            statusMessage = Component.translatable("gui.lumavision.screen_config.provider_unavailable");
            sourceList.setSources(List.of(), null);
            return;
        } else if (!selectedProvider.isEnabled()) {
            statusMessage = Component.translatable("gui.lumavision.screen_config.provider_disabled");
            sourceList.setSources(List.of(), null);
            return;
        } else if (!selectedProvider.isAvailable()) {
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
        } else if (canConfigure || statusMessage.getString().isEmpty()) {
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
        if (applyButton == null) {
            return;
        }
        if (!canConfigure) {
            applyButton.active = false;
            return;
        }
        if (activeTab == ConfigTab.DISPLAY) {
            applyButton.active = true;
            return;
        }
        applyButton.active = selectedSource != null && selectedSource.selectable();
    }

    private void applySelection() {
        if (!canConfigure) {
            return;
        }

        if (activeTab == ConfigTab.SOURCE) {
            if (selectedSource == null || !selectedSource.selectable()) {
                return;
            }
            ModNetworking.CHANNEL.sendToServer(new SetScreenSourcePacket(
                    menu.getGroupOrigin(),
                    selectedSource.descriptor().toSourceId()
            ));
        } else {
            ModNetworking.CHANNEL.sendToServer(new SetScreenDisplayPacket(
                    menu.getGroupOrigin(),
                    displaySettings
            ));
        }
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
    public void renderBackground(GuiGraphics graphics) {
        // Custom background in render() — skip vanilla dirt/menu tiles.
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        GuiTheme.drawOverlay(graphics, width, height);
        GuiTheme.drawPanel(graphics, panelLeft, panelTop, PANEL_WIDTH, panelHeight);
        GuiTheme.drawHeader(graphics, panelLeft, panelTop, PANEL_WIDTH, HEADER_HEIGHT);

        graphics.drawString(font, title, panelLeft + PADDING, panelTop + 10, GuiTheme.TEXT_PRIMARY, false);

        if (!subtitle.getString().isEmpty()) {
            graphics.drawString(font, subtitle, panelLeft + PADDING, panelTop + 24, GuiTheme.TEXT_SECONDARY, false);
        }

        int columnTop = panelTop + HEADER_HEIGHT;
        int footerTop = panelTop + panelHeight - FOOTER_HEIGHT;
        GuiTheme.drawDividerHorizontal(graphics, panelLeft + 1, panelLeft + PANEL_WIDTH - 1, columnTop);
        GuiTheme.drawDividerHorizontal(graphics, panelLeft + 1, panelLeft + PANEL_WIDTH - 1, footerTop);

        if (activeTab == ConfigTab.SOURCE) {
            if (providerList != null) {
                GuiTheme.drawListBackground(
                        graphics,
                        providerList.getLeft(),
                        providerList.getTop(),
                        providerList.getWidth(),
                        providerList.getBottom() - providerList.getTop()
                );
            }
            if (sourceList != null) {
                GuiTheme.drawListBackground(
                        graphics,
                        sourceList.getLeft(),
                        sourceList.getTop(),
                        sourceList.getWidth(),
                        sourceList.getBottom() - sourceList.getTop()
                );
            }

            GuiTheme.drawDividerVertical(
                    graphics,
                    panelLeft + PROVIDER_COLUMN_WIDTH,
                    columnTop + TAB_HEIGHT + 1,
                    footerTop
            );
            int labelY = panelTop + HEADER_HEIGHT + TAB_HEIGHT + 2;
            graphics.drawString(font, Component.translatable("gui.lumavision.screen_config.provider"),
                    panelLeft + PADDING, labelY, GuiTheme.TEXT_SECONDARY, false);
            graphics.drawString(font, Component.translatable("gui.lumavision.screen_config.sources"),
                    panelLeft + PROVIDER_COLUMN_WIDTH + PADDING, labelY, GuiTheme.TEXT_SECONDARY, false);

            int statusY = footerTop - STATUS_HEIGHT - 2;
            if (!statusMessage.getString().isEmpty()) {
                graphics.drawString(font, statusMessage,
                        panelLeft + PROVIDER_COLUMN_WIDTH + PADDING, statusY, GuiTheme.TEXT_ERROR, false);
            }
        } else {
            graphics.drawString(font, Component.translatable("gui.lumavision.screen_config.display_title"),
                    panelLeft + PADDING, panelTop + HEADER_HEIGHT + TAB_HEIGHT + 4, GuiTheme.TEXT_SECONDARY, false);
            if (!canConfigure && !statusMessage.getString().isEmpty()) {
                graphics.drawString(font, statusMessage,
                        panelLeft + PADDING, footerTop - STATUS_HEIGHT - 2, GuiTheme.TEXT_ERROR, false);
            }
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
