package com.awakenedmod.awakened.content.hydro;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

final class LandingLock {
   private static final int GRACE = 20;
   private final Map<UUID, List<LandingLock.Entry>> locks;

   LandingLock(Map<UUID, List<LandingLock.Entry>> locks) {
      this.locks = locks;
   }

   boolean locked(ServerPlayer caster, Entity body) {
      List<LandingLock.Entry> list = this.locks.get(caster.getUUID());
      if (list == null) {
         return false;
      } else {
         for (LandingLock.Entry e : list) {
            if (e.body == body) {
               return true;
            }
         }

         return false;
      }
   }

   void lock(ServerPlayer caster, Entity body, int delay) {
      if (!this.locked(caster, body)) {
         this.locks.computeIfAbsent(caster.getUUID(), k -> new ArrayList<>()).add(new LandingLock.Entry(body, delay));
      }
   }

   void release(ServerPlayer caster, Entity body) {
      List<LandingLock.Entry> list = this.locks.get(caster.getUUID());
      if (list != null) {
         list.removeIf(e -> e.body == body);
      }
   }

   void tick(ServerPlayer caster) {
      List<LandingLock.Entry> list = this.locks.get(caster.getUUID());
      if (list != null) {
         list.removeIf(e -> e.done(caster));
         if (list.isEmpty()) {
            this.locks.remove(caster.getUUID());
         }
      }
   }

   private static boolean landed(Entity body) {
      if (!body.onGround() && !body.isInWater() && !body.isPassenger()) {
         if (body instanceof Player p && p.getAbilities().flying) {
            return true;
         }

         if (body instanceof LivingEntity l && l.onClimbable()) {
            return true;
         }

         return false;
      } else {
         return true;
      }
   }

   static final class Entry {
      private final Entity body;
      private int delay;
      private int age;
      private boolean aloft;

      private Entry(Entity body, int delay) {
         this.body = body;
         this.delay = delay;
      }

      private boolean done(ServerPlayer caster) {
         if (this.body.isRemoved() || !this.body.isAlive() || this.body.level() != caster.level()) {
            return true;
         } else if (this.delay > 0) {
            this.delay--;
            return false;
         } else {
            this.age++;
            if (!LandingLock.landed(this.body)) {
               this.aloft = true;
               return false;
            } else {
               return this.aloft || this.age > 20;
            }
         }
      }
   }
}
