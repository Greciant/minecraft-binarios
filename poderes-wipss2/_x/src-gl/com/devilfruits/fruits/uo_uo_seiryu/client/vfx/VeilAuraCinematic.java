package com.devilfruits.fruits.uo_uo_seiryu.client.vfx;

import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxClock;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.fx.BoltStrike;
import com.devilfruits.client.vfx.particle.Ribbon;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.particle.VfxPresets;
import com.devilfruits.client.vfx.render.MeshTex;
import com.devilfruits.client.vfx.render.MeshWriter;
import com.devilfruits.client.vfx.sprite.VfxSprites;
import com.devilfruits.fruits.uo_uo_seiryu.client.render.HybridAnchors;
import com.mojang.blaze3d.vertex.BufferBuilder;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class VeilAuraCinematic extends HybridCinematic {
   private static final int WISPS = 3;
   private static final int[] WISP_ANCHOR = new int[]{4, 4, 3};
   private static final float[] WISP_SIDE = new float[]{0.2F, -0.2F, 0.0F};
   private static final float[] WISP_SPEED = new float[]{0.42F, -0.38F, 0.55F};
   private final int delay;
   private final Ribbon[] wisps = new Ribbon[3];
   private final double[] head = new double[9];
   private float pulse;
   private float prevPulse;
   private int lastHurt;
   private boolean shown;

   public VeilAuraCinematic(ClientLevel level, VfxAnchor anchor, VfxParams params) {
      super(level, anchor, params);
      this.delay = Math.max(0, params.extra());

      for (int i = 0; i < 3; i++) {
         this.wisps[i] = Ribbon.create(14);
         if (this.wisps[i] != null) {
            this.wisps[i].look(0.11F * this.height / 3.6F + 0.02F, 9.0F, 1.35F, 0.6F, 3.0F, 1).texture(MeshTex.FLAME_RIBBON, 0.16F);
         }
      }
   }

   @Nullable
   public static VeilAuraCinematic of(Entity e) {
      return VfxDirector.find(VeilAuraCinematic.class, a -> a.follows(e));
   }

   public static float sheen(Entity e, float partial) {
      VeilAuraCinematic a = of(e);
      if (a != null && a.shown) {
         float p = Mth.lerp(partial, a.prevPulse, a.pulse);
         return 0.22F + 0.08F * Mth.sin(((float)a.age + partial) * 0.3F) + 0.7F * p;
      } else {
         return 0.0F;
      }
   }

   public static void pulseOf(@Nullable Entity e) {
      VeilAuraCinematic a = e != null ? of(e) : null;
      if (a != null) {
         a.pulse();
      }
   }

   public void pulse() {
      if (this.shown) {
         this.pulse = 1.0F;
         double cy = this.y + (double)this.height * 0.55;

         for (int i = 0; i < VfxSettings.count(10.0F); i++) {
            VfxParticles.emit(
               VfxPresets.THUNDER_SPARK,
               this.x + this.spread(0.4),
               cy + this.spread(0.6),
               this.z + this.spread(0.4),
               this.spread(0.35),
               0.1 + this.random.nextDouble() * 0.2,
               this.spread(0.35)
            );
         }

         VfxParticles.emit(VfxPresets.GLOW, this.x, cy, this.z, 0.0, 0.0, 0.0).size(this.height * 1.4F).tint(11955455).alpha(0.5F).life(1.5F);
         this.arc(0.9F, 0.09F);
      }
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      Entity e = this.caster(level);
      if (e != null && VeilAuraEffect.veiled(e) && !VfxSettings.legacy()) {
         this.prevPulse = this.pulse;
         this.pulse = Math.max(0.0F, this.pulse - 0.1F);
         if (this.age >= this.delay) {
            this.shown = true;
            if (e instanceof LivingEntity living) {
               if (living.hurtTime > 0 && this.lastHurt == 0) {
                  this.pulse();
               }

               this.lastHurt = living.hurtTime;
            }

            boolean fp = this.firstPersonCaster();
            double floor = this.onFloor(2.5) ? this.groundY : this.y;

            for (int i = 0; i < VfxSettings.count(1.3F + this.pulse * 3.0F); i++) {
               double a = this.random.nextDouble() * (float) (Math.PI * 2);
               double d = (double)this.height * (0.14 + this.random.nextDouble() * 0.16);
               VfxParticles.emit(
                  VfxPresets.THUNDER_MOTE,
                  this.x + Math.cos(a) * d,
                  floor + 0.05 + this.random.nextDouble() * 0.3,
                  this.z + Math.sin(a) * d,
                  this.spread(0.02),
                  0.03 + this.random.nextDouble() * 0.05,
                  this.spread(0.02)
               );
            }

            if (!fp) {
               float t = VfxClock.time();
               float yaw = (e instanceof LivingEntity l ? l.yBodyRot : e.getYRot()) * (float) (Math.PI / 180.0);

               for (int i = 0; i < 3; i++) {
                  HybridAnchors.get(e, WISP_ANCHOR[i], 1.0F, this.anchor3);
                  double side = (double)(WISP_SIDE[i] * this.height);
                  float a = t * WISP_SPEED[i] + (float)i * 2.1F;
                  float rr = this.height * (WISP_ANCHOR[i] == 3 ? 0.12F : 0.1F);
                  double hx = this.anchor3.x - (double)Mth.cos(yaw) * side + (double)(Mth.cos(a) * rr);
                  double hy = this.anchor3.y + (WISP_ANCHOR[i] == 3 ? -0.02 : 0.06) * (double)this.height + (double)(Mth.sin(a * 1.7F) * rr) * 0.5;
                  double hz = this.anchor3.z - (double)Mth.sin(yaw) * side + (double)(Mth.sin(a) * rr);
                  this.head[i * 3] = hx;
                  this.head[i * 3 + 1] = hy;
                  this.head[i * 3 + 2] = hz;
                  if (this.wisps[i] != null) {
                     this.wisps[i].push(hx, hy, hz, t);
                  }
               }

               if (this.age % 7 == 3 && this.random.nextFloat() < 0.8F) {
                  this.arc(0.45F + this.random.nextFloat() * 0.35F, 0.05F);
               }
            }
         }
      } else {
         this.finish();
      }
   }

   private void arc(float length, float width) {
      int i = this.random.nextInt(3);
      Vec3 from = new Vec3(this.head[i * 3], this.head[i * 3 + 1], this.head[i * 3 + 2]);
      if (from.lengthSqr() != 0.0) {
         double a = this.random.nextDouble() * (float) (Math.PI * 2);
         BoltStrike.between(
               from,
               from.add(Math.cos(a) * (double)length, (this.random.nextDouble() - 0.2) * (double)length, Math.sin(a) * (double)length),
               this.seed + (long)this.age * 131L
            )
            .width(width)
            .life(3)
            .forks(1)
            .rough(0.25F)
            .endGlow(0.4F)
            .spawn();
      }
   }

   @Override
   public void render(VfxFrame f) {
      if (this.shown && !this.firstPersonCaster()) {
         float fade = Math.min(1.0F, ((float)(this.age - this.delay) + f.partial) / 6.0F);

         for (int i = 0; i < 3; i++) {
            if (this.wisps[i] != null) {
               this.wisps[i].render(f, this.head[i * 3], this.head[i * 3 + 1], this.head[i * 3 + 2], fade * (0.75F + 0.25F * this.pulse));
            }
         }

         BufferBuilder d = f.distortion();
         if (d != null) {
            VfxSprites.Sprite glow = VfxSprites.GLOW;
            float p = f.entityPartial;
            MeshWriter.distortQuad(
               d,
               f.rx(this.rx(p)),
               f.ry(this.ry(p) + (double)this.height * 0.55),
               f.rz(this.rz(p)),
               this.height * 0.42F,
               0.22F + 0.3F * this.pulse,
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
               f.time * 0.004F,
               0.7F
            );
         }
      }
   }

   @Override
   public void removed() {
      for (Ribbon r : this.wisps) {
         if (r != null) {
            r.release();
         }
      }
   }
}
