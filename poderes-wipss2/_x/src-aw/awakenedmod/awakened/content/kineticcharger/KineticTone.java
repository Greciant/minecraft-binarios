package com.awakenedmod.awakened.content.kineticcharger;

import com.awakenedmod.awakened.content.vfx.VfxEntity;
import com.awakenedmod.awakened.content.vfx.VoxelSprites;

public final class KineticTone {
   public static final int THEME = 13073919;
   public static final int BODY = 8072383;
   public static final int MID = 13073919;
   public static final int TIP = 15390463;
   public static final int CORE = 16182527;
   public static final int AIR_BODY = 16053496;
   public static final int AIR_GLOW = 16777215;
   public static final float HUE_SPREAD = 12.0F;
   public static final VoxelSprites.Look LOOK = new VoxelSprites.Look(
      VfxEntity.Kind.KINETIC_VOXEL_SPRITE, VfxEntity.Kind.KINETIC_VOXEL_INRUSH, 8072383, 13073919
   );

   private KineticTone() {
   }
}
