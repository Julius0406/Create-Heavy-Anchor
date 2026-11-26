package dev.julius.heavyanchor.block;

import dev.julius.heavyanchor.index.HeavyAnchorBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class HeavyAnchorBlock extends BaseEntityBlock implements dev.simulated_team.simulated.content.blocks.rope.RopeHolderBlock<HeavyAnchorBlockEntity>, dev.ryanhcode.sable.api.block.BlockSubLevelCollisionShape, dev.ryanhcode.sable.api.block.BlockSubLevelCustomCenterOfMass {
    // The anchor's crown and flukes carry most of its mass, below the rope eye.
    @Override public org.joml.Vector3dc getCenterOfMass(BlockGetter level, BlockState state) {
        return new org.joml.Vector3d(0.5, 0.125, 0.5);
    }
    @Override public Class<HeavyAnchorBlockEntity> getBlockEntityClass() { return HeavyAnchorBlockEntity.class; }
    @Override public BlockEntityType<? extends HeavyAnchorBlockEntity> getBlockEntityType() { return HeavyAnchorBlockEntities.HEAVY_ANCHOR.get(); }
    @Override public VoxelShape getSubLevelCollisionShape(BlockGetter level, BlockState state) { return PHYSICS_SHAPE; }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        com.simibubi.create.foundation.block.IBE.onRemove(state,level,pos,next);
    }
    public static final MapCodec<HeavyAnchorBlock> CODEC = simpleCodec(HeavyAnchorBlock::new);
    public static final BooleanProperty HEAVY = BooleanProperty.create("heavy");
    public static final int RELEASE_TICKS = 60;
    public static final IntegerProperty RELEASE = IntegerProperty.create("release", 0, RELEASE_TICKS - 1);
    // Matching model and collision geometry: eye at +Y, flukes at -Y.
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(6, 15, 6, 10, 16, 10),
            Block.box(6, 12, 6, 7, 15, 10),
            Block.box(9, 12, 6, 10, 15, 10),
            Block.box(6, 11, 6, 10, 12, 10),
            Block.box(7, 2, 7, 9, 12, 9),
            Block.box(3, 9, 6, 13, 10.5, 10),
            Block.box(5, 0, 6, 11, 3, 10),
            Block.box(2, 1, 6, 6, 4, 10),
            Block.box(10, 1, 6, 14, 4, 10),
            Block.box(1, 3, 6, 4, 6, 10),
            Block.box(12, 3, 6, 15, 6, 10)
    );

    // The native rope has a 2/16-block collision radius. Do not put the
    // rope's terminal particle inside the eye's solid physics collider.
    // Keep the visible/selection shape, but use a simple non-bouncy hull
    // under the eye for the rigid body. Maximum height = 11/16.
    private static final VoxelShape PHYSICS_SHAPE = Shapes.or(
            Block.box(2, 0, 6, 14, 4, 10),
            Block.box(1, 3, 6, 4, 6, 10),
            Block.box(12, 3, 6, 15, 6, 10),
            Block.box(7, 3, 7, 9, 9, 9),
            Block.box(3, 9, 6, 13, 11, 10)
    );

    public HeavyAnchorBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HEAVY, false).setValue(RELEASE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HEAVY, RELEASE);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, HeavyAnchorBlockEntities.HEAVY_ANCHOR.get(), (l, pos, st, be) -> be.tick());
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HeavyAnchorBlockEntity(HeavyAnchorBlockEntities.HEAVY_ANCHOR.get(), pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
