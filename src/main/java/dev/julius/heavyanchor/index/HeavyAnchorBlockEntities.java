package dev.julius.heavyanchor.index;

import dev.julius.heavyanchor.CreateHeavyAnchor;
import dev.julius.heavyanchor.block.HeavyAnchorBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class HeavyAnchorBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CreateHeavyAnchor.MODID);
    public static final Supplier<BlockEntityType<HeavyAnchorBlockEntity>> HEAVY_ANCHOR = BLOCK_ENTITY_TYPES.register("heavy_anchor", () ->
            BlockEntityType.Builder.of(HeavyAnchorBlockEntities::create, HeavyAnchorBlocks.HEAVY_ANCHOR.get()).build(null));

    private static HeavyAnchorBlockEntity create(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        return new HeavyAnchorBlockEntity(HEAVY_ANCHOR.get(), pos, state);
    }

    private HeavyAnchorBlockEntities() {}
}
