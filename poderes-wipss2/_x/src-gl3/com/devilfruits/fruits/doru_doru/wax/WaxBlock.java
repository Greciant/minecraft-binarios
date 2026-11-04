package com.devilfruits.fruits.doru_doru.wax;

import com.devilfruits.fruits.doru_doru.DoruDoru;
import com.devilfruits.fruits.doru_doru.DoruItems;
import com.mojang.serialization.MapCodec;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WaxBlock extends BaseEntityBlock {
   public static final MapCodec<WaxBlock> CODEC = simpleCodec(WaxBlock::new);
   public static final BooleanProperty FULL = BooleanProperty.create("full");
   public static final int LUMPS_PER_BLOCK = 4;

   public WaxBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(FULL, true));
   }

   protected MapCodec<? extends BaseEntityBlock> codec() {
      return CODEC;
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{FULL});
   }

   @Nullable
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new WaxBlockEntity(pos, state);
   }

   protected RenderShape getRenderShape(BlockState state) {
      return RenderShape.MODEL;
   }

   public static long maskAt(BlockGetter level, BlockPos pos) {
      return level.getBlockEntity(pos) instanceof WaxBlockEntity be ? be.mask() : -1L;
   }

   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return state.getValue(FULL) ? Shapes.block() : WaxShapes.shape(maskAt(level, pos));
   }

   protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return this.getShape(state, level, pos, context);
   }

   protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
      return state.getValue(FULL) ? Shapes.block() : Shapes.empty();
   }

   protected int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
      return state.getValue(FULL) ? 1 : 0;
   }

   protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
      return !(Boolean)state.getValue(FULL);
   }

   protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
      return 1.0F;
   }

   public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
      return true;
   }

   public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
      return 0;
   }

   public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
      return 0;
   }

   protected boolean isRandomlyTicking(BlockState state) {
      return true;
   }

   protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
      if (level.getBlockEntity(pos) instanceof WaxBlockEntity be) {
         long lifetime = (long)(DoruDoru.setting("structureLifetimeMinutes") * 1200.0);
         if (lifetime >= 0L && be.expires() == 0L && level.getGameTime() - be.placed() > lifetime) {
            be.setExpires(level.getGameTime());
         }
      }

      WaxHeatService.poke(level, pos);
   }

   protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
      if (level.getBlockEntity(pos) instanceof WaxBlockEntity be) {
         long var8 = level.getGameTime();
         if (!be.cured() && var8 - be.placed() >= 600L) {
            be.setCured(true);
            WaxWorld.sync(level, pos);
         }

         if (be.expires() != 0L && var8 >= be.expires()) {
            WaxHeatService.poke(level, pos);
         } else if (be.expires() != 0L) {
            level.scheduleTick(pos, this, (int)Math.min(2147483647L, be.expires() - var8));
         }
      }
   }

   protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
      super.onPlace(state, level, pos, old, moved);
      if (level instanceof ServerLevel server && WaxHeatService.tooHot(server)) {
         WaxHeatService.poke(server, pos);
      }
   }

   protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos from, boolean moving) {
      super.neighborChanged(state, level, pos, neighbor, from, moving);
      if (level instanceof ServerLevel server) {
         BlockState other = level.getBlockState(from);
         if (WaxHeatService.sourceWeight(other) > 0.0F || !other.getFluidState().isEmpty()) {
            WaxHeatService.poke(server, pos);
         }

         WaxIntegrityHooks.supportChanged(server, pos, from);
      }
   }

   protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
      if (!state.is(newState.getBlock()) && level instanceof ServerLevel server) {
         if (level.getBlockEntity(pos) instanceof WaxBlockEntity be && be.owner() != -1) {
            WaxOwners.get(server.getServer()).add(be.owner(), -1);
         }

         WaxIntegrityHooks.removed(server, pos);
      }

      super.onRemove(state, level, pos, newState, moved);
   }

   public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
      super.playerDestroy(level, player, pos, state, blockEntity, tool);
      if (level instanceof ServerLevel server && blockEntity instanceof WaxBlockEntity be && !player.isCreative()) {
         UUID owner = WaxOwners.get(server.getServer()).owner(be.owner());
         if (!player.getUUID().equals(owner) && player.hasCorrectToolForDrops(state, level, pos)) {
            int lumps = Math.round(WaxMask.fill(be.mask()) * 4.0F);
            if (lumps > 0) {
               popResource(level, pos, new ItemStack((ItemLike)DoruItems.WAX_LUMP.get(), lumps));
            }
         }
      }
   }

   public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
      return new ItemStack((ItemLike)DoruItems.WAX_LUMP.get());
   }

   public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
      if (level.getBlockEntity(pos) instanceof WaxBlockEntity be) {
         float chance = be.melt() == 2 ? 0.7F : (be.melt() == 1 ? 0.1F : 0.0F);
         if (random.nextFloat() < chance) {
            double x = (double)pos.getX() + 0.15 + random.nextDouble() * 0.7;
            double z = (double)pos.getZ() + 0.15 + random.nextDouble() * 0.7;
            level.addParticle(
               new ItemParticleOption(ParticleTypes.ITEM, new ItemStack((ItemLike)DoruItems.WAX_LUMP.get())), x, (double)pos.getY() + 0.05, z, 0.0, -0.06, 0.0
            );
         }

         if (be.melt() == 2 && random.nextFloat() < 0.12F) {
            level.addParticle(
               ParticleTypes.WHITE_SMOKE,
               (double)pos.getX() + random.nextDouble(),
               (double)pos.getY() + 1.0,
               (double)pos.getZ() + random.nextDouble(),
               0.0,
               0.02,
               0.0
            );
         }

         if (!be.cured() && random.nextFloat() < 0.015F) {
            level.addParticle(
               ParticleTypes.WHITE_SMOKE,
               (double)pos.getX() + random.nextDouble(),
               (double)pos.getY() + 1.0,
               (double)pos.getZ() + random.nextDouble(),
               0.0,
               0.01,
               0.0
            );
         }
      }
   }
}
