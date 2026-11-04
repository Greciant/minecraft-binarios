package com.awakenedmod.awakened.content.ruin;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

final class RuinRotDrip {
   private Map<Integer, Integer> standing = new HashMap<>();

   void tick(ServerPlayer caster, Map<Integer, LivingEntity> onRot, Set<Integer> struck) {
      Map<Integer, Integer> next = new HashMap<>();

      for (Entry<Integer, LivingEntity> e : onRot.entrySet()) {
         int id = e.getKey();
         int stood = struck.contains(id) ? 0 : this.standing.getOrDefault(id, 0) + 1;
         if (stood >= 10) {
            stood = 0;
            LivingEntity body = e.getValue();
            if (RuinStacks.add(body, 1, caster, body.position())) {
               RuinSpreadFx.fester((ServerLevel)body.level(), body);
            }
         }

         next.put(id, stood);
      }

      this.standing = next;
   }
}
