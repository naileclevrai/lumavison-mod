package fr.lumavision.registry;

import fr.lumavision.LumaVisionMod;
import fr.lumavision.client.gui.ScreenConfigMenu;
import fr.lumavision.menu.CameraConfigMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, LumaVisionMod.MOD_ID);

    public static final RegistryObject<MenuType<ScreenConfigMenu>> SCREEN_CONFIG =
            MENUS.register("screen_config", () -> IForgeMenuType.create(ScreenConfigMenu::new));

    public static final RegistryObject<MenuType<CameraConfigMenu>> CAMERA_CONFIG =
            MENUS.register("camera_config", () -> IForgeMenuType.create(CameraConfigMenu::new));

    private ModMenuTypes() {
    }

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}
