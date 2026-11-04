package com.devilfruits.fruits.horo_horo.client.vfx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class HollowGhost {
   private final HoroModel model;
   private final float unit;
   private final float centre;
   public Vec3 pos;
   private Vec3 prevPos;
   public Vec3 facing = new Vec3(0.0, 0.0, 1.0);
   private Vec3 prevFacing = this.facing;
   public float scale = 1.0F;
   private float prevScale = 1.0F;
   public float alpha = 0.92F;
   public float lean;
   private float prevLean;
   public final float phase;
   private String clip = "fly";
   private boolean loop = true;
   private float clipTime;
   private String lastClip;
   private boolean lastLoop;
   private float lastTime;
   private float blend = 1.0F;

   public HollowGhost(HoroModel model, float unit, float centre, Vec3 pos, float phase) {
      this.model = model;
      this.unit = unit;
      this.centre = centre;
      this.pos = pos;
      this.prevPos = pos;
      this.phase = phase;
   }

   public void tick() {
      this.prevPos = this.pos;
      this.prevFacing = this.facing;
      this.prevScale = this.scale;
      this.prevLean = this.lean;
      this.clipTime++;
      this.lastTime++;
      this.blend = Math.min(1.0F, this.blend + 0.34F);
   }

   public void play(String clip, boolean loop) {
      if (!clip.equals(this.clip)) {
         this.lastClip = this.clip;
         this.lastLoop = this.loop;
         this.lastTime = this.clipTime;
         this.clip = clip;
         this.loop = loop;
         this.clipTime = 0.0F;
         this.blend = 0.0F;
      }
   }

   public String clip() {
      return this.clip;
   }

   public float clipTime() {
      return this.clipTime;
   }

   public void moveTo(Vec3 to) {
      Vec3 d = to.subtract(this.pos);
      Vec3 flat = new Vec3(d.x, 0.0, d.z);
      if (flat.lengthSqr() > 1.0E-4) {
         this.facing = flat.normalize();
      }

      this.lean = Mth.clamp((float)flat.length() * 0.55F, 0.0F, 0.6F);
      this.pos = to;
   }

   public void face(Vec3 dir) {
      Vec3 flat = new Vec3(dir.x, 0.0, dir.z);
      if (flat.lengthSqr() > 1.0E-6) {
         this.facing = flat.normalize();
      }
   }

   public Vec3 position(float partial) {
      return this.prevPos.lerp(this.pos, (double)partial);
   }

   public void draw(PoseStack ps, MultiBufferSource buffers, Vec3 cam, float partial) {
      float s = Mth.lerp(partial, this.prevScale, this.scale);
      if (!(s <= 0.01F) && !(this.alpha <= 0.01F) && this.model.ready()) {
         Vec3 p = this.position(partial);
         Vec3 f = this.prevFacing.lerp(this.facing, (double)partial);
         float yaw = (float)Mth.atan2(-f.x, -f.z);
         ps.pushPose();
         ps.translate(p.x - cam.x, p.y - cam.y, p.z - cam.z);
         ps.mulPose(Axis.YP.rotation(yaw));
         ps.mulPose(Axis.XP.rotation(-Mth.lerp(partial, this.prevLean, this.lean)));
         float k = this.unit * s;
         ps.scale(k, k, k);
         ps.translate(0.0F, -this.centre, 0.0F);
         this.model.pose();
         float t = this.clipTime + partial;
         if (this.blend < 1.0F && this.lastClip != null) {
            float b = Mth.clamp(this.blend + partial * 0.34F, 0.0F, 1.0F);
            this.model.play(this.lastClip, this.lastTime + partial + this.phase, this.lastLoop, 1.0F - b);
            this.model.play(this.clip, t + (this.loop ? this.phase : 0.0F), this.loop, b);
         } else {
            this.model.play(this.clip, t + (this.loop ? this.phase : 0.0F), this.loop, 1.0F);
         }

         this.model.drawGhost(ps, buffers, this.alpha);
         ps.popPose();
      }
   }
}
