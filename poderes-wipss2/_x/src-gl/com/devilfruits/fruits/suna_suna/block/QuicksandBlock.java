package com.devilfruits.fruits.suna_suna.block;

import com.devilfruits.core.logia.LogiaManager;
import com.devilfruits.fruits.suna_suna.SunaSuna;
import com.devilfruits.registry.DFDamageTypes;
import com.mojang.serialization.MapCodec;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class QuicksandBlock extends Block {
   public static final MapCodec<QuicksandBlock> CODEC = simpleCodec(QuicksandBlock::new);
   public static final BooleanProperty RED = BooleanProperty.create("red");
   public static final float CHOKE_DAMAGE = 1.5F;
   private static final double CLIMB_SPEED = 0.09;
   public static Predicate<Entity> jumpHeld = entity -> false;

   public QuicksandBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(RED, false));
   }

   protected MapCodec<? extends Block> codec() {
      return CODEC;
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{RED});
   }

   public BlockState of(BlockState sand) {
      return (BlockState)this.defaultBlockState().setValue(RED, sand.is(Blocks.RED_SAND));
   }

   public static BlockState sand(BlockState quicksand) {
      return quicksand.hasProperty(RED) && quicksand.getValue(RED) ? Blocks.RED_SAND.defaultBlockState() : Blocks.SAND.defaultBlockState();
   }

   public static boolean walksOn(Entity entity) {
      if (entity instanceof Player player && SunaSuna.isUser(player) && !LogiaManager.counterActive(player, "wet")) {
         return true;
      }

      return false;
   }

   protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      if (context instanceof EntityCollisionContext entityContext) {
         Entity entity = entityContext.getEntity();
         if (entity instanceof FallingBlockEntity
            || entity != null && walksOn(entity) && context.isAbove(Shapes.block(), pos, false) && !context.isDescending()) {
            return Shapes.block();
         }
      }

      return Shapes.empty();
   }

   protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
      return Shapes.empty();
   }

   protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return Shapes.empty();
   }

   protected boolean skipRendering(BlockState state, BlockState adjacent, Direction direction) {
      return adjacent.is(this) || super.skipRendering(state, adjacent, direction);
   }

   protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
      if (!walksOn(entity)) {
         entity.makeStuckInBlock(state, new Vec3(0.45, 0.3, 0.45));
         entity.resetFallDistance();
         if (jumpHeld.test(entity)) {
            Vec3 motion = entity.getDeltaMovement();
            entity.setDeltaMovement(motion.x, 0.3, motion.z);
         }

         if (!level.isClientSide) {
            if (entity instanceof LivingEntity living
               && entity.tickCount % 20 == 0
               && level.getBlockState(BlockPos.containing(entity.getEyePosition())).is(this)) {
               living.hurt(DFDamageTypes.source(level, SunaSuna.QUICKSAND_DAMAGE, null), 1.5F);
            }
         } else {
            RandomSource random = level.getRandom();
            boolean moving = entity.xOld != entity.getX() || entity.zOld != entity.getZ();
            if (moving && random.nextInt(3) == 0) {
               level.addParticle(
                  new BlockParticleOption(ParticleTypes.BLOCK, sand(state)),
                  entity.getX(),
                  (double)pos.getY() + 1.0,
                  entity.getZ(),
                  ((double)random.nextFloat() - 0.5) * 0.1,
                  0.06,
                  ((double)random.nextFloat() - 0.5) * 0.1
               );
            }
         }
      }
   }

   public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
   }
}
