package dev.julius.heavyanchor.tow;

import com.mojang.serialization.DataResult;
import dev.julius.heavyanchor.config.HeavyAnchorConfig;
import dev.julius.heavyanchor.block.HeavyAnchorBlockEntity;
import dev.julius.heavyanchor.entity.HeavyAnchorEntity;
import dev.julius.heavyanchor.index.HeavyAnchorItems;
import dev.julius.heavyanchor.network.packet.ClientboundTowRopeDataPacket;
import dev.julius.heavyanchor.network.packet.ClientboundTowRopeRemovePacket;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.simulated_team.simulated.index.SimItems;
import dev.simulated_team.simulated.content.blocks.rope.rope_winch.RopeWinchBlockEntity;
import dev.simulated_team.simulated.service.SimConfigService;
import foundry.veil.api.network.VeilPacketManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class TowRopeSavedData extends SavedData {
    public static final String ID = "heavyanchor_tow_ropes";

    private final Map<UUID, ServerTowRope> ropes = new LinkedHashMap<>();
    private ServerLevel level;

    public TowRopeSavedData() {
        this(null);
    }

    public TowRopeSavedData(final @Nullable ServerLevel level) {
        this.level = level;
    }

    private static TowRopeSavedData create(final ServerLevel level, final CompoundTag tag) {
        final TowRopeSavedData data = new TowRopeSavedData(level);
        final ListTag list = tag.getList(ID, Tag.TAG_COMPOUND);
        for (final Tag entry : list) {
            if (!(entry instanceof CompoundTag compoundTag)) {
                continue;
            }

            final DataResult<TowRopeState> decoded = TowRopeState.CODEC.parse(NbtOps.INSTANCE, compoundTag);
            decoded.result().ifPresent(state -> data.ropes.put(state.ropeId(), new ServerTowRope(state.ropeId(), state.startAttachment(), state.endAttachment(), state.points())));
        }
        return data;
    }

    public static TowRopeSavedData get(final ServerLevel level) {
        final TowRopeSavedData data = level.getChunkSource().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(TowRopeSavedData::new, (tag, registries) -> create(level, tag), null),
                ID
        );
        data.level = level;
        return data;
    }


    public boolean tryCreate(final BlockPos winchPos, final BlockPos anchorPos) {
        if (!(this.level.getBlockEntity(winchPos) instanceof RopeWinchBlockEntity)
                || !(this.level.getBlockEntity(anchorPos) instanceof HeavyAnchorBlockEntity)
                || !TowAnchorPoints.isHeavyAnchorBlock(this.level, anchorPos)
                || !TowAnchorPoints.isTowBlockAttachment(this.level, winchPos)) {
            return false;
        }
        if (TowAnchorPoints.hasSimulatedRopeAttachment(this.level, winchPos)
                || this.hasBlockAttachment(winchPos) || this.hasBlockAttachment(anchorPos)) {
            return false;
        }

        final TowRopeAttachment startAttachment = TowRopeAttachment.block(winchPos);
        final TowRopeAttachment endAttachment = TowRopeAttachment.block(anchorPos);
        final ServerTowRope.AttachmentEndpoint startEndpoint = ServerTowRope.resolveAttachmentEndpoint(this.level, startAttachment);
        final ServerTowRope.AttachmentEndpoint endEndpoint = ServerTowRope.resolveAttachmentEndpoint(this.level, endAttachment);
        if (startEndpoint == null || endEndpoint == null) return false;

        final Vec3 globalWinch = Sable.HELPER.projectOutOfSubLevel(this.level, JOMLConversion.toMojang(startEndpoint.localPoint()));
        final Vec3 globalAnchor = Sable.HELPER.projectOutOfSubLevel(this.level, JOMLConversion.toMojang(endEndpoint.localPoint()));
        final double maxRange = SimConfigService.INSTANCE.server().blocks.maxRopeRange.get();
        if (Sable.HELPER.distanceSquaredWithSubLevels(this.level, globalWinch, globalAnchor) > maxRange * maxRange) {
            return false;
        }

        final ServerTowRope rope = new ServerTowRope(UUID.randomUUID(), startAttachment, endAttachment, createInitialPoints(globalWinch, globalAnchor));
        this.ropes.put(rope.ropeId(), rope);
        this.setDirty();
        return true;
    }

    public boolean tryCreate(final BlockPos blockPos, final UUID entityId) {
        final Entity entity = this.level.getEntity(entityId);
        if (!(entity instanceof HeavyAnchorEntity) || !entity.isAlive()
                || !(this.level.getBlockEntity(blockPos) instanceof RopeWinchBlockEntity)
                || !TowAnchorPoints.isTowBlockAttachment(this.level, blockPos)) {
            return false;
        }

        if (TowAnchorPoints.hasSimulatedRopeAttachment(this.level, blockPos)) {
            return false;
        }

        final TowRopeAttachment startAttachment = TowRopeAttachment.block(blockPos);
        final TowRopeAttachment endAttachment = TowRopeAttachment.entity(entityId);

        final int maxRopesPerEntity = HeavyAnchorConfig.SERVER.maxRopesPerEntity.get();
        int entityRopeCount = 0;
        for (final ServerTowRope rope : this.ropes.values()) {
            if (rope.matchesEntity(entityId)) {
                entityRopeCount++;
                if (entityRopeCount >= maxRopesPerEntity) {
                    return false;
                }
            }
            if (rope.matchesBlock(blockPos)) {
                return false;
            }
        }

        final ServerTowRope.AttachmentEndpoint startEndpoint = ServerTowRope.resolveAttachmentEndpoint(this.level, startAttachment);
        final ServerTowRope.AttachmentEndpoint endEndpoint = ServerTowRope.resolveAttachmentEndpoint(this.level, endAttachment);
        if (startEndpoint == null || endEndpoint == null) {
            return false;
        }

        final Vec3 globalHandle = Sable.HELPER.projectOutOfSubLevel(this.level, JOMLConversion.toMojang(startEndpoint.localPoint()));
        final Vec3 globalEntity = Sable.HELPER.projectOutOfSubLevel(this.level, JOMLConversion.toMojang(endEndpoint.localPoint()));

        final double maxRange = SimConfigService.INSTANCE.server().blocks.maxRopeRange.get();
        if (Sable.HELPER.distanceSquaredWithSubLevels(this.level, globalHandle, globalEntity) > maxRange * maxRange) {
            return false;
        }

        final ServerTowRope rope = new ServerTowRope(UUID.randomUUID(), startAttachment, endAttachment, createInitialPoints(globalHandle, globalEntity));
        this.ropes.put(rope.ropeId(), rope);
        this.setDirty();
        return true;
    }

    public boolean removeByBlock(final BlockPos blockPos, final boolean dropItem) {
        boolean removed = false;
        final List<UUID> toRemove = new ArrayList<>();
        for (final ServerTowRope rope : this.ropes.values()) {
            if (rope.matchesBlock(blockPos)) {
                toRemove.add(rope.ropeId());
            }
        }

        for (final UUID ropeId : toRemove) {
            removed |= this.removeRope(ropeId, dropItem);
        }
        return removed;
    }

    public boolean hasEntityAttachment(final UUID entityId) {
        for (final ServerTowRope rope : this.ropes.values()) {
            if (rope.matchesEntity(entityId)) {
                return true;
            }
        }

        return false;
    }

    public Vec3 constrainEntityMovement(final Entity entity, final Vec3 movement) {
        return this.constrainEntityMovement(entity, movement, true);
    }

    public Vec3 previewConstrainedEntityMovement(final Entity entity, final Vec3 movement) {
        return this.constrainEntityMovement(entity, movement, false);
    }

    private Vec3 constrainEntityMovement(final Entity entity, final Vec3 movement, final boolean recordPullDemand) {
        final List<ServerTowRope> matchingRopes = new ArrayList<>();
        for (final ServerTowRope rope : this.ropes.values()) {
            if (rope.matchesEntity(entity.getUUID())) {
                matchingRopes.add(rope);
            }
        }

        if (recordPullDemand) {
            for (final ServerTowRope rope : matchingRopes) {
                rope.recordEntityPullDemand(this.level, entity, movement);
            }
        }

        Vec3 constrained = movement;
        for (final ServerTowRope rope : matchingRopes) {
            constrained = rope.constrainEntityMovement(this.level, entity, constrained);
            if (constrained.lengthSqr() <= 1.0E-10) {
                return Vec3.ZERO;
            }
        }

        return constrained;
    }

    public int countActiveTowRopes(final Entity entity, final TowEntityProfile profile) {
        int activeTowRopes = 0;
        for (final ServerTowRope rope : this.ropes.values()) {
            if (rope.hasActiveTowDemand(this.level, entity, profile)) {
                activeTowRopes++;
            }
        }
        return activeTowRopes;
    }

    public boolean hasBlockAttachment(final BlockPos blockPos) {
        for (final ServerTowRope rope : this.ropes.values()) {
            if (rope.matchesBlock(blockPos)) {
                return true;
            }
        }

        return false;
    }

    public void physicsTick(final SubLevelPhysicsSystem physicsSystem, final double timeStep) {
        final Iterator<ServerTowRope> iterator = this.ropes.values().iterator();
        while (iterator.hasNext()) {
            final ServerTowRope rope = iterator.next();
            final ServerTowRope.AttachmentState attachmentState = rope.getAttachmentState(this.level);
            if (attachmentState == ServerTowRope.AttachmentState.NOT_LOADED) {
                rope.clearInvalidAttachmentGrace();
                this.removeRuntimeOnly(rope);
                continue;
            }

            if (attachmentState == ServerTowRope.AttachmentState.INVALID) {
                this.removeRuntimeOnly(rope);
                if (!rope.shouldBreakForInvalidAttachments()) {
                    continue;
                }

                iterator.remove();
                this.sendRemoval(rope);
                this.dropCoupling(rope);
                this.setDirty();
                continue;
            }

            rope.clearInvalidAttachmentGrace();
            if (!rope.prePhysicsTick(this.level)) {
                this.removeRuntimeOnly(rope);
                iterator.remove();
                this.sendRemoval(rope);
                this.dropCoupling(rope);
                this.setDirty();
                continue;
            }

            rope.physicsTick(physicsSystem, timeStep);

            if (!rope.isActive()) {
                if (physicsSystem.getTicketManager().wouldBeLoaded(this.level, rope)) {
                    physicsSystem.addObject(rope);
                }
                continue;
            }
        }
    }

    public void serverTick() {
        if (this.ropes.isEmpty()) {
            return;
        }
        boolean persistentStateChanged = false;
        final List<UUID> retrievedRopes = new ArrayList<>();
        for (final ServerTowRope rope : this.ropes.values()) {
            persistentStateChanged |= rope.serverTick(this.level);
            if (rope.isAnchorRetrieved(this.level)) {
                retrievedRopes.add(rope.ropeId());
            }
        }
        for (final UUID ropeId : retrievedRopes) {
            this.removeRope(ropeId, true);
        }

        if (persistentStateChanged || !retrievedRopes.isEmpty()) {
            this.setDirty();
        }
    }

    public Iterable<UUID> neededTrackingPlayers() {
        final Set<UUID> players = new LinkedHashSet<>();
        for (final ServerTowRope rope : this.ropes.values()) {
            if (!rope.isActive()) {
                continue;
            }

            for (final ServerPlayer player : this.getTrackingPlayers(rope)) {
                players.add(player.getUUID());
            }
        }
        return players;
    }

    public void sendTrackingData(final int interpolationTick) {
        for (final ServerTowRope rope : this.ropes.values()) {
            if (!rope.isActive()) {
                continue;
            }

            rope.updatePose();
            rope.updateVisualEntityEndpoint(this.level);
            final Collection<ServerPlayer> trackingPlayers = this.getTrackingPlayers(rope);
            if (trackingPlayers.isEmpty()) {
                continue;
            }

            final ClientboundTowRopeDataPacket packet =
                    new ClientboundTowRopeDataPacket(
                            interpolationTick,
                            rope.ropeId(),
                            rope.startAttachment(),
                            rope.endAttachment(),
                            copyPoints(rope.getPoints())
                    );

            for (final ServerPlayer player : trackingPlayers) {
                VeilPacketManager.player(player).sendPacket(packet);
            }
        }
    }

    private boolean removeRope(final UUID ropeId, final boolean dropItem) {
        final ServerTowRope rope = this.ropes.remove(ropeId);
        if (rope == null) {
            return false;
        }

        this.removeRuntimeOnly(rope);
        this.sendRemoval(rope);
        if (dropItem) {
            this.dropCoupling(rope);
        }
        this.setDirty();
        return true;
    }

    private void removeRuntimeOnly(final ServerTowRope rope) {
        if (rope.isActive()) {
            final ServerSubLevelContainer container = SubLevelContainer.getContainer(this.level);
            if (container != null) {
                container.physicsSystem().removeObject(rope);
            }
        }
    }

    private void sendRemoval(final ServerTowRope rope) {
        final ClientboundTowRopeRemovePacket packet = new ClientboundTowRopeRemovePacket(rope.ropeId());
        for (final ServerPlayer player : this.getTrackingPlayers(rope)) {
            VeilPacketManager.player(player).sendPacket(packet);
        }
    }

    private Collection<ServerPlayer> getTrackingPlayers(final ServerTowRope rope) {
        final Set<ServerPlayer> players = new LinkedHashSet<>();
        for (final BlockPos blockPos : rope.trackingBlockPositions(this.level)) {
            players.addAll(this.getTrackingPlayers(blockPos));
        }
        return players;
    }

    private Collection<ServerPlayer> getTrackingPlayers(final @Nullable BlockPos blockPos) {
        if (blockPos == null) {
            return List.of();
        }
        return this.level.getChunkSource().chunkMap.getPlayers(new ChunkPos(blockPos), false);
    }

    private void dropCoupling(final ServerTowRope rope) {
        final Entity endpoint = rope.endAttachment().isEntity() ? this.level.getEntity(rope.endAttachment().entityId()) : null;
        if (endpoint instanceof HeavyAnchorEntity anchor && anchor.isRetrieving()) {
            final Vec3 at = anchor.position();
            anchor.discard();
            this.level.addFreshEntity(new ItemEntity(this.level, at.x, at.y, at.z, new ItemStack(HeavyAnchorItems.HEAVY_ANCHOR.get())));
            this.level.addFreshEntity(new ItemEntity(this.level, at.x, at.y + 0.15, at.z, new ItemStack(SimItems.ROPE_COUPLING.get())));
            return;
        }

        final List<Vector3d> points = rope.getPoints();
        final Vector3d dropPoint;
        if (points.isEmpty()) {
            final BlockPos trackingPos = rope.trackingBlockPos();
            if (trackingPos == null) {
                dropPoint = new Vector3d();
            } else {
                dropPoint = new Vector3d(trackingPos.getX() + 0.5, trackingPos.getY() + 0.5, trackingPos.getZ() + 0.5);
            }
        } else {
            dropPoint = new Vector3d(points.get(points.size() / 2));
        }
        this.level.addFreshEntity(new ItemEntity(this.level, dropPoint.x, dropPoint.y, dropPoint.z, new ItemStack(SimItems.ROPE_COUPLING.get())));
    }

    private static List<Vector3d> copyPoints(final List<Vector3d> points) {
        final List<Vector3d> copiedPoints = new ArrayList<>(points.size());
        for (final Vector3d point : points) {
            copiedPoints.add(new Vector3d(point));
        }
        return copiedPoints;
    }

    @Override
    public @NotNull CompoundTag save(final CompoundTag tag, final HolderLookup.@NotNull Provider provider) {
        final ListTag list = new ListTag();
        for (final ServerTowRope rope : this.ropes.values()) {
            TowRopeState.CODEC.encodeStart(NbtOps.INSTANCE, rope.toState()).result().ifPresent(list::add);
        }
        tag.put(ID, list);
        return tag;
    }

    private static List<Vector3d> createInitialPoints(final Vec3 ropeStart, final Vec3 ropeEnd) {
        final double distance = ropeEnd.distanceTo(ropeStart);
        final int wholeSegments = (int) Math.floor(distance);
        final int points = Math.max(1, wholeSegments + 1);
        final double shortSegmentLength = distance - wholeSegments;

        final Vec3 direction = ropeEnd.subtract(ropeStart).normalize();
        final List<Vector3d> pointList = new ArrayList<>(points + 1);
        pointList.add(JOMLConversion.toJOML(ropeStart));

        for (int i = 0; i < points; i++) {
            pointList.add(JOMLConversion.toJOML(ropeStart.add(direction.scale(i + shortSegmentLength))));
        }

        return pointList;
    }
}
