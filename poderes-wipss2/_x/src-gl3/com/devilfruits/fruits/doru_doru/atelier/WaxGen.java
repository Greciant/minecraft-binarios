package com.devilfruits.fruits.doru_doru.atelier;

import com.devilfruits.fruits.doru_doru.wax.WaxBlockEntity;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;

public final class WaxGen {
   @Nullable
   public static AtelierGen.Result generate(SculptRequest r, AtelierGen.Context ctx) {
      if (r.tool() == SculptRequest.Tool.SHAPE) {
         VoxelSet set = ShapeGen.generate(r);
         return set == null ? null : new AtelierGen.Result(set, null);
      } else {
         return AtelierGen.generate(r, ctx);
      }
   }

   public static AtelierGen.Context context(final BlockGetter level, @Nullable final CastSlots casts) {
      final MutableBlockPos m = new MutableBlockPos();
      return new AtelierGen.Context() {
         @Override
         public boolean solid(int sx, int sy, int sz) {
            m.set(sx >> 2, sy >> 2, sz >> 2);
            if (level instanceof Level l && !l.isLoaded(m)) {
               return true;
            }

            return level.getBlockEntity(m) instanceof WaxBlockEntity be
               ? (be.mask() >>> (sx & 3 | (sz & 3) << 2 | (sy & 3) << 4) & 1L) != 0L
               : !level.getBlockState(m).getCollisionShape(level, m).isEmpty();
         }

         @Override
         public CastData cast(int slot) {
            return casts == null ? null : casts.get(slot);
         }
      };
   }

   private WaxGen() {
   }
}
