package dev.julius.heavyanchor.interaction;

import dev.julius.heavyanchor.data.PendingTowEndpoint;
import dev.julius.heavyanchor.index.HeavyAnchorDataComponents;
import dev.julius.heavyanchor.tow.TowAnchorPoints;
import dev.julius.heavyanchor.tow.TowRopeSavedData;
import dev.simulated_team.simulated.index.SimDataComponents;
import dev.simulated_team.simulated.index.SimItems;
import dev.simulated_team.simulated.index.SimTags;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import dev.julius.heavyanchor.entity.HeavyAnchorEntity;
import dev.simulated_team.simulated.content.blocks.rope.rope_winch.RopeWinchBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jetbrains.annotations.Nullable;

public final class HeavyAnchorInteractionHandler {
    private HeavyAnchorInteractionHandler() {
    }

    public static void handleRightClickBlock(final PlayerInteractEvent.RightClickBlock event) {
        final Level level = event.getLevel();
        final BlockPos pos = event.getPos();
        final Player player = event.getEntity();
        final InteractionHand hand = event.getHand();
        final ItemStack stack = player.getItemInHand(hand);

        if (level.getBlockEntity(pos) instanceof dev.julius.heavyanchor.block.HeavyAnchorBlockEntity anchor) {
            if (player.isShiftKeyDown() && !stack.is(SimItems.ROPE_COUPLING.get())) {
                if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                    serverPlayer.openMenu(new net.minecraft.world.SimpleMenuProvider(
                            (id, inv, who) -> new dev.julius.heavyanchor.menu.AnchorMenu(id, inv, anchor),
                            net.minecraft.network.chat.Component.translatable("heavyanchor.menu.title")));
                }
                consume(event, InteractionResult.sidedSuccess(level.isClientSide));
            }
            return; // ordinary rope use goes through Simulated itself
        }

        if (!TowAnchorPoints.isTowBlockAttachment(level, pos)) {
            return;
        }

        if (stack.is(SimTags.Items.DESTROYS_ROPE)) {
            final TowRopeSavedData data = getServerData(level);
            if (data != null && data.hasBlockAttachment(pos)) {
                // A deployed anchor may only be released by winding the winch back in.
                consume(event, InteractionResult.sidedSuccess(level.isClientSide));
            }
            return;
        }

        if (!stack.is(SimItems.ROPE_COUPLING.get())) {
            return;
        }

        if (player.isShiftKeyDown()) {
            clearConnectionState(stack);
            consume(event, InteractionResult.sidedSuccess(level.isClientSide));
            return;
        }

        final PendingTowEndpoint pending = stack.get(HeavyAnchorDataComponents.PENDING_TOW_ENDPOINT);
        final boolean occupiedBySimulatedRope = TowAnchorPoints.hasSimulatedRopeAttachment(level, pos);
        final TowRopeSavedData data = getServerData(level);
        final boolean occupiedByHeavyAnchorRope = data != null && data.hasBlockAttachment(pos);
        if (occupiedBySimulatedRope || occupiedByHeavyAnchorRope) {
            clearConnectionState(stack);
            consume(event, InteractionResult.sidedSuccess(level.isClientSide));
            return;
        }

        if (pending != null && pending.isEntity()) {
            final boolean created = data != null && data.tryCreate(pos, pending.entityId());
            clearConnectionState(stack);
            if (created) {
                finalizeCreation(player, stack, pos);
            }
            consume(event, InteractionResult.sidedSuccess(level.isClientSide));
            return;
        }

        if (pending != null && pending.isBlock()) {
            clearHeavyAnchorPending(stack);
        }
    }

    public static void handleEntityInteract(final PlayerInteractEvent.EntityInteract event) {
        handleEntityInteract(event.getEntity(), event.getLevel(), event.getHand(), event.getTarget(), result -> consume(event, result));
    }

    public static void handleEntityInteract(final PlayerInteractEvent.EntityInteractSpecific event) {
        handleEntityInteract(event.getEntity(), event.getLevel(), event.getHand(), event.getTarget(), result -> consume(event, result));
    }

    private static void handleEntityInteract(final Player player,
                                             final Level level,
                                             final InteractionHand hand,
                                             final Entity target,
                                             final java.util.function.Consumer<InteractionResult> consumeAction) {
        final ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(SimItems.ROPE_COUPLING.get()) || !(target instanceof HeavyAnchorEntity anchor) || !anchor.isAlive()) {
            return;
        }

        if (player.isShiftKeyDown()) {
            clearConnectionState(stack);
            consumeAction.accept(InteractionResult.sidedSuccess(level.isClientSide));
            return;
        }

        final PendingTowEndpoint pending = stack.get(HeavyAnchorDataComponents.PENDING_TOW_ENDPOINT);
        final BlockPos firstBlock = pending != null && pending.isBlock()
                ? pending.blockPos()
                : stack.get(SimDataComponents.ROPE_FIRST_CONNECTION);
        final TowRopeSavedData data = getServerData(level);
        if (data != null) {
            if (firstBlock != null) {
                final boolean created = data.tryCreate(firstBlock, anchor.getUUID());
                clearConnectionState(stack);
                if (created) {
                    finalizeCreation(player, stack, anchor.blockPosition());
                }
            } else {
                stack.set(HeavyAnchorDataComponents.PENDING_TOW_ENDPOINT, PendingTowEndpoint.entity(anchor.getUUID()));
            }
        } else if (firstBlock != null) {
            clearConnectionState(stack);
        } else if (pending == null) {
            stack.set(HeavyAnchorDataComponents.PENDING_TOW_ENDPOINT, PendingTowEndpoint.entity(anchor.getUUID()));
        }

        consumeAction.accept(InteractionResult.sidedSuccess(level.isClientSide));
    }

    public static void handleRightClickItem(final PlayerInteractEvent.RightClickItem event) {
        final Player player = event.getEntity();
        final ItemStack stack = player.getItemInHand(event.getHand());

        if (!stack.is(SimItems.ROPE_COUPLING.get())
                || !player.isShiftKeyDown()
                || (!stack.has(HeavyAnchorDataComponents.PENDING_TOW_ENDPOINT) && !stack.has(SimDataComponents.ROPE_FIRST_CONNECTION))) {
            return;
        }

        clearConnectionState(stack);

        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        event.setCanceled(true);
    }

    private static void clearHeavyAnchorPending(final ItemStack stack) {
        stack.remove(HeavyAnchorDataComponents.PENDING_TOW_ENDPOINT);
    }

    private static void clearConnectionState(final ItemStack stack) {
        clearHeavyAnchorPending(stack);
        stack.remove(SimDataComponents.ROPE_FIRST_CONNECTION);
    }

    private static @Nullable TowRopeSavedData getServerData(final Level level) {
        return level instanceof final ServerLevel serverLevel ? TowRopeSavedData.get(serverLevel) : null;
    }

    private static void finalizeCreation(final Player player, final ItemStack stack, final BlockPos soundPos) {
        if (!player.isCreative()) {
            stack.shrink(1);
        }
        player.level().playSound(null, soundPos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.5F, 1.0F);
    }

    private static void consume(final PlayerInteractEvent.RightClickBlock event, final InteractionResult result) {
        event.setCancellationResult(result);
        event.setCanceled(true);
    }

    private static void consume(final PlayerInteractEvent.EntityInteract event, final InteractionResult result) {
        event.setCancellationResult(result);
        event.setCanceled(true);
    }

    private static void consume(final PlayerInteractEvent.EntityInteractSpecific event, final InteractionResult result) {
        event.setCancellationResult(result);
        event.setCanceled(true);
    }
}
