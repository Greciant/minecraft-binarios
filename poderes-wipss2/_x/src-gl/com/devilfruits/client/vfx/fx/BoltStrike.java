package com.devilfruits.client.vfx.fx;

import com.devilfruits.client.vfx.Cinematic;
import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.particle.VfxPresets;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

public class BoltStrike extends Cinematic {
   public static final int GLOW = 11955455;
   public static final int MAX_LIVE = 24;
   private static int live;
   private boolean counted;
   private final Bolt bolt = new Bolt();
   private final Vec3 from;
   private final Vec3 to;
   private final long seed;
   private float width = 0.28F;
   private int life = 6;
   private int forks = 3;
   private float rough = 0.13F;
   private int rejitter = 2;
   private float heat = 1.1F;
   private float endGlow = 1.0F;

   private BoltStrike(Vec3 from, Vec3 to, long seed) {
      super(new VfxAnchor(-1, to), VfxParams.NONE);
      this.from = from;
      this.to = to;
      this.seed = seed;
   }

   public static BoltStrike between(Vec3 from, Vec3 to, long seed) {
      return new BoltStrike(from, to, seed);
   }

   public BoltStrike width(float w) {
      this.width = w;
      return this;
   }

   public BoltStrike life(int ticks) {
      this.life = Math.max(1, ticks);
      return this;
   }

   public BoltStrike forks(int n) {
      this.forks = n;
      return this;
   }

   public BoltStrike rough(float r) {
      this.rough = r;
      return this;
   }

   public BoltStrike rejitter(int ticks) {
      this.rejitter = ticks;
      return this;
   }

   public BoltStrike heat(float h) {
      this.heat = h;
      return this;
   }

   public BoltStrike endGlow(float scale) {
      this.endGlow = scale;
      return this;
   }

   public BoltStrike spawn() {
      if (live >= 24) {
         return this;
      } else {
         live++;
         this.counted = true;
         this.bolt.build(this.from.x, this.from.y, this.from.z, this.to.x, this.to.y, this.to.z, this.rough, this.forks, this.seed);
         if (this.endGlow > 0.0F) {
            VfxParticles.emit(VfxPresets.GLOW, this.to.x, this.to.y, this.to.z, 0.0, 0.0, 0.0)
               .size(this.width * 14.0F * this.endGlow)
               .tint(11955455)
               .alpha(0.8F)
               .life((float)this.life / 5.0F);
         }

         return VfxDirector.spawn(this);
      }
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (this.age >= this.life) {
         this.finish();
      } else {
         if (this.rejitter > 0 && this.age % this.rejitter == 0) {
            this.bolt.build(this.from.x, this.from.y, this.from.z, this.to.x, this.to.y, this.to.z, this.rough, this.forks, this.seed + (long)this.age * 7919L);
         }
      }
   }

   @Override
   public void render(VfxFrame f) {
      float t = ((float)this.age + f.partial) / (float)this.life;
      float flicker = (this.age & 1) == 0 ? 1.0F : 0.7F;
      float alpha = t < 0.25F ? 1.0F : (1.0F - t) / 0.75F * flicker;
      this.bolt.render(f, this.width * (t < 0.25F ? 1.0F : 0.75F + 0.25F * (1.0F - t)), alpha, this.heat);
   }

   @Override
   public void removed() {
      if (this.counted) {
         this.counted = false;
         live--;
      }
   }

   @Override
   public double x() {
      return (this.from.x + this.to.x) * 0.5;
   }

   @Override
   public double y() {
      return (this.from.y + this.to.y) * 0.5;
   }

   @Override
   public double z() {
      return (this.from.z + this.to.z) * 0.5;
   }

   @Override
   public double cullDistance() {
      return 96.0 + (double)this.bolt.length() * 0.5;
   }
}
