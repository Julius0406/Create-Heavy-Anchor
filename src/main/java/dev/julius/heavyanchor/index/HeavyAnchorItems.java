package dev.julius.heavyanchor.index;

import dev.julius.heavyanchor.CreateHeavyAnchor;
import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class HeavyAnchorItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CreateHeavyAnchor.MODID);
    public static final DeferredItem<BlockItem> HEAVY_ANCHOR = ITEMS.registerSimpleBlockItem(HeavyAnchorBlocks.HEAVY_ANCHOR);

    private HeavyAnchorItems() {}
}
