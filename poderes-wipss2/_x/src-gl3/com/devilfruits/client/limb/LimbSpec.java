package com.devilfruits.client.limb;

import com.devilfruits.core.limb.LimbUv;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;

public final class LimbSpec {
   public LimbUv.Limb limb = LimbUv.Limb.ARM;
   public LimbUv.Side side = LimbUv.Side.RIGHT;
   public float tension;
   public float thinnest = 0.75F;
   public float fist = 1.0F;
   public float twistTurns;
   public float spin;
   public boolean overlay = true;
   public boolean replaces = true;
   public int color = -1;
   public boolean swarm;
   @Nullable
   public TexelBoxMesh.RingShaper shaper;
   public ResourceLocation swirl;
   public float swirlScroll;
   public int swirlColor = -1056964609;

   public LimbSpec arm(LimbUv.Side side) {
      this.limb = LimbUv.Limb.ARM;
      this.side = side;
      return this;
   }

   public LimbSpec leg(LimbUv.Side side) {
      this.limb = LimbUv.Limb.LEG;
      this.side = side;
      return this;
   }
}
