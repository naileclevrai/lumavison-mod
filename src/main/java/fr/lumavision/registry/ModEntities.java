package fr.lumavision.registry;

import fr.lumavision.LumaVisionMod;
import fr.lumavision.entity.CameraSeatEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, LumaVisionMod.MOD_ID);

    public static final RegistryObject<EntityType<CameraSeatEntity>> CAMERA_SEAT =
            ENTITIES.register("camera_seat", () -> EntityType.Builder.<CameraSeatEntity>of(CameraSeatEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(10)
                    .setCustomClientFactory((packet, level) -> new CameraSeatEntity(packet, level))
                    .build("camera_seat"));

    private ModEntities() {
    }

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
    }
}
