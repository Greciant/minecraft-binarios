package com.devilfruits.fruits.kage_kage.client.vfx;

import com.devilfruits.client.vfx.VfxClock;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage;

public final class KageRender {
   private static final List<KageRender.Drawable> ACTIVE = new ArrayList<>();

   public static void add(KageRender.Drawable d) {
      if (!ACTIVE.contains(d)) {
         ACTIVE.add(d);
      }
   }

   public static void remove(KageRender.Drawable d) {
      ACTIVE.remove(d);
   }

   public static void clear() {
      ACTIVE.clear();
   }

   public static void onRenderStage(RenderLevelStageEvent event) {
      if (event.getStage() == Stage.AFTER_ENTITIES && !ACTIVE.isEmpty()) {
         PoseStack ps = event.getPoseStack();
         Vec3 cam = event.getCamera().getPosition();
         float partial = VfxClock.partial();
         float entityPartial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
         BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();

         for (KageRender.Drawable d : List.copyOf(ACTIVE)) {
            d.draw(false, ps, buffers, cam, partial, entityPartial);
         }

         buffers.endBatch();

         for (KageRender.Drawable d : List.copyOf(ACTIVE)) {
            d.draw(true, ps, buffers, cam, partial, entityPartial);
         }

         buffers.endBatch();
      }
   }

   private KageRender() {
   }

   public interface Drawable {
      void draw(boolean var1, PoseStack var2, MultiBufferSource var3, Vec3 var4, float var5, float var6);
   }
}
