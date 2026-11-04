package com.devilfruits.fruits.doru_doru.client.atelier;

import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.fruits.doru_doru.atelier.DoruNetwork;
import com.devilfruits.fruits.doru_doru.client.vfx.PourCinematic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;

public final class PourClient {
   public static void handle(DoruNetwork.PourPayload p) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level != null) {
         PourCinematic running = VfxDirector.find(PourCinematic.class, c -> c.entityId() == p.entity());
         if (running != null) {
            running.end();
         }

         if (!p.end()) {
            Entity e = level.getEntity(p.entity());
            if (e != null && p.path().length != 0) {
               VfxDirector.spawn(new PourCinematic(e, p.path(), p.total(), p.rate(), p.start(), p.carve(), p.color()));
            }
         }
      }
   }

   private PourClient() {
   }
}
