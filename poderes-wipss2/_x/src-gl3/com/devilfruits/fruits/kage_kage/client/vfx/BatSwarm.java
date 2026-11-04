package com.devilfruits.fruits.kage_kage.client.vfx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class BatSwarm {
   private static final KageModel MODEL = new KageModel("bat", "body", "ear_r", "ear_l");
   private static final float SIZE = 0.02625F;

   public static void draw(BatSwarm.Bat bat, boolean glow, PoseStack ps, MultiBufferSource buffers, Vec3 cam, float partial, int light, RimConsumer rim) {
      float s = Mth.lerp(partial, bat.prevScale, bat.scale);
      if (!(s <= 0.01F) && !(bat.alpha <= 0.01F) && MODEL.ready()) {
         Vec3 p = bat.position(partial);
         Vec3 h = bat.prevHeading.lerp(bat.heading, (double)partial);
         float yaw = (float)Mth.atan2(-h.x, -h.z);
         float pitch = (float)Math.asin(Mth.clamp(h.y / Math.max(1.0E-4, h.length()), -1.0, 1.0));
         float st = Mth.lerp(partial, bat.prevStretch, bat.stretch);
         ps.pushPose();
         ps.translate(p.x - cam.x, p.y - cam.y, p.z - cam.z);
         ps.mulPose(Axis.YP.rotation(yaw));
         ps.mulPose(Axis.XP.rotation(pitch));
         float k = 0.02625F * s;
         ps.scale(k / Mth.sqrt(st), k / Mth.sqrt(st), k * st);
         MODEL.pose();
         if (bat.swoopTime >= 0.0F) {
            MODEL.play("swoop", bat.swoopTime + partial, false);
         } else {
            MODEL.play("flap", bat.flapTime + bat.flapSpeed * partial + bat.phase, true);
         }

         float flicker = glow ? 0.85F + 0.15F * Mth.sin((bat.flapTime + bat.phase * 7.0F) * 1.7F) : 1.0F;
         MODEL.draw(glow, ps, buffers, light, bat.alpha * flicker, bat.heat, 0.018F, rim);
         ps.popPose();
      }
   }

   private BatSwarm() {
   }

   public static final class Bat {
      public Vec3 pos;
      private Vec3 prevPos;
      public Vec3 heading = new Vec3(0.0, 0.0, 1.0);
      private Vec3 prevHeading = this.heading;
      public float scale = 1.0F;
      private float prevScale = 1.0F;
      public float alpha = 1.0F;
      public float stretch = 1.0F;
      private float prevStretch = 1.0F;
      public float swoopTime = -1.0F;
      public float flapSpeed = 1.0F;
      public float heat = 1.0F;
      public final float phase;
      private float flapTime;

      public Bat(Vec3 pos, float phase) {
         this.pos = pos;
         this.prevPos = pos;
         this.phase = phase;
      }

      public void tick() {
         this.prevPos = this.pos;
         this.prevHeading = this.heading;
         this.prevScale = this.scale;
         this.prevStretch = this.stretch;
         this.flapTime = this.flapTime + this.flapSpeed;
         if (this.swoopTime >= 0.0F) {
            this.swoopTime++;
         }
      }

      public void moveTo(Vec3 to) {
         Vec3 d = to.subtract(this.pos);
         if (d.lengthSqr() > 1.0E-4) {
            this.heading = d.normalize();
         }

         this.pos = to;
      }

      public Vec3 position(float partial) {
         return this.prevPos.lerp(this.pos, (double)partial);
      }
   }
}
