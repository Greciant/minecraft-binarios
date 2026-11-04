package com.devilfruits.fruits.yomi_yomi.client.vfx;

import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxClock;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.camera.CameraDirector;
import com.devilfruits.client.vfx.fx.GroundWave;
import com.devilfruits.client.vfx.fx.ShockRing;
import com.devilfruits.client.vfx.fx.VfxWorld;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.particle.VfxPresets;
import com.devilfruits.client.vfx.post.VfxPost;
import com.devilfruits.client.vfx.render.MeshTex;
import com.devilfruits.client.vfx.render.MeshWriter;
import com.devilfruits.fruits.yomi_yomi.YomiSounds;
import com.devilfruits.fruits.yomi_yomi.ability.EarthCrushingAbility;
import com.devilfruits.fruits.yomi_yomi.ability.YomiTiming;
import com.devilfruits.fruits.yomi_yomi.client.SoulKingGhost;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class SoulKingCinematic extends YomiCinematic {
   private static final int[] NOTES = new int[]{0, 2, 4, 6, 8, 10, 12, 14, 17, 19, 21, 23, 26, 28, 29};
   private static final double NOTE_GREEN = 0.93;
   private final SoulKingCinematic.Mode mode;
   private final int entityId;
   private final Vec3 dir;
   private final double range;
   private final int step;
   private final int length;
   @Nullable
   private SoulFlames.Web web;
   private float webStart;
   private int summonAt = -1;
   private boolean riffCut;
   @Nullable
   private SoundInstance music;
   private final List<float[]> trailL = new ArrayList<>();
   private final List<float[]> trailR = new ArrayList<>();
   private static final int TRAIL_MAX = 24;
   private static final float[] TX = new float[24];
   private static final float[] TY = new float[24];
   private static final float[] TZ = new float[24];
   private static final float[] TW = new float[24];
   private static final float[] TH = new float[24];
   private static final float[] TA = new float[24];

   private SoulKingCinematic(Entity user, Vec3 at, Vec3 dir, SoulKingCinematic.Mode mode, double range, int step, int length, long seed) {
      super(VfxAnchor.of(user, at), at, seed);
      this.mode = mode;
      this.entityId = user.getId();
      this.dir = dir.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : dir.normalize();
      this.range = range;
      this.step = step;
      this.length = length;
   }

   public static SoulKingCinematic riff(Entity user, long seed) {
      SoulKingCinematic c = new SoulKingCinematic(user, user.position(), Vec3.ZERO, SoulKingCinematic.Mode.RIFF, 0.0, 0, 100, seed);
      c.web = new SoulFlames.Web(seed, user.position(), 7.5, 9, 28.0F, 90.0F);
      return c;
   }

   public void cut() {
      if (this.summonAt < 0) {
         this.riffCut = true;
         if (this.music != null) {
            Minecraft.getInstance().getSoundManager().stop(this.music);
         }
      }
   }

   private void music(Entity user) {
      if (this.music == null) {
         this.music = new EntityBoundSoundInstance(YomiSounds.KING_RIFF.get(), SoundSource.PLAYERS, 1.0F, 1.0F, user, this.random.nextLong());
         Minecraft.getInstance().getSoundManager().play(this.music);
      }
   }

   public static void summon(Entity user, long seed) {
      SoulKingCinematic c = VfxDirector.find(SoulKingCinematic.class, k -> k.entityId == user.getId() && k.mode == SoulKingCinematic.Mode.RIFF && !k.isDone());
      if (c == null) {
         c = new SoulKingCinematic(user, user.position(), Vec3.ZERO, SoulKingCinematic.Mode.RIFF, 0.0, 0, 70, seed);
         c.web = new SoulFlames.Web(seed, user.position(), 6.0, 8, 8.0F, 60.0F);
         VfxDirector.spawn(c);
      }

      c.summonAt = c.age;
      c.riffCut = false;
      c.music(user);
      SoulKingGhost.play(user.getId(), "summon");
   }

   public static SoulKingCinematic strike(Entity user, Vec3 dir, double reach, int step, long seed) {
      SoulKingGhost.play(user.getId(), step == 0 ? "swing_right" : (step == 1 ? "swing_left" : "push"));
      return new SoulKingCinematic(user, user.position(), dir, SoulKingCinematic.Mode.STRIKE, reach, step, YomiTiming.KING_STRIKE_LENGTH[step], seed);
   }

   public static SoulKingCinematic crush(ClientLevel level, Entity user, Vec3 apex, Vec3 dir, double range, long seed) {
      SoulKingGhost.play(user.getId(), "earth_crushing");
      return new SoulKingCinematic(user, apex, dir, SoulKingCinematic.Mode.CRUSH, range, 0, 44, seed);
   }

   public static SoulKingCinematic roar(Entity user, double radius, long seed) {
      SoulKingGhost.play(user.getId(), "roar");
      return new SoulKingCinematic(user, user.position(), Vec3.ZERO, SoulKingCinematic.Mode.ROAR, radius, 0, 48, seed);
   }

   public static SoulKingCinematic vanish(Entity user, long seed) {
      SoulKingGhost.play(user.getId(), "vanish");
      return new SoulKingCinematic(user, user.position(), Vec3.ZERO, SoulKingCinematic.Mode.VANISH, 0.0, 0, 34, seed);
   }

   public static void hit(Entity target, Vec3 at, Vec3 fling, int step, long seed) {
      RandomSource r = RandomSource.create(seed);
      VfxParticles.emit(YomiLook.SOUL_FLASH, at.x, at.y, at.z, 0.0, 0.0, 0.0).size(step == 2 ? 1.6F : 1.1F);
      Vec3 d = fling.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 1.0, 0.0) : fling.normalize();

      for (int i = 0; i < VfxSettings.count(step == 2 ? 14.0F : 8.0F); i++) {
         VfxParticles.emit(
            YomiLook.STREAK,
            at.x,
            at.y,
            at.z,
            d.x * 0.4 + (double)(r.nextFloat() - 0.5F) * 0.3,
            d.y * 0.4 + (double)(r.nextFloat() - 0.5F) * 0.3,
            d.z * 0.4 + (double)(r.nextFloat() - 0.5F) * 0.3
         );
      }

      SoulFire.burst(r, at, step == 2 ? 1.2F : 0.8F);
      if (step == 1) {
         GroundWave.at(target.getX(), target.getY(), target.getZ(), 1.6F, 8).strength(0.5F).dust(8, 9076848, 0.3F, 0.06F, seed).spawn();
      }

      if (VfxWorld.local(target.getId()) || VfxWorld.firstPersonCaster(target.getId())) {
         VfxClock.hitStop(1);
      }
   }

   @Override
   protected int length() {
      return this.length;
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      Entity e = this.anchorEntity();
      switch (this.mode) {
         case RIFF:
            this.tickRiff(level, e);
            break;
         case STRIKE:
            this.tickStrike(e);
            break;
         case CRUSH:
            this.tickCrush(level, e);
            break;
         case ROAR:
            this.tickRoar(level, e);
            break;
         case VANISH:
            this.tickVanish(e);
      }
   }

   private boolean near(Vec3 p, double blocks) {
      return VfxWorld.local(this.entityId) || Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceToSqr(p) < blocks * blocks;
   }

   private void tickRiff(ClientLevel level, @Nullable Entity e) {
      if (this.web != null) {
         this.web.burn(this.random, this.webLife((float)this.age), 2.2F);
      }

      if (this.summonAt < 0 && !this.riffCut && e != null) {
         this.music(e);

         for (int note : NOTES) {
            if (note == this.age - 1) {
               double yr = Math.toRadians(e instanceof LivingEntity l ? (double)l.yBodyRot : (double)e.getYRot());
               double x = e.getX() - Math.cos(yr) * 0.25 - Math.sin(yr) * 0.35;
               double z = e.getZ() - Math.sin(yr) * 0.25 + Math.cos(yr) * 0.35;
               level.addParticle(
                  ParticleTypes.NOTE,
                  x + (double)(this.random.nextFloat() - 0.5F) * 0.3,
                  e.getY() + 1.0 + (double)this.random.nextFloat() * 0.3,
                  z + (double)(this.random.nextFloat() - 0.5F) * 0.3,
                  0.93 + (double)(this.random.nextFloat() - 0.5F) * 0.03,
                  0.0,
                  0.0
               );
               VfxParticles.emit(
                  YomiLook.STREAK, x, e.getY() + 0.95, z, (double)(this.random.nextFloat() - 0.5F) * 0.2, 0.12, (double)(this.random.nextFloat() - 0.5F) * 0.2
               );
            }
         }

         float k = Math.min(1.0F, (float)this.age / 30.0F);
         SoulFire.pool(this.random, e.getX(), e.getY(), e.getZ(), 0.4 + (double)k * 0.6, 0.8F + k * 1.4F, 0.5F + k * 0.45F);
         SoulFire.floorGlow(e.getX(), e.getY(), e.getZ(), 2.0F + k * 3.0F, 8191802, 0.2F + k * 0.3F);
      }

      if (this.summonAt >= 0 && e != null) {
         int t = this.age - this.summonAt;
         float yaw = SoulKingGhost.yaw((Player)e);
         double yr = Math.toRadians((double)yaw);
         Vec3 base = SoulKingGhost.root(e.position(), yaw);
         if (t == 0) {
            SoulFire.pool(this.random, base.x, base.y, base.z, 1.6, 14.0F, 1.4F);
            VfxParticles.emit(YomiLook.SOUL_CRACKS, base.x, base.y + 0.04, base.z, 0.0, 0.0, 0.0)
               .size(3.6F)
               .rotation(this.random.nextFloat() * (float) (Math.PI * 2));
            SoulFire.embers(this.random, base.add(0.0, 0.3, 0.0), 1.4, VfxSettings.count(16.0F));
            if (this.near(base, 24.0)) {
               CameraDirector.shake(0.2F, 0.02F, 1.2F, 8.0F);
            }
         }

         if (t > 0 && t < 12) {
            SoulFire.pool(this.random, base.x, base.y, base.z, 1.3, 3.0F, 1.3F + (float)t * 0.08F);
            SoulFire.floorGlow(base.x, base.y, base.z, 4.0F + (float)t * 0.3F, 8191802, 0.5F);
         }

         if (t == 12) {
            Vec3 up = base.add(0.0, 1.5, 0.0);
            VfxParticles.emit(YomiLook.SOUL_FLASH, up.x, up.y, up.z, 0.0, 0.0, 0.0).size(4.0F);
            SoulFire.burst(this.random, up, 2.6F);
            SoulFire.pool(this.random, base.x, base.y, base.z, 2.0, 14.0F, 2.0F);
            GroundWave.at(base.x, e.getY(), base.z, 4.0F, 12).strength(0.6F).dust(18, 8161898, 0.6F, 0.12F, this.random.nextLong()).spawn();
            if (this.near(base, 32.0)) {
               CameraDirector.shake(0.4F, 0.04F, 2.0F, 10.0F);
               VfxPost.flash(13172586, 0.15F);
            }
         }

         if (t == 26) {
            Vec3 head = SoulKingGhost.point(this.entityId, 0);
            Vec3 c = head != null ? head : base.add(0.0, 4.4, 0.0);
            ShockRing.at(c, new Vec3(Math.sin(yr + Math.PI), 0.0, -Math.cos(yr + Math.PI)))
               .radius(0.8F, 9.0F)
               .life(14)
               .strength(0.9F)
               .band(0.3F)
               .rim(0.0F, 16777215)
               .spawn();
            GroundWave.at(e.getX(), e.getY(), e.getZ(), 9.0F, 16).strength(0.9F).spawn();
            SoulFire.pool(this.random, e.getX(), e.getY(), e.getZ(), 3.0, 12.0F, 1.3F);
            SoulFire.burst(this.random, c, 2.0F);
            VfxParticles.emit(YomiLook.SOUL_FLASH, c.x, c.y, c.z, 0.0, 0.0, 0.0).size(5.0F);
            if (this.near(c, 40.0)) {
               CameraDirector.shake(0.75F, 0.03F, 3.2F, 12.0F);
               CameraDirector.fovPunch(0.06F);
               VfxPost.flash(13172586, 0.3F);
               VfxPost.chromatic(0.5F);
               VfxClock.slowMo(10, 0.5F);
            }
         }
      }
   }

   private float webLife(float a) {
      float life = a - this.webStart;
      return this.riffCut ? Math.max(life, 80.0F) : life;
   }

   private void tickStrike(@Nullable Entity e) {
      if (e != null) {
         int hit = YomiTiming.KING_STRIKE_HIT[this.step];
         Vec3 right = new Vec3(-this.dir.z, 0.0, this.dir.x);
         if (this.age >= hit - 2 && this.age <= hit + 1) {
            float k = Mth.clamp((float)(this.age - (hit - 3)) / 4.0F, 0.0F, 1.0F);
            Vec3 p = this.claw(e.position(), right, k, (float)this.range * 0.85F);
            Vec3 v = this.claw(e.position(), right, Math.min(1.0F, k + 0.1F), (float)this.range * 0.85F).subtract(p);

            for (int i = 0; i < VfxSettings.count(4.0F); i++) {
               VfxParticles.emit(YomiLook.STREAK, p.x, p.y, p.z, v.x * 0.6 + (double)(this.random.nextFloat() - 0.5F) * 0.1, v.y * 0.6, v.z * 0.6);
            }

            for (int i = 0; i < VfxSettings.count(3.0F); i++) {
               Vec3 q = p.add(v.scale(this.random.nextDouble()));
               SoulFire.lick(q.x, q.y, q.z, v.x * 0.15, v.y * 0.15 + 0.03, v.z * 0.15, 0.6F + this.random.nextFloat() * 0.3F, true, false);
            }
         }

         if (this.age == hit) {
            if (this.step == 2) {
               Vec3 chest = e.position().add(0.0, 1.8, 0.0).add(this.dir.scale(1.2));
               ShockRing.at(chest, this.dir).radius(0.6F, 3.4F).life(10).move(this.dir.scale(0.45)).strength(0.8F).band(0.45F).rim(0.0F, 16777215).spawn();

               for (int side = -1; side <= 1; side += 2) {
                  Vec3 palm = chest.add(right.scale((double)side * 0.7)).add(this.dir.scale(0.6));
                  VfxParticles.emit(YomiLook.SOUL_FLASH, palm.x, palm.y, palm.z, 0.0, 0.0, 0.0).size(1.8F);
               }

               for (int i = 0; i < VfxSettings.count(14.0F); i++) {
                  Vec3 sp = this.dir
                     .scale(0.35 + this.random.nextDouble() * 0.35)
                     .add((this.random.nextDouble() - 0.5) * 0.25, (this.random.nextDouble() - 0.5) * 0.2, (this.random.nextDouble() - 0.5) * 0.25);
                  SoulFire.lick(chest.x, chest.y, chest.z, sp.x, sp.y, sp.z, 1.0F + this.random.nextFloat() * 0.5F, i % 3 == 0, false);
               }

               if (this.near(chest, 20.0)) {
                  CameraDirector.shake(0.3F, 0.05F, 1.5F, 10.0F);
               }
            } else if (this.near(e.position(), 16.0)) {
               CameraDirector.shake(0.15F, 0.06F, 1.0F, 10.0F);
            }
         }
      }
   }

   private Vec3 claw(Vec3 feet, Vec3 right, float k, float r) {
      Vec3 c = feet.add(0.0, 1.6, 0.0);
      if (this.step == 2) {
         return c.add(this.dir.scale(0.8 + (double)k * ((double)r - 0.8)));
      } else {
         double a = (double)k * Math.PI;
         Vec3 u = this.step == 0 ? right.scale(-1.0) : right.scale(-1.0).add(0.0, 1.2, 0.0).normalize();
         return c.add(u.scale(Math.cos(a) * (double)r)).add(this.dir.scale(Math.sin(a) * (double)r));
      }
   }

   private void tickCrush(ClientLevel level, @Nullable Entity e) {
      if (e != null) {
         Vec3 ground = new Vec3(this.at.x, groundAt(level, this.at), this.at.z);
         if (this.age >= 25) {
            int t = this.age - 25;
            if (t == 0) {
               this.impact(level, ground);
            }

            if (t >= 0 && t < 8) {
               this.front(level, ground, EarthCrushingAbility.band(this.range, t), EarthCrushingAbility.band(this.range, t + 1));
            }

            if (t > 0 && t < 30) {
               float burn = 1.0F - (float)t / 30.0F;
               SoulFire.pool(this.random, ground.x, ground.y, ground.z, 1.4, 2.5F * burn, 0.8F + burn * 0.6F);
               SoulFire.floorGlow(ground.x, ground.y, ground.z, 5.0F, 8191802, 0.5F * burn);
            }
         } else {
            float k = (float)this.age / 25.0F;

            for (int side = 0; side < 2; side++) {
               Vec3 hand = SoulKingGhost.point(this.entityId, side == 0 ? 5 : 7);
               if (hand != null) {
                  for (int i = 0; i < VfxSettings.count(1.0F + k * 2.0F); i++) {
                     SoulFire.lick(
                        hand.x + (this.random.nextDouble() - 0.5) * 0.6,
                        hand.y + (this.random.nextDouble() - 0.5) * 0.4,
                        hand.z + (this.random.nextDouble() - 0.5) * 0.6,
                        0.0,
                        0.05,
                        0.0,
                        0.6F + k * 0.9F,
                        i % 2 == 1,
                        false
                     );
                  }

                  if (VfxSettings.count(1.5F) > 0) {
                     Vec3 from = hand.add(
                        (this.random.nextDouble() - 0.5) * 5.0, (this.random.nextDouble() - 0.5) * 5.0, (this.random.nextDouble() - 0.5) * 5.0
                     );
                     Vec3 v = hand.subtract(from).scale(0.2);
                     VfxParticles.emit(YomiLook.STREAK, from.x, from.y, from.z, v.x, v.y, v.z);
                  }

                  SoulFire.halo(hand.x, hand.y, hand.z, 1.5F + k * 2.5F, 8191802, 0.25F + k * 0.35F);
               }
            }

            if (this.age > 8) {
               SoulFire.floorGlow(ground.x, ground.y, ground.z, 1.5F + k * 4.0F, 8191802, 0.2F + k * 0.45F);
               if (this.age % 3 == 0) {
                  int dust = VfxWorld.groundColour(level, ground.x, ground.y - 0.5, ground.z);
                  VfxParticles.emit(
                        VfxPresets.DUST,
                        ground.x + (this.random.nextDouble() - 0.5) * 2.0,
                        ground.y + 0.1,
                        ground.z + (this.random.nextDouble() - 0.5) * 2.0,
                        0.0,
                        0.02,
                        0.0
                     )
                     .tint(dust)
                     .size(0.5F)
                     .alpha(0.5F);
               }
            }
         }
      }
   }

   private void impact(ClientLevel level, Vec3 ground) {
      int dust = VfxWorld.groundColour(level, ground.x, ground.y - 0.5, ground.z);
      float r = (float)this.range;
      Vec3 c = ground.add(0.0, 0.6, 0.0);
      VfxParticles.emit(YomiLook.SOUL_FLASH, c.x, c.y + 0.4, c.z, 0.0, 0.0, 0.0).size(5.0F);
      SoulFire.burst(this.random, c, 3.0F);
      SoulFire.pool(this.random, ground.x, ground.y, ground.z, 1.8, 14.0F, 1.6F);
      VfxParticles.emit(YomiLook.SOUL_CRACKS, ground.x, ground.y + 0.04, ground.z, 0.0, 0.0, 0.0)
         .size(5.5F)
         .rotation(this.random.nextFloat() * (float) (Math.PI * 2));
      GroundWave.at(ground.x, ground.y, ground.z, r, 14).strength(1.5F).dust(36, dust, 0.5F, 0.3F, this.random.nextLong()).spawn();
      ShockRing.at(ground.add(0.0, 0.5, 0.0), new Vec3(0.0, 1.0, 0.0)).radius(1.0F, r).life(10).strength(1.3F).band(0.3F).rim(0.0F, 16777215).spawn();
      ShockRing.at(ground.add(0.0, 1.6, 0.0), new Vec3(0.0, 1.0, 0.0)).radius(0.6F, r * 0.75F).life(12).strength(0.8F).band(0.25F).rim(0.0F, 16777215).spawn();
      VfxParticles.emit(VfxPresets.BLAST_DUST_RING, ground.x, ground.y + 0.06, ground.z, 0.0, 0.0, 0.0).size(r * 1.1F).tint(dust).alpha(0.9F);

      for (int i = 0; i < VfxSettings.count(26.0F); i++) {
         double a = this.random.nextDouble() * (float) (Math.PI * 2);
         double sp = 0.1 + this.random.nextDouble() * 0.25;
         VfxParticles.emit(
               VfxPresets.BLAST_DEBRIS[this.random.nextInt(VfxPresets.BLAST_DEBRIS.length)],
               ground.x + Math.cos(a) * 0.8,
               ground.y + 0.3,
               ground.z + Math.sin(a) * 0.8,
               Math.cos(a) * sp,
               0.3 + this.random.nextDouble() * 0.35,
               Math.sin(a) * sp
            )
            .tint(dust)
            .size(0.8F + this.random.nextFloat() * 0.8F);
      }

      for (int i = 0; i < VfxSettings.count(24.0F); i++) {
         double a = this.random.nextDouble() * (float) (Math.PI * 2);
         VfxParticles.emit(YomiLook.STREAK, c.x, c.y, c.z, Math.cos(a) * 0.9, 0.15 + this.random.nextDouble() * 0.4, Math.sin(a) * 0.9);
      }

      if (this.near(ground, this.range + 30.0)) {
         CameraDirector.shake(1.1F, 0.03F, 4.0F, 14.0F);
         CameraDirector.fovPunch(0.08F);
         VfxPost.flash(13172586, 0.3F);
         VfxPost.chromatic(0.7F);
         VfxPost.radialBlur(ground.add(0.0, 1.0, 0.0), 0.1F);
         VfxClock.hitStop(3);
      }
   }

   private void front(ClientLevel level, Vec3 ground, double near, double far) {
      int dust = VfxWorld.groundColour(level, ground.x, ground.y - 0.5, ground.z);
      int n = VfxSettings.count((float)(far * 1.8));

      for (int i = 0; i < n; i++) {
         double a = this.random.nextDouble() * (float) (Math.PI * 2);
         double d = Mth.lerp(this.random.nextDouble(), near, far);
         double x = ground.x + Math.cos(a) * d;
         double z = ground.z + Math.sin(a) * d;
         double top = VfxWorld.ground(level, x, ground.y + 1.5, z, 4);
         double y = Double.isNaN(top) ? ground.y : top;
         SoulFire.lick(
            x,
            y + 0.2,
            z,
            Math.cos(a) * 0.12,
            0.05 + this.random.nextDouble() * 0.05,
            Math.sin(a) * 0.12,
            0.8F + this.random.nextFloat() * 0.4F,
            i % 2 == 0,
            false
         );
         if (i % 2 == 0) {
            VfxParticles.emit(VfxPresets.DUST, x, y + 0.2, z, Math.cos(a) * 0.18, 0.04, Math.sin(a) * 0.18).tint(dust).size(1.2F).alpha(0.8F);
         }

         if (i % 3 == 0) {
            VfxParticles.emit(
                  VfxPresets.BLAST_DEBRIS[this.random.nextInt(VfxPresets.BLAST_DEBRIS.length)],
                  x,
                  y + 0.2,
                  z,
                  Math.cos(a) * 0.12,
                  0.25 + this.random.nextDouble() * 0.2,
                  Math.sin(a) * 0.12
               )
               .tint(dust)
               .size(0.6F + this.random.nextFloat() * 0.5F);
         }
      }
   }

   private static double groundAt(ClientLevel level, Vec3 p) {
      double top = VfxWorld.ground(level, p.x, p.y + 1.5, p.z, 5);
      return Double.isNaN(top) ? p.y : top;
   }

   private void tickRoar(ClientLevel level, @Nullable Entity e) {
      if (e != null) {
         Vec3 jaw = SoulKingGhost.point(this.entityId, 2);
         Vec3 mouth = jaw != null ? jaw : e.position().add(0.0, 4.2, 0.0);
         if (this.age < 25) {
            for (int i = 0; i < VfxSettings.count(3.0F); i++) {
               double a = this.random.nextDouble() * Math.PI * 2.0;
               double d = 5.0 + this.random.nextDouble() * 5.0;
               Vec3 from = e.position().add(Math.cos(a) * d, 0.3 + this.random.nextDouble() * 4.0, Math.sin(a) * d);
               Vec3 v = mouth.subtract(from).scale(0.09);
               VfxParticles.emit(YomiLook.STREAK, from.x, from.y, from.z, v.x, v.y, v.z).life(1.6F);
            }

            if (this.age % 4 == 0) {
               Frost.mist(this.random, e.position(), 6.0, 2);
            }
         } else {
            if (this.age == 25) {
               double yr = Math.toRadians((double)SoulKingGhost.yaw((Player)e));
               Vec3 fwd = new Vec3(-Math.sin(yr), 0.0, Math.cos(yr));
               VfxParticles.emit(YomiLook.SOUL_FLASH, mouth.x, mouth.y, mouth.z, 0.0, 0.0, 0.0).size(4.0F);
               GroundWave.at(e.getX(), e.getY(), e.getZ(), (float)this.range, 18).strength(1.0F).spawn();
               if (this.near(mouth, this.range + 24.0)) {
                  CameraDirector.shake(1.0F, 0.025F, 4.0F, 14.0F);
                  CameraDirector.fovPunch(0.08F);
                  VfxPost.chromatic(0.9F);
                  VfxPost.desaturate(0.5F);
                  VfxPost.flash(13172586, 0.25F);
               }

               for (int i = 0; i < VfxSettings.count(30.0F); i++) {
                  double a = this.random.nextDouble() * Math.PI * 2.0;
                  Vec3 v = new Vec3(Math.cos(a), (this.random.nextDouble() - 0.3) * 0.4, Math.sin(a)).scale(0.9 + this.random.nextDouble() * 0.5);
                  VfxParticles.emit(YomiLook.STREAK, mouth.x, mouth.y, mouth.z, v.x, v.y, v.z);
               }

               ShockRing.at(mouth, fwd).radius(0.6F, 6.0F).life(12).move(fwd.scale(0.4)).strength(1.0F).band(0.35F).rim(0.0F, 16777215).spawn();
            }

            if (this.age >= 25 && this.age <= 37 && (this.age - 25) % 3 == 0) {
               float k = (float)(this.age - 25) / 12.0F;
               ShockRing.at(mouth, new Vec3(0.0, 1.0, 0.0))
                  .radius(1.0F, (float)this.range)
                  .life(16)
                  .strength(0.8F * (1.0F - k * 0.5F))
                  .band(0.2F)
                  .rim(0.0F, 16777215)
                  .spawn();
               SoulFire.pool(this.random, e.getX(), e.getY(), e.getZ(), this.range * 0.3, 3.0F, 0.9F);
            }
         }
      }
   }

   private void tickVanish(@Nullable Entity e) {
      Vec3 base = SoulKingGhost.point(this.entityId, 8);
      Vec3 c = base != null ? base : this.at;
      if (this.age < 28) {
         float burn = 1.0F - (float)this.age / 28.0F;
         SoulFire.pool(this.random, c.x, c.y, c.z, 1.4, 2.0F * burn + 0.5F, 1.2F + burn * 0.6F);
         SoulFire.embers(this.random, c.add(0.0, 0.3, 0.0), 1.2, VfxSettings.count(2.0F));
      }

      if (this.age == 28) {
         VfxParticles.emit(YomiLook.SOUL_FLASH, c.x, c.y + 0.4, c.z, 0.0, 0.0, 0.0).size(2.4F);
         SoulFire.burst(this.random, c.add(0.0, 0.3, 0.0), 1.4F);
      }
   }

   @Override
   public void render(VfxFrame f) {
      float t = (float)this.age + f.partial;
      if (this.web != null) {
         this.web.render(f, this.webLife(t), 0.22F);
      }

      if (this.mode == SoulKingCinematic.Mode.STRIKE) {
         int hit = YomiTiming.KING_STRIKE_HIT[this.step];
         if (t >= (float)(hit - 5) && t <= (float)(hit + 2)) {
            if (this.step != 1) {
               sample(this.trailR, SoulKingGhost.point(this.entityId, 7), t);
            }

            if (this.step != 0) {
               sample(this.trailL, SoulKingGhost.point(this.entityId, 5), t);
            }
         }

         claws(f, this.trailR, t);
         claws(f, this.trailL, t);
      }
   }

   private static void sample(List<float[]> trail, @Nullable Vec3 p, float t) {
      if (p != null) {
         if (!trail.isEmpty()) {
            float[] last = trail.get(trail.size() - 1);
            if (Math.abs(last[3] - t) < 0.05F
               || (p.x - (double)last[0]) * (p.x - (double)last[0])
                     + (p.y - (double)last[1]) * (p.y - (double)last[1])
                     + (p.z - (double)last[2]) * (p.z - (double)last[2])
                  < 0.01) {
               return;
            }
         }

         trail.add(new float[]{(float)p.x, (float)p.y, (float)p.z, t});
         if (trail.size() > 24) {
            trail.remove(0);
         }
      }
   }

   private static void claws(VfxFrame f, List<float[]> trail, float t) {
      trail.removeIf(sx -> t - sx[3] > 4.0F);
      int n = Math.min(trail.size(), 24);
      if (n >= 2 && f.allowMesh()) {
         for (int c = -1; c <= 1; c++) {
            for (int i = 0; i < n; i++) {
               float[] s = trail.get(i);
               float k = (float)i / (float)(n - 1);
               float age = Mth.clamp((t - s[3]) / 4.0F, 0.0F, 1.0F);
               TX[i] = f.rx((double)s[0]);
               TY[i] = f.ry((double)(s[1] + (float)c * 0.3F));
               TZ[i] = f.rz((double)s[2]);
               TW[i] = (c == 0 ? 0.24F : 0.16F) * (0.3F + 0.7F * k);
               TH[i] = (c == 0 ? 0.92F : 0.74F) * (0.55F + 0.45F * k);
               TA[i] = (1.0F - age) * (0.3F + 0.7F * k);
            }

            MeshWriter.strip(f.mesh(MeshTex.CUT_RIBBON), n, TX, TY, TZ, TW, TH, TA, -t * 0.6F, 0.5F, 16777215, 6, 2.4F);
         }
      }
   }

   @Override
   public double cullDistance() {
      return 96.0;
   }

   public int entityId() {
      return this.entityId;
   }

   private static enum Mode {
      RIFF,
      STRIKE,
      CRUSH,
      ROAR,
      VANISH;
   }
}
