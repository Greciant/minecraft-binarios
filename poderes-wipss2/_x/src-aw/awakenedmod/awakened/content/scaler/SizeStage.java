package com.awakenedmod.awakened.content.scaler;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public enum SizeStage {
   SPECK("speck", 0.1F, 0.1F, 2.0F, 0.6F, -1.0, 0.0, 2.5F, 0.11F, 0.0, 3),
   MITE("mite", 0.25F, 0.3F, 1.6F, 0.85F, -0.75, 0.0, 1.8F, 0.05F, 0.0, 2),
   SMALL("small", 0.5F, 0.7F, 1.25F, 1.1F, -0.5, 0.0, 1.3F, 0.015F, 0.0, 1),
   NORMAL("normal", 1.0F, 1.0F, 1.0F, 1.0F, 0.0, 0.0, 1.0F, 0.0F, 0.0, 0),
   BRUTE("brute", 2.0F, 1.4F, 0.85F, 0.95F, 0.5, 0.3, 1.0F, 0.015F, 0.4, 1),
   TITAN("titan", 3.0F, 1.9F, 0.75F, 0.85F, 1.0, 0.6, 1.0F, 0.05F, 0.9, 2),
   COLOSSUS("colossus", 5.0F, 2.8F, 0.65F, 0.75F, 2.0, 0.95, 1.0F, 0.11F, 1.4, 3);

   private static final SizeStage[] VALUES = values();
   private final String key;
   private final float scale;
   private final float damageDealtMult;
   private final float damageTakenMult;
   private final float speedMult;
   private final double attackKnockbackBonus;
   private final double knockbackResist;
   private final float knockbackTakenMult;
   private final float drainPerTick;
   private final double stepHeightBonus;
   private final int unlockDistance;

   private SizeStage(
      String key,
      float scale,
      float damageDealtMult,
      float damageTakenMult,
      float speedMult,
      double attackKnockbackBonus,
      double knockbackResist,
      float knockbackTakenMult,
      float drainPerTick,
      double stepHeightBonus,
      int unlockDistance
   ) {
      this.key = key;
      this.scale = scale;
      this.damageDealtMult = damageDealtMult;
      this.damageTakenMult = damageTakenMult;
      this.speedMult = speedMult;
      this.attackKnockbackBonus = attackKnockbackBonus;
      this.knockbackResist = knockbackResist;
      this.knockbackTakenMult = knockbackTakenMult;
      this.drainPerTick = drainPerTick;
      this.stepHeightBonus = stepHeightBonus;
      this.unlockDistance = unlockDistance;
   }

   public float scale() {
      return this.scale;
   }

   public float damageDealtMult() {
      return this.damageDealtMult;
   }

   public float damageTakenMult() {
      return this.damageTakenMult;
   }

   public float speedMult() {
      return this.speedMult;
   }

   public double attackKnockbackBonus() {
      return this.attackKnockbackBonus;
   }

   public double knockbackResist() {
      return this.knockbackResist;
   }

   public float knockbackTakenMult() {
      return this.knockbackTakenMult;
   }

   public float drainPerTick() {
      return this.drainPerTick;
   }

   public double stepHeightBonus() {
      return this.stepHeightBonus;
   }

   public int unlockDistance() {
      return this.unlockDistance;
   }

   public Component displayName() {
      return Component.translatable("awakened.scaler.stage." + this.key);
   }

   public SizeStage shifted(int dir) {
      return byOrdinal(this.ordinal() + dir);
   }

   public SizeStage towardNormal() {
      return this == NORMAL ? this : VALUES[this.ordinal() + (this.ordinal() < NORMAL.ordinal() ? 1 : -1)];
   }

   public static SizeStage byOrdinal(int ordinal) {
      return VALUES[Mth.clamp(ordinal, 0, VALUES.length - 1)];
   }

   public static int count() {
      return VALUES.length;
   }
}
