package com.devilfruits.fruits.supa_supa.client.vfx;

import com.devilfruits.client.vfx.Cinematic;
import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.VfxSound;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.render.MeshWriter;
import com.devilfruits.client.vfx.sprite.VfxSprites;
import com.devilfruits.core.config.CoreClientConfig;
import com.devilfruits.fruits.supa_supa.BladeLimbs;
import com.devilfruits.fruits.supa_supa.SupaSounds;
import com.mojang.blaze3d.vertex.BufferBuilder;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class SpiralCinematic extends Cinematic {
   private final Player user;
   private final int maxTicks;
   private final RandomSource random;
   private VfxSound whirr;
   private float spin;

   public SpiralCinematic(Player user, int maxTicks, long seed) {
      super(VfxAnchor.of(user, user.position()), VfxParams.NONE);
      this.user = user;
      this.maxTicks = maxTicks;
      this.random = RandomSource.create(seed);
   }

   private boolean spinning() {
      return !this.user.isRemoved() && BladeLimbs.of(this.user).mode() == BladeLimbs.ArmMode.SAW;
   }

   private Vec3 front(float partial) {
      Vec3 eye = this.user.getEyePosition(partial);
      return eye.add(0.0, -0.4, 0.0);
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      boolean on = this.spinning();
      this.spin = Mth.clamp(this.spin + (on ? 0.12F : -0.2F), 0.0F, 1.0F);
      if ((on || !(this.spin <= 0.0F)) && this.age <= this.maxTicks + 40) {
         if (this.whirr == null && on) {
            this.whirr = VfxSound.play(
               SupaSounds.SPIRAL_SPIN.get(),
               this.user,
               this.user.position(),
               true,
               ax -> 0.25F + 0.55F * this.spin,
               ax -> 0.7F + 0.6F * this.spin,
               this.maxTicks + 40,
               () -> this.spin > 0.02F && !this.user.isRemoved()
            );
         }

         if (VfxSettings.tier() != CoreClientConfig.CinematicQuality.LOW && !(this.spin < 0.3F)) {
            Vec3 look = this.user.getLookAngle();
            Vec3 side = look.cross(new Vec3(0.0, 1.0, 0.0));
            side = side.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : side.normalize();
            Vec3 up = side.cross(look).normalize();
            Vec3 c = this.front(1.0F);

            for (int i = 0; i < VfxSettings.count(4.0F * this.spin); i++) {
               double a = (double)this.age * 0.9 + this.random.nextDouble() * Math.PI * 2.0;
               double r = 0.35 + this.random.nextDouble() * 0.25;
               Vec3 ring = side.scale(Math.cos(a) * r).add(up.scale(Math.sin(a) * r));
               Vec3 tangent = side.scale(-Math.sin(a)).add(up.scale(Math.cos(a)));
               Vec3 p = c.add(look.scale(0.3 + this.random.nextDouble() * 0.6)).add(ring);
               Vec3 v = tangent.scale(0.35).add(look.scale(0.25));
               VfxParticles.emit(SupaLook.AIR_SLIT, p.x, p.y, p.z, v.x, v.y, v.z).alpha(0.6F * this.spin);
            }
         }
      } else {
         this.finish();
      }
   }

   @Override
   public void render(VfxFrame f) {
      BufferBuilder d = f.distortion();
      if (d != null && !(this.spin < 0.2F)) {
         Vec3 look = this.user.getViewVector(f.entityPartial);
         Vec3 c = this.front(f.entityPartial);
         VfxSprites.Sprite glow = VfxSprites.GLOW;
         Vec3 side = look.cross(new Vec3(0.0, 1.0, 0.0));
         side = side.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : side.normalize();
         Vec3 up = side.cross(look).normalize();

         for (int i = 0; i < 10; i++) {
            float t = (float)i / 9.0F;
            double along = 0.4 + (double)t * 2.2;
            double r = 0.55 * (double)(1.0F - t) + 0.08;
            double a = (double)f.time * 0.8 + (double)t * 5.0 + (double)i * 0.7;
            Vec3 p = c.add(look.scale(along)).add(side.scale(Math.cos(a) * r)).add(up.scale(Math.sin(a) * r));
            MeshWriter.distortQuad(
               d,
               f.rx(p.x),
               f.ry(p.y),
               f.rz(p.z),
               (float)(0.55 - 0.3 * (double)t),
               0.75F * this.spin * (1.0F - t * 0.6F),
               1,
               false,
               f.left.x,
               f.left.y,
               f.left.z,
               f.up.x,
               f.up.y,
               f.up.z,
               glow.u0(),
               glow.v0(),
               glow.u1(),
               glow.v1(),
               f.time * 0.06F + (float)i * 0.21F,
               0.7F
            );
         }
      }
   }

   @Override
   public double x() {
      return this.user.getX();
   }

   @Override
   public double y() {
      return this.user.getY();
   }

   @Override
   public double z() {
      return this.user.getZ();
   }

   @Nullable
   public static SpiralCinematic of(@Nullable Entity e, int maxTicks, long seed) {
      return e instanceof Player p ? new SpiralCinematic(p, maxTicks, seed) : null;
   }
}
