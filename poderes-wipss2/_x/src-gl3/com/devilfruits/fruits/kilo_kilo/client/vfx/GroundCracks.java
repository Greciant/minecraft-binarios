package com.devilfruits.fruits.kilo_kilo.client.vfx;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public final class GroundCracks {
   private static final int MAX = 128;
   private static int nextId = -1258291200;
   private static final Map<BlockPos, GroundCracks.Crack> ACTIVE = new LinkedHashMap<>();

   public static void crack(ClientLevel level, BlockPos pos, float stage, int life) {
      if (!level.getBlockState(pos).isAir() && !(stage < 0.5F)) {
         GroundCracks.Crack c = ACTIVE.get(pos);
         if (c == null) {
            if (ACTIVE.size() >= 128) {
               Iterator<Entry<BlockPos, GroundCracks.Crack>> it = ACTIVE.entrySet().iterator();
               GroundCracks.Crack old = it.next().getValue();
               Minecraft.getInstance().levelRenderer.destroyBlockProgress(old.id, BlockPos.ZERO, -1);
               it.remove();
            }

            c = new GroundCracks.Crack();
            ACTIVE.put(pos.immutable(), c);
         }

         float left = c.life > 0 ? c.stage * (1.0F - (float)c.age / (float)c.life) : 0.0F;
         if (stage >= left) {
            c.stage = Math.min(9.0F, stage);
            c.life = life;
            c.age = 0;
         }
      }
   }

   public static void patch(ClientLevel level, double x, double y, double z, double radius, float stage, int life, long seed) {
      RandomSource r = RandomSource.create(seed);
      int reach = Mth.ceil(radius);
      MutableBlockPos p = new MutableBlockPos();

      for (int dx = -reach; dx <= reach; dx++) {
         for (int dz = -reach; dz <= reach; dz++) {
            double d = Math.sqrt(
               ((double)dx + 0.5 - (x - Math.floor(x))) * ((double)dx + 0.5 - (x - Math.floor(x)))
                  + ((double)dz + 0.5 - (z - Math.floor(z))) * ((double)dz + 0.5 - (z - Math.floor(z)))
            );
            double edge = radius * (0.7 + 0.5 * r.nextDouble());
            if (!(d > edge)) {
               p.set(Mth.floor(x) + dx, Mth.floor(y - 0.2), Mth.floor(z) + dz);
               float s = (float)((double)stage * (1.0 - d / (edge + 0.5)) * (0.75 + 0.35 * r.nextDouble()));
               crack(level, p, s, (int)((double)life * (0.7 + 0.5 * r.nextDouble())));
            }
         }
      }
   }

   static void tick(ClientLevel level) {
      Iterator<Entry<BlockPos, GroundCracks.Crack>> it = ACTIVE.entrySet().iterator();

      while (it.hasNext()) {
         Entry<BlockPos, GroundCracks.Crack> e = it.next();
         GroundCracks.Crack c = e.getValue();
         c.age++;
         int stage = c.age >= c.life ? -1 : Mth.floor(c.stage * (1.0F - (float)c.age / (float)c.life) + 0.5F);
         if (stage < 0 || level.getBlockState(e.getKey()).isAir()) {
            Minecraft.getInstance().levelRenderer.destroyBlockProgress(c.id, e.getKey(), -1);
            it.remove();
         } else if (stage != c.shown) {
            c.shown = stage;
            Minecraft.getInstance().levelRenderer.destroyBlockProgress(c.id, e.getKey(), Math.min(9, stage));
         }
      }
   }

   public static void tickAll() {
      ClientLevel level = Minecraft.getInstance().level;
      if (level == null) {
         ACTIVE.clear();
      } else {
         tick(level);
      }
   }

   public static void clear() {
      for (Entry<BlockPos, GroundCracks.Crack> e : ACTIVE.entrySet()) {
         Minecraft.getInstance().levelRenderer.destroyBlockProgress(e.getValue().id, e.getKey(), -1);
      }

      ACTIVE.clear();
   }

   private GroundCracks() {
   }

   private static final class Crack {
      final int id = GroundCracks.nextId--;
      float stage;
      int life;
      int age;
      int shown = -1;
   }
}
