package dev.julius.heavyanchor.event;

import dev.julius.heavyanchor.CreateHeavyAnchor;
import dev.julius.heavyanchor.interaction.HeavyAnchorInteractionHandler;
import dev.julius.heavyanchor.index.HeavyAnchorBlocks;
import dev.julius.heavyanchor.tow.TowRopeSavedData;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = CreateHeavyAnchor.MODID)
public final class HeavyAnchorNeoForgeCommonEvents {
    private HeavyAnchorNeoForgeCommonEvents() {
    }

    @SubscribeEvent
    public static void onLevelTickPost(final LevelTickEvent.Post event) {
        if (event.getLevel() instanceof final ServerLevel serverLevel) {
            HeavyAnchorCommonEvents.onServerLevelTickEnd(serverLevel);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(final PlayerInteractEvent.RightClickBlock event) {
        HeavyAnchorInteractionHandler.handleRightClickBlock(event);
    }

    @SubscribeEvent
    public static void onBreakAnchorBlock(final BlockEvent.BreakEvent event) {
        if (!event.getState().is(HeavyAnchorBlocks.HEAVY_ANCHOR.get())) return;
        if (event.getLevel() instanceof ServerLevel serverLevel
                && TowRopeSavedData.get(serverLevel).hasBlockAttachment(event.getPos())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(final PlayerInteractEvent.EntityInteract event) {
        HeavyAnchorInteractionHandler.handleEntityInteract(event);
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(final PlayerInteractEvent.EntityInteractSpecific event) {
        HeavyAnchorInteractionHandler.handleEntityInteract(event);
    }

    @SubscribeEvent
    public static void onRightClickItem(final PlayerInteractEvent.RightClickItem event) {
        HeavyAnchorInteractionHandler.handleRightClickItem(event);
    }
}
