package com.devilfruits.fruits.horo_horo.client.vfx;

import com.devilfruits.client.vfx.Cinematic;
import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.particle.ParticleStyle;
import com.devilfruits.client.vfx.particle.Ribbon;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.render.MeshTex;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public abstract class HoroCinematic extends Cinematic implements HoroRender.Drawable {
   protected final RandomSource random;

   protected HoroCinematic(VfxAnchor anchor, long seed) {
      super(anchor, VfxParams.NONE);
      this.random = RandomSource.create(seed);
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (this.age == 1) {
         HoroRender.add(this);
      }
   }

   @Override
   public void removed() {
      HoroRender.remove(this);
   }

   protected static void squareUp(@Nullable Entity user) {
      if (user instanceof LivingEntity living) {
         living.yBodyRot = living.getYHeadRot();
         living.yBodyRotO = living.yBodyRot;
      }
   }

   protected double jit(double a) {
      return (this.random.nextDouble() * 2.0 - 1.0) * a;
   }

   @Nullable
   protected static Ribbon mistRibbon(int points, float halfWidth, float life, int rgb) {
      Ribbon r = Ribbon.create(points);
      if (r != null) {
         r.look(halfWidth, life, -1.0F, -1.0F, 0.7F, 0).texture(MeshTex.HORO_MIST, 0.05F).tint(rgb);
      }

      return r;
   }

   private static ParticleOptions dust(int rgb, float size) {
      return new DustParticleOptions(new Vector3f((float)(rgb >> 16 & 0xFF) / 255.0F, (float)(rgb >> 8 & 0xFF) / 255.0F, (float)(rgb & 0xFF) / 255.0F), size);
   }

   protected void emit(ClientLevel level, ParticleStyle style, Vec3 at, Vec3 vel, int rgb, float size) {
      if (VfxSettings.legacy()) {
         level.addParticle(dust(rgb, Math.min(2.0F, 0.6F + size)), at.x, at.y, at.z, vel.x, vel.y, vel.z);
      } else {
         VfxParticles.emit(style, at.x, at.y, at.z, vel.x, vel.y, vel.z).tint(rgb).size(size).unflipped();
      }
   }

   protected void puffs(ClientLevel level, ParticleStyle style, Vec3 at, int n, double speed, float size, int... colours) {
      int count = VfxSettings.legacy() ? n : VfxSettings.count((float)n);

      for (int i = 0; i < count; i++) {
         Vec3 v = new Vec3(this.jit(1.0), this.jit(0.7) + 0.2, this.jit(1.0)).normalize().scale(speed * (0.5 + this.random.nextDouble() * 0.5));
         this.emit(
            level,
            style,
            at.add(this.jit(0.15), this.jit(0.15), this.jit(0.15)),
            v,
            colours[i % colours.length],
            size * (0.75F + this.random.nextFloat() * 0.5F)
         );
      }
   }

   protected void sparkles(ClientLevel level, Vec3 at, int n, double spread, double speed, int... colours) {
      int count = VfxSettings.legacy() ? Math.max(1, n / 2) : VfxSettings.count((float)n);

      for (int i = 0; i < count; i++) {
         ParticleStyle s = HoroLook.SPARKLES[this.random.nextInt(HoroLook.SPARKLES.length)];
         Vec3 p = at.add(this.jit(spread), this.jit(spread), this.jit(spread));
         Vec3 v = new Vec3(this.jit(speed), this.jit(speed) + speed * 0.3, this.jit(speed));
         int rgb = colours[i % colours.length];
         if (VfxSettings.legacy()) {
            level.addParticle(dust(rgb, 0.8F), p.x, p.y, p.z, v.x, v.y, v.z);
         } else {
            VfxParticles.emit(s, p.x, p.y, p.z, v.x, v.y, v.z)
               .tint(rgb)
               .size(0.6F + this.random.nextFloat() * 0.8F)
               .rotation(this.random.nextFloat() < 0.3F ? 0.25F : 0.0F);
         }
      }
   }

   protected static void flash(Vec3 at, float size, int rgb, float alpha) {
      if (!VfxSettings.legacy() && VfxSettings.hitFlashes()) {
         VfxParticles.emit(HoroLook.FLASH, at.x, at.y, at.z, 0.0, 0.0, 0.0).size(size).tint(rgb).alpha(alpha * VfxSettings.flashCap());
      }
   }

   protected static void halo(Vec3 at, float size, int rgb, float alpha) {
      if (!VfxSettings.legacy()) {
         VfxParticles.emit(HoroLook.HALO, at.x, at.y, at.z, 0.0, 0.0, 0.0).size(size).tint(rgb).alpha(alpha);
      }
   }

   protected void giggle(ClientLevel level, Vec3 at) {
      if (!VfxSettings.legacy()) {
         VfxParticles.emit(HoroLook.GIGGLE, at.x, at.y, at.z, this.jit(0.02), 0.025 + this.random.nextDouble() * 0.02, this.jit(0.02))
            .size(0.8F + this.random.nextFloat() * 0.5F)
            .unflipped();
      }
   }
}
