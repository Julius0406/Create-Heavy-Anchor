package dev.julius.heavyanchor.entity;

import dev.ryanhcode.sable.Sable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class HeavyAnchorEntity extends Entity {
    public static final double ANCHOR_MASS_KG = 10_000.0;
    private static final EntityDataAccessor<Boolean> ANCHORED = SynchedEntityData.defineId(HeavyAnchorEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> RETRIEVING = SynchedEntityData.defineId(HeavyAnchorEntity.class, EntityDataSerializers.BOOLEAN);
    private double pinnedX;
    private double pinnedY;
    private double pinnedZ;

    public HeavyAnchorEntity(EntityType<? extends HeavyAnchorEntity> type, Level level) {
        super(type, level);
        this.setInvulnerable(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ANCHORED, false);
        builder.define(RETRIEVING, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;

        if (this.isRetrieving()) {
            this.setNoGravity(true);
            this.setOnGround(false);
            this.setDeltaMovement(Vec3.ZERO);
            return;
        }

        if (this.isAnchored()) {
            this.setNoGravity(true);
            this.setDeltaMovement(Vec3.ZERO);
            this.setPos(this.pinnedX, this.pinnedY, this.pinnedZ);
            return;
        }

        // The anchor falls normally, but contributes no load to the rope until it reaches static ground.
        this.setNoGravity(false);
        Vec3 movement = this.getDeltaMovement().add(0.0, -0.04, 0.0);
        this.move(MoverType.SELF, movement);
        this.setDeltaMovement(movement.scale(0.98));

        // A landing on a moving Aeronautics sublevel is not an actual ground anchor.
        if (this.onGround() && Sable.HELPER.getContaining(this) == null) {
            this.pinToWorld();
        }
    }

    public boolean isAnchored() {
        return this.entityData.get(ANCHORED);
    }

    public boolean isRetrieving() {
        return this.entityData.get(RETRIEVING);
    }

    public double getMassKg() {
        return this.isAnchored() ? ANCHOR_MASS_KG : 0.0;
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    public void beginRetrieval() {
        if (!this.isAlive()) return;
        this.entityData.set(RETRIEVING, true);
        this.entityData.set(ANCHORED, false);
        this.setNoGravity(true);
        this.setOnGround(false);
        this.setDeltaMovement(Vec3.ZERO);
    }

    public void stopRetrieval() {
        if (!this.isAlive()) return;
        this.entityData.set(RETRIEVING, false);
        this.setNoGravity(false);
        if (this.onGround() && Sable.HELPER.getContaining(this) == null) {
            this.pinToWorld();
        }
    }

    private void pinToWorld() {
        this.pinnedX = this.getX();
        this.pinnedY = this.getY();
        this.pinnedZ = this.getZ();
        this.entityData.set(ANCHORED, true);
        this.entityData.set(RETRIEVING, false);
        this.setNoGravity(true);
        this.setDeltaMovement(Vec3.ZERO);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putBoolean("Anchored", this.isAnchored());
        tag.putBoolean("Retrieving", this.isRetrieving());
        tag.putDouble("PinnedX", this.pinnedX);
        tag.putDouble("PinnedY", this.pinnedY);
        tag.putDouble("PinnedZ", this.pinnedZ);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.pinnedX = tag.getDouble("PinnedX");
        this.pinnedY = tag.getDouble("PinnedY");
        this.pinnedZ = tag.getDouble("PinnedZ");
        this.entityData.set(ANCHORED, tag.getBoolean("Anchored"));
        this.entityData.set(RETRIEVING, tag.getBoolean("Retrieving"));
        this.setNoGravity(this.isAnchored() || this.isRetrieving());
    }


}
