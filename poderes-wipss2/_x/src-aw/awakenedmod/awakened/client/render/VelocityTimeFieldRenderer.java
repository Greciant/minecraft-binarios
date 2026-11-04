package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.velocity.VelocityPalette;
import com.awakenedmod.awakened.content.velocity.VelocityVisualState;
import com.awakenedmod.awakened.content.vfx.VfxEntity;
import com.awakenedmod.awakened.core.data.VelocityVisual;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage;

public final class VelocityTimeFieldRenderer {
   private static final int HELD_LIFETIME = 1073741823;
   private static final Map<Integer, VelocityTimeFieldRenderer.Field> FIELDS = new HashMap<>();
   private static ClientLevel fieldLevel;

   private VelocityTimeFieldRenderer() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(VelocityTimeFieldRenderer::onClientTick);
      gameBus.addListener(event -> {
         if (event.getStage() == Stage.AFTER_TRANSLUCENT_BLOCKS) {
            draw(event);
         }
      });
   }

   private static void onClientTick(Post event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != fieldLevel) {
         FIELDS.clear();
         fieldLevel = mc.level;
      }

      if (mc.level != null && !mc.isPaused()) {
         long now = mc.level.getGameTime();

         for (Player p : mc.level.players()) {
            VelocityTimeFieldRenderer.Field field = FIELDS.get(p.getId());
            VelocityVisual visual = VelocityVisualState.of(p);
            if (visual.timeless()) {
               if (field == null || field.released()) {
                  field = new VelocityTimeFieldRenderer.Field(now);
                  FIELDS.put(p.getId(), field);
               }

               field.radius = (float)visual.fieldRadius();
            } else if (field != null && !field.released()) {
               field.releaseTick = now;
            }
         }

         Iterator<Entry<Integer, VelocityTimeFieldRenderer.Field>> it = FIELDS.entrySet().iterator();

         while (it.hasNext()) {
            Entry<Integer, VelocityTimeFieldRenderer.Field> e = it.next();
            VelocityTimeFieldRenderer.Field field = e.getValue();
            boolean gone = mc.level.getEntity(e.getKey()) == null;
            if (gone || field.released() && now - field.releaseTick > 6L) {
               it.remove();
            }
         }
      }
   }

   private static void draw(RenderLevelStageEvent event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && !FIELDS.isEmpty()) {
         float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
         Vec3 cam = event.getCamera().getPosition();
         PoseStack poseStack = event.getPoseStack();
         if (poseStack != null) {
            BufferSource buffers = mc.renderBuffers().bufferSource();
            VfxDraw.Sink sink = new VfxDraw.Sink(buffers);
            float time = (float)mc.level.getGameTime() + partialTick;
            boolean drawn = false;

            for (Entry<Integer, VelocityTimeFieldRenderer.Field> e : FIELDS.entrySet()) {
               if (mc.level.getEntity(e.getKey()) instanceof Player p) {
                  VfxRenderState state = state(e.getValue(), p, time);
                  Vec3 pos = p.getPosition(partialTick);
                  poseStack.pushPose();
                  poseStack.translate(pos.x - cam.x, pos.y - cam.y, pos.z - cam.z);
                  VelocityTimeFieldDrawer.drawLive(poseStack, sink, state);
                  poseStack.popPose();
                  drawn = true;
               }
            }

            if (drawn) {
               buffers.endBatch();
            }
         }
      }
   }

   private static VfxRenderState state(VelocityTimeFieldRenderer.Field field, Player p, float time) {
      VfxRenderState state = new VfxRenderState();
      state.kind = VfxEntity.Kind.VELOCITY_TIME_FIELD;
      state.scale = field.radius;
      VelocityPalette palette = VelocityPalette.of(p);
      state.color = palette.body();
      state.color2 = palette.dark();
      state.intensity = 1.0F;
      state.delay = 0;
      state.seed = p.getId();
      state.ageInTicks = time - (float)field.startTick;
      state.lifetime = field.released() ? (int)(field.releaseTick - field.startTick) + 6 : 1073741823;
      return state;
   }

   private static final class Field {
      final long startTick;
      long releaseTick = -1L;
      float radius;

      Field(long startTick) {
         this.startTick = startTick;
      }

      boolean released() {
         return this.releaseTick >= 0L;
      }
   }
}
