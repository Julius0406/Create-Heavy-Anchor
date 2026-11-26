package dev.julius.heavyanchor.index;

import dev.julius.heavyanchor.CreateHeavyAnchor;
import dev.julius.heavyanchor.entity.HeavyAnchorEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class HeavyAnchorEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, CreateHeavyAnchor.MODID);
    public static final Supplier<EntityType<HeavyAnchorEntity>> HEAVY_ANCHOR = ENTITY_TYPES.register("heavy_anchor", () ->
            EntityType.Builder.<HeavyAnchorEntity>of(HeavyAnchorEntity::new, MobCategory.MISC)
                    .sized(0.65F, 0.55F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("heavy_anchor"));

    private HeavyAnchorEntities() {}
}
