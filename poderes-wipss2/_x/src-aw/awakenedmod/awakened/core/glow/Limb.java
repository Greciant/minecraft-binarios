package com.awakenedmod.awakened.core.glow;

public enum Limb {
   HEAD,
   BODY,
   LEFT_ARM,
   RIGHT_ARM,
   LEFT_LEG,
   RIGHT_LEG,
   ALL;

   public static final Limb[] REAL = new Limb[]{HEAD, BODY, LEFT_ARM, RIGHT_ARM, LEFT_LEG, RIGHT_LEG};

   public int mask() {
      return this == ALL ? 63 : 1 << this.ordinal();
   }

   public boolean isArm() {
      return this == LEFT_ARM || this == RIGHT_ARM;
   }
}
