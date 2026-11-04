package com.awakenedmod.awakened.core.skill;

import net.minecraft.network.chat.Component;

public enum Stat {
   MAX_STAMINA("max_stamina", 10, 10.0F),
   MAX_HEALTH("max_health", 10, 2.0F),
   STAMINA_REGEN("stamina_regen", 10, 1.0F),
   ABILITY_POWER("ability_power", 10, 0.05F);

   private static final Stat[] VALUES = values();
   private final String id;
   private final int maxPoints;
   private final float perPoint;

   private Stat(String id, int maxPoints, float perPoint) {
      this.id = id;
      this.maxPoints = maxPoints;
      this.perPoint = perPoint;
   }

   public String id() {
      return this.id;
   }

   public int maxPoints() {
      return this.maxPoints;
   }

   public float perPoint() {
      return this.perPoint;
   }

   public Component displayName() {
      return Component.translatable("awakened.stat.awakened." + this.id);
   }

   public Component description() {
      return Component.translatable("awakened.stat.awakened." + this.id + ".desc");
   }

   public static Stat byId(int ordinal) {
      return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : null;
   }

   public static int count() {
      return VALUES.length;
   }
}
