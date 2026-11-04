package com.devilfruits.client.render.vfx;

import com.devilfruits.client.render.CameraEffects;
import com.devilfruits.client.render.VfxRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage;

@EventBusSubscriber(
   modid = "devilfruits",
   value = {Dist.CLIENT}
)
public final class VfxManager {
   private static final int MAX_EFFECTS = 160;
   private static final List<VfxEffect> EFFECTS = new ArrayList<>();
   private static final VfxContext CONTEXT = new VfxContext();

   public static <T extends VfxEffect> T add(T effect) {
      if (EFFECTS.size() >= 160) {
         EFFECTS.remove(0).removed();
      }

      EFFECTS.add(effect);
      return effect;
   }

   @Nullable
   public static <T extends VfxEffect> T find(Class<T> type, Predicate<T> filter) {
      for (VfxEffect effect : EFFECTS) {
         if (type.isInstance(effect) && !effect.isDone()) {
            T t = (T)type.cast(effect);
            if (filter.test(t)) {
               return t;
            }
         }
      }

      return null;
   }

   @SubscribeEvent
   public static void onClientTick(Post event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null) {
         clear();
      } else if (!mc.isPaused()) {
         for (int i = 0; i < EFFECTS.size(); i++) {
            VfxEffect effect = EFFECTS.get(i);
            if (!effect.isDone()) {
               effect.tick(mc.level);
            }
         }

         EFFECTS.removeIf(effectx -> {
            if (effectx.isDone()) {
               effectx.removed();
               return true;
            } else {
               return false;
            }
         });
         CameraEffects.tick();
      }
   }

   @SubscribeEvent
   public static void onRenderLevel(RenderLevelStageEvent event) {
      if (event.getStage() == Stage.AFTER_PARTICLES && !EFFECTS.isEmpty()) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.level != null) {
            Camera camera = event.getCamera();
            Vec3 cam = camera.getPosition();
            PoseStack poseStack = event.getPoseStack();
            float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
            VfxContext ctx = CONTEXT;
            ctx.matrix = poseStack.last().pose();
            ctx.camera = camera;
            ctx.camX = cam.x;
            ctx.camY = cam.y;
            ctx.camZ = cam.z;
            ctx.partialTick = partial;
            ctx.time = (float)mc.level.getGameTime() + partial;
            ctx.camLeft.set(camera.getLeftVector());
            ctx.camUp.set(camera.getUpVector());
            ctx.camLook.set(camera.getLookVector());
            BufferSource buffers = mc.renderBuffers().bufferSource();
            ctx.buffer = buffers.getBuffer(VfxRenderTypes.SHELL);

            for (VfxEffect effect : EFFECTS) {
               if (visible(effect, cam)) {
                  effect.renderShell(ctx);
               }
            }

            buffers.endBatch(VfxRenderTypes.SHELL);
            ctx.buffer = buffers.getBuffer(VfxRenderTypes.GLOW);

            for (VfxEffect effectx : EFFECTS) {
               if (visible(effectx, cam)) {
                  effectx.renderGlow(ctx);
               }
            }

            buffers.endBatch(VfxRenderTypes.GLOW);
            ctx.buffer = null;
            ctx.buffers = buffers;

            for (VfxEffect effectxx : EFFECTS) {
               if (visible(effectxx, cam)) {
                  effectxx.renderTextured(ctx);
               }
            }

            buffers.endBatch();
            ctx.buffers = null;
         }
      }
   }

   private static boolean visible(VfxEffect effect, Vec3 cam) {
      if (effect.isDone()) {
         return false;
      } else {
         double dx = effect.x() - cam.x;
         double dy = effect.y() - cam.y;
         double dz = effect.z() - cam.z;
         double cull = effect.cullDistance();
         return dx * dx + dy * dy + dz * dz <= cull * cull;
      }
   }

   @SubscribeEvent
   public static void onLogout(LoggingOut event) {
      clear();
      CameraEffects.reset();
   }

   private static void clear() {
      for (VfxEffect effect : EFFECTS) {
         effect.removed();
      }

      EFFECTS.clear();
   }

   private VfxManager() {
   }
}
