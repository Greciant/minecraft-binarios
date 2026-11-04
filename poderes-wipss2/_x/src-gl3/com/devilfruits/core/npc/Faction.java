package com.devilfruits.core.npc;

import com.devilfruits.DevilFruits;
import net.minecraft.resources.ResourceLocation;

public enum Faction {
   PIRATE("pirate"),
   MARINE("marine"),
   SHADOW("shadow");

   private final String id;

   private Faction(String id) {
      this.id = id;
   }

   public String id() {
      return this.id;
   }

   public ResourceLocation key() {
      return DevilFruits.id(this.id);
   }

   public boolean hostileTo(Faction other) {
      return this != other;
   }
}
