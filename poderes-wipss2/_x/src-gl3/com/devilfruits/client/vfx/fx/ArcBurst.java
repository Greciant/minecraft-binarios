package com.devilfruits.client.vfx.fx;

import com.devilfruits.client.vfx.Cinematic;
import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.render.MeshTex;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class ArcBurst extends Cinematic {
   private static final int MAX_ARCS = 6;
   private double cx;
   private double cy;
   private double cz;
   private int followId = -1;
   private float followLift = 0.5F;
   private int arcs = 3;
   private float radius0 = 1.0F;
   private float radius1 = 1.0F;
   private int life = 12;
   private float spin = 0.6F;
   private float arc = 4.08407F;
   private float width = 0.3F;
   private float heat = 1.1F;
   private float alpha = 1.0F;
   private int palette = 0;
   private float emissive = 1.6F;
   private int rgb = 16777215;
   private MeshTex texture = MeshTex.FLAME_RIBBON;
   private float tiltMin = 0.3F;
   private float tiltMax = 1.3F;
   private float fadeIn = 0.15F;
   private int points = 36;
   private final long seed;
   private final float[] tilt = new float[6];
   private final float[] azim = new float[6];
   private final float[] phase = new float[6];
   private final float[] speed = new float[6];
   private final float[] axes = new float[6];

   private ArcBurst(double x, double y, double z, long seed) {
      super(new VfxAnchor(-1, new Vec3(x, y, z)), VfxParams.NONE);
      this.cx = x;
      this.cy = y;
      this.cz = z;
      this.seed = seed;
   }

   public static ArcBurst at(Vec3 c, long seed) {
      return new ArcBurst(c.x, c.y, c.z, seed);
   }

   public ArcBurst follow(@Nullable Entity e, float lift) {
      this.followId = e != null ? e.getId() : -1;
      this.followLift = lift;
      return this;
   }

   public ArcBurst arcs(int n) {
      this.arcs = Mth.clamp(n, 1, 6);
      return this;
   }

   public ArcBurst radius(float start, float end) {
      this.radius0 = start;
      this.radius1 = end;
      return this;
   }

   public ArcBurst life(int ticks) {
      this.life = Math.max(2, ticks);
      return this;
   }

   public ArcBurst spin(float radPerTick) {
      this.spin = radPerTick;
      return this;
   }

   public ArcBurst arc(float radians) {
      this.arc = radians;
      return this;
   }

   public ArcBurst look(MeshTex tex, int palette, float width, float heat, float emissive) {
      this.texture = tex;
      this.palette = palette;
      this.width = width;
      this.heat = heat;
      this.emissive = emissive;
      return this;
   }

   public ArcBurst colour(MeshTex tex, int rgb, float width) {
      this.texture = tex;
      this.rgb = rgb;
      this.width = width;
      this.heat = -1.0F;
      this.emissive = 0.4F;
      return this;
   }

   public ArcBurst alpha(float a) {
      this.alpha = a;
      return this;
   }

   public ArcBurst tilt(float min, float max) {
      this.tiltMin = min;
      this.tiltMax = max;
      return this;
   }

   public ArcBurst fadeIn(float fraction) {
      this.fadeIn = fraction;
      return this;
   }

   public ArcBurst points(int n) {
      this.points = n;
      return this;
   }

   public ArcBurst spawn() {
      RandomSource r = RandomSource.create(this.seed);

      for (int i = 0; i < this.arcs; i++) {
         this.tilt[i] = this.tiltMin + (this.tiltMax - this.tiltMin) * r.nextFloat();
         this.azim[i] = (float)i * (float) (Math.PI * 2) / (float)this.arcs + r.nextFloat() * 0.9F;
         this.phase[i] = r.nextFloat() * (float) (Math.PI * 2);
         this.speed[i] = 0.8F + 0.4F * r.nextFloat();
      }

      return VfxDirector.spawn(this);
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (this.followId >= 0) {
         Entity e = level.getEntity(this.followId);
         if (e != null) {
            this.cx = e.getX();
            this.cy = e.getY() + (double)(e.getBbHeight() * this.followLift);
            this.cz = e.getZ();
         }
      }

      if (this.age >= this.life) {
         this.finish();
      }
   }

   @Override
   public void render(VfxFrame f) {
      float t = Mth.clamp(((float)this.age + f.partial) / (float)this.life, 0.0F, 1.0F);
      float ease = 1.0F - (1.0F - t) * (1.0F - t);
      float r = this.radius0 + (this.radius1 - this.radius0) * ease;
      float a = this.alpha * (t < this.fadeIn ? t / this.fadeIn : 1.0F - (t - this.fadeIn) / (1.0F - this.fadeIn) * (t - this.fadeIn) / (1.0F - this.fadeIn));
      double x = this.cx;
      double y = this.cy;
      double z = this.cz;
      if (this.followId >= 0 && Minecraft.getInstance().level != null) {
         Entity e = Minecraft.getInstance().level.getEntity(this.followId);
         if (e != null) {
            x = Mth.lerp((double)f.entityPartial, e.xOld, e.getX());
            y = Mth.lerp((double)f.entityPartial, e.yOld, e.getY()) + (double)(e.getBbHeight() * this.followLift);
            z = Mth.lerp((double)f.entityPartial, e.zOld, e.getZ());
         }
      }

      float time = (float)this.age + f.partial;

      for (int i = 0; i < this.arcs; i++) {
         ArcStrip.plane(this.tilt[i], this.azim[i], this.axes);
         float head = this.phase[i] + this.spin * this.speed[i] * time;
         ArcStrip.draw(
            f,
            this.texture,
            f.rx(x),
            f.ry(y),
            f.rz(z),
            this.axes[0],
            this.axes[1],
            this.axes[2],
            this.axes[3],
            this.axes[4],
            this.axes[5],
            r,
            head,
            Math.signum(this.spin) * this.arc,
            this.width,
            this.heat,
            a,
            this.palette,
            this.emissive,
            this.rgb,
            -time * 0.12F + (float)i * 0.37F,
            this.points
         );
      }
   }

   @Override
   public double x() {
      return this.cx;
   }

   @Override
   public double y() {
      return this.cy;
   }

   @Override
   public double z() {
      return this.cz;
   }
}
