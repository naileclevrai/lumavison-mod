package fr.lumavision.registry;

import fr.lumavision.LumaVisionMod;
import fr.lumavision.blockentity.LedScreenBlockEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Central registry for LumaVision items.
 * <p>
 * Placeable blocks typically use {@link BlockItem}.
 */
public final class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, LumaVisionMod.MOD_ID);

    public static final RegistryObject<Item> LED_SCREEN = ITEMS.register("led_screen",
            () -> new BlockItem(ModBlocks.LED_SCREEN.get(), new Item.Properties()));

    public static final RegistryObject<Item> CAMERA = ITEMS.register("camera",
            () -> new BlockItem(ModBlocks.CAMERA.get(), new Item.Properties()));

    public static final RegistryObject<Item> PTZ_CAMERA = ITEMS.register("ptz_camera",
            () -> new BlockItem(ModBlocks.PTZ_CAMERA.get(), new Item.Properties()));

    public static final RegistryObject<Item> CAMERA_RAIL = ITEMS.register("camera_rail",
            () -> new BlockItem(ModBlocks.CAMERA_RAIL.get(), new Item.Properties()));

    public static final RegistryObject<Item> CAMERA_MOUNT = ITEMS.register("camera_mount",
            () -> new BlockItem(ModBlocks.CAMERA_MOUNT.get(), new Item.Properties()));

    public static final RegistryObject<Item> CAMERA_BOOM = ITEMS.register("camera_boom",
            () -> new BlockItem(ModBlocks.CAMERA_BOOM.get(), new Item.Properties()));

    public static final RegistryObject<Item> CAMERA_CRANE = ITEMS.register("camera_crane",
            () -> new BlockItem(ModBlocks.CAMERA_CRANE.get(), new Item.Properties()));

    private ModItems() {
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
