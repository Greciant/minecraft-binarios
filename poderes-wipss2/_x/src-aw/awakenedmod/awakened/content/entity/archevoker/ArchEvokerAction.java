package com.awakenedmod.awakened.content.entity.archevoker;

public final class ArchEvokerAction {
   public static final byte NONE = 0;
   public static final byte TRIPLE_FANG_LINE = 1;
   public static final byte FANG_WAVE = 2;
   public static final byte VEX_SUMMON = 3;
   public static final byte VEXPLOSIVES = 4;
   public static final byte EVOGATOR_SUMMON = 5;
   public static final byte CLONES = 6;
   public static final byte GARGANTUAN_FANG = 7;
   public static final byte REPULSE = 8;

   private ArchEvokerAction() {
   }

   public static boolean isSummonCast(byte action) {
      return action == 3 || action == 4 || action == 6;
   }

   public static boolean isFangCast(byte action) {
      return action == 1 || action == 2 || action == 5 || action == 7 || action == 8;
   }
}
