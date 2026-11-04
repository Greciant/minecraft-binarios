package com.devilfruits.fruits.horo_horo.client;

import com.devilfruits.client.fx.ClientFxRegistry;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.fruits.horo_horo.HoroFx;
import com.devilfruits.fruits.horo_horo.client.vfx.GiantHollowCinematic;
import com.devilfruits.fruits.horo_horo.client.vfx.MiniHollowCinematic;
import com.devilfruits.fruits.horo_horo.client.vfx.NegativeHollowCinematic;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public final class HoroClientFx {
   static void register() {
      ClientFxRegistry.register(
         HoroFx.NEGATIVE_LAUNCH,
         (level, entity, p) -> VfxDirector.spawn(new NegativeHollowCinematic(entity, p.pos(), p.dir(), (double)p.power(), p.extra(), p.seed()))
      );
      ClientFxRegistry.register(HoroFx.NEGATIVE_HIT, (level, entity, p) -> {
         int id = entity != null ? entity.getId() : -1;
         NegativeHollowCinematic c = VfxDirector.find(NegativeHollowCinematic.class, k -> !k.isDone() && !k.hit() && k.targetId() == id);
         if (c != null) {
            c.onHit(level, p.pos(), p.dir(), entity);
         }
      });
      ClientFxRegistry.register(
         HoroFx.MINI_LAUNCH, (level, entity, p) -> VfxDirector.spawn(new MiniHollowCinematic(entity, p.pos(), p.dir(), (double)p.power(), p.extra(), p.seed()))
      );
      ClientFxRegistry.register(HoroFx.CLAP, (level, entity, p) -> {
         if (entity != null) {
            MiniHollowCinematic c = VfxDirector.find(MiniHollowCinematic.class, k -> !k.isDone() && !k.clapped() && k.anchor().entityId() == entity.getId());
            if (c != null) {
               c.onClap(level);
            }
         }
      });
      ClientFxRegistry.register(HoroFx.MINI_POP, (level, entity, p) -> {
         int id = entity != null ? entity.getId() : -1;
         MiniHollowCinematic c = VfxDirector.find(MiniHollowCinematic.class, k -> !k.isDone() && k.targetId() == id);
         if (c != null) {
            c.onPop(level, p.extra(), p.pos(), p.power());
         }
      });
      ClientFxRegistry.register(HoroFx.GHOST_EXIT, (level, entity, p) -> GhostLook.onExit(level, entity, p.pos(), p.extra(), p.seed()));
      ClientFxRegistry.register(HoroFx.POSSESS, (level, entity, p) -> GhostLook.onPossess(entity, p.pos(), p.extra()));
      ClientFxRegistry.register(
         HoroFx.GIANT_LAUNCH,
         (level, entity, p) -> VfxDirector.spawn(new GiantHollowCinematic(entity, p.pos(), p.dir(), (double)p.power(), p.extra(), p.seed()))
      );
      ClientFxRegistry.register(HoroFx.GIANT_ABSORB, (level, entity, p) -> {
         int id = entity != null ? entity.getId() : -1;
         GiantHollowCinematic c = VfxDirector.find(GiantHollowCinematic.class, k -> !k.isDone() && !k.absorbed() && k.targetId() == id);
         if (c != null) {
            c.onAbsorb(level, p.pos(), p.extra());
         }

         if (entity != null && entity == Minecraft.getInstance().player) {
            SwallowedView.begin(p.extra());
         }
      });
      ClientFxRegistry.register(HoroFx.GIANT_END, (level, entity, p) -> {
         GiantHollowCinematic[] best = new GiantHollowCinematic[]{null};
         double[] bestD = new double[]{64.0};
         VfxDirector.find(GiantHollowCinematic.class, k -> {
            double d = distSq(k, p.pos());
            if (!k.isDone() && k.endPending() && d < bestD[0]) {
               bestD[0] = d;
               best[0] = k;
            }

            return false;
         });
         if (best[0] != null) {
            best[0].onEnd(level, p.pos(), p.extra() == 1);
         }

         SwallowedView.end();
      });
   }

   private static double distSq(GiantHollowCinematic c, Vec3 at) {
      double dx = c.x() - at.x;
      double dy = c.y() - at.y;
      double dz = c.z() - at.z;
      return dx * dx + dy * dy + dz * dz;
   }

   private HoroClientFx() {
   }
}
