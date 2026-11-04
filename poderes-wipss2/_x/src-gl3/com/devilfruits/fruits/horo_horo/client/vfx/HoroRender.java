package com.devilfruits.fruits.horo_horo.client.vfx;

import com.devilfruits.client.vfx.VfxClock;
import com.devilfruits.client.vfx.sprite.VfxSprites;
import com.devilfruits.fruits.horo_horo.negative.Negative;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage;
import org.joml.Matrix4f;

public final class HoroRender {
   private static final List<HoroRender.Drawable> ACTIVE = new ArrayList<>();

   public static void add(HoroRender.Drawable d) {
      if (!ACTIVE.contains(d)) {
         ACTIVE.add(d);
      }
   }

   public static void remove(HoroRender.Drawable d) {
      ACTIVE.remove(d);
   }

   public static void clear() {
      ACTIVE.clear();
   }

   public static float gloom(Entity e, float partial) {
      if (e instanceof LivingEntity && e.hasData(Negative.VIEW)) {
         Negative.View v = Negative.view(e);
         float now = (float)e.level().getGameTime() + partial;
         return (float)v.until() <= now
            ? 0.0F
            : Mth.clamp((now - (float)v.since()) / 8.0F, 0.0F, 1.0F) * Mth.clamp(((float)v.until() - now) / 10.0F, 0.0F, 1.0F);
      } else {
         return 0.0F;
      }
   }

   public static void onRenderStage(RenderLevelStageEvent event) {
      if (event.getStage() == Stage.AFTER_ENTITIES) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.level != null) {
            PoseStack ps = event.getPoseStack();
            Camera camera = event.getCamera();
            Vec3 cam = camera.getPosition();
            BufferSource buffers = mc.renderBuffers().bufferSource();
            if (!ACTIVE.isEmpty()) {
               float partial = VfxClock.partial();

               for (HoroRender.Drawable d : List.copyOf(ACTIVE)) {
                  d.draw(ps, buffers, cam, partial);
               }

               buffers.endBatch();
            }

            gloomClouds(ps, buffers, camera, event.getPartialTick().getGameTimeDeltaPartialTick(false));
         }
      }
   }

   private static void gloomClouds(PoseStack ps, BufferSource buffers, Camera camera, float partial) {
      Minecraft mc = Minecraft.getInstance();
      Vec3 cam = camera.getPosition();
      RenderType type = RenderType.entityTranslucent(VfxSprites.ATLAS);
      VertexConsumer vc = null;
      long time = mc.level.getGameTime();

      for (Entity e : mc.level.entitiesForRendering()) {
         if (!e.isInvisible() && !(e.distanceToSqr(cam) > 2304.0) && (e != mc.player || !mc.options.getCameraType().isFirstPerson())) {
            float k = gloom(e, partial);
            if (!(k <= 0.01F)) {
               if (vc == null) {
                  vc = buffers.getBuffer(type);
               }

               Vec3 at = e.getPosition(partial);
               float h = e.getBbHeight();
               float w = Math.max(0.95F, e.getBbWidth() * 1.7F);
               double dx = cam.x - at.x;
               double dz = cam.z - at.z;
               float yaw = (float)Mth.atan2(dx, dz);
               int light = LevelRenderer.getLightColor(mc.level, BlockPos.containing(at.x, at.y + (double)h, at.z));
               float t = (float)time + partial + (float)(e.getId() * 7);
               Vec3 top = bowed(e, h, partial, k);
               float cloudY = 0.5F + 0.05F * Mth.sin(t * 0.12F) + (1.0F - k) * 0.35F;
               int frame = (int)((time + (long)e.getId()) / 5L) % VfxSprites.HORO_GLOOM.frames();
               float grow = 0.85F + 0.15F * k;
               ps.pushPose();
               ps.translate(at.x + top.x - cam.x, at.y + top.y + (double)cloudY + (double)(w * 0.25F * grow) - cam.y, at.z + top.z - cam.z);
               ps.mulPose(Axis.YP.rotation(yaw));
               quad(vc, ps.last().pose(), VfxSprites.HORO_GLOOM, frame, w * grow, w * 0.5F * grow, 0.0F, light, k);
               ps.popPose();
               Vec3 eye = bowed(e, e.getEyeHeight(), partial, k);
               float lw = Math.max(0.5F, e.getBbWidth() * 0.95F);
               ps.pushPose();
               ps.translate(at.x + eye.x - cam.x, at.y + eye.y - cam.y, at.z + eye.z - cam.z);
               ps.mulPose(Axis.YP.rotation(yaw));
               ps.translate(0.0F, 0.0F, e.getBbWidth() * 0.5F + 0.04F);
               quad(vc, ps.last().pose(), VfxSprites.HORO_LINES, 0, lw, lw, lw * 0.1F, light, k * 0.9F);
               ps.popPose();
            }
         }
      }

      if (vc != null) {
         buffers.endBatch(type);
      }
   }

   public static Vec3 bowed(Entity e, float y, float partial, float k) {
      float yaw = (e instanceof LivingEntity l ? Mth.rotLerp(partial, l.yBodyRotO, l.yBodyRot) : e.getYRot()) * (float) (Math.PI / 180.0);
      double fx = (double)(-Mth.sin(yaw));
      double fz = (double)Mth.cos(yaw);
      if (e instanceof Player) {
         return new Vec3(fx * 0.3 * (double)k, (double)y - 0.6 * (double)k, fz * 0.3 * (double)k);
      } else {
         float bow = 26.0F * k * (float) (Math.PI / 180.0);
         double forward = (double)(y * Mth.sin(bow));
         return new Vec3(fx * forward, (double)(y * Mth.cos(bow)) - 0.16 * (double)e.getBbHeight() * (double)k, fz * forward);
      }
   }

   private static void quad(VertexConsumer vc, Matrix4f m, VfxSprites.Sprite sprite, int frame, float w, float h, float dy, int light, float alpha) {
      float u0 = sprite.frameU(frame);
      float v0 = sprite.frameV(frame);
      float u1 = u0 + sprite.cellU();
      float v1 = v0 + sprite.cellV();
      float x = w * 0.5F;
      float y = h * 0.5F;
      int a = Mth.clamp(Math.round(alpha * 255.0F), 0, 255);
      vc.addVertex(m, -x, -y - dy, 0.0F)
         .setColor(255, 255, 255, a)
         .setUv(u0, v1)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(light)
         .setNormal(0.0F, 0.0F, 1.0F);
      vc.addVertex(m, x, -y - dy, 0.0F)
         .setColor(255, 255, 255, a)
         .setUv(u1, v1)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(light)
         .setNormal(0.0F, 0.0F, 1.0F);
      vc.addVertex(m, x, y - dy, 0.0F)
         .setColor(255, 255, 255, a)
         .setUv(u1, v0)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(light)
         .setNormal(0.0F, 0.0F, 1.0F);
      vc.addVertex(m, -x, y - dy, 0.0F)
         .setColor(255, 255, 255, a)
         .setUv(u0, v0)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(light)
         .setNormal(0.0F, 0.0F, 1.0F);
   }

   private HoroRender() {
   }

   public interface Drawable {
      void draw(PoseStack var1, MultiBufferSource var2, Vec3 var3, float var4);
   }
}
