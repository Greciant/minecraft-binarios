package com.devilfruits.client.vfx.fx;

import com.devilfruits.client.render.CameraEffects;
import com.devilfruits.client.vfx.Cinematic;
import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxClock;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.camera.CameraDirector;
import com.devilfruits.client.vfx.particle.ParticleStyle;
import com.devilfruits.client.vfx.particle.Ribbon;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.particle.VfxPresets;
import com.devilfruits.client.vfx.post.VfxPost;
import com.devilfruits.client.vfx.render.MeshTex;
import com.devilfruits.client.vfx.render.MeshWriter;
import com.devilfruits.client.vfx.sprite.VfxSprites;
import com.devilfruits.core.fx.BombExplosionPayload;
import com.devilfruits.core.fx.ExplosionProfile;
import com.devilfruits.registry.DFParticles;
import com.devilfruits.registry.DFSounds;
import com.mojang.blaze3d.vertex.BufferBuilder;
import javax.annotation.Nullable;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class ExplosionCinematic extends Cinematic {
   private static final double SOUND_BLOCKS_PER_TICK = 17.0;
   private static final int RIBBONS_MAX = 10;
   private static final ExplosionCinematic.Look[] LOOKS = new ExplosionCinematic.Look[]{
      new ExplosionCinematic.Look(2, 5, 12, 8, 3, 0, 1.5F, 6.0F, 26.0F, 0.12F, 0.01F, 1, 0.12F),
      new ExplosionCinematic.Look(4, 10, 22, 14, 8, 4, 4.0F, 8.0F, 44.0F, 0.24F, 0.025F, 2, 0.25F),
      new ExplosionCinematic.Look(7, 18, 34, 24, 16, 8, 10.0F, 14.0F, 90.0F, 0.45F, 0.05F, 3, 0.45F),
      new ExplosionCinematic.Look(10, 26, 48, 34, 24, 10, 18.0F, 20.0F, 140.0F, 0.6F, 0.07F, 4, 0.6F)
   };
   private final ClientLevel level;
   private final Vec3 at;
   private final Vec3 normal;
   private final ExplosionProfile profile;
   private final ExplosionCinematic.Look look;
   private final float r;
   private final RandomSource random;
   private final int debrisRgb;
   private final double ground;
   private final boolean floor;
   private final float light;
   private final int soundDelay;
   private final double dist;
   @Nullable
   private final Entity caster;
   private final boolean firstPerson;
   private final int life;
   private final double[] hx = new double[10];
   private final double[] hy = new double[10];
   private final double[] hz = new double[10];
   private final double[] hvx = new double[10];
   private final double[] hvy = new double[10];
   private final double[] hvz = new double[10];
   private final Ribbon[] ribbons = new Ribbon[10];
   private int ribbonCount;

   private ExplosionCinematic(ClientLevel level, @Nullable Entity caster, BombExplosionPayload p) {
      super(VfxAnchor.of(caster, p.pos()), new VfxParams(p.pos(), p.normal(), p.power(), p.seed(), p.extra()));
      this.level = level;
      this.at = p.pos();
      this.normal = p.normal().lengthSqr() < 1.0E-6 ? new Vec3(0.0, 1.0, 0.0) : p.normal().normalize();
      this.profile = p.profile();
      this.look = LOOKS[this.profile.ordinal()];
      this.r = Mth.clamp(p.power(), 0.6F, 16.0F);
      this.random = RandomSource.create(p.seed());
      this.caster = caster;
      this.firstPerson = caster != null && VfxWorld.firstPersonCaster(caster.getId());
      this.debrisRgb = blockColour(level, p.blockState(), this.at);
      double g = VfxWorld.ground(level, this.at.x, this.at.y + 0.6, this.at.z, Math.max(3, (int)this.r));
      this.floor = !Double.isNaN(g) && this.at.y - g < (double)this.r * 0.9 + 1.2;
      this.ground = this.floor ? g : this.at.y;
      this.light = VfxWorld.light(level, this.at.x, this.at.y + 0.5, this.at.z);
      Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
      this.dist = cam.distanceTo(this.at);
      this.soundDelay = Math.min(12, (int)(this.dist / 17.0));
      this.life = (int)(this.look.columnSeconds() * 20.0F) + 40;
   }

   public static void play(ClientLevel level, @Nullable Entity caster, BombExplosionPayload p) {
      if (VfxSettings.legacy()) {
         legacy(level, p);
      } else {
         VfxDirector.spawn(new ExplosionCinematic(level, caster, p)).burst();
      }
   }

   private void burst() {
      int prof = this.profile.ordinal();
      Vec3 c = this.at;
      float soften = this.firstPerson ? 0.35F : 1.0F;
      VfxParticles.emit(VfxPresets.FLARE, c.x, c.y, c.z, 0.0, 0.0, 0.0).size(0.55F * this.r).rotation(this.random.nextFloat() * (float) (Math.PI * 2));
      VfxParticles.emit(VfxPresets.BLAST_STREAK, c.x, c.y, c.z, 0.0, 0.0, 0.0).size(this.r * (2.4F + (float)prof * 0.6F) * soften).rotation(0.0F);
      VfxParticles.emit(VfxPresets.GLOW, c.x, c.y, c.z, 0.0, 0.0, 0.0).size(this.r * 3.2F).tint(16767392).alpha(0.9F).life(2.2F);

      for (int i = 0; i < VfxSettings.count((float)this.look.fireballs()); i++) {
         double ox = i == 0 ? 0.0 : this.spread((double)this.r * 0.45);
         double oy = i == 0 ? 0.0 : this.random.nextDouble() * (double)this.r * 0.35 - (double)this.r * 0.05;
         double oz = i == 0 ? 0.0 : this.spread((double)this.r * 0.45);
         float size = this.r * (i == 0 ? 1.9F : 1.15F + this.random.nextFloat() * 0.6F);
         VfxParticles.emit(VfxPresets.BLAST_FIREBALL, c.x + ox, c.y + oy, c.z + oz, ox * 0.03, 0.015 + oy * 0.02, oz * 0.03)
            .size(size)
            .life(this.lifeScale() * (0.85F + this.random.nextFloat() * 0.3F))
            .heat(0.92F + this.random.nextFloat() * 0.16F);
      }

      for (int i = 0; i < VfxSettings.count((float)(3 + prof * 3)); i++) {
         Vec3 d = this.randomDir().scale(0.12 + 0.05 * (double)this.r);
         VfxParticles.emit(VfxPresets.FIRE_PUFF, c.x, c.y, c.z, d.x, Math.abs(d.y) * 0.6 + 0.03, d.z).size(0.45F * this.r);
      }

      for (int i = 0; i < VfxSettings.count((float)this.look.sparks()); i++) {
         Vec3 d = this.randomDir().scale((0.25 + this.random.nextDouble() * 0.45) * Math.sqrt((double)this.r));
         VfxParticles.emit(VfxPresets.BLAST_SPARK, c.x, c.y, c.z, d.x, Math.abs(d.y) * 0.8 + 0.08, d.z).size(0.8F + this.r * 0.08F);
      }

      for (int i = 0; i < VfxSettings.count((float)this.look.embers()); i++) {
         Vec3 d = this.randomDir().scale(0.06 + this.random.nextDouble() * 0.12 * Math.sqrt((double)this.r));
         VfxParticles.emit(
               VfxPresets.BLAST_EMBER,
               c.x + this.spread((double)this.r * 0.3),
               c.y + this.spread((double)this.r * 0.3),
               c.z + this.spread((double)this.r * 0.3),
               d.x,
               Math.abs(d.y) + 0.02,
               d.z
            )
            .life(1.0F + (float)prof * 0.25F);
      }

      this.ribbonCount = Math.min(10, this.look.ribbons());

      for (int i = 0; i < this.ribbonCount; i++) {
         Ribbon rb = Ribbon.create(14);
         this.ribbons[i] = rb;
         if (rb != null) {
            rb.look(0.035F + 0.006F * this.r, 9.0F, 1.35F, 0.45F, 3.0F, 4).texture(MeshTex.FLAME_RIBBON, 0.2F);
         }

         Vec3 d = this.randomDir().scale((0.45 + this.random.nextDouble() * 0.35) * Math.sqrt((double)this.r) * 0.8);
         this.hx[i] = c.x;
         this.hy[i] = c.y;
         this.hz[i] = c.z;
         this.hvx[i] = d.x;
         this.hvy[i] = Math.abs(d.y) * 0.9 + 0.15;
         this.hvz[i] = d.z;
      }

      if (!this.firstPerson) {
         for (int i = 0; i < VfxSettings.count((float)this.look.debris()); i++) {
            Vec3 d = this.randomDir().scale(0.18 + this.random.nextDouble() * 0.28 * Math.sqrt((double)this.r));
            ParticleStyle chunk = VfxPresets.BLAST_DEBRIS[this.random.nextInt(VfxPresets.BLAST_DEBRIS.length)];
            VfxParticles.emit(chunk, c.x + this.spread(0.4), c.y + 0.2, c.z + this.spread(0.4), d.x, Math.abs(d.y) + 0.22, d.z)
               .size(0.6F + this.random.nextFloat() * (0.5F + this.r * 0.06F))
               .tint(VfxWorld.shade(this.debrisRgb, this.light));
         }
      }

      Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
      Vec3 toCam = cam.subtract(c);
      if (prof >= 1) {
         ShockRing.at(c, toCam)
            .radius(this.r * 0.3F, this.r * (2.6F + (float)prof * 0.4F))
            .life(8 + prof * 2)
            .strength(0.8F + (float)prof * 0.25F)
            .band(0.4F)
            .rim(0.03F, 16774376)
            .spawn();
      }

      if (prof >= 2) {
         VfxParticles.emit(VfxPresets.BLAST_RING, c.x, c.y + (double)this.r * 0.15, c.z, 0.0, 0.0, 0.0)
            .size(this.r * (5.0F + (float)prof))
            .alpha(0.35F)
            .tint(16054010);
      }

      if (this.floor) {
         int dust = VfxWorld.shade(VfxWorld.groundColour(this.level, c.x, this.ground - 0.5, c.z), this.light);
         GroundWave.at(c.x, this.ground, c.z, this.r * (1.6F + (float)prof * 0.35F), 12 + prof * 3)
            .strength(0.7F + (float)prof * 0.2F)
            .dust(VfxSettings.count((float)(6 + prof * 6)), dust, 0.6F, 0.3F + (float)prof * 0.05F, 1.2F + this.r * 0.12F, this.random.nextLong())
            .spawn();
         if (prof >= 1) {
            VfxParticles.emit(VfxPresets.BLAST_DUST_RING, c.x, this.ground + 0.06, c.z, 0.0, 0.0, 0.0)
               .size(this.r * (2.4F + (float)prof * 0.4F))
               .tint(dust)
               .alpha(0.85F)
               .life(1.0F + (float)prof * 0.15F);
         }

         float rot = this.random.nextFloat() * (float) (Math.PI * 2);
         VfxParticles.emit(VfxPresets.BLAST_SOOT, c.x, this.ground + 0.025, c.z, 0.0, 0.0, 0.0)
            .size(this.r * 2.6F)
            .rotation(rot)
            .tint(VfxWorld.shade(9078144, this.light))
            .alpha(0.7F)
            .life(0.6F + (float)prof * 0.25F);
         VfxParticles.emit(VfxPresets.BLAST_SCORCH, c.x, this.ground + 0.03, c.z, 0.0, 0.0, 0.0)
            .size(this.r * 1.5F)
            .rotation(rot + 1.3F)
            .life(0.5F + (float)prof * 0.3F);
         VfxParticles.emit(VfxPresets.SCORCH_GLOW, c.x, this.ground + 0.035, c.z, 0.0, 0.0, 0.0).size(this.r * 1.2F).rotation(rot).life(1.2F);
      }

      if (this.profile == ExplosionProfile.MASSIVE) {
         double base = this.floor ? this.ground : c.y - (double)this.r * 0.5;
         float size = this.r * 4.2F;
         VfxParticles.emit(VfxPresets.BLAST_MUSHROOM, c.x, base + (double)size * 0.5 - (double)size * 0.08, c.z, 0.0, 0.0, 0.0)
            .size(size)
            .tint(VfxWorld.shade(15920874, Math.max(this.light, 0.45F)))
            .rotation(0.0F)
            .unflipped();
      }

      float ex = VfxWorld.exposure(this.caster, c, (double)this.look.near(), (double)this.look.far());
      if (ex > 0.0F) {
         CameraDirector.shake(this.look.trauma() * ex * soften, 0.06F, 2.5F + (float)prof, 2.6F);
         CameraDirector.fovPunch(this.look.fov() * ex);
         VfxPost.flash(16769720, this.look.flash() * ex * soften);
         VfxPost.chromatic((0.12F + 0.08F * (float)prof) * ex * soften);
         VfxPost.radialBlur(c, (0.03F + 0.025F * (float)prof) * ex * soften);
         if (ex > 0.5F && this.look.hitStop() > 0) {
            VfxClock.hitStop(this.look.hitStop());
         }
      }

      if (this.soundDelay == 0) {
         this.sounds();
      }
   }

   private float lifeScale() {
      return switch (this.profile) {
         case SMALL -> 0.7F;
         case MEDIUM -> 1.0F;
         case LARGE -> 1.3F;
         case MASSIVE -> 1.6F;
      };
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (this.age == this.soundDelay && this.soundDelay > 0) {
         this.sounds();
      }

      if (this.age == this.soundDelay + 8 && this.profile.ordinal() >= 1) {
         this.sound(DFSounds.EXPLOSION_DEBRIS.get(), 0.5F + 0.15F * (float)this.profile.ordinal(), 0.9F + this.random.nextFloat() * 0.2F);
      }

      int prof = this.profile.ordinal();
      if (this.floor && this.age < 14 + prof * 4) {
         float k = 1.0F - (float)this.age / (float)(14 + prof * 4);
         VfxParticles.emit(VfxPresets.GROUND_GLOW, this.at.x, this.ground + 0.05, this.at.z, 0.0, 0.0, 0.0)
            .size(this.r * (4.0F + (float)prof))
            .tint(16747066)
            .alpha(0.7F * k * k);
      }

      int smokeTicks = 4 + prof;
      if (this.age <= smokeTicks) {
         int n = VfxSettings.count((float)this.look.smoke() / (float)(smokeTicks + 1));

         for (int i = 0; i < n; i++) {
            this.billow((float)this.age / (float)smokeTicks);
         }
      }

      if (prof >= 2 && this.age == 3) {
         for (int i = 0; i < VfxSettings.count((float)(2 + prof)); i++) {
            VfxParticles.emit(
                  VfxPresets.BLAST_FIREBALL,
                  this.at.x + this.spread((double)this.r * 0.6),
                  this.at.y + (double)this.r * (0.2 + this.random.nextDouble() * 0.4),
                  this.at.z + this.spread((double)this.r * 0.6),
                  0.0,
                  0.03,
                  0.0
               )
               .size(this.r * (0.9F + this.random.nextFloat() * 0.5F))
               .life(this.lifeScale())
               .heat(0.85F);
         }
      }

      float columnTicks = this.look.columnSeconds() * 20.0F;
      if (this.age > 6 && (float)this.age < columnTicks && this.age % 7 == 0) {
         float k = 1.0F - (float)this.age / columnTicks;
         double base = this.floor ? this.ground : this.at.y;
         float size = this.r * (1.2F + 0.4F * (float)prof);
         int tint = VfxWorld.shade(this.age < 30 ? 8287854 : 9801608, this.light);
         VfxParticles.emit(
               VfxPresets.BLAST_COLUMN,
               this.at.x + this.spread((double)this.r * 0.2),
               base + (double)size * 0.45,
               this.at.z + this.spread((double)this.r * 0.2),
               this.spread(0.01),
               0.02 + 0.01 * (double)prof,
               this.spread(0.01)
            )
            .size(size)
            .tint(tint)
            .alpha(0.75F * k)
            .unflipped();
         if ((double)this.age < (double)columnTicks * 0.5 && prof >= 2) {
            VfxParticles.emit(
                  VfxPresets.BLAST_COLUMN,
                  this.at.x + this.spread((double)this.r * 0.3),
                  base + (double)size * 1.2,
                  this.at.z + this.spread((double)this.r * 0.3),
                  0.0,
                  0.03,
                  0.0
               )
               .size(size * 1.3F)
               .tint(tint)
               .alpha(0.55F * k)
               .unflipped();
         }
      }

      if (this.age > 2
         && (double)this.age < (double)columnTicks * 0.6
         && this.floor
         && this.random.nextFloat() < 0.35F * (1.0F - (float)this.age / columnTicks)) {
         VfxParticles.emit(
            VfxPresets.BLAST_EMBER,
            this.at.x + this.spread((double)this.r * 0.6),
            this.ground + 0.1,
            this.at.z + this.spread((double)this.r * 0.6),
            this.spread(0.01),
            0.04,
            this.spread(0.01)
         );
      }

      if (this.age == 10 && prof >= 2) {
         this.sound(DFSounds.EXPLOSION_CRACKLE.get(), 0.5F, 0.85F);
      }

      float t = VfxClock.time();

      for (int i = 0; i < this.ribbonCount && this.age <= 22; i++) {
         this.hvx[i] = this.hvx[i] * 0.95;
         this.hvy[i] = this.hvy[i] * 0.95 - 0.035;
         this.hvz[i] = this.hvz[i] * 0.95;
         this.hx[i] = this.hx[i] + this.hvx[i];
         this.hy[i] = this.hy[i] + this.hvy[i];
         this.hz[i] = this.hz[i] + this.hvz[i];
         if (this.floor && this.hy[i] < this.ground + 0.05) {
            this.hy[i] = this.ground + 0.05;
            this.hvy[i] = -this.hvy[i] * 0.3;
         }

         if (this.ribbons[i] != null) {
            this.ribbons[i].push(this.hx[i], this.hy[i], this.hz[i], t);
         }
      }

      if (this.age > this.life) {
         this.finish();
      }
   }

   private void billow(float phase) {
      Vec3 d = this.randomDir();
      double reach = (double)this.r * (0.25 + 0.5 * (double)phase + this.random.nextDouble() * 0.3);
      double x = this.at.x + d.x * reach;
      double y = this.at.y + Math.abs(d.y) * reach * 0.7 + (double)this.r * 0.1;
      double z = this.at.z + d.z * reach;
      float warm = Mth.clamp(1.0F - phase * 1.3F, 0.0F, 1.0F) * (0.5F + this.random.nextFloat() * 0.5F);
      int grey = this.random.nextBoolean() ? 9077370 : 10261902;
      int tint = VfxWorld.shade(lerpRgb(grey, 13666896, warm), Math.max(this.light, warm * 0.8F));
      float size = this.r * (1.2F + this.random.nextFloat() * 1.1F + phase * 0.6F);
      VfxParticles.emit(
            VfxPresets.BLAST_SMOKE,
            x,
            y,
            z,
            d.x * 0.03 * Math.sqrt((double)this.r),
            0.02 + 0.015 * (double)this.profile.ordinal() + Math.abs(d.y) * 0.02,
            d.z * 0.03 * Math.sqrt((double)this.r)
         )
         .size(size)
         .tint(tint)
         .alpha(0.82F)
         .life(1.0F + (float)this.profile.ordinal() * 0.35F)
         .rotation(this.sunRotation() + (this.random.nextFloat() - 0.5F) * 0.5F)
         .unflipped();
   }

   private float sunRotation() {
      float angle = this.level.getSunAngle(1.0F);
      Vector3f sun = new Vector3f(-Mth.sin(angle), Mth.cos(angle), 0.0F);
      if (sun.y < -0.05F) {
         sun.negate();
      }

      Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
      Vector3f left = camera.getLeftVector();
      Vector3f up = camera.getUpVector();
      float sr = -sun.dot(left);
      float su = sun.dot(up);
      return sr * sr + su * su < 1.0E-4F ? 0.0F : (float)Math.atan2((double)su, (double)sr) - (float) (Math.PI * 3.0 / 4.0);
   }

   @Override
   public void render(VfxFrame f) {
      float fade = Mth.clamp(1.0F - (float)this.age / 24.0F, 0.0F, 1.0F);

      for (int i = 0; i < this.ribbonCount; i++) {
         if (this.ribbons[i] != null && fade > 0.0F) {
            this.ribbons[i].render(f, this.hx[i], this.hy[i], this.hz[i], fade);
         }
      }

      BufferBuilder d = f.distortion();
      int hazeTicks = 30 + this.profile.ordinal() * 15;
      if (d != null && this.age < hazeTicks) {
         float k = 1.0F - (float)this.age / (float)hazeTicks;
         VfxSprites.Sprite glow = VfxSprites.GLOW;
         MeshWriter.distortQuad(
            d,
            f.rx(this.at.x),
            f.ry(this.at.y + (double)this.r * 0.4),
            f.rz(this.at.z),
            this.r * 1.6F,
            0.6F * k,
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
            f.time * 0.01F,
            0.3F
         );
      }
   }

   @Override
   public void removed() {
      for (int i = 0; i < this.ribbonCount; i++) {
         if (this.ribbons[i] != null) {
            this.ribbons[i].release();
         }
      }
   }

   private void sounds() {
      int prof = this.profile.ordinal();
      float near = (float)Mth.clamp(1.0 - this.dist / (double)(24 + prof * 24), 0.0, 1.0);
      float pitch = 1.05F - (float)prof * 0.12F + (this.random.nextFloat() - 0.5F) * 0.1F;
      this.sound(DFSounds.EXPLOSION_BOOM.get(), 1.2F + (float)prof * 0.8F, pitch);
      if (near > 0.0F) {
         this.sound(DFSounds.EXPLOSION_CRACK.get(), (0.6F + (float)prof * 0.2F) * near, 1.1F - (float)prof * 0.08F);
      }

      if (prof >= 2) {
         this.sound(DFSounds.EXPLOSION_DEEP.get(), 2.5F + (float)prof, 0.9F - (float)prof * 0.08F);
      }

      if (this.dist > 32.0 || prof >= 3) {
         this.sound(DFSounds.EXPLOSION_DISTANT.get(), 2.0F + (float)prof * 1.5F, 0.8F);
      }
   }

   private void sound(SoundEvent event, float volume, float pitch) {
      this.level.playLocalSound(this.at.x, this.at.y, this.at.z, event, SoundSource.BLOCKS, volume, pitch, false);
   }

   private static void legacy(ClientLevel level, BombExplosionPayload p) {
      RandomSource random = RandomSource.create(p.seed());
      int prof = p.profile().ordinal();
      float r = Mth.clamp(p.power(), 0.6F, 16.0F);
      Vec3 c = p.pos();
      int fire = 3 + prof * 4;
      int smoke = 4 + prof * 6;

      for (int i = 0; i < fire; i++) {
         double size = Math.floor((double)r * (2.0 + random.nextDouble() * 2.0));
         level.addParticle(
            (ParticleOptions)DFParticles.EXPLOSION_FIRE.get(),
            c.x + (random.nextDouble() - 0.5) * (double)r,
            c.y + (random.nextDouble() - 0.3) * (double)r * 0.6,
            c.z + (random.nextDouble() - 0.5) * (double)r,
            (random.nextDouble() - 0.5) * 0.08,
            size + 0.05,
            (random.nextDouble() - 0.5) * 0.08
         );
      }

      for (int i = 0; i < smoke; i++) {
         double size = Math.floor((double)r * (3.0 + random.nextDouble() * 3.0));
         level.addParticle(
            (ParticleOptions)DFParticles.EXPLOSION_SMOKE.get(),
            c.x + (random.nextDouble() - 0.5) * (double)r * 1.4,
            c.y + random.nextDouble() * (double)r * 0.8,
            c.z + (random.nextDouble() - 0.5) * (double)r * 1.4,
            (random.nextDouble() - 0.5) * 0.05,
            size + 0.03,
            (random.nextDouble() - 0.5) * 0.05
         );
      }

      if (prof >= 2) {
         level.addParticle(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y, c.z, 0.0, 0.0, 0.0);
      }

      level.playLocalSound(c.x, c.y, c.z, DFSounds.EXPLOSION_BOOM.get(), SoundSource.BLOCKS, 1.2F + (float)prof * 0.8F, 1.05F - (float)prof * 0.12F, false);
      if (prof >= 2) {
         level.playLocalSound(c.x, c.y, c.z, DFSounds.EXPLOSION_DEEP.get(), SoundSource.BLOCKS, 2.5F + (float)prof, 0.9F - (float)prof * 0.08F, false);
      }

      CameraEffects.shakeAt(c, 0.25F + (float)prof * 0.2F, (double)(12 + prof * 16));
   }

   private static int blockColour(ClientLevel level, int stateId, Vec3 at) {
      BlockState state = Block.stateById(stateId);
      if (state.isAir()) {
         return 8025198;
      } else {
         int col = state.getMapColor(level, BlockPos.containing(at)).col;
         return col == 0 ? 8025198 : col;
      }
   }

   private static int lerpRgb(int a, int b, float t) {
      int r = (int)Mth.lerp(t, (float)(a >> 16 & 0xFF), (float)(b >> 16 & 0xFF));
      int g = (int)Mth.lerp(t, (float)(a >> 8 & 0xFF), (float)(b >> 8 & 0xFF));
      int bl = (int)Mth.lerp(t, (float)(a & 0xFF), (float)(b & 0xFF));
      return r << 16 | g << 8 | bl;
   }

   private Vec3 randomDir() {
      double u = this.random.nextDouble() * 2.0 - 1.0;
      double a = this.random.nextDouble() * (float) (Math.PI * 2);
      double s = Math.sqrt(1.0 - u * u);
      Vec3 d = new Vec3(s * Math.cos(a), u, s * Math.sin(a));
      return d.dot(this.normal) < 0.0 ? d.subtract(this.normal.scale(2.0 * d.dot(this.normal))) : d;
   }

   private double spread(double amount) {
      return (this.random.nextDouble() * 2.0 - 1.0) * amount;
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

   @Override
   public double cullDistance() {
      return (double)(160 + this.profile.ordinal() * 40);
   }

   private static record Look(
      int fireballs,
      int smoke,
      int sparks,
      int embers,
      int debris,
      int ribbons,
      float columnSeconds,
      float near,
      float far,
      float trauma,
      float fov,
      int hitStop,
      float flash
   ) {
   }
}
