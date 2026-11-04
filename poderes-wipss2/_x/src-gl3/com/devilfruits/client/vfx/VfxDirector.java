package com.devilfruits.client.vfx;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.vfx.camera.CameraDirector;
import com.devilfruits.client.vfx.particle.Ribbon;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.post.VfxPost;
import com.devilfruits.client.vfx.render.VfxRenderer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
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
public final class VfxDirector {
   private static final int MAX_CINEMATICS = 64;
   private static final Map<ResourceLocation, VfxDirector.Factory> FACTORIES = new HashMap<>();
   private static final List<Cinematic> ACTIVE = new ArrayList<>();
   private static final VfxFrame FRAME = new VfxFrame();

   public static void register(ResourceLocation effect, VfxDirector.Factory factory) {
      if (FACTORIES.putIfAbsent(effect, factory) != null) {
         throw new IllegalStateException("Duplicate cinematic effect " + effect);
      }
   }

   @Nullable
   public static Cinematic play(ResourceLocation effect, VfxAnchor anchor, VfxParams params) {
      VfxDirector.Factory factory = FACTORIES.get(effect);
      ClientLevel level = Minecraft.getInstance().level;
      if (factory != null && level != null) {
         Cinematic c = factory.create(level, anchor, params);
         if (c != null) {
            if (ACTIVE.size() >= 64) {
               ACTIVE.remove(0).removed();
            }

            ACTIVE.add(c);
         }

         return c;
      } else {
         if (factory == null) {
            DevilFruits.LOGGER.warn("No cinematic registered for {}", effect);
         }

         return null;
      }
   }

   public static <T extends Cinematic> T spawn(T c) {
      if (Minecraft.getInstance().level != null) {
         if (ACTIVE.size() >= 64) {
            ACTIVE.remove(0).removed();
         }

         ACTIVE.add(c);
      }

      return c;
   }

   @Nullable
   public static <T extends Cinematic> T find(Class<T> type, Predicate<T> filter) {
      for (Cinematic c : ACTIVE) {
         if (type.isInstance(c) && !c.isDone()) {
            T t = (T)type.cast(c);
            if (filter.test(t)) {
               return t;
            }
         }
      }

      return null;
   }

   public static boolean busy() {
      return !ACTIVE.isEmpty() || VfxParticles.count() > 0;
   }

   @SubscribeEvent
   public static void onClientTick(Post event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null) {
         if (!ACTIVE.isEmpty() || VfxParticles.count() > 0) {
            clear();
         }
      } else {
         boolean running = !mc.isPaused() && mc.level.tickRateManager().runsNormally();
         if (VfxClock.tick(running)) {
            for (int i = 0; i < ACTIVE.size(); i++) {
               Cinematic c = ACTIVE.get(i);
               if (!c.isDone()) {
                  c.tick(mc.level);
               }
            }

            ACTIVE.removeIf(cx -> {
               if (cx.isDone()) {
                  cx.removed();
                  return true;
               } else {
                  return false;
               }
            });
            VfxParticles.tick(mc.level);
            CameraDirector.tick();
            VfxPost.tick();
            VfxAudio.tick();
         }
      }
   }

   @SubscribeEvent
   public static void onRenderStage(RenderLevelStageEvent event) {
      if (event.getStage() == Stage.AFTER_PARTICLES) {
         Minecraft mc = Minecraft.getInstance();
         VfxClock.frame(event.getPartialTick().getGameTimeDeltaPartialTick(false));
         if (mc.level == null || !busy() || VfxSettings.legacy()) {
            return;
         }

         Camera camera = event.getCamera();
         Vec3 cam = camera.getPosition();
         VfxFrame f = FRAME;
         f.camera = camera;
         f.camX = cam.x;
         f.camY = cam.y;
         f.camZ = cam.z;
         f.partial = VfxClock.partial();
         f.entityPartial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
         f.time = VfxClock.time();
         f.left.set(camera.getLeftVector());
         f.up.set(camera.getUpVector());
         f.look.set(camera.getLookVector());
         VfxRenderer.render(f, event.getModelViewMatrix(), event.getProjectionMatrix(), VfxDirector::renderCinematics);
      } else if (event.getStage() == Stage.AFTER_LEVEL && Minecraft.getInstance().level != null && !VfxSettings.legacy()) {
         VfxPost.apply(event.getModelViewMatrix(), event.getProjectionMatrix());
      }
   }

   private static void renderCinematics(VfxFrame f) {
      for (Cinematic c : ACTIVE) {
         if (!c.isDone()) {
            double dx = c.x() - f.camX;
            double dy = c.y() - f.camY;
            double dz = c.z() - f.camZ;
            double cull = c.cullDistance();
            if (dx * dx + dy * dy + dz * dz <= cull * cull) {
               c.render(f);
            }
         }
      }
   }

   @SubscribeEvent
   public static void onFrameEnd(net.neoforged.neoforge.client.event.RenderFrameEvent.Post event) {
      VfxSettings.frame(busy() && Minecraft.getInstance().level != null);
   }

   @SubscribeEvent
   public static void onLogout(LoggingOut event) {
      clear();
   }

   private static void clear() {
      for (Cinematic c : ACTIVE) {
         c.removed();
      }

      ACTIVE.clear();
      VfxParticles.clear();
      Ribbon.resetBudget();
      CameraDirector.reset();
      VfxPost.reset();
      VfxAudio.reset();
      VfxClock.reset();
   }

   private VfxDirector() {
   }

   @FunctionalInterface
   public interface Factory {
      @Nullable
      Cinematic create(ClientLevel var1, VfxAnchor var2, VfxParams var3);
   }
}
