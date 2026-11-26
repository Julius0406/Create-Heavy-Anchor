package dev.julius.heavyanchor;

import com.mojang.logging.LogUtils;
import dev.julius.heavyanchor.config.HeavyAnchorConfig;
import dev.julius.heavyanchor.index.HeavyAnchorDataComponents;
import dev.julius.heavyanchor.network.HeavyAnchorPacketManager;
import dev.julius.heavyanchor.platform.HeavyAnchorPlatform;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import dev.julius.heavyanchor.index.HeavyAnchorEntities;
import dev.julius.heavyanchor.index.HeavyAnchorBlocks;
import dev.julius.heavyanchor.index.HeavyAnchorBlockEntities;
import dev.julius.heavyanchor.index.HeavyAnchorItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

@Mod(CreateHeavyAnchor.MODID)
public class CreateHeavyAnchor {
    public static final String MODID = "heavyanchor";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CreateHeavyAnchor(final IEventBus modEventBus, final ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreative);
        HeavyAnchorEntities.ENTITY_TYPES.register(modEventBus); // retained for worlds from 1.0.0
        HeavyAnchorBlocks.BLOCKS.register(modEventBus);
        HeavyAnchorBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        HeavyAnchorItems.ITEMS.register(modEventBus);
        dev.julius.heavyanchor.index.AnchorMenus.MENUS.register(modEventBus);

        HeavyAnchorDataComponents.register();
        HeavyAnchorPacketManager.init();
        HeavyAnchorPlatform.init();
        modContainer.registerConfig(ModConfig.Type.SERVER, HeavyAnchorConfig.SERVER_SPEC);
    }

    private void addCreative(final BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(HeavyAnchorItems.HEAVY_ANCHOR);
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Initializing {}", MODID);
    }

    public static ResourceLocation path(final String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
