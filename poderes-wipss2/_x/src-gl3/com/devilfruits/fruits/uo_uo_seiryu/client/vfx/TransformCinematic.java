package com.devilfruits.fruits.uo_uo_seiryu.client.vfx;

import com.devilfruits.client.sound.SoundDesign;
import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.VfxSound;
import com.devilfruits.client.vfx.fx.ArcBurst;
import com.devilfruits.client.vfx.fx.GroundWave;
import com.devilfruits.client.vfx.fx.Impact;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.particle.VfxPresets;
import com.devilfruits.client.vfx.post.VfxPost;
import com.devilfruits.client.vfx.render.MeshTex;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuSounds;
import com.devilfruits.registry.DFSounds;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class TransformCinematic extends HybridCinematic {
   private static final int SMOKE_RED = 12075038;
   public static final int REVEAL = 10;
   private static final int AFTER = 44;
   private final int windup;
   private int changed = -1;
   @Nullable
   private VfxSound swell;

   public TransformCinematic(ClientLevel level, VfxAnchor anchor, VfxParams params) {
      super(level, anchor, params);
      this.windup = Math.max(0, params.extra());
      Entity e = anchor.entity(level);
      if (e != null && this.windup > 0) {
         this.swell = VfxSound.play(
            SeiryuSounds.FORM_WINDUP.get(),
            e,
            e.position(),
            false,
            a -> 0.4F + 0.6F * Math.min(1.0F, (float)a / (float)this.windup),
            a -> 0.8F + 0.3F * Math.min(1.0F, (float)a / (float)this.windup),
            this.windup + 6,
            () -> !this.isDone() && this.changed < 0
         );
         SoundDesign.layerOnly(e.position(), SeiryuSounds.LAYER_FLAME, 0.7F, 0.8F);
         this.slash(e, 0.95F);
      }
   }

   @Nullable
   public static TransformCinematic of(Entity e) {
      return VfxDirector.find(TransformCinematic.class, c -> c.follows(e));
   }

   public static float reveal(Entity e, float partial) {
      TransformCinematic c = of(e);
      if (c != null && c.changed >= 0) {
         float p = Mth.clamp(((float)c.changed + partial) / 10.0F, 0.0F, 1.0F);
         return 0.04F + 0.96F * (1.0F - (1.0F - p) * (1.0F - p));
      } else {
         return 1.0F;
      }
   }

   private void slash(Entity e, float tilt) {
      ArcBurst.at(e.position(), this.seed ^ (long)this.age)
         .follow(e, 0.55F)
         .arcs(1)
         .radius(this.height * 0.45F, this.height * 0.7F)
         .life(7)
         .spin(1.3F)
         .arc(4.08407F)
         .tilt(tilt, tilt + 0.2F)
         .colour(MeshTex.FLAME_RIBBON, 16777215, 0.3F)
         .alpha(0.95F)
         .spawn();
   }

   public void cancel() {
      if (this.changed < 0) {
         this.finish();
      }
   }

   public void change(ClientLevel level) {
      if (this.changed < 0) {
         this.changed = 0;
         if (this.swell != null) {
            this.swell.end();
         }

         Entity e = this.caster(level);
         Vec3 mid = new Vec3(this.x, this.y + (double)this.height * 0.55, this.z);
         Impact.at(level, mid, this.seed ^ 122L)
            .caster(e)
            .flash(1.5F)
            .glow(16765503)
            .sparks(18, 0.55F)
            .embers(30)
            .trauma(0.55F, 0.07F, 3.0F)
            .fov(0.12F)
            .lens(0.7F)
            .screenFlash(0.3F)
            .play();

         for (int i = 0; i < VfxSettings.count(2.0F) + 1; i++) {
            VfxParticles.emit(
                  VfxPresets.FIREBALL, mid.x + this.spread(0.6), mid.y + this.spread(0.7), mid.z + this.spread(0.6), this.spread(0.1), 0.05, this.spread(0.1)
               )
               .size(0.6F + this.random.nextFloat() * 0.25F)
               .life(0.5F);
         }

         for (int i = 0; i < VfxSettings.count(16.0F); i++) {
            double a = this.random.nextDouble() * (float) (Math.PI * 2);
            VfxParticles.emit(VfxPresets.FIRE_PUFF, mid.x, mid.y + this.spread(0.6), mid.z, Math.cos(a) * 0.5, this.spread(0.15), Math.sin(a) * 0.5)
               .size(1.0F)
               .life(0.8F);
         }

         if (e != null) {
            ArcBurst.at(mid, this.seed ^ 123L)
               .follow(e, 0.55F)
               .arcs(3)
               .radius(this.height * 0.42F, this.height * 0.62F)
               .life(22)
               .spin(0.75F)
               .arc((float) (Math.PI * 3.0 / 2.0))
               .tilt(0.3F, 1.3F)
               .look(MeshTex.FLAME_RIBBON, 0, 0.42F, 1.25F, 2.0F)
               .fadeIn(0.08F)
               .spawn();
            ArcBurst.at(mid, this.seed ^ 124L)
               .follow(e, 0.4F)
               .arcs(2)
               .radius(this.height * 0.55F, this.height * 0.8F)
               .life(18)
               .spin(-0.9F)
               .arc(3.7699115F)
               .tilt(0.1F, 0.5F)
               .look(MeshTex.FLAME_RIBBON, 0, 0.3F, 1.15F, 1.6F)
               .fadeIn(0.08F)
               .spawn();
         }

         if (this.onFloor(3.0)) {
            GroundWave.at(this.x, this.groundY, this.z, this.height * 1.5F, 14).strength(1.3F).ring(16742938, 0.6F).air(mid.y, this.height * 0.9F).spawn();
         }

         SoundDesign.play(
            new Vec3(this.x, this.y, this.z),
            SeiryuSounds.FORM_HYBRID,
            1.3F,
            1.0F,
            SoundDesign.layer(DFSounds.LAYER_BOOM, 0.8F, 0.9F),
            SoundDesign.layer(SeiryuSounds.LAYER_BLAST, 0.6F, 1.1F),
            SoundDesign.layer(SeiryuSounds.LAYER_GROWL, 0.8F, 1.1F)
         );
      }
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      Entity e = this.caster(level);
      if (this.firstPersonCaster()) {
         VfxParticles.nearFade(3.0F);
      }

      double cy = this.y + (double)this.height * 0.5;
      if (this.changed < 0) {
         float p = this.windup > 0 ? Math.min(1.0F, (float)this.age / (float)this.windup) : 1.0F;
         VfxParticles.emit(VfxPresets.GLOW, this.x, cy, this.z, 0.0, 0.0, 0.0).size(this.height * (0.7F + 0.5F * p)).tint(16777215).alpha(0.4F + 0.5F * p);
         VfxParticles.emit(VfxPresets.GLOW, this.x, cy, this.z, 0.0, 0.0, 0.0).size(this.height * 0.45F).tint(16777215).alpha(0.3F + 0.6F * p).life(0.7F);

         for (int i = 0; i < VfxSettings.count(0.5F + 0.6F * p); i++) {
            double a = this.random.nextDouble() * (float) (Math.PI * 2);
            double d = (double)this.height * (0.2 + this.random.nextDouble() * 0.25);
            VfxParticles.emit(
                  VfxPresets.SMOKE,
                  this.x + Math.cos(a) * d,
                  this.y + 0.3 + this.random.nextDouble() * (double)this.height * 0.6,
                  this.z + Math.sin(a) * d,
                  Math.cos(a) * 0.02,
                  0.04,
                  Math.sin(a) * 0.02
               )
               .tint(this.shade(12075038))
               .size(0.4F)
               .alpha(0.45F)
               .life(0.45F);
         }

         for (int i = 0; i < VfxSettings.count(2.0F + 2.0F * p); i++) {
            double a = this.random.nextDouble() * (float) (Math.PI * 2);
            double r = (double)this.height * (0.5 + this.random.nextDouble() * 0.3);
            double px = this.x + Math.cos(a) * r;
            double pz = this.z + Math.sin(a) * r;
            double py = this.y + this.random.nextDouble() * (double)this.height;
            VfxParticles.emit(VfxPresets.EMBER, px, py, pz, (this.x - px) * 0.08, (cy - py) * 0.05, (this.z - pz) * 0.08);
         }

         if (e != null && this.windup > 6 && this.age == this.windup / 2) {
            this.slash(e, 0.35F);
         }

         float ex = this.exposure();
         if (ex > 0.0F) {
            VfxPost.grade(16765503, 0.12F * p * ex);
         }

         if (this.age > this.windup + 40) {
            this.finish();
         }
      } else {
         this.changed++;
         float p = Math.min(1.0F, (float)this.changed / 44.0F);

         for (int i = 0; i < VfxSettings.count(2.5F * (1.0F - p)); i++) {
            double a = this.random.nextDouble() * (float) (Math.PI * 2);
            double d = this.random.nextDouble() * (double)this.height * 0.5;
            VfxParticles.emit(
                  VfxPresets.EMBER,
                  this.x + Math.cos(a) * d,
                  this.y + this.random.nextDouble() * (double)this.height,
                  this.z + Math.sin(a) * d,
                  this.spread(0.02),
                  0.03 + this.random.nextDouble() * 0.04,
                  this.spread(0.02)
               )
               .heat(0.95F);
         }

         if (this.onFloor(3.0)) {
            VfxParticles.emit(VfxPresets.GROUND_GLOW, this.x, this.groundY + 0.04, this.z, 0.0, 0.0, 0.0)
               .size(this.height * 1.6F)
               .tint(16742938)
               .alpha(0.5F * (1.0F - p));
         }

         float exx = this.exposure();
         if (exx > 0.0F) {
            VfxPost.grade(16769724, 0.3F * (1.0F - p) * exx);
         }

         if (this.changed > 44) {
            this.finish();
         }
      }
   }

   @Override
   public void render(VfxFrame f) {
   }

   @Override
   public void removed() {
      if (this.swell != null) {
         this.swell.end();
      }
   }
}
