package com.devilfruits.fruits.supa_supa.client.vfx;

import com.devilfruits.client.vfx.Cinematic;
import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.camera.CameraDirector;
import com.devilfruits.client.vfx.fx.VfxWorld;
import com.devilfruits.client.vfx.particle.VfxParticles;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class LungeCinematic extends Cinematic {
   private static final int LIFE = 9;
   private final Vec3 from;
   private final Vec3 dir;
   private final Vec3 side;
   private final float reach;
   private final float speed;
   private final RandomSource random;

   public LungeCinematic(@Nullable Entity user, Vec3 eye, Vec3 dir, float reach, float speed, long seed) {
      super(new VfxAnchor(user != null ? user.getId() : -1, eye), VfxParams.NONE);
      this.from = eye.add(0.0, -0.45, 0.0);
      this.dir = dir.normalize();
      Vec3 flat = new Vec3(dir.x, 0.0, dir.z);
      flat = flat.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
      this.side = new Vec3(-flat.z, 0.0, flat.x);
      this.reach = reach;
      this.speed = speed;
      this.random = RandomSource.create(seed);

      for (int i = 0; i < VfxSettings.count(8.0F + speed * 10.0F); i++) {
         double t = this.random.nextDouble();
         Vec3 p = this.from
            .add(this.dir.scale(t * (double)reach))
            .add(this.side.scale((this.random.nextDouble() - 0.5) * 0.9))
            .add(0.0, (this.random.nextDouble() - 0.5) * 0.6, 0.0);
         VfxParticles.emit(SupaLook.AIR_SLIT, p.x, p.y, p.z, -this.dir.x * 0.4, -this.dir.y * 0.4, -this.dir.z * 0.4).alpha(0.6F);
      }

      if (user != null && VfxWorld.local(user.getId())) {
         CameraDirector.fovPunch(0.03F + speed * 0.04F);
      }
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (this.age > 9) {
         this.finish();
      }
   }

   @Override
   public void render(VfxFrame f) {
      float t = (float)this.age + f.partial;
      float fade = Math.max(0.0F, 1.0F - t / 9.0F);
      float open = Math.min(1.0F, t / 1.5F);
      float half = (0.6F + this.speed * 0.9F) * open;
      Vec3 c = this.from.add(this.dir.scale((double)this.reach * 0.85));
      Vec3 a = c.add(this.side.scale((double)(-half))).add(0.0, 0.12, 0.0);
      Vec3 b = c.add(this.side.scale((double)half)).add(0.0, -0.12, 0.0);
      Cuts.line(f, a.x, a.y, a.z, b.x, b.y, b.z, 0.34F, 0.6F + 0.5F * fade, fade, f.time * 0.5F);
   }

   @Override
   public double x() {
      return this.from.x;
   }

   @Override
   public double y() {
      return this.from.y;
   }

   @Override
   public double z() {
      return this.from.z;
   }
}
