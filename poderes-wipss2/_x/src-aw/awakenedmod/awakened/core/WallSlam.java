package com.awakenedmod.awakened.core;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class WallSlam {
   private final int windowTicks;
   private final double reach;
   private final Map<UUID, Map<Integer, WallSlam.Flung>> watched = new HashMap<>();

   public WallSlam(int windowTicks, double reach) {
      this.windowTicks = windowTicks;
      this.reach = reach;
   }

   public void track(ServerPlayer caster, LivingEntity body, Vec3 push) {
      Vec3 flat = new Vec3(push.x, 0.0, push.z);
      if (!(flat.lengthSqr() < 1.0E-6)) {
         this.watched
            .computeIfAbsent(caster.getUUID(), k -> new LinkedHashMap<>())
            .put(body.getId(), new WallSlam.Flung(body, flat.normalize(), this.windowTicks));
      }
   }

   public void tick(ServerPlayer caster, WallSlam.Landed landed) {
      Map<Integer, WallSlam.Flung> map = this.watched.get(caster.getUUID());
      if (map != null) {
         Iterator<WallSlam.Flung> it = map.values().iterator();

         while (it.hasNext()) {
            WallSlam.Flung f = it.next();
            if (f.body.isAlive() && !f.body.isRemoved() && f.body.level() == caster.level() && f.left-- > 0) {
               Vec3 wall = this.wall(f);
               if (wall != null) {
                  it.remove();
                  landed.slam(caster, f.body, wall);
               }
            } else {
               it.remove();
            }
         }

         if (map.isEmpty()) {
            this.watched.remove(caster.getUUID());
         }
      }
   }

   public void clear(ServerPlayer caster) {
      this.watched.remove(caster.getUUID());
   }

   @Nullable
   private Vec3 wall(WallSlam.Flung f) {
      Vec3 hit = AimUtil.wallWithin(f.body, f.dir, this.reach);
      if (hit != null) {
         return hit;
      } else {
         Vec3 v = f.body.getDeltaMovement();
         if (f.body instanceof Player) {
            return v.horizontalDistanceSqr() > 1.0E-4 ? AimUtil.wallWithin(f.body, v, this.reach) : null;
         } else {
            return f.body.horizontalCollision ? f.body.position().add(0.0, (double)f.body.getBbHeight() * 0.5, 0.0) : null;
         }
      }
   }

   private static final class Flung {
      final LivingEntity body;
      final Vec3 dir;
      int left;

      Flung(LivingEntity body, Vec3 dir, int left) {
         this.body = body;
         this.dir = dir;
         this.left = left;
      }
   }

   public interface Landed {
      void slam(ServerPlayer var1, LivingEntity var2, Vec3 var3);
   }
}
