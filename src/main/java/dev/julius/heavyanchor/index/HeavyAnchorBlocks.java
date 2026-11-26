package dev.julius.heavyanchor.index;

import dev.julius.heavyanchor.CreateHeavyAnchor;
import dev.julius.heavyanchor.block.HeavyAnchorBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class HeavyAnchorBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CreateHeavyAnchor.MODID);
    public static final DeferredBlock<HeavyAnchorBlock> HEAVY_ANCHOR = BLOCKS.register("heavy_anchor", () ->
            new HeavyAnchorBlock(BlockBehaviour.Properties.of()
                    .strength(4.0F, 1200.0F)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .pushReaction(PushReaction.BLOCK)));

    private HeavyAnchorBlocks() {}
}
