package com.awakenedmod.awakened.client.ruin;

import com.awakenedmod.awakened.core.Lab;
import net.minecraft.util.Mth;

public final class RuinLook {
   static final Lab.Value CRACK_ALPHA_1 = Lab.CLIENT.def("ruin.crack.alpha1", 0.7, 0.0, 1.0, 0.01);
   static final Lab.Value CRACK_ALPHA_2 = Lab.CLIENT.def("ruin.crack.alpha2", 0.82, 0.0, 1.0, 0.01);
   static final Lab.Value CRACK_ALPHA_3 = Lab.CLIENT.def("ruin.crack.alpha3", 0.92, 0.0, 1.0, 0.01);
   static final Lab.Value CRACK_ALPHA_4 = Lab.CLIENT.def("ruin.crack.alpha4", 1.0, 0.0, 1.0, 0.01);
   static final Lab.Value FLASH_TICKS = Lab.CLIENT.def("ruin.flash.ticks", 9.0, 2.0, 30.0, 1.0);
   static final Lab.Value GREY_STRENGTH = Lab.CLIENT.def("ruin.grey.strength", 0.88, 0.0, 1.0, 0.01);
   static final Lab.Value GREY_START = Lab.CLIENT.def("ruin.grey.start_stacks", 4.0, 1.0, 30.0, 1.0);
   static final Lab.Value GREY_FULL = Lab.CLIENT.def("ruin.grey.full_stacks", 24.0, 2.0, 80.0, 1.0);
   static final Lab.Value GREY_DARKEN = Lab.CLIENT.def("ruin.grey.darken", 0.22, 0.0, 0.6, 0.01);
   static final Lab.Value ASH_RATE = Lab.CLIENT.def("ruin.ash.rate", 7.0, 0.0, 40.0, 0.5);
   static final Lab.Value FLAKE_RATE = Lab.CLIENT.def("ruin.flake.rate", 4.0, 0.0, 30.0, 0.5);
   static final Lab.Value PAYOUT_CHUNKS = Lab.CLIENT.def("ruin.payout.chunks_per_stack", 0.8, 0.0, 4.0, 0.1);
   static final Lab.Value CRUMBLE_TICKS = Lab.CLIENT.def("ruin.crumble.ticks", 15.0, 4.0, 20.0, 1.0);
   static final Lab.Value CRUMBLE_ASH = Lab.CLIENT.def("ruin.crumble.ash", 7.0, 0.0, 30.0, 1.0);
   static final Lab.Value CRUMBLE_CHUNKS = Lab.CLIENT.def("ruin.crumble.chunks", 3.0, 0.0, 20.0, 1.0);
   static final Lab.Value CRUMBLE_INSIDE = Lab.CLIENT.def("ruin.crumble.inside", 0.32, 0.0, 1.0, 0.01);
   static final Lab.Value COUNTER_SCALE = Lab.CLIENT.def("ruin.counter.scale", 0.85, 0.3, 2.0, 0.05);
   static final Lab.Value COUNTER_NEAR = Lab.CLIENT.def("ruin.counter.near", 16.0, 2.0, 64.0, 1.0);
   static final Lab.Value COUNTER_FAR = Lab.CLIENT.def("ruin.counter.far", 40.0, 4.0, 128.0, 1.0);
   static final Lab.Value COUNTER_DIM_TICKS = Lab.CLIENT.def("ruin.counter.dim_ticks", 30.0, 0.0, 80.0, 1.0);
   static final Lab.Value COUNTER_DIM = Lab.CLIENT.def("ruin.counter.dim", 0.35, 0.0, 1.0, 0.05);
   static final float RISE_SHARE = 0.4F;
   static final float RISE_MIN = 0.6F;
   static final float FALL_SHARE = 0.14F;
   static final float FALL_MIN = 0.2F;
   static final long PULSE_STALE_TICKS = 40L;
   public static final int STAGES = 4;

   private RuinLook() {
   }

   public static float depthOf(float level) {
      if (level <= 0.0F) {
         return 0.0F;
      } else if (level < 1.0F) {
         return level / 1.0F;
      } else if (level < 5.0F) {
         return 1.0F + (level - 1.0F) / 4.0F;
      } else if (level < 10.0F) {
         return 2.0F + (level - 5.0F) / 5.0F;
      } else if (level < 20.0F) {
         return 3.0F + (level - 10.0F) / 10.0F;
      } else {
         float beyond = (float)(Math.log((double)(level / 20.0F)) / Math.log(2.0)) * 0.5F;
         return 4.0F + Math.min(1.0F, beyond);
      }
   }

   static float greyOf(float level, float start, float full) {
      if (full <= start) {
         return level >= full ? 1.0F : 0.0F;
      } else {
         return smooth(Mth.clamp((level - start) / (full - start), 0.0F, 1.0F));
      }
   }

   static float crackAlpha(int stage) {
      return switch (stage) {
         case 1 -> CRACK_ALPHA_1.getF();
         case 2 -> CRACK_ALPHA_2.getF();
         case 3 -> CRACK_ALPHA_3.getF();
         case 4 -> CRACK_ALPHA_4.getF();
         default -> 0.0F;
      };
   }

   static float smooth(float t) {
      return t * t * (3.0F - 2.0F * t);
   }

   static int hash(int a) {
      a = a ^ 61 ^ a >>> 16;
      a += a << 3;
      a ^= a >>> 4;
      a *= 668265261;
      return a ^ a >>> 15;
   }

   public static void preloadLabKeys() {
      CRACK_ALPHA_1.get();
   }

   public static record Look(int entityId, int stacks, float level, float depth, float grey, float flash, float crumble, boolean mine, float pop, float left) {
      public boolean crumbling() {
         return this.crumble >= 0.0F;
      }
   }
}
