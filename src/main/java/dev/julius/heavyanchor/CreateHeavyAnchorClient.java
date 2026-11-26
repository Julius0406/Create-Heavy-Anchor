package dev.julius.heavyanchor;

import dev.julius.heavyanchor.client.HeavyAnchorClientEvents;
import dev.julius.heavyanchor.client.HeavyAnchorEntityRenderer;
import dev.julius.heavyanchor.index.HeavyAnchorEntities;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = CreateHeavyAnchor.MODID, dist = Dist.CLIENT)
public class CreateHeavyAnchorClient {
    public CreateHeavyAnchorClient(final IEventBus modEventBus, final ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modEventBus.addListener(this::registerRenderers);
        modEventBus.addListener((net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) -> event.register(dev.julius.heavyanchor.index.AnchorMenus.ANCHOR.get(), dev.julius.heavyanchor.client.AnchorScreen::new));
        NeoForge.EVENT_BUS.register(HeavyAnchorClientEvents.class);
    }

    private void registerRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(HeavyAnchorEntities.HEAVY_ANCHOR.get(), HeavyAnchorEntityRenderer::new);
    }
}
