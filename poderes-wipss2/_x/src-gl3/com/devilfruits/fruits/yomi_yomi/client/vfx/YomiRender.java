package com.devilfruits.fruits.yomi_yomi.client.vfx;

import com.devilfruits.client.render.VfxRenderTypes;
import com.devilfruits.client.vfx.VfxClock;
import com.devilfruits.client.vfx.sprite.VfxSprites;
import com.devilfruits.fruits.yomi_yomi.client.SoulKingGhost;
import com.devilfruits.fruits.yomi_yomi.frost.YomiFrost;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage;
import org.joml.Matrix4f;

public final class YomiRender {
   private static final List<YomiRender.Drawable> ACTIVE = new ArrayList<>();

   public static void add(YomiRender.Drawable d) {
      if (!ACTIVE.contains(d)) {
         ACTIVE.add(d);
      }
   }

   public static void remove(YomiRender.Drawable d) {
      ACTIVE.remove(d);
   }

   public static void clear() {
      ACTIVE.clear();
   }

   public static void onRenderStage(RenderLevelStageEvent event) {
      if (event.getStage() == Stage.AFTER_ENTITIES) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.level != null) {
            PoseStack ps = event.getPoseStack();
            Camera camera = event.getCamera();
            Vec3 cam = camera.getPosition();
            BufferSource buffers = mc.renderBuffers().bufferSource();
            SoulKingGhost.draw(ps, buffers, cam, VfxClock.partial());
            buffers.endBatch();
            if (!ACTIVE.isEmpty()) {
               float partial = VfxClock.partial();

               for (YomiRender.Drawable d : List.copyOf(ACTIVE)) {
                  d.draw(ps, buffers, cam, partial);
               }

               buffers.endBatch();
            }

            pips(ps, buffers, camera, event.getPartialTick().getGameTimeDeltaPartialTick(false));
         }
      }
   }

   private static void pips(PoseStack ps, BufferSource buffers, Camera camera, float partial) {
      Minecraft mc = Minecraft.getInstance();
      long now = mc.level.getGameTime();
      Vec3 cam = camera.getPosition();
      VertexConsumer vc = null;

      for (Entity e : mc.level.entitiesForRendering()) {
         if (e instanceof LivingEntity
            && e.hasData(YomiFrost.TYPE)
            && !e.isInvisible()
            && !(e.distanceToSqr(cam) > 1600.0)
            && (e != mc.player || !mc.options.getCameraType().isFirstPerson())) {
            int chill = YomiFrost.get(e).chillAt(now);
            if (chill > 0) {
               if (vc == null) {
                  vc = buffers.getBuffer(VfxRenderTypes.shadeTextured(VfxSprites.YOMI_CHILL_PIP));
               }

               Vec3 at = e.getPosition(partial).add(0.0, (double)e.getBbHeight() + 0.45 + (!e.hasCustomName() && !(e instanceof Player) ? 0.0 : 0.3), 0.0);
               ps.pushPose();
               ps.translate(at.x - cam.x, at.y - cam.y, at.z - cam.z);
               ps.mulPose(camera.rotation());
               Matrix4f m = ps.last().pose();
               float size = 0.13F;
               float gap = 0.3F;

               for (int i = 0; i < 4; i++) {
                  float x = ((float)i - 1.5F) * gap;
                  boolean full = i < chill;
                  float pulse = full && chill == 3 ? 0.75F + 0.25F * (float)Math.sin((double)(((float)now + partial) * 0.8F)) : 1.0F;
                  int a = (int)((float)(full ? 255 : 110) * pulse);
                  int g = full ? 255 : 150;
                  float s = full ? size : size * 0.7F;
                  vc.addVertex(m, x - s, -s, 0.0F).setUv(0.0F, 1.0F).setColor(g, 255, 255, a);
                  vc.addVertex(m, x + s, -s, 0.0F).setUv(1.0F, 1.0F).setColor(g, 255, 255, a);
                  vc.addVertex(m, x + s, s, 0.0F).setUv(1.0F, 0.0F).setColor(g, 255, 255, a);
                  vc.addVertex(m, x - s, s, 0.0F).setUv(0.0F, 0.0F).setColor(g, 255, 255, a);
               }

               ps.popPose();
            }
         }
      }

      if (vc != null) {
         buffers.endBatch(VfxRenderTypes.shadeTextured(VfxSprites.YOMI_CHILL_PIP));
      }
   }

   private YomiRender() {
   }

   public interface Drawable {
      void draw(PoseStack var1, MultiBufferSource var2, Vec3 var3, float var4);
   }
}
