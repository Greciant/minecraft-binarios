package com.devilfruits.fruits.suna_suna;

import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.world.CastBudget;
import com.devilfruits.core.world.TerrainDestruction;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

public final class Desiccation {
   public static final int QUIET = 18;

   public static boolean changesBlocks(ServerLevel level) {
      return SunaSuna.fruit().config().flag("desiccationChangesBlocks") && TerrainDestruction.destructionAllowed(level);
   }

   public static CastBudget budget() {
      return new CastBudget((int)SunaSuna.fruit().config().setting("desiccationBlocksPerCast"), 0);
   }

   public static int restoreTicks() {
      return (int)(SunaSuna.fruit().config().setting("desiccationRestoreSeconds") * 20.0);
   }

   public static int dry(ServerLevel level, ServerPlayer caster, Vec3 centre, double radius, Desiccation.Options options, CastBudget budget) {
      int changed = dry(level, caster, centre, radius, 0.0, options, budget);
      ServerFx.send(caster, SunaFx.DESICCATE, centre, Vec3.ZERO, (float)radius, level.getRandom().nextLong(), changed);
      return changed;
   }

   public static int dry(ServerLevel level, ServerPlayer caster, Vec3 centre, double radius, double inner, Desiccation.Options options, CastBudget budget) {
      if (!changesBlocks(level)) {
         return 0;
      } else {
         int changed = 0;
         int restoreTicks = restoreTicks();
         int r = Mth.ceil(radius);
         int cx = Mth.floor(centre.x);
         int cz = Mth.floor(centre.z);
         int cy = Mth.floor(centre.y);

         for (int ring = 0; ring <= r && budget.hasBlocks(); ring++) {
            for (int dx = -ring; dx <= ring && budget.hasBlocks(); dx++) {
               for (int dz = -ring; dz <= ring && budget.hasBlocks(); dz++) {
                  if (Math.max(Math.abs(dx), Math.abs(dz)) == ring) {
                     double d2 = (double)(dx * dx + dz * dz);
                     if (!(d2 > radius * radius) && (!(inner > 0.0) || !(d2 <= inner * inner))) {
                        changed += dryColumn(level, caster, cx + dx, cy, cz + dz, options, budget, restoreTicks + jitter(dx, dz));
                     }
                  }
               }
            }
         }

         return changed;
      }
   }

   private static int jitter(int dx, int dz) {
      return (int)Math.sqrt((double)(dx * dx + dz * dz)) * 6;
   }

   private static int dryColumn(ServerLevel level, ServerPlayer caster, int x, int y, int z, Desiccation.Options options, CastBudget budget, int restoreTicks) {
      int changed = 0;
      MutableBlockPos pos = new MutableBlockPos(x, y + 4, z);
      int soilLeft = options.soilDepth;
      int waterLeft = options.waterDepth;
      boolean surfaceFound = false;
      int i = 0;

      while (i < 9 + options.waterDepth && budget.hasBlocks()) {
         BlockState state = level.getBlockState(pos);
         if (state.isAir()) {
            if (surfaceFound) {
               break;
            }
         } else {
            BlockState to = null;
            if (!state.is(Blocks.WATER) && (!state.is(SunaTags.DRIES_AWAY) || state.getFluidState().isEmpty())) {
               if (state.is(SunaTags.DRIES_AWAY)) {
                  to = Blocks.AIR.defaultBlockState();
               } else if (state.is(SunaTags.WITHERS)) {
                  to = withered(level, pos);
               } else if (options.leaves && state.is(BlockTags.LEAVES)) {
                  to = Blocks.AIR.defaultBlockState();
               } else if (state.hasProperty(BlockStateProperties.WATERLOGGED) && (Boolean)state.getValue(BlockStateProperties.WATERLOGGED)) {
                  to = (BlockState)state.setValue(BlockStateProperties.WATERLOGGED, false);
               } else if (state.isSolidRender(level, pos) || state.is(SunaTags.DRIES_TO_SAND) || state.is((Block)SunaBlocks.QUICKSAND.get())) {
                  surfaceFound = true;
                  if (soilLeft-- <= 0) {
                     break;
                  }

                  if (state.is(SunaTags.DRIES_TO_SAND)) {
                     to = Blocks.SAND.defaultBlockState();
                  }
               }
            } else if (waterLeft-- > 0) {
               to = Blocks.AIR.defaultBlockState();
            }

            if (to != null && TerrainDestruction.transform(level, caster, pos, to, budget, restoreTicks, null, 18)) {
               changed++;
            }
         }

         i++;
         pos.move(0, -1, 0);
      }

      return changed;
   }

   @Nullable
   private static BlockState withered(ServerLevel level, BlockPos pos) {
      BlockState below = level.getBlockState(pos.below());
      if (below.is(SunaTags.WITHERS)) {
         return Blocks.AIR.defaultBlockState();
      } else {
         boolean soil = below.is(SunaTags.DRIES_TO_SAND) || below.is(BlockTags.DEAD_BUSH_MAY_PLACE_ON);
         return soil ? Blocks.DEAD_BUSH.defaultBlockState() : Blocks.AIR.defaultBlockState();
      }
   }

   private Desiccation() {
   }

   public static record Options(int soilDepth, int waterDepth, boolean leaves) {
      public static final Desiccation.Options SURFACE = new Desiccation.Options(1, 3, false);

      public Desiccation.Options withLeaves() {
         return new Desiccation.Options(this.soilDepth, this.waterDepth, true);
      }
   }
}
