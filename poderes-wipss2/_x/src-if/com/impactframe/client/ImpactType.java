package com.impactframe.client;

import java.util.concurrent.ThreadLocalRandom;

public enum ImpactType {
   BLOCK_PLACE("blockPlace", 0.45F, ImpactTier.LIGHT),
   BLOCK_BREAK("blockBreak", 0.65F, ImpactTier.LIGHT),
   ATTACK("attack", 0.75F, ImpactTier.NORMAL),
   LAND("land", 0.75F, ImpactTier.NORMAL),
   FOOTSTEP("footstep", 0.45F, ImpactTier.LIGHT),
   DAMAGE("damage", 0.85F, ImpactTier.NORMAL),
   CRIT("crit", 0.9F, ImpactTier.HEAVY),
   EXPLOSION("explosion", 0.95F, ImpactTier.HEAVY),
   KILL("kill", 1.0F, ImpactTier.HEAVY),
   MACE("mace", 1.0F, ImpactTier.SMASH);

   private static final int SFX_VARIANT_COUNT = 5;
   public final String key;
   public final float baseStrength;
   public final ImpactTier defaultTier;

   private ImpactType(String key, float baseStrength, ImpactTier defaultTier) {
      this.key = key;
      this.baseStrength = baseStrength;
      this.defaultTier = defaultTier;
   }

   public String randomSfxTranslationKey() {
      int variant = ThreadLocalRandom.current().nextInt(5);
      return "impactframe.sfx." + this.key + (variant == 0 ? "" : "." + (variant + 1));
   }

   public float[] tint() {
      return switch (this) {
         case DAMAGE -> new float[]{0.95F, 0.12F, 0.12F};
         case CRIT -> new float[]{1.0F, 0.78F, 0.12F};
         case EXPLOSION -> new float[]{1.0F, 0.5F, 0.1F};
         case KILL -> new float[]{0.7F, 0.2F, 0.95F};
         case MACE -> new float[]{0.1F, 0.85F, 1.0F};
         default -> null;
      };
   }
}
