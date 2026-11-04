package com.devilfruits.mobs.pirate;

public final class PirateClips {
   public static final float WALK_SPEED = 2.2F;
   public static final float RUN_SPEED = 4.6F;
   public static final float BACKPEDAL_SPEED = 1.6F;
   public static final float STRAFE_SPEED = 1.8F;
   public static final float CAPTAIN_WALK_SPEED = 2.0F;
   public static final float CAPTAIN_RUN_SPEED = 4.0F;
   public static final int WALK_LENGTH = 16;
   public static final int RUN_LENGTH = 12;
   public static final int BACKPEDAL_LENGTH = 18;
   public static final int STRAFE_LENGTH = 16;
   public static final int CAPTAIN_WALK_LENGTH = 20;
   public static final int CAPTAIN_RUN_LENGTH = 14;
   public static final int DRAW = 0;
   public static final int SLASH_1 = 1;
   public static final int SLASH_2 = 2;
   public static final int BARGE = 3;
   public static final int TAUNT = 4;
   public static final int FIRE_PISTOL = 5;
   public static final int RELOAD_PISTOL = 6;
   public static final int FIRE_MUSKET = 7;
   public static final int RELOAD_MUSKET = 8;
   public static final int FLINCH_RETREAT = 9;
   public static final int CAPTAIN_SLASH = 10;
   public static final int SLAM = 11;
   public static final int LUNGE = 12;
   public static final int QUICKDRAW = 13;
   public static final int RALLY = 14;
   public static final int ENRAGE = 15;
   public static final String[] NAMES = new String[]{
      "draw",
      "slash_1",
      "slash_2",
      "barge",
      "taunt",
      "fire_pistol",
      "reload_pistol",
      "fire_musket",
      "reload_musket",
      "flinch_retreat",
      "captain_slash",
      "slam",
      "lunge",
      "quickdraw",
      "rally",
      "enrage"
   };
   public static final int[] LENGTH = new int[]{10, 12, 12, 16, 30, 12, 100, 16, 160, 12, 18, 40, 20, 16, 32, 30};
   public static final int[] HIT = new int[]{5, 6, 5, 8, -1, 2, 92, 3, 148, -1, 9, 26, 11, 8, 12, 14};
   public static final int[] MOVE = new int[]{-1, -1, -1, 4, -1, -1, -1, -1, -1, 3, -1, -1, 6, -1, -1, -1};
   public static final int[] LEFT_FROM = new int[]{-1, -1, -1, -1, -1, -1, 10, -1, 12, -1, -1, -1, -1, -1, -1, -1};
   public static final int[] LEFT_TO = new int[]{-1, -1, -1, -1, -1, -1, 86, -1, 136, -1, -1, -1, -1, -1, -1, -1};
   public static final int[] HALF_COCK = new int[]{-1, -1, -1, -1, -1, -1, 14, -1, 18, -1, -1, -1, -1, -1, -1, -1};
   public static final int[] BITE = new int[]{-1, -1, -1, -1, -1, -1, 30, -1, 38, -1, -1, -1, -1, -1, -1, -1};
   public static final int[] PRIME = new int[]{-1, -1, -1, -1, -1, -1, 34, -1, 44, -1, -1, -1, -1, -1, -1, -1};
   public static final int[] PAN_SHUT = new int[]{-1, -1, -1, -1, -1, -1, 38, -1, 50, -1, -1, -1, -1, -1, -1, -1};
   public static final int[] POUR = new int[]{-1, -1, -1, -1, -1, -1, 46, -1, 66, -1, -1, -1, -1, -1, -1, -1};
   public static final int[] ROD_OUT = new int[]{-1, -1, -1, -1, -1, -1, 54, -1, 78, -1, -1, -1, -1, -1, -1, -1};
   public static final int[] RAM_1 = new int[]{-1, -1, -1, -1, -1, -1, 64, -1, 98, -1, -1, -1, -1, -1, -1, -1};
   public static final int[] RAM_2 = new int[]{-1, -1, -1, -1, -1, -1, 72, -1, 110, -1, -1, -1, -1, -1, -1, -1};
   public static final int[] ROD_IN = new int[]{-1, -1, -1, -1, -1, -1, 80, -1, 126, -1, -1, -1, -1, -1, -1, -1};

   public static float reloadState(int action, float tick) {
      if (action < 0 || action >= NAMES.length || HALF_COCK[action] < 0 || tick < (float)HALF_COCK[action] || tick >= (float)HIT[action]) {
         return 0.0F;
      } else if (tick < (float)PAN_SHUT[action]) {
         return 0.25F;
      } else {
         return tick >= (float)ROD_OUT[action] && tick < (float)ROD_IN[action] ? 0.75F : 0.5F;
      }
   }

   public static int byName(String name) {
      for (int i = 0; i < NAMES.length; i++) {
         if (NAMES[i].equals(name)) {
            return i;
         }
      }

      return -1;
   }

   private PirateClips() {
   }
}
