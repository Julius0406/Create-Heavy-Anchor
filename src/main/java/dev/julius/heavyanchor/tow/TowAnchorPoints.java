package dev.julius.heavyanchor.tow;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.julius.heavyanchor.block.HeavyAnchorBlockEntity;
import dev.julius.heavyanchor.index.HeavyAnchorBlocks;
import dev.ryanhcode.sable.Sable;
import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBehavior;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

public final class TowAnchorPoints {
    private TowAnchorPoints() {
    }

    public static boolean isTowBlockAttachment(final Level level, final BlockPos blockPos) {
        return isHeavyAnchorBlock(level, blockPos) || isTowBlockAttachment(level.getBlockEntity(blockPos));
    }

    public static boolean isHeavyAnchorBlock(final Level level, final BlockPos blockPos) {
        return level.getBlockState(blockPos).is(HeavyAnchorBlocks.HEAVY_ANCHOR.get())
                && level.getBlockEntity(blockPos) instanceof HeavyAnchorBlockEntity anchor
                && Sable.HELPER.getContaining(anchor) == null;
    }

    public static boolean isTowBlockAttachment(final @Nullable BlockEntity blockEntity) {
        if (blockEntity instanceof HeavyAnchorBlockEntity anchor) {
            return Sable.HELPER.getContaining(anchor) == null;
        }
        return getRopeHolder(blockEntity) != null;
    }

    public static boolean hasSimulatedRopeAttachment(final Level level, final BlockPos blockPos) {
        final RopeStrandHolderBehavior ropeHolder = getRopeHolder(level.getBlockEntity(blockPos));
        return ropeHolder != null && ropeHolder.isAttached();
    }

    public static @Nullable Vector3d resolveBlockTowPoint(final @Nullable BlockEntity blockEntity) {
        if (blockEntity instanceof HeavyAnchorBlockEntity anchor) {
            if (Sable.HELPER.getContaining(anchor) != null) return null;
            final BlockPos pos = anchor.getBlockPos();
            return new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        }
        final RopeStrandHolderBehavior ropeHolder = getRopeHolder(blockEntity);
        if (ropeHolder != null) {
            final Vec3 attachmentPoint = ropeHolder.getAttachmentPoint();
            return new Vector3d(attachmentPoint.x, attachmentPoint.y, attachmentPoint.z);
        }

        return null;
    }

    public static @Nullable Vec3 resolveVisualBlockTowPoint(final @Nullable BlockEntity blockEntity) {
        if (blockEntity instanceof HeavyAnchorBlockEntity anchor) {
            if (Sable.HELPER.getContaining(anchor) != null) return null;
            return Vec3.atCenterOf(anchor.getBlockPos());
        }
        final RopeStrandHolderBehavior ropeHolder = getRopeHolder(blockEntity);
        return ropeHolder != null ? ropeHolder.getVisualAttachmentPoint() : null;
    }

    private static @Nullable RopeStrandHolderBehavior getRopeHolder(final @Nullable BlockEntity blockEntity) {
        if (blockEntity instanceof final SmartBlockEntity smartBlockEntity) {
            return smartBlockEntity.getBehaviour(RopeStrandHolderBehavior.TYPE);
        }

        return null;
    }

    public static Vec3 resolveEntityTowPoint(final Entity entity) {
        return entity.getBoundingBox().getCenter();
    }
}
