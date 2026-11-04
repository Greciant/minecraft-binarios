package com.awakenedmod.awakened.core;

import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.common.Tags.EntityTypes;

public final class Bosses {
   private Bosses() {
   }

   public static boolean is(Entity entity) {
      return entity instanceof AwakenedThreat threat && threat.isBoss() || entity instanceof Bosses.Part || entity.getType().is(EntityTypes.BOSSES);
   }

   public interface Part {
   }
}
