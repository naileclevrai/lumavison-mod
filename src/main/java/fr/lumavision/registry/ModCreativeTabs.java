package fr.lumavision.registry;

import fr.lumavision.LumaVisionMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Onglet créatif dédié aux écrans et accessoires LumaVision.
 */
public final class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, LumaVisionMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> MAIN = CREATIVE_TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.lumavision"))
                    .icon(() -> new ItemStack(net.minecraft.world.item.Items.GLOWSTONE))
                    .displayItems((parameters, output) -> {
                        ModBlocks.BLOCKS.getEntries().forEach(holder -> output.accept(holder.get()));
                        ModItems.ITEMS.getEntries().forEach(holder -> {
                            if (!(holder.get() instanceof net.minecraft.world.item.BlockItem)) {
                                output.accept(holder.get());
                            }
                        });
                    })
                    .build());

    private ModCreativeTabs() {
    }

    public static void register(IEventBus bus) {
        CREATIVE_TABS.register(bus);
    }
}
