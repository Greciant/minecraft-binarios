package com.awakenedmod.awakened.content.ruin;

import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Harm;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

final class RuinSpreadFront {
   private final ServerLevel level;
   private final RuinStrip strip;
   private final Set<Integer> reached = new HashSet<>();
   private int age;

   RuinSpreadFront(ServerLevel level, RuinStrip strip) {
      this.level = level;
      this.strip = strip;
   }

   static int lifetime(RuinStrip strip) {
      return strip.travel() + 80;
   }

   boolean tick(ServerPlayer caster, Map<Integer, LivingEntity> onRot, Set<Integer> struck) {
      this.age++;
      this.reach(caster, this.strip.frontAt(Math.min(this.age, this.strip.travel())), onRot, struck);
      return this.age >= lifetime(this.strip);
   }

   private void reach(ServerPlayer caster, double front, Map<Integer, LivingEntity> onRot, Set<Integer> struck) {
      if (!(front <= 0.05)) {
         for (LivingEntity body : Harm.hostilesOnly(
            caster, AimUtil.livingInGroundWedge(caster, this.strip.apex(), this.strip.dir(), front, this.strip.halfNear(), this.strip.halfAt(front), 1.5)
         )) {
            int id = body.getId();
            onRot.put(id, body);
            if (this.reached.add(id)) {
               struck.add(id);
               if (RuinStacks.add(body, 3, caster, body.position())) {
                  RuinSpreadFx.touch(this.level, body);
               }
            }
         }
      }
   }
}
