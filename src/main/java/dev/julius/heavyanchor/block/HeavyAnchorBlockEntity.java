package dev.julius.heavyanchor.block;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBehavior;
import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBlockEntity;
import dev.simulated_team.simulated.content.blocks.rope.strand.client.ClientRopeStrand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import com.simibubi.create.Create;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;
import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.createmod.catnip.data.Couple;
import dev.ryanhcode.sable.Sable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.SimpleContainer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A permanent anchor block that exposes the same rope-holder behaviour as
 * Simulated's native Rope Connector, allowing the standard Rope Coupling to
 * recognize it as a valid endpoint.
 */
public class HeavyAnchorBlockEntity extends SmartBlockEntity implements RopeStrandHolderBlockEntity, IHaveGoggleInformation {
    private RopeStrandHolderBehavior ropeHolder;
    private final ItemStack[] frequencies = {ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
    private boolean manual, lastOn, lastOff;
    private boolean waitForRedeploy;
    private java.util.UUID checkedMassBody;
    public boolean isHeavy() { return getBlockState().getValue(HeavyAnchorBlock.HEAVY); }
    public ItemStack frequency(int index) { return frequencies[index].copy(); }
    public void setFrequency(int index, ItemStack stack) {
        frequencies[index] = stack.copyWithCount(1);
        setChanged();
        sendData();
    }
    private boolean powered(int offset) {
        if (frequencies[offset].isEmpty() || frequencies[offset+1].isEmpty()) return false;
        var key = Couple.create(Frequency.of(frequencies[offset]), Frequency.of(frequencies[offset+1]));
        var links = Create.REDSTONE_LINK_NETWORK_HANDLER.networksIn(level).get(key);
        if (links == null) return false;
        double range = AllConfigs.server().logistics.linkRange.get();
        Vec3 here = Sable.HELPER.projectOutOfSubLevel(level, Vec3.atCenterOf(worldPosition));
        for (IRedstoneLinkable link : links) {
            if (link.isListening() || !link.isAlive() || link.getTransmittedStrength() <= 0) continue;
            Vec3 there = Sable.HELPER.projectOutOfSubLevel(level, Vec3.atCenterOf(link.getLocation()));
            if (here.distanceToSqr(there) <= range * range) return true;
        }
        return false;
    }
    private int releaseStep() {
        return getBlockState().getValue(HeavyAnchorBlock.RELEASE);
    }

    private void updateWeightState(boolean heavy, int step) {
        if (level == null || level.isClientSide) return;
        BlockState next = getBlockState().setValue(HeavyAnchorBlock.HEAVY, heavy)
                .setValue(HeavyAnchorBlock.RELEASE, step);
        if (next != getBlockState()) {
            level.setBlock(worldPosition, next, 3);
            setChanged();
        }
    }

    private void activateWeight() {
        waitForRedeploy = false;
        updateWeightState(true, 0);
    }

    // Keep the native rope attached. Change its endpoint's real Sable mass
    // gradually using persistent block states, one step per server tick.
    private void advanceRelease() {
        if (!isHeavy()) return;
        waitForRedeploy = true;
        int next = releaseStep() + 1;
        if (next >= HeavyAnchorBlock.RELEASE_TICKS) updateWeightState(false, 0);
        else updateWeightState(true, next);
    }

    private float winchMovement() {
        if (ropeHolder == null || !ropeHolder.isAttached()) return 0;
        var strand = ropeHolder.getAttachedStrand();
        if (strand == null) return 0;
        for (var endpoint : strand.getAttachments()) {
            if (level.getBlockEntity(endpoint.blockAttachment()) instanceof dev.simulated_team.simulated.content.blocks.rope.rope_winch.RopeWinchBlockEntity winch)
                return winch.getMovementSpeed();
        }
        return 0;
    }
    private boolean groundContact() {
        if (level == null || Sable.HELPER.getContaining(this) == null) return false;
        // Test the underside against actual terrain, not against the sublevel's storage coordinates.
        for (double x : new double[]{0.36,0.5,0.64}) for (double z : new double[]{0.36,0.5,0.64}) {
            Vec3 world = Sable.HELPER.projectOutOfSubLevel(level, new Vec3(worldPosition.getX()+x,worldPosition.getY()-0.06,worldPosition.getZ()+z));
            BlockPos below = BlockPos.containing(world);
            if (!level.isLoaded(below)) continue;
            if (Sable.HELPER.getContaining(level, new org.joml.Vector3d(world.x,world.y,world.z)) != null) continue;
            var shape = level.getBlockState(below).getCollisionShape(level, below);
            for (AABB box : shape.toAabbs()) if (box.move(below).inflate(0.08).contains(world)) return true;
        }
        return false;
    }
    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putBoolean("ManualWeight", manual);
        tag.putBoolean("WaitForRedeploy", waitForRedeploy);
        for (int i=0;i<4;i++) if (!frequencies[i].isEmpty()) tag.put("WeightFrequency"+i, frequencies[i].save(registries));
    }
    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        manual = tag.getBoolean("ManualWeight");
        waitForRedeploy = tag.getBoolean("WaitForRedeploy");
        for (int i=0;i<4;i++) frequencies[i] = tag.contains("WeightFrequency"+i) ? ItemStack.parseOptional(registries, tag.getCompound("WeightFrequency"+i)) : ItemStack.EMPTY;
    }
    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean sneaking) {
        tooltip.add(Component.literal("    ").append(Component.translatable("heavyanchor.goggle.title").withStyle(ChatFormatting.GOLD)));
        if (isHeavy() && releaseStep() > 0) {
            tooltip.add(Component.literal("    ").append(Component.translatable("heavyanchor.goggle.releasing",
                    String.format(java.util.Locale.ROOT, "%.1f", (HeavyAnchorBlock.RELEASE_TICKS - releaseStep()) / 20.0))
                    .withStyle(ChatFormatting.YELLOW)));
        } else {
            tooltip.add(Component.literal("    ").append(Component.translatable(isHeavy() ? "heavyanchor.goggle.heavy" : "heavyanchor.goggle.light").withStyle(isHeavy() ? ChatFormatting.RED : ChatFormatting.GREEN)));
        }
        if (level != null) {
            double mass = dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper.getMass(level, worldPosition, getBlockState());
            tooltip.add(Component.literal("    ").append(Component.translatable("heavyanchor.goggle.actual", String.format(java.util.Locale.ROOT, "%.3f", mass)).withStyle(ChatFormatting.GRAY)));
        }
        tooltip.add(Component.literal("    ").append(Component.translatable(manual ? "heavyanchor.goggle.manual" : "heavyanchor.goggle.auto").withStyle(ChatFormatting.GRAY)));
        tooltip.add(Component.literal("    ").append(Component.translatable(ropeHolder != null && ropeHolder.isAttached() ? "heavyanchor.goggle.connected" : "heavyanchor.goggle.disconnected").withStyle(ChatFormatting.GRAY)));
        return true;
    }

    public HeavyAnchorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        this.ropeHolder = new RopeStrandHolderBehavior(this);
        behaviours.add(this.ropeHolder);
    }

    @Override
    public RopeStrandHolderBehavior getBehavior() {
        return this.ropeHolder;
    }

    @Override
    public Vec3 getAttachmentPoint(BlockPos pos, BlockState state) {
        // Put the rope connection at the anchor's upper ring, not inside its base.
        return Vec3.atBottomCenterOf(pos).add(0.0, 0.96875, 0.0);
    }

    @Override
    public Vec3 getVisualAttachmentPoint(BlockPos pos, BlockState state) {
        return getAttachmentPoint(pos, state);
    }

    // Simulated 1.3.3 exposes lock state publicly but not active drag state.
    // Cache access to its drag sessions so the staff can take full control.
    private static final class StaffAccess {
        static final java.lang.reflect.Field SESSIONS = field(dev.simulated_team.simulated.content.physics_staff.PhysicsStaffServerHandler.class, "draggingSessions");
        static final java.lang.reflect.Field TARGET;
        static final java.lang.reflect.Field REMOVED;
        static {
            Class<?> type = null;
            try {
                type = Class.forName("dev.simulated_team.simulated.content.physics_staff.PhysicsStaffServerHandler$DragSession");
            } catch (ClassNotFoundException ignored) {}
            TARGET = field(type, "subLevel");
            REMOVED = field(type, "markedForRemoval");
        }
        private static java.lang.reflect.Field field(Class<?> type, String name) {
            if (type == null) return null;
            try {
                var f = type.getDeclaredField(name);
                return f.trySetAccessible() ? f : null;
            } catch (ReflectiveOperationException | RuntimeException ignored) { return null; }
        }
        static boolean controlled(dev.ryanhcode.sable.sublevel.ServerSubLevel body) {
            var staff = dev.simulated_team.simulated.content.physics_staff.PhysicsStaffServerHandler.get(body.getLevel());
            if (staff.isLocked(body)) return true;
            if (SESSIONS == null || TARGET == null || REMOVED == null) return false;
            try {
                var sessions = (java.util.Map<?, ?>) SESSIONS.get(staff);
                for (Object session : sessions.values()) {
                    if (TARGET.get(session) == body && !REMOVED.getBoolean(session)) return true;
                }
            } catch (IllegalAccessException ignored) {}
            return false;
        }
    }

    @Override
    public void sable$physicsTick(dev.ryanhcode.sable.sublevel.ServerSubLevel subLevel,
                                 dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle handle, double dt) {
        // Never brake a ship or a larger assembly containing an anchor.
        var bounds = subLevel.getPlot().getBoundingBox();
        if (bounds == null || bounds.minX() != bounds.maxX() || bounds.minY() != bounds.maxY()
                || bounds.minZ() != bounds.maxZ() || !subLevel.getPlot().getContraptions().isEmpty()) return;
        // Confirm the real Sable tracker picked up the new local center, once
        // per independent anchor body. No mutation of a live physics tracker.
        if (!subLevel.getUniqueId().equals(checkedMassBody)) {
            var tracker = subLevel.getSelfMassTracker();
            var center = tracker == null ? null : tracker.getCenterOfMass();
            if (center != null && center.isFinite()) {
                var local = new org.joml.Vector3d(center).sub(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
                dev.julius.heavyanchor.CreateHeavyAnchor.LOGGER.info(
                        "Anchor mass center local: {} (expected 0.5, 0.125, 0.5)", local);
                checkedMassBody = subLevel.getUniqueId();
            }
        }
        if (ropeHolder == null || !ropeHolder.isAttached() || !handle.isValid() || !(dt > 0)) return;
        var strand = ropeHolder.getAttachedStrand();
        if (strand == null || StaffAccess.controlled(subLevel)) return;
        // Damp movement relative to the moving winch, not the world origin.
        var referenceVelocity = new org.joml.Vector3d();
        for (var endpoint : strand.getAttachments()) {
            if (level.getBlockEntity(endpoint.blockAttachment()) instanceof dev.simulated_team.simulated.content.blocks.rope.rope_winch.RopeWinchBlockEntity winch) {
                if (Sable.HELPER.getContaining(winch) instanceof dev.ryanhcode.sable.sublevel.ServerSubLevel ship)
                    referenceVelocity.set(ship.latestLinearVelocity);
                break;
            }
        }
        var velocity = new org.joml.Vector3d(handle.getLinearVelocity()).sub(referenceVelocity);
        var spin = new org.joml.Vector3d(handle.getAngularVelocity());
        if (!velocity.isFinite() || !spin.isFinite()) return;
        // Exponential, timestep-independent damping. Preserve controlled winding,
        // while reducing transverse swinging, bouncing and free spinning.
        double lateral = 1.0 - Math.exp(-5.0 * dt);
        double vertical = 1.0 - Math.exp(-3.0 * dt);
        double angular = 1.0 - Math.exp(-10.0 * dt);
        var dv = new org.joml.Vector3d(-velocity.x * lateral, -velocity.y * vertical, -velocity.z * lateral);
        var dw = new org.joml.Vector3d(spin).mul(-angular);
        // Align local +Y (the eye) with world +Y using a bounded corrective
        // angular velocity. Use a deterministic axis even at exactly 180°.
        // No teleporting: the native rope and collision solver remain active.
        if (!isHeavy() || releaseStep() > 45) {
            var physics = dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem.getCurrentlySteppingSystem();
            var pose = physics == null ? subLevel.logicalPose() :
                    physics.getPipeline().readPose(subLevel, new dev.ryanhcode.sable.companion.math.Pose3d());
            var up = pose.orientation().transform(new org.joml.Vector3d(0, 1, 0));
            if (up.isFinite() && up.lengthSquared() > 1e-9) {
                up.normalize();
                double dot = Math.max(-1.0, Math.min(1.0, up.y));
                var axis = new org.joml.Vector3d(-up.z, 0, up.x);
                double angle = Math.acos(dot);
                if (axis.lengthSquared() < 1e-10 && dot < 0) axis.set(1, 0, 0);
                if (angle > 0.035 && axis.lengthSquared() > 1e-10) {
                    double desired = Math.min(0.9, 1.8 * angle);
                    dw.add(axis.normalize().mul(desired * angular));
                }
            }
        }
        // Soft speed limiter for extreme releases; ordinary winch motion is unaffected.
        double speed = velocity.length();
        if (speed > 12.0) dv.add(velocity.mul(-Math.min(1.0, dt * 8.0) * (speed - 12.0) / speed));
        if (dv.lengthSquared() > 1e-10 || dw.lengthSquared() > 1e-10)
            handle.addLinearAndAngularVelocity(dv, dw);
    }

    @Override
    public void tick() {
        super.tick();
        if (level instanceof ServerLevel) {
            boolean on = powered(0), off = powered(2);
            boolean requestRelease = off && !lastOff;
            boolean requestHeavy = on && !lastOn && !off;
            lastOn = on;
            lastOff = off;
            if (requestRelease || requestHeavy) {
                manual = true;
                setChanged();
                sendData();
            }
            if (requestHeavy) {
                activateWeight();
            } else {
                float movement = manual ? 0 : winchMovement();
                boolean retracting = movement < -0.0001f;
                // Once started, finish the release even if the winch stops.
                if (isHeavy() && (requestRelease || releaseStep() > 0 || (!manual && retracting))) {
                    advanceRelease();
                } else if (!manual && !isHeavy() && !retracting) {
                    boolean grounded = groundContact();
                    // Don't immediately jump back to full mass on the same patch of ground.
                    if (waitForRedeploy && (!grounded || movement > 0.0001f)) {
                        waitForRedeploy = false;
                        setChanged();
                    }
                    if (!waitForRedeploy && grounded) activateWeight();
                }
            }
        }
        if (this.level != null && this.level.isClientSide) {
            this.invalidateRenderBoundingBox();
        }
    }

    @Override
    public AABB getRenderBoundingBox() {
        if (this.ropeHolder != null && this.ropeHolder.ownsRope()) {
            final ClientRopeStrand strand = this.ropeHolder.getClientStrand();
            if (strand != null) {
                final AABB bounds = strand.getBounds();
                if (bounds != null) return bounds.inflate(3.0);
            }
        }
        return super.getRenderBoundingBox();
    }
}
