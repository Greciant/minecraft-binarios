package com.awakenedmod.awakened.content.brawler;

import net.minecraft.world.entity.LivingEntity;

final class MarkBonus {
   static final MarkBonus.Row PUNCH = new MarkBonus.Row(0.3F, 0.3F, 0.3F, 0.4F, 0.3F);
   static final MarkBonus.Row RISING_HOOK = new MarkBonus.Row(0.2F, 0.4F, 0.4F, 0.5F, 0.9F);
   static final MarkBonus.Row SLEDGE_DROP = new MarkBonus.Row(0.9F, 0.2F, 0.4F, 0.9F, 0.4F);
   static final MarkBonus.Row SHOULDER_BARGE = MarkBonus.Row.flat(0.4F, 0.2F, 0.4F);
   static final MarkBonus.Row REEL_IN = new MarkBonus.Row(0.5F, 0.2F, 0.4F, 0.5F, 0.4F);

   private MarkBonus() {
   }

   static float of(MarkBonus.Row row, LivingEntity target) {
      return of(row, Marks.current(target), Marks.flavour(target));
   }

   static float of(MarkBonus.Row row, Mark mark, Mark.Flavour flavour) {
      if (mark == null) {
         return 0.0F;
      } else {
         return switch (mark) {
            case AIRBORNE -> row.airborne();
            case GROUNDED -> row.grounded();
            case PINNED -> {
               switch (flavour == null ? Mark.Flavour.NONE : flavour) {
                  case WALL:
                     yield row.pinnedWall();
                  case HAULED:
                     yield row.pinnedHauled();
                  default:
                     yield row.pinned();
               }
            }
         };
      }
   }

   static record Row(float airborne, float grounded, float pinned, float pinnedWall, float pinnedHauled) {
      static MarkBonus.Row flat(float airborne, float grounded, float pinned) {
         return new MarkBonus.Row(airborne, grounded, pinned, pinned, pinned);
      }
   }
}
