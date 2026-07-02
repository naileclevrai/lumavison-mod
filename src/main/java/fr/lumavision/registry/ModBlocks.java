package fr.lumavision.registry;

import fr.lumavision.LumaVisionMod;
import fr.lumavision.block.CameraBlock;
import fr.lumavision.block.CameraBoomBlock;
import fr.lumavision.block.CameraRailBlock;
import fr.lumavision.block.LedScreenBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Central registry for LumaVision blocks (LED screens, controllers, etc.).
 * <p>
 * Add new blocks via {@code BLOCKS.register("name", () -> new MyBlock(...))}.
 */
public final class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, LumaVisionMod.MOD_ID);

    public static final RegistryObject<LedScreenBlock> LED_SCREEN = BLOCKS.register("led_screen",
            () -> new LedScreenBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(1.5F, 6.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .isViewBlocking((blockState, level, pos) -> false)
                    .isSuffocating((blockState, level, pos) -> false)));

    public static final RegistryObject<CameraBlock> CAMERA = BLOCKS.register("camera",
            () -> new CameraBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(1.5F, 6.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final RegistryObject<CameraBlock> PTZ_CAMERA = BLOCKS.register("ptz_camera",
            () -> new CameraBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(1.5F, 6.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final RegistryObject<CameraRailBlock> CAMERA_RAIL = BLOCKS.register("camera_rail",
            () -> new CameraRailBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(1.5F, 6.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final RegistryObject<Block> CAMERA_MOUNT = BLOCKS.register("camera_mount",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(1.5F, 6.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final RegistryObject<CameraBoomBlock> CAMERA_BOOM = BLOCKS.register("camera_boom",
            () -> new CameraBoomBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(1.5F, 6.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    private ModBlocks() {
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
