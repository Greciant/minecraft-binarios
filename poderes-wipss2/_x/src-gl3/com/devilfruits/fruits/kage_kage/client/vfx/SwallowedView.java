package com.devilfruits.fruits.kage_kage.client.vfx;

import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.camera.CameraDirector;
import com.devilfruits.client.vfx.post.VfxPost;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class SwallowedView extends KageCinematic {
   private static final int ARMS = 5;
   private static final int STEPS = 26;
   private final Player me;
   private final int length;
   private final int hold;

   public SwallowedView(Player me, int hold, long seed) {
      super(VfxAnchor.of(me, me.position()), seed);
      this.me = me;
      this.hold = Math.max(10, hold);
      this.length = 9 + this.hold + 7;
   }

   private float strength(float t) {
      return t < (float)this.length ? Mth.clamp(t / 6.0F, 0.0F, 1.0F) : Mth.clamp(1.0F - (t - (float)this.length) / 8.0F, 0.0F, 1.0F);
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (!this.me.isRemoved() && this.age <= this.length + 8) {
         float k = this.strength((float)this.age);
         VfxPost.grade(1706024, 0.6F * k);
         VfxPost.vignette(0.85F * k);
         VfxPost.desaturate(0.35F * k);
         int shut = 9;
         int step = Math.max(1, this.hold / 3);
         if (this.age >= shut && this.age < shut + this.hold && (this.age - shut) % step == 0) {
            CameraDirector.shake(0.3F, 0.1F, 2.5F, 18.0F);
            VfxPost.chromatic(0.4F);
         }

         if (this.age == this.length) {
            CameraDirector.fovPunch(0.12F);
         }
      } else {
         this.finish();
      }
   }

   @Override
   protected void drawModels(boolean glow, PoseStack ps, MultiBufferSource buffers, Vec3 cam, float partial, float entityPartial) {
      float t = (float)this.age + partial;
      float k = this.strength(t);
      if (!(k <= 0.01F) && Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
         Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
         Vector3f look = camera.getLookVector();
         Vector3f up = camera.getUpVector();
         Vector3f left = camera.getLeftVector();
         float d = 0.22F;
         Matrix4f m = ps.last().pose();
         VertexConsumer vc = buffers.getBuffer(glow ? RenderType.eyes(KageLook.white()) : RenderType.entityTranslucent(KageLook.white()));
         float turn = t * 0.16F;

         for (int a = 0; a < 5; a++) {
            for (int j = 4; j < 26; j++) {
               int h = (a * 7919 + j * 104729 + (int)(t / 3.0F) * 31) * -1640531535;
               float n1 = (float)(h >>> 8 & 0xFF) / 255.0F;
               float n2 = (float)(h >>> 16 & 0xFF) / 255.0F;
               if (!(n2 < 0.25F)) {
                  float r = (0.012F + (float)j * 0.0085F + (n1 - 0.5F) * 0.012F) * (0.6F + 0.4F * k);
                  float th = turn + (float)j * 0.3F + (float)a * (float) (Math.PI * 2) / 5.0F + (glow ? 0.22F : 0.0F) + (n2 - 0.5F) * 0.25F;
                  float s = (0.0018F + (float)j * 6.5E-4F) * (glow ? 0.6F : 1.0F);
                  float cx = Mth.cos(th) * r;
                  float cy = Mth.sin(th) * r;
                  Vector3f c = new Vector3f(look).mul(d).add(new Vector3f(left).mul(cx)).add(new Vector3f(up).mul(cy));
                  int argb;
                  if (glow) {
                     float heat = 0.35F + 0.65F * (1.0F - (float)j / 26.0F);
                     argb = 0xFF000000 | KageLook.scale(KageLook.rim(1.0F + heat * 0.5F), k * heat * 0.8F);
                  } else {
                     argb = (int)(200.0F * k * (0.35F + 0.65F * (float)j / 26.0F)) << 24 | 459788;
                  }

                  quad(vc, m, c, left, up, s, argb);
               }
            }
         }
      }
   }

   private static void quad(VertexConsumer vc, Matrix4f m, Vector3f c, Vector3f left, Vector3f up, float s, int argb) {
      float[][] corners = new float[][]{{-1.0F, -1.0F}, {-1.0F, 1.0F}, {1.0F, 1.0F}, {1.0F, -1.0F}};

      for (float[] q : corners) {
         Vector3f p = new Vector3f(c).add(new Vector3f(left).mul(q[0] * s)).add(new Vector3f(up).mul(q[1] * s));
         m.transformPosition(p);
         vc.addVertex(p.x, p.y, p.z, argb, 0.5F, 0.5F, OverlayTexture.NO_OVERLAY, 15728880, 0.0F, 0.0F, 1.0F);
      }
   }

   @Override
   public double x() {
      return this.me.getX();
   }

   @Override
   public double y() {
      return this.me.getY() + 1.0;
   }

   @Override
   public double z() {
      return this.me.getZ();
   }
}
