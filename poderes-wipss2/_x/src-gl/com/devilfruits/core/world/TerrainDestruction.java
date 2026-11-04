package com.devilfruits.core.world;

import com.devilfruits.core.config.CoreConfig;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.BlockImpactService;
import com.devilfruits.core.physics.PhysicsStructure;
import com.devilfruits.core.physics.PhysicsWorld;
import com.devilfruits.core.poneglyph.Poneglyphs;
import com.devilfruits.registry.DFTags;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.level.BlockEvent.BreakEvent;

public final class TerrainDestruction {
   public static boolean destructionAllowed(ServerLevel level) {
      return !CoreConfig.BLOCK_DESTRUCTION.get()
         ? false
         : !(Boolean)CoreConfig.RESPECT_MOB_GRIEFING.get() || level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
   }

   public static boolean canAffect(ServerLevel level, ServerPlayer caster, BlockPos pos, BlockState state) {
      return canAffect(level, caster, pos, state, false);
   }

   public static boolean canAffect(ServerLevel level, ServerPlayer caster, BlockPos pos, BlockState state, boolean anyHardness) {
      if (!state.isAir()
         && state.getFluidState().isEmpty()
         && !state.hasBlockEntity()
         && !state.is(DFTags.DESTRUCTION_IMMUNE)
         && !state.is(Poneglyphs.UNBREAKABLE)) {
         float hardness = state.getDestroySpeed(level, pos);
         if (!(hardness < 0.0F) && (anyHardness ? !state.is(DFTags.RESISTS_TECHNIQUES) : !((double)hardness > (Double)CoreConfig.MAX_BLOCK_HARDNESS.get()))) {
            if (!level.mayInteract(caster, pos)) {
               return false;
            } else {
               BreakEvent event = new BreakEvent(level, pos, state, caster);
               NeoForge.EVENT_BUS.post(event);
               return !event.isCanceled();
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public static boolean tearOut(ServerLevel level, ServerPlayer caster, BlockPos pos, @Nullable Vec3 velocity, CastBudget budget, int restoreJitter) {
      return tearOut(level, caster, pos, velocity, budget, restoreJitter, false);
   }

   public static boolean tearOut(
      ServerLevel level, ServerPlayer caster, BlockPos pos, @Nullable Vec3 velocity, CastBudget budget, int restoreJitter, boolean anyHardness
   ) {
      return tearOut(level, caster, pos, velocity, budget, restoreJitter, anyHardness, true);
   }

   public static boolean tearOut(
      ServerLevel level, ServerPlayer caster, BlockPos pos, @Nullable Vec3 velocity, CastBudget budget, int restoreJitter, boolean anyHardness, boolean burst
   ) {
      PhysicsStructure structure = PhysicsWorld.structureAt(level, pos);
      if (structure != null) {
         return BlockImpactService.breakStructureBlock(level, caster, structure, pos, velocity, budget, anyHardness);
      } else {
         BlockState state = level.getBlockState(pos);
         if (budget.hasBlocks() && canAffect(level, caster, pos, state, anyHardness) && budget.takeBlock()) {
            BlockPos otherPos = null;
            BlockState other = null;
            if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
               boolean lower = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER;
               otherPos = lower ? pos.above() : pos.below();
               other = level.getBlockState(otherPos);
               if (!other.is(state.getBlock())) {
                  otherPos = null;
                  other = null;
               }
            }

            boolean temporary = CoreConfig.DEBRIS_MODE.get() == CoreConfig.DebrisMode.TEMPORARY;
            if (temporary) {
               long restoreAt = level.getGameTime() + (long)((Integer)CoreConfig.RESTORE_DELAY_SECONDS.get()).intValue() * 20L + (long)restoreJitter;
               BlockRestoreData.get(level).add(pos, state, restoreAt);
               if (other != null) {
                  BlockRestoreData.get(level).add(otherPos, other, restoreAt);
               }
            } else if ((Boolean)CoreConfig.DROP_BLOCKS.get()) {
               boolean lowerHere = other == null || state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER;
               Block.dropResources(lowerHere ? state : other, level, lowerHere ? pos : otherPos, null, caster, ItemStack.EMPTY);
            }

            if (other != null) {
               level.setBlock(otherPos, Blocks.AIR.defaultBlockState(), 18);
            }

            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            Vec3 center = Vec3.atCenterOf(pos);
            if (velocity != null && budget.takeDebris()) {
               level.addFreshEntity(new DebrisEntity(level, center.x, (double)pos.getY(), center.z, state, velocity));
            } else if (burst) {
               ServerFx.blockBurst(level, center, state, 0.6F);
            }

            return true;
         } else {
            return false;
         }
      }
   }

   public static boolean ignite(ServerLevel level, ServerPlayer caster, BlockPos pos, int restoreJitter) {
      if (destructionAllowed(level) && level.getBlockState(pos).isAir() && level.mayInteract(caster, pos)) {
         BlockState fire = BaseFireBlock.getState(level, pos);
         if (!fire.canSurvive(level, pos)) {
            return false;
         } else if (EventHooks.onBlockPlace(caster, BlockSnapshot.create(level.dimension(), level, pos), Direction.UP)) {
            return false;
         } else {
            level.setBlock(pos, fire, 3);
            if (CoreConfig.DEBRIS_MODE.get() == CoreConfig.DebrisMode.TEMPORARY && PhysicsWorld.structureAt(level, pos) == null) {
               long restoreAt = level.getGameTime() + (long)((Integer)CoreConfig.RESTORE_DELAY_SECONDS.get()).intValue() * 20L + (long)restoreJitter;
               BlockRestoreData.get(level).addTransform(pos, Blocks.AIR.defaultBlockState(), fire, restoreAt);
            }

            return true;
         }
      } else {
         return false;
      }
   }

   public static boolean transform(
      ServerLevel level, ServerPlayer caster, BlockPos pos, BlockState to, CastBudget budget, int restoreTicks, @Nullable BlockState permanentRevert, int flags
   ) {
      BlockState state = level.getBlockState(pos);
      if (PhysicsWorld.structureAt(level, pos) != null) {
         return false;
      } else if (state != to && budget.hasBlocks() && canTransform(level, caster, pos, state) && budget.takeBlock()) {
         long restoreAt = level.getGameTime() + (long)Math.max(1, restoreTicks);
         if (CoreConfig.DEBRIS_MODE.get() == CoreConfig.DebrisMode.TEMPORARY) {
            BlockRestoreData.get(level).addTransform(pos, state, to, restoreAt);
         } else if (permanentRevert != null) {
            BlockRestoreData.get(level).addTransform(pos, permanentRevert, to, restoreAt);
         }

         level.setBlock(pos, to, flags);
         return true;
      } else {
         return false;
      }
   }

   public static boolean canTransform(ServerLevel level, ServerPlayer caster, BlockPos pos, BlockState state) {
      if (!state.isAir() && !state.hasBlockEntity() && !state.is(DFTags.DESTRUCTION_IMMUNE) && !state.is(Poneglyphs.UNBREAKABLE)) {
         boolean water = state.is(Blocks.WATER) || state.getFluidState().is(FluidTags.WATER);
         if (!state.getFluidState().isEmpty() && !water) {
            return false;
         } else {
            if (!state.is(Blocks.WATER)) {
               float hardness = state.getDestroySpeed(level, pos);
               if (hardness < 0.0F || (double)hardness > (Double)CoreConfig.MAX_BLOCK_HARDNESS.get()) {
                  return false;
               }
            }

            if (!level.mayInteract(caster, pos)) {
               return false;
            } else {
               BreakEvent event = new BreakEvent(level, pos, state, caster);
               NeoForge.EVENT_BUS.post(event);
               return !event.isCanceled();
            }
         }
      } else {
         return false;
      }
   }

   @Nullable
   public static BlockPos findSurface(ServerLevel level, int x, int startY, int z, int up, int down) {
      MutableBlockPos pos = new MutableBlockPos(x, startY + up, z);

      for (int i = 0; i <= up + down; i++) {
         BlockState state = level.getBlockState(pos);
         if (!state.isAir() && state.isSolidRender(level, pos) && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()) {
            return pos.immutable();
         }

         pos.move(0, -1, 0);
      }

      return null;
   }

   private TerrainDestruction() {
   }
}
