package com.devilfruits.fruits.supa_supa.client.vfx;

import com.devilfruits.client.vfx.Cinematic;
import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.core.config.CoreClientConfig;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class CutHitCinematic extends Cinematic {
   private static final int LIFE = 8;
   private final Entity target;
   private final Vec3 dir;
   private final Vec3 across;
   private final float size;
   private Vec3 at;

   public CutHitCinematic(@Nullable Entity target, Vec3 at, Vec3 dir, float damage, long seed) {
      super(new VfxAnchor(target != null ? target.getId() : -1, at), VfxParams.NONE);
      this.target = target;
      this.at = at;
      this.dir = dir.normalize();
      RandomSource random = RandomSource.create(seed ^ 23834L);
      Vec3 flat = new Vec3(dir.x, 0.0, dir.z);
      flat = flat.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
      Vec3 side = new Vec3(-flat.z, 0.0, flat.x);
      float tilt = (random.nextFloat() - 0.5F) * 1.6F;
      this.across = side.scale(Math.cos((double)tilt)).add(0.0, Math.sin((double)tilt), 0.0).normalize();
      this.size = Math.min(1.5F, 0.6F + damage * 0.04F) * (target != null ? Math.max(0.7F, target.getBbHeight() * 0.55F) : 1.0F);
      if (VfxSettings.tier() != CoreClientConfig.CinematicQuality.LOW) {
         VfxParticles.emit(SupaLook.GLINT, at.x, at.y, at.z, 0.0, 0.0, 0.0).size(0.9F + this.size * 0.4F);

         for (int i = 0; i < VfxSettings.count(6.0F); i++) {
            VfxParticles.emit(
               SupaLook.FILING,
               at.x,
               at.y,
               at.z,
               this.dir.x * 0.25 + (double)(random.nextFloat() - 0.5F) * 0.25,
               0.06 + (double)random.nextFloat() * 0.12,
               this.dir.z * 0.25 + (double)(random.nextFloat() - 0.5F) * 0.25
            );
         }

         for (int i = 0; i < VfxSettings.count(2.0F); i++) {
            VfxParticles.emit(
               SupaLook.SPARK,
               at.x,
               at.y,
               at.z,
               this.dir.x * 0.3 + (double)(random.nextFloat() - 0.5F) * 0.2,
               0.1 + (double)random.nextFloat() * 0.1,
               this.dir.z * 0.3 + (double)(random.nextFloat() - 0.5F) * 0.2
            );
         }
      }
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (this.target != null && !this.target.isRemoved()) {
         this.at = this.target.getBoundingBox().getCenter();
      }

      if (this.age > 8) {
         this.finish();
      }
   }

   @Override
   public void render(VfxFrame f) {
      float t = (float)this.age + f.partial;
      float fade = Math.max(0.0F, 1.0F - t / 8.0F);
      float grow = Math.min(1.0F, t / 1.2F);
      Vec3 a = this.at.add(this.across.scale((double)(-this.size * grow))).add(this.dir.scale(-0.1));
      Vec3 b = this.at.add(this.across.scale((double)(this.size * grow))).add(this.dir.scale(0.1));
      Cuts.line(f, a.x, a.y, a.z, b.x, b.y, b.z, 0.4F, 0.6F + 0.5F * fade, fade, f.time * 0.6F);
   }

   @Override
   public double x() {
      return this.at.x;
   }

   @Override
   public double y() {
      return this.at.y;
   }

   @Override
   public double z() {
      return this.at.z;
   }
}
