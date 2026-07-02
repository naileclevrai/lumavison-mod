package fr.lumavision.registry;

import fr.lumavision.LumaVisionMod;
import fr.lumavision.blockentity.CameraBlockEntity;
import fr.lumavision.blockentity.LedScreenBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Central registry for LumaVision block entities.
 * <p>
 * LED screens will store URL, NDI source, playback state, etc. in their block entities.
 */
public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, LumaVisionMod.MOD_ID);

    public static final RegistryObject<BlockEntityType<LedScreenBlockEntity>> LED_SCREEN =
            BLOCK_ENTITIES.register("led_screen",
                    () -> BlockEntityType.Builder.of(
                            LedScreenBlockEntity::new,
                            ModBlocks.LED_SCREEN.get()
                    ).build(null));

    public static final RegistryObject<BlockEntityType<CameraBlockEntity>> CAMERA =
            BLOCK_ENTITIES.register("camera",
                    () -> BlockEntityType.Builder.of(
                            CameraBlockEntity::new,
                            ModBlocks.CAMERA.get(),
                            ModBlocks.PTZ_CAMERA.get()
                    ).build(null));

    private ModBlockEntities() {
    }

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }
}
