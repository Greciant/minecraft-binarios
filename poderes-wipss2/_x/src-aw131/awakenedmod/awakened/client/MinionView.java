package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.content.summoning.SummonAttachments;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

public final class MinionView {
   private MinionView() {
   }

   public static List<Mob> owned(Minecraft mc) {
      List<Mob> out = new ArrayList<>();
      if (mc.player != null && mc.level != null) {
         UUID self = mc.player.getUUID();

         for (Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof Mob) {
               Mob m = (Mob)e;
               if (m.isAlive() && SummonAttachments.minion(m).isOwnedBy(self)) {
                  out.add(m);
               }
            }
         }

         return out;
      } else {
         return out;
      }
   }

   public static int fielded(Minecraft mc) {
      return fielded(owned(mc));
   }

   public static int fielded(List<Mob> owned) {
      int n = 0;

      for (Mob m : owned) {
         if (!SummonAttachments.minion(m).conjured()) {
            n++;
         }
      }

      return n;
   }
}
