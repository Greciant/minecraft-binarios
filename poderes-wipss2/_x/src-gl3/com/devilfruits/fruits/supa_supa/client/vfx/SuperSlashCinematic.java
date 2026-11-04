package com.devilfruits.fruits.supa_supa.client.vfx;

import com.devilfruits.client.vfx.Cinematic;
import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.camera.CameraDirector;
import com.devilfruits.client.vfx.fx.ShockRing;
import com.devilfruits.client.vfx.fx.VfxWorld;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.post.VfxPost;
import com.devilfruits.client.vfx.render.MeshWriter;
import com.devilfruits.client.vfx.sprite.VfxSprites;
import com.devilfruits.core.config.CoreClientConfig;
import com.devilfruits.core.physics.PhysicsWorld;
import com.devilfruits.core.physics.PhysicsWorldBridge;
import com.devilfruits.fruits.supa_supa.slash.SlashPlane;
import com.devilfruits.fruits.supa_supa.slash.SuperSlash;
import com.mojang.blaze3d.vertex.BufferBuilder;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class SuperSlashCinematic extends Cinematic {
   private static final int FADE = 10;
   private static final int TURN = 8;
   private static final int SETTLE = 3;
   private static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);
   private Vec3 velocity;
   private Vec3 dir;
   private Vec3 side;
   private final int height;
   private final int lifetime;
   private final long seed;
   private final RandomSource random;
   private Vec3 base;
   private Vec3 prev;
   private int ended = -1;
   private boolean flashed;
   private SuperSlashCinematic.Mode mode = SuperSlashCinematic.Mode.FLY;
   private Vec3 up = UP;
   private Vec3 prevUp = UP;
   private Vec3 flightUp = UP;
   private final Vec3 flat;
   private Vec3 entryUp = UP;
   private Vec3 glideSpan = UP;
   @Nullable
   private ClientLevel glideLevel;
   private Vec3 glideStart = Vec3.ZERO;
   private Vec3 glideVel = Vec3.ZERO;
   private Vec3 entryBase = Vec3.ZERO;
   private int glideTicks;
   private int glideAge;
   private double glideSpeed;
   private double exitLeft;
   private int exitAge;
   private Vec3 exitUp = UP;

   public SuperSlashCinematic(@Nullable Entity user, Vec3 base, Vec3 velocity, int height, int lifetime, long seed) {
      super(new VfxAnchor(user != null ? user.getId() : -1, base), VfxParams.NONE);
      this.base = base;
      this.prev = base;
      this.velocity = velocity;
      this.dir = velocity.lengthSqr() < 1.0E-8 ? new Vec3(0.0, 0.0, 1.0) : velocity.normalize();
      Vec3 look = user != null ? user.getLookAngle() : this.dir;
      Vec3 f = new Vec3(look.x, 0.0, look.z);
      this.flat = f.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : f.normalize();
      this.flightUp = SuperSlash.bladeUp(this.dir, this.flat);
      this.up = this.flightUp;
      this.prevUp = this.flightUp;
      this.side = this.dir.cross(this.flightUp).normalize();
      this.height = height;
      this.lifetime = lifetime;
      this.seed = seed;
      this.random = RandomSource.create(seed);
   }

   public long seed() {
      return this.seed;
   }

   public void glide(ClientLevel level, Vec3 localStart, Vec3 localVel, int ticks, Vec3 localSpan) {
      if (this.ended < 0) {
         this.glideLevel = level;
         this.glideStart = localStart;
         this.glideVel = localVel;
         this.glideTicks = Math.max(1, ticks);
         this.glideAge = 0;
         this.glideSpeed = localVel.length();
         this.glideSpan = localSpan;
         this.entryBase = this.base;
         this.entryUp = this.up;
         this.mode = SuperSlashCinematic.Mode.GLIDE;
      }
   }

   public void exit(Vec3 at, Vec3 fullVelocity, double distance) {
      if (this.ended < 0) {
         this.exitUp = this.up;
         this.prev = this.base;
         this.base = at;
         this.velocity = fullVelocity;
         this.dir = fullVelocity.lengthSqr() < 1.0E-8 ? this.dir : fullVelocity.normalize();
         this.flightUp = SuperSlash.bladeUp(this.dir, this.flat);
         this.side = this.dir.cross(this.flightUp).normalize();
         this.exitLeft = distance;
         this.exitAge = 0;
         this.mode = SuperSlashCinematic.Mode.EXIT;
         if (distance <= 0.0) {
            this.end(at);
         }
      }
   }

   @Nullable
   private Vec3[] glidePose(float partial) {
      PhysicsWorldBridge bridge = PhysicsWorld.bridge();
      Vec3 local = this.glideStart.add(this.glideVel.scale((double)Math.min((float)this.glideTicks, (float)this.glideAge + partial)));
      Vec3 at = bridge.renderPosition(this.glideLevel, local, partial);
      Vec3 u = bridge.renderDirection(this.glideLevel, local, this.glideSpan, partial);
      if (at != null && u != null) {
         float s = Mth.clamp(((float)this.glideAge + partial) / 3.0F, 0.0F, 1.0F);
         float k = Mth.clamp(((float)this.glideAge + partial) / 8.0F, 0.0F, 1.0F);
         s = s * s * (3.0F - 2.0F * s);
         k = k * k * (3.0F - 2.0F * k);
         Vec3 b = this.entryBase.add(at.subtract(this.entryBase).scale((double)s));
         Vec3 v = this.entryUp.add(u.normalize().subtract(this.entryUp).scale((double)k)).normalize();
         return new Vec3[]{b, v, bridge.renderDirection(this.glideLevel, local, this.glideVel.normalize(), partial)};
      } else {
         return null;
      }
   }

   public void end(Vec3 at) {
      if (this.ended < 0) {
         this.ended = 0;
         this.base = at;
         this.burst();
      }
   }

   private void burst() {
      if (VfxSettings.tier() != CoreClientConfig.CinematicQuality.LOW) {
         Vec3 c = this.base.add(this.up.scale((double)this.height * 0.5));
         VfxParticles.emit(SupaLook.GLINT, c.x, c.y, c.z, 0.0, 0.0, 0.0).size(3.0F);

         for (int i = 0; i < VfxSettings.count(16.0F); i++) {
            Vec3 q = this.base.add(this.up.scale(this.random.nextDouble() * (double)this.height));
            VfxParticles.emit(
               SupaLook.FILING,
               q.x,
               q.y,
               q.z,
               this.dir.x * 0.2 + (this.random.nextDouble() - 0.5) * 0.3,
               this.dir.y * 0.2 + 0.05 + this.random.nextDouble() * 0.15,
               this.dir.z * 0.2 + (this.random.nextDouble() - 0.5) * 0.3
            );
         }

         ShockRing.at(c, this.dir).radius(0.4F, (float)this.height * 0.75F).life(9).strength(0.6F).band(0.4F).rim(0.0F, 15398143).spawn();
      }
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (this.ended >= 0) {
         if (++this.ended > 10) {
            this.finish();
         }
      } else {
         this.prevUp = this.up;
         if (this.mode == SuperSlashCinematic.Mode.GLIDE) {
            this.glideTick();
         } else {
            this.prev = this.base;
            if (this.mode == SuperSlashCinematic.Mode.EXIT) {
               double v = SlashPlane.exitSpeed(this.exitAge, this.glideSpeed, this.velocity.length());
               this.base = this.base.add(this.dir.scale(v));
               float k = Math.min(1.0F, (float)(++this.exitAge) / 5.0F);
               this.up = this.exitUp.add(this.flightUp.subtract(this.exitUp).scale((double)k)).normalize();
               this.exitLeft -= v;
               if (this.exitLeft <= 1.0E-6) {
                  this.end(this.base);
                  return;
               }
            } else {
               this.base = this.base.add(this.velocity);
               if (this.age >= this.lifetime) {
                  this.end(this.base);
                  return;
               }
            }

            boolean fancy = VfxSettings.tier() != CoreClientConfig.CinematicQuality.LOW;
            if (fancy) {
               for (int i = 0; i < VfxSettings.count(3.0F); i++) {
                  Vec3 q = this.base.add(this.up.scale(this.random.nextDouble() * (double)this.height));
                  VfxParticles.emit(
                        SupaLook.SNOW,
                        q.x,
                        q.y,
                        q.z,
                        -this.dir.x * 0.15 + this.side.x * (this.random.nextDouble() - 0.5) * 0.1,
                        -this.dir.y * 0.15 + 0.01,
                        -this.dir.z * 0.15 + this.side.z * (this.random.nextDouble() - 0.5) * 0.1
                     )
                     .tint(15660287)
                     .alpha(0.35F)
                     .size(1.4F);
               }
            }

            double g = VfxWorld.ground(level, this.base.x, this.base.y + 1.0, this.base.z, 3);
            if (fancy && !Double.isNaN(g) && this.base.y - g < 1.6) {
               int dust = VfxWorld.shade(
                  VfxWorld.groundColour(level, this.base.x, g - 0.5, this.base.z), VfxWorld.light(level, this.base.x, g + 0.5, this.base.z)
               );

               for (int i = 0; i < VfxSettings.count(5.0F); i++) {
                  VfxParticles.emit(
                     SupaLook.SPARK,
                     this.base.x,
                     g + 0.05,
                     this.base.z,
                     -this.dir.x * (0.1 + this.random.nextDouble() * 0.2) + this.side.x * (this.random.nextDouble() - 0.5) * 0.3,
                     0.15 + this.random.nextDouble() * 0.25,
                     -this.dir.z * (0.1 + this.random.nextDouble() * 0.2) + this.side.z * (this.random.nextDouble() - 0.5) * 0.3
                  );
               }

               for (int i = 0; i < VfxSettings.count(2.0F); i++) {
                  VfxParticles.emit(
                        SupaLook.DUST,
                        this.base.x,
                        g + 0.1,
                        this.base.z,
                        this.side.x * (this.random.nextDouble() - 0.5) * 0.25,
                        0.08,
                        this.side.z * (this.random.nextDouble() - 0.5) * 0.25
                     )
                     .tint(dust)
                     .size(1.2F)
                     .alpha(0.55F);
               }

               VfxParticles.emit(SupaLook.SCRATCH, this.base.x, Math.floor(g) + 0.01 + (g - Math.floor(g)), this.base.z, 0.0, 0.0, 0.0)
                  .rotation((float)Math.atan2(this.dir.z, this.dir.x))
                  .tint(3027514)
                  .alpha(0.8F)
                  .size(1.6F);
            }

            Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            Vec3 rel = cam.subtract(this.base);
            double ahead = rel.dot(this.dir);
            double off = Math.abs(rel.dot(this.side));
            double along = rel.dot(this.up);
            if (!this.flashed
               && fancy
               && ahead < 0.5
               && ahead > -this.velocity.length() - 0.5
               && off < 3.0
               && along > -1.0
               && along < (double)(this.height + 1)) {
               this.flashed = true;
               VfxPost.flash(15398143, 0.4F);
               CameraDirector.shake(0.25F, 0.12F, 1.2F, 14.0F);
            }
         }
      }
   }

   private void glideTick() {
      this.glideAge++;
      Vec3[] pose = this.glidePose(0.0F);
      if (pose == null) {
         this.end(this.base);
      } else {
         this.prev = this.base;
         this.base = pose[0];
         this.up = pose[1];
         if (this.glideAge > this.glideTicks + 40) {
            this.end(this.base);
         } else if (VfxSettings.tier() != CoreClientConfig.CinematicQuality.LOW) {
            Vec3 travel = pose[2] != null ? pose[2] : this.dir;

            for (int i = 0; i < VfxSettings.count(2.0F); i++) {
               double h = this.random.nextDouble() * (double)this.height;
               Vec3 q = this.base.add(this.up.scale(h));
               VfxParticles.emit(SupaLook.SNOW, q.x, q.y, q.z, -travel.x * 0.04, 0.01, -travel.z * 0.04).tint(15660287).alpha(0.3F).size(1.1F);
            }

            if (this.glideAge % 2 == 0) {
               Vec3 top = this.base.add(this.up.scale((double)this.height + 0.1));
               VfxParticles.emit(SupaLook.GLINT, top.x, top.y, top.z, 0.0, 0.0, 0.0).size(1.6F);
               VfxParticles.emit(SupaLook.GLINT, this.base.x, this.base.y, this.base.z, 0.0, 0.0, 0.0).size(1.1F);
            }

            for (int i = 0; i < VfxSettings.count(3.0F); i++) {
               Vec3 q = this.base.add(this.up.scale(this.random.nextDouble() * (double)this.height));
               VfxParticles.emit(
                     SupaLook.FILING,
                     q.x,
                     q.y,
                     q.z,
                     this.up.x * 0.12 + (this.random.nextDouble() - 0.5) * 0.04,
                     this.up.y * 0.12,
                     this.up.z * 0.12 + (this.random.nextDouble() - 0.5) * 0.04
                  )
                  .size(0.8F);
            }

            if (this.random.nextInt(3) == 0) {
               Vec3 q = this.base.add(this.up.scale(this.random.nextDouble() * (double)this.height));
               VfxParticles.emit(SupaLook.AIR_SLIT, q.x, q.y, q.z, -travel.x * 0.06, -travel.y * 0.06, -travel.z * 0.06).size(1.4F);
            }
         }
      }
   }

   @Override
   public void render(VfxFrame f) {
      float p = f.partial;
      Vec3 at;
      Vec3 u;
      Vec3 back;
      float trail;
      if (this.mode == SuperSlashCinematic.Mode.GLIDE && this.ended < 0) {
         Vec3[] pose = this.glidePose(p);
         if (pose == null) {
            return;
         }

         at = pose[0];
         u = pose[1];
         back = pose[2] != null ? pose[2] : this.dir;
         trail = 0.35F;
      } else {
         at = this.ended >= 0 ? this.base : this.prev.add(this.base.subtract(this.prev).scale((double)p));
         u = this.prevUp.add(this.up.subtract(this.prevUp).scale((double)p)).normalize();
         back = this.dir;
         trail = 1.0F;
      }

      float fade = this.ended >= 0 ? Math.max(0.0F, 1.0F - ((float)this.ended + p) / 10.0F) : 1.0F;
      float heat = this.ended >= 0 ? 0.6F + 0.5F * fade : 1.15F;

      for (int g = 3; g >= 1; g--) {
         Vec3 b = at.subtract(back.scale((double)g * 0.38 * (double)trail));
         float a = fade * trail * (g == 1 ? 0.32F : (g == 2 ? 0.16F : 0.07F)) * (this.ended >= 0 ? 0.0F : 1.0F);
         if (a > 0.01F) {
            Vec3 lo = b.subtract(u.scale(0.2));
            Vec3 hi = b.add(u.scale((double)this.height + 0.2));
            Cuts.line(f, lo.x, lo.y, lo.z, hi.x, hi.y, hi.z, 0.5F, heat * 0.8F, a, f.time * 0.4F + (float)g);
         }
      }

      boolean held = trail < 1.0F;
      Vec3 lo = at.subtract(u.scale(0.25));
      Vec3 hi = at.add(u.scale((double)this.height + 0.25));
      if (held) {
         Cuts.line(f, lo.x, lo.y, lo.z, hi.x, hi.y, hi.z, 1.5F, heat * 0.75F, fade * 0.35F, f.time * 0.3F + 7.0F);
      }

      Cuts.line(f, lo.x, lo.y, lo.z, hi.x, hi.y, hi.z, held ? 0.9F : 0.62F, heat, fade, f.time * 0.6F);
      lo = at.subtract(u.scale(0.1));
      hi = at.add(u.scale((double)this.height + 0.1));
      Cuts.line(f, lo.x, lo.y, lo.z, hi.x, hi.y, hi.z, held ? 0.24F : 0.16F, heat * (held ? 1.35F : 1.2F), fade, f.time * 0.9F + 3.0F);
      BufferBuilder d = f.distortion();
      if (d != null && fade > 0.05F) {
         VfxSprites.Sprite glow = VfxSprites.GLOW;

         for (int i = 0; i < this.height * 2; i++) {
            Vec3 y = at.add(u.scale(((double)i + 0.5) * 0.5));

            for (int k = 0; k < 2; k++) {
               Vec3 q = y.subtract(back.scale((0.4 + (double)k * 0.7) * (double)trail));
               MeshWriter.distortQuad(
                  d,
                  f.rx(q.x),
                  f.ry(q.y),
                  f.rz(q.z),
                  0.7F - (float)k * 0.15F,
                  0.8F * fade * (1.0F - (float)k * 0.4F),
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
                  f.time * 0.05F + (float)i * 0.3F,
                  0.7F
               );
            }
         }
      }
   }

   @Override
   public double cullDistance() {
      return 160.0;
   }

   @Override
   public double x() {
      return this.base.x;
   }

   @Override
   public double y() {
      return this.base.y + (double)this.height * 0.5;
   }

   @Override
   public double z() {
      return this.base.z;
   }

   private static enum Mode {
      FLY,
      GLIDE,
      EXIT;
   }
}
