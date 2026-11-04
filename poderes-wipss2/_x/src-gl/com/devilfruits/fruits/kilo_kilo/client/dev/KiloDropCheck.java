package com.devilfruits.fruits.kilo_kilo.client.dev;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.dev.AutoTest;
import com.devilfruits.fruits.kilo_kilo.KiloAbilities;
import com.devilfruits.fruits.kilo_kilo.KiloKilo;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

public final class KiloDropCheck implements AutoTest.Script {
   private static final int TOWER = 24;

   @Override
   public int write(AutoTest s, int start) {
      s.add(start, mc -> {
         s.camera(mc, CameraType.THIRD_PERSON_BACK);
         s.refresh(mc, KiloKilo.ID);
         s.server(mc, p -> {
            tower(p.serverLevel(), s.origin(), 20);
            tower(p.serverLevel(), s.origin(), 26);
         });
      });
      s.add(start + 10, mc -> s.tap(KiloAbilities.PRESS));
      drop(s, start + 60, 20, 5, "5");
      drop(s, start + 160, 26, 30, "30");
      s.add(start + 260, mc -> s.tap(KiloAbilities.PRESS));
      return start + 270;
   }

   private static void drop(AutoTest s, int at, int x, int height, String name) {
      s.add(at, mc -> s.server(mc, p -> {
            BlockPos o = s.origin();
            p.teleportTo(p.serverLevel(), (double)(o.getX() + x) + 0.5, (double)(o.getY() + 24 + height), (double)o.getZ() + 0.5, 0.0F, 60.0F);
            p.getAbilities().flying = false;
            p.onUpdateAbilities();
            p.fallDistance = 0.0F;
         }));
      s.add(
         at + 70,
         mc -> {
            s.shot(mc, "vfx_kilo_drop_" + name);
            s.server(
               mc,
               p -> {
                  double top = (double)(s.origin().getY() + 24);
                  DevilFruits.LOGGER
                     .info(
                        "[devilfruits autotest] kilo drop from {} blocks: {} blocks below the tower top (limit 10), weight {} kg",
                        new Object[]{height, top - p.getY(), KiloKilo.weight(p).kg()}
                     );
               }
            );
         }
      );
   }

   private static void tower(ServerLevel level, BlockPos o, int x) {
      for (int y = 0; y < 24; y++) {
         for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
               level.setBlockAndUpdate(new BlockPos(o.getX() + x + dx, o.getY() + y, o.getZ() + dz), Blocks.DIRT.defaultBlockState());
            }
         }
      }
   }
}
