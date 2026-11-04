package com.devilfruits.fruits.doru_doru.atelier;

import com.devilfruits.fruits.doru_doru.wax.WaxBlock;
import com.devilfruits.fruits.doru_doru.wax.WaxBlockEntity;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class WaxViews {
   public static WaxIntegrity.View of(final BlockGetter level, @Nullable final Long2LongMap added, @Nullable final LongSet removed) {
      final MutableBlockPos m = new MutableBlockPos();
      return new WaxIntegrity.View() {
         @Override
         public long mask(long pos) {
            if (removed != null && removed.contains(pos)) {
               return 0L;
            } else {
               long extra = added == null ? 0L : added.get(pos);
               m.set(pos);
               if (level instanceof Level l && !l.isLoaded(m)) {
                  return extra;
               }

               long now = level.getBlockEntity(m) instanceof WaxBlockEntity be ? be.mask() : 0L;
               return now | extra;
            }
         }

         @Override
         public boolean anchor(long pos, Direction face) {
            m.set(pos);
            if (level instanceof Level l && !l.isLoaded(m)) {
               return false;
            }

            BlockState s = level.getBlockState(m);
            return !(s.getBlock() instanceof WaxBlock) && !s.isAir() && s.isFaceSturdy(level, m, face);
         }
      };
   }

   private WaxViews() {
   }
}
