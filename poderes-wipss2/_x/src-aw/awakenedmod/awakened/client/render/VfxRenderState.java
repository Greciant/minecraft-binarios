package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.vfx.VfxEntity;
import net.minecraft.world.phys.Vec3;

public class VfxRenderState {
   public float ageInTicks;
   public int lightCoords = 15728880;
   public VfxEntity.Kind kind = VfxEntity.Kind.SPHERE;
   public float scale = 1.0F;
   public Vec3 secondary = Vec3.ZERO;
   public int lifetime = 20;
   public float intensity = 1.0F;
   public int color = 16762170;
   public int color2 = -1;
   public float yaw;
   public float pitch;
   public int seed;
   public Vec3 worldPos = Vec3.ZERO;
   public int ownerId = -1;
   public float ownerDim = 1.0F;
   public boolean noDepth = false;
   public int delay = 0;
}
