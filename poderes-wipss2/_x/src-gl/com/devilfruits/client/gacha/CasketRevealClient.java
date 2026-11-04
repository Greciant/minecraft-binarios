package com.devilfruits.client.gacha;

import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.core.gacha.CasketRevealPayload;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage;

public final class CasketRevealClient {
   private static final List<CasketRevealCinematic> ACTIVE = new ArrayList<>();

   public static void handle(CasketRevealPayload payload) {
      ClientLevel level = Minecraft.getInstance().level;
      Entity opener = level == null ? null : level.getEntity(payload.entityId());
      if (opener != null) {
         VfxDirector.spawn(new CasketRevealCinematic(level, opener, payload));
      }
   }

   static void add(CasketRevealCinematic reveal) {
      ACTIVE.add(reveal);
   }

   static void remove(CasketRevealCinematic reveal) {
      ACTIVE.remove(reveal);
   }

   static void onRenderStage(RenderLevelStageEvent event) {
      if (event.getStage() == Stage.AFTER_ENTITIES && !ACTIVE.isEmpty()) {
         PoseStack pose = event.getPoseStack();
         Vec3 camera = event.getCamera().getPosition();
         BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();

         for (CasketRevealCinematic reveal : ACTIVE) {
            if (!reveal.isDone()) {
               reveal.renderModels(pose, buffers, camera);
            }
         }

         buffers.endBatch();
      }
   }

   static void clear() {
      ACTIVE.clear();
   }

   private CasketRevealClient() {
   }
}
