package com.devilfruits.fruits.uo_uo_seiryu.client.vfx;

import com.devilfruits.client.model.geo.GeoAnimation;
import com.devilfruits.client.model.geo.GeoResources;
import com.devilfruits.client.sound.SoundDesign;
import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.VfxSound;
import com.devilfruits.client.vfx.camera.CameraDirector;
import com.devilfruits.client.vfx.fx.BoltStrike;
import com.devilfruits.client.vfx.fx.GroundWave;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.particle.VfxPresets;
import com.devilfruits.client.vfx.post.VfxPost;
import com.devilfruits.client.vfx.render.MeshTex;
import com.devilfruits.client.vfx.render.MeshWriter;
import com.devilfruits.client.vfx.sprite.VfxSprites;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuSounds;
import com.devilfruits.fruits.uo_uo_seiryu.UoUoSeiryu;
import com.devilfruits.fruits.uo_uo_seiryu.client.render.HybridAnchors;
import com.devilfruits.registry.DFSounds;
import com.mojang.blaze3d.vertex.BufferBuilder;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class VeilCinematic extends HybridCinematic {
   public static final int WHITE = 15984895;
   public static final int LIGHT = 11955455;
   public static final int VIOLET = 8011007;
   public static final int DEEP = 2757984;
   public static final int SMOKE = 1708080;
   private static final int GRADE = 13812991;
   private static final int AFTER = 24;
   private static final int TAG_COCOON = 32273;
   private final int cocoonAt;
   private final int burstAt;
   private final float radius;
   private float crackU;
   private float crackV;
   @Nullable
   private VfxSound swell;

   public VeilCinematic(ClientLevel level, VfxAnchor anchor, VfxParams params) {
      super(level, anchor, params);
      GeoAnimation clip = GeoResources.animation(UoUoSeiryu.anim("hybrid/thunderclad_veil"));
      this.cocoonAt = Math.max(1, Math.round(clip != null ? clip.marker("cocoon", 8.0F) : 8.0F));
      this.burstAt = Math.max(this.cocoonAt + 1, Math.round(clip != null ? clip.marker("burst", 24.0F) : 24.0F));
      this.radius = Math.max(1.5F, this.height * 0.47F);
      Entity e = anchor.entity(level);
      if (e != null) {
         this.swell = VfxSound.play(
            SeiryuSounds.VEIL_CAST.get(),
            e,
            e.position(),
            false,
            a -> 0.25F + 0.75F * Math.min(1.0F, (float)a / (float)this.cocoonAt),
            a -> 0.6F + 0.3F * Math.min(1.0F, (float)a / (float)this.burstAt),
            this.burstAt + 4,
            () -> !this.isDone()
         );
      }
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      double cy = this.y + (double)this.height * 0.5;
      if (this.firstPersonCaster()) {
         VfxParticles.nearFade(3.5F);
      }

      if (this.age < this.cocoonAt) {
         this.tickGather(level, cy);
      } else if (this.age == this.cocoonAt) {
         this.cocoon(level, cy);
      } else if (this.age < this.burstAt) {
         this.tickCocoon(level, cy);
      } else if (this.age == this.burstAt) {
         this.burst(level, cy);
      } else if (this.age > this.burstAt + 24) {
         this.finish();
      } else {
         this.tickAfter(cy);
      }
   }

   private void tickGather(ClientLevel level, double cy) {
      float p = (float)this.age / (float)this.cocoonAt;
      Vec3 chest = new Vec3(this.x, this.y + (double)this.height * 0.62, this.z);
      double floor = this.onFloor(3.0) ? this.groundY : this.y;

      for (int i = 0; i < VfxSettings.count(4.0F + 5.0F * p); i++) {
         double a = this.random.nextDouble() * (float) (Math.PI * 2);
         double d = (double)this.radius * (0.9 + this.random.nextDouble() * 0.9);
         double px = this.x + Math.cos(a) * d;
         double pz = this.z + Math.sin(a) * d;
         double py = floor + 0.1 + this.random.nextDouble() * 0.4;
         double k = 0.08 + 0.05 * (double)p;
         VfxParticles.emit(VfxPresets.THUNDER_MOTE, px, py, pz, (chest.x - px) * k, (chest.y - py) * k, (chest.z - pz) * k).size(1.7F).life(0.8F);
      }

      if (this.random.nextFloat() < 0.6F * VfxSettings.spawnScale()) {
         double a = this.random.nextDouble() * (float) (Math.PI * 2);
         double d = (double)this.radius * 0.7;
         VfxParticles.emit(
            VfxPresets.THUNDER_SPARK,
            this.x + Math.cos(a) * d,
            floor + 0.1,
            this.z + Math.sin(a) * d,
            -Math.cos(a) * 0.08,
            0.28 + this.random.nextDouble() * 0.2,
            -Math.sin(a) * 0.08
         );
      }

      if (this.age % 3 == 1) {
         double a = this.random.nextDouble() * (float) (Math.PI * 2);
         Vec3 from = new Vec3(this.x + Math.cos(a) * (double)this.radius * 0.5, floor + 0.08, this.z + Math.sin(a) * (double)this.radius * 0.5);
         BoltStrike.between(from, from.add(Math.cos(a + 1.3) * 0.9, 0.25 + this.random.nextDouble() * 0.4, Math.sin(a + 1.3) * 0.9), this.seed + (long)this.age)
            .width(0.06F)
            .life(3)
            .forks(1)
            .rough(0.22F)
            .endGlow(0.5F)
            .spawn();
      }

      Entity e = this.caster(level);
      if (e != null) {
         for (int id : new int[]{0, 1}) {
            HybridAnchors.get(e, id, 1.0F, this.anchor3);
            VfxParticles.emit(VfxPresets.GLOW, this.anchor3.x, this.anchor3.y, this.anchor3.z, 0.0, 0.0, 0.0)
               .size(0.6F + 1.1F * p)
               .tint(11955455)
               .alpha(0.4F + 0.4F * p);
         }
      }

      this.groundGlow(0.25F + 0.4F * p, this.radius * 2.4F);
      float ex = this.exposure();
      if (ex > 0.0F) {
         VfxPost.vignette(0.3F * p * ex);
         VfxPost.grade(13812991, 0.22F * p * ex);
      }

      if (this.local()) {
         CameraDirector.dolly(1.0F - 0.05F * p, 0.3F);
      }
   }

   private void cocoon(ClientLevel level, double cy) {
      if (this.onFloor(3.0)) {
         GroundWave.at(this.x, this.groundY, this.z, this.radius * 1.9F, 12)
            .strength(1.0F)
            .ring(15984895, 0.9F)
            .dust(26, this.shade(10130090), this.radius * 0.7F, 0.34F, 0.36F, this.seed ^ 49344L)
            .spawn();
      }

      VfxParticles.emit(VfxPresets.GLOW, this.x, cy, this.z, 0.0, 0.0, 0.0).size(this.radius * 2.2F).tint(11955455).alpha(0.35F).life(0.8F);
      float ex = this.exposure();
      if (ex > 0.0F) {
         CameraDirector.snapDolly();
         CameraDirector.shake(0.3F * ex, 0.08F, 1.8F, 2.6F);
         CameraDirector.fovPunch(0.04F * ex);
      }

      SoundDesign.play(
         new Vec3(this.x, cy, this.z),
         SeiryuSounds.VEIL_CAST,
         0.6F,
         0.55F,
         SoundDesign.layer(DFSounds.LAYER_BOOM, 0.9F, 0.55F),
         SoundDesign.layer(DFSounds.LAYER_THUNDER, 0.3F, 1.5F)
      );
   }

   private void tickCocoon(ClientLevel level, double cy) {
      if (this.age % 2 == 0) {
         this.crackU = this.random.nextFloat();
         this.crackV = this.random.nextFloat();
      }

      if (this.age % 3 == 0) {
         double a = this.random.nextDouble() * (float) (Math.PI * 2);
         double b = (this.random.nextDouble() - 0.3) * 1.2;
         double nx = Math.cos(a) * Math.cos(b);
         double ny = Math.sin(b);
         double nz = Math.sin(a) * Math.cos(b);
         Vec3 from = new Vec3(this.x + nx * (double)this.radius * 0.98, cy + ny * (double)this.radius * 0.98, this.z + nz * (double)this.radius * 0.98);
         double len = 0.5 + this.random.nextDouble() * 0.6;
         BoltStrike.between(
               from, from.add(nx * len + this.spread(0.3), ny * len + this.spread(0.3), nz * len + this.spread(0.3)), this.seed + (long)this.age * 31L
            )
            .width(0.07F)
            .life(3)
            .forks(1)
            .rough(0.25F)
            .endGlow(0.6F)
            .spawn();
      }

      for (int i = 0; i < VfxSettings.count(2.0F); i++) {
         double a = this.random.nextDouble() * (float) (Math.PI * 2);
         double d = (double)this.radius * 1.8;
         double px = this.x + Math.cos(a) * d;
         double pz = this.z + Math.sin(a) * d;
         double py = cy + this.spread((double)this.radius);
         VfxParticles.emit(VfxPresets.THUNDER_MOTE, px, py, pz, (this.x - px) * 0.09, (cy - py) * 0.09, (this.z - pz) * 0.09).life(0.7F);
      }

      this.groundGlow(0.5F, this.radius * 2.8F);
      float ex = this.exposure();
      if (ex > 0.0F) {
         VfxPost.vignette(0.3F * ex);
         VfxPost.grade(13812991, 0.25F * ex);
         if (this.age % 4 == 0) {
            CameraDirector.shake(32273, 0.25F * ex, 0.06F, 0.5F, 3.6F);
         }
      }
   }

   private void burst(ClientLevel level, double cy) {
      if (this.swell != null) {
         this.swell.end();
      }

      Vec3 center = new Vec3(this.x, cy, this.z);

      for (int i = 0; i < VfxSettings.count(18.0F); i++) {
         double a = this.random.nextDouble() * (float) (Math.PI * 2);
         double b = (this.random.nextDouble() - 0.35) * 1.3;
         double nx = Math.cos(a) * Math.cos(b);
         double ny = Math.sin(b);
         double nz = Math.sin(a) * Math.cos(b);
         double sp = 0.16 + this.random.nextDouble() * 0.14;
         VfxParticles.emit(
               VfxPresets.SMOKE,
               this.x + nx * (double)this.radius * 0.7,
               cy + ny * (double)this.radius * 0.7,
               this.z + nz * (double)this.radius * 0.7,
               nx * sp,
               ny * sp * 0.6 - 0.01,
               nz * sp
            )
            .tint(this.shade(1708080))
            .size(0.5F + this.random.nextFloat() * 0.3F)
            .life(0.35F)
            .alpha(0.95F);
      }

      for (int i = 0; i < VfxSettings.count(34.0F); i++) {
         double a = this.random.nextDouble() * (float) (Math.PI * 2);
         double b = (this.random.nextDouble() - 0.3) * 1.4;
         double nx = Math.cos(a) * Math.cos(b);
         double ny = Math.sin(b);
         double nz = Math.sin(a) * Math.cos(b);
         double sp = 0.45 + this.random.nextDouble() * 0.45;
         VfxParticles.emit(
            VfxPresets.THUNDER_SHARD,
            this.x + nx * (double)this.radius * 0.8,
            cy + ny * (double)this.radius * 0.8,
            this.z + nz * (double)this.radius * 0.8,
            nx * sp,
            ny * sp,
            nz * sp
         );
      }

      for (int i = 0; i < 5; i++) {
         double a = (double)((float)i * (float) (Math.PI * 2) / 5.0F) + this.random.nextDouble() * 0.6;
         double b = (this.random.nextDouble() - 0.4) * 0.9;
         double nx = Math.cos(a) * Math.cos(b);
         double ny = Math.sin(b);
         double nz = Math.sin(a) * Math.cos(b);
         Vec3 from = center.add(nx * (double)this.radius * 0.6, ny * (double)this.radius * 0.6, nz * (double)this.radius * 0.6);
         double len = (double)this.radius * (1.1 + this.random.nextDouble() * 0.6);
         BoltStrike.between(from, from.add(nx * len, ny * len, nz * len), this.seed + (long)i * 977L).width(0.11F).life(5).forks(2).spawn();
      }

      VfxParticles.emit(VfxPresets.THUNDER_FLARE, this.x, cy, this.z, 0.0, 0.0, 0.0).size(0.8F).rotation(this.random.nextFloat() * 6.28F);
      VfxParticles.emit(VfxPresets.GLOW, this.x, cy, this.z, 0.0, 0.0, 0.0).size(this.radius * 4.5F).tint(11955455).alpha(0.85F).life(2.0F);
      if (this.onFloor(3.0)) {
         GroundWave.at(this.x, this.groundY, this.z, this.radius * 2.4F, 12).strength(1.1F).ring(11955455, 0.8F).air(cy, this.radius * 1.6F).spawn();
      }

      float ex = this.exposure();
      if (ex > 0.0F) {
         float lens = this.firstPersonCaster() ? 0.35F : 1.0F;
         CameraDirector.shake(0.45F * ex, 0.09F, 2.6F, 3.0F);
         CameraDirector.fovPunch(0.05F * ex);
         VfxPost.flash(11955455, 0.16F * ex * lens);
         VfxPost.chromatic(0.45F * ex * lens);
      }

      SoundDesign.play(
         center, SeiryuSounds.VEIL_BURST, 1.4F, 1.0F, SoundDesign.layer(DFSounds.LAYER_THUNDER, 0.8F, 1.3F), SoundDesign.layer(DFSounds.LAYER_BOOM, 0.5F, 1.2F)
      );
   }

   private void tickAfter(double cy) {
      float p = (float)(this.age - this.burstAt) / 24.0F;

      for (int i = 0; i < VfxSettings.count(2.0F * (1.0F - p)); i++) {
         double a = this.random.nextDouble() * (float) (Math.PI * 2);
         double d = this.random.nextDouble() * (double)this.radius * 1.2;
         VfxParticles.emit(
            VfxPresets.THUNDER_MOTE,
            this.x + Math.cos(a) * d,
            this.y + this.random.nextDouble() * (double)this.height,
            this.z + Math.sin(a) * d,
            this.spread(0.02),
            0.03 + this.random.nextDouble() * 0.04,
            this.spread(0.02)
         );
      }

      this.groundGlow(0.45F * (1.0F - p), this.radius * 2.6F);
      float ex = this.exposure();
      if (ex > 0.0F) {
         VfxPost.grade(13812991, 0.2F * (1.0F - p) * ex);
      }
   }

   private void groundGlow(float alpha, float size) {
      if (this.onFloor(4.0)) {
         VfxParticles.emit(VfxPresets.GROUND_GLOW, this.x, this.groundY + 0.04, this.z, 0.0, 0.0, 0.0)
            .size(size)
            .tint(8011007)
            .alpha(alpha * (0.85F + 0.15F * Mth.sin((float)this.age * 1.9F)));
      }
   }

   @Override
   public void render(VfxFrame f) {
      float t = (float)this.age + f.partial;
      if (!(t < (float)this.cocoonAt) && !(t > (float)(this.burstAt + 6))) {
         float p = f.entityPartial;
         float cx = f.rx(this.rx(p));
         float cy = f.ry(this.ry(p) + (double)this.height * 0.5);
         float cz = f.rz(this.rz(p));
         float since = t - (float)this.cocoonAt;
         float grow = Mth.clamp(since / 2.5F, 0.0F, 1.0F);
         float r = this.radius * (0.55F + 0.45F * (1.0F - (1.0F - grow) * (1.0F - grow))) * (1.0F + 0.025F * Mth.sin(t * 1.3F));
         float dissolve = since < 3.0F ? 1.0F - since / 3.0F : 0.0F;
         if (t > (float)this.burstAt) {
            float b = Mth.clamp((t - (float)this.burstAt) / 6.0F, 0.0F, 1.0F);
            dissolve = Math.max(dissolve, 0.15F + 0.85F * b);
            r *= 1.0F + 0.18F * b;
         }

         float d = Mth.sqrt(cx * cx + cy * cy + cz * cz);
         boolean inside = d < r * 1.05F;
         if (!inside) {
            float pull = Math.min(0.35F, d - r * 1.05F) / d;
            cx -= cx * pull;
            cy -= cy * pull;
            cz -= cz * pull;
            d -= d * pull;
         }

         if (f.allowMesh()) {
            int shell = MeshWriter.argb(920086, inside ? 0.86F : 1.0F);
            MeshWriter.sphere(f.mesh(MeshTex.NOISE), cx, cy, cz, r, 16, 28, t * 0.002F, 0.0F, 2.0F, shell, -1.0F, 1, 1.6F, dissolve, 0.0F);
            float ax = inside ? 0.0F : -cx / d;
            float ay = inside ? 1.0F : -cy / d;
            float az = inside ? 0.0F : -cz / d;
            float cap = inside ? (float) Math.PI : (float) (Math.PI / 2);
            float alive = t > (float)this.burstAt ? 1.0F - Mth.clamp((t - (float)this.burstAt) / 4.0F, 0.0F, 1.0F) : grow;
            if (alive > 0.02F && !inside && f.allowMesh()) {
               MeshWriter.dome(
                  f.mesh(MeshTex.NOISE),
                  cx,
                  cy,
                  cz,
                  ax,
                  ay,
                  az,
                  r * 0.985F,
                  cap,
                  8,
                  28,
                  t * 0.01F,
                  0.0F,
                  2.0F,
                  1.0F,
                  MeshWriter.argb(16777215, 0.42F * alive),
                  0.95F,
                  1,
                  0.9F,
                  dissolve,
                  1.0F
               );
            }

            if (alive > 0.02F && f.allowMesh()) {
               MeshWriter.dome(
                  f.mesh(MeshTex.CRACKLE),
                  cx,
                  cy,
                  cz,
                  ax,
                  ay,
                  az,
                  r * 1.01F,
                  cap,
                  8,
                  28,
                  this.crackU + t * 0.004F,
                  this.crackV,
                  2.0F,
                  1.0F,
                  MeshWriter.argb(16777215, (inside ? 0.3F : 0.62F) * alive),
                  1.05F,
                  1,
                  1.5F,
                  dissolve,
                  0.0F
               );
            }

            this.renderHaze(f, t);
         }
      } else {
         this.renderHaze(f, t);
      }
   }

   private void renderHaze(VfxFrame f, float t) {
      BufferBuilder d = f.distortion();
      if (d != null) {
         float k = t < (float)this.cocoonAt
            ? 0.3F * t / (float)this.cocoonAt
            : (t < (float)this.burstAt ? 0.45F : 0.45F * Math.max(0.0F, 1.0F - (t - (float)this.burstAt) / 24.0F));
         if (!(k <= 0.02F)) {
            VfxSprites.Sprite glow = VfxSprites.GLOW;
            float p = f.entityPartial;
            MeshWriter.distortQuad(
               d,
               f.rx(this.rx(p)),
               f.ry(this.ry(p) + (double)this.height * 0.5),
               f.rz(this.rz(p)),
               this.radius * 1.4F,
               k,
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
               t * 0.005F,
               0.4F
            );
         }
      }
   }

   @Override
   public void removed() {
      if (this.swell != null) {
         this.swell.end();
      }
   }

   public int burstTick() {
      return this.burstAt;
   }
}
