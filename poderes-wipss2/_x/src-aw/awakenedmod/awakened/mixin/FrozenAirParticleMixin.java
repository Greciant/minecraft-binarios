package com.awakenedmod.awakened.mixin;

import com.awakenedmod.awakened.client.FrozenAirParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin({Particle.class})
public abstract class FrozenAirParticleMixin implements FrozenAirParticle {
   @Shadow
   protected double xo;
   @Shadow
   protected double yo;
   @Shadow
   protected double zo;
   @Shadow
   protected double x;
   @Shadow
   protected double y;
   @Shadow
   protected double z;
   @Shadow
   protected boolean removed;
   @Unique
   private int awakened$divisor;
   @Unique
   private int awakened$phase;
   @Unique
   private double awakened$fromX;
   @Unique
   private double awakened$fromY;
   @Unique
   private double awakened$fromZ;
   @Unique
   private double awakened$toX;
   @Unique
   private double awakened$toY;
   @Unique
   private double awakened$toZ;

   @Override
   public boolean awakened$beginTick(int divisor) {
      if (divisor < 2) {
         if (this.awakened$divisor != 0) {
            this.awakened$restoreTrue();
            this.awakened$divisor = 0;
         }

         return false;
      } else if (this.awakened$divisor != divisor) {
         if (this.awakened$divisor != 0) {
            this.awakened$restoreTrue();
         }

         this.awakened$divisor = divisor;
         this.awakened$phase = 0;
         return false;
      } else {
         this.awakened$phase++;
         if (this.awakened$phase >= divisor) {
            this.awakened$phase = 0;
            this.awakened$restoreTrue();
            return false;
         } else {
            float f0 = (float)this.awakened$phase / (float)divisor;
            float f1 = (float)(this.awakened$phase + 1) / (float)divisor;
            this.xo = Mth.lerp((double)f0, this.awakened$fromX, this.awakened$toX);
            this.yo = Mth.lerp((double)f0, this.awakened$fromY, this.awakened$toY);
            this.zo = Mth.lerp((double)f0, this.awakened$fromZ, this.awakened$toZ);
            this.x = Mth.lerp((double)f1, this.awakened$fromX, this.awakened$toX);
            this.y = Mth.lerp((double)f1, this.awakened$fromY, this.awakened$toY);
            this.z = Mth.lerp((double)f1, this.awakened$fromZ, this.awakened$toZ);
            return true;
         }
      }
   }

   @Override
   public void awakened$endTick() {
      if (this.awakened$divisor >= 2 && !this.removed) {
         this.awakened$fromX = this.xo;
         this.awakened$fromY = this.yo;
         this.awakened$fromZ = this.zo;
         this.awakened$toX = this.x;
         this.awakened$toY = this.y;
         this.awakened$toZ = this.z;
         float f1 = 1.0F / (float)this.awakened$divisor;
         this.x = Mth.lerp((double)f1, this.awakened$fromX, this.awakened$toX);
         this.y = Mth.lerp((double)f1, this.awakened$fromY, this.awakened$toY);
         this.z = Mth.lerp((double)f1, this.awakened$fromZ, this.awakened$toZ);
      }
   }

   @Override
   public double awakened$trueX() {
      return this.awakened$divisor != 0 ? this.awakened$toX : this.x;
   }

   @Override
   public double awakened$trueY() {
      return this.awakened$divisor != 0 ? this.awakened$toY : this.y;
   }

   @Override
   public double awakened$trueZ() {
      return this.awakened$divisor != 0 ? this.awakened$toZ : this.z;
   }

   @Unique
   private void awakened$restoreTrue() {
      this.x = this.awakened$toX;
      this.y = this.awakened$toY;
      this.z = this.awakened$toZ;
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
   }
}
