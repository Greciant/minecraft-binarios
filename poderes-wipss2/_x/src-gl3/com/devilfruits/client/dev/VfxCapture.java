package com.devilfruits.client.dev;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.particle.VfxParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent.Post;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(
   modid = "devilfruits",
   value = {Dist.CLIENT}
)
public final class VfxCapture {
   private static String clipName;
   private static int clipFrames;
   private static int clipIndex;
   private static double clipNext;
   private static double clipStep;
   private static String fpsLabel;
   private static long fpsStart;
   private static long fpsLast;
   private static long fpsWorst;
   private static int fpsFrames;
   private static int fpsPeakParticles;
   private static int fpsShedding;

   public static void clip(String name, int frames, double ticksPerFrame) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null) {
         clipName = name;
         clipFrames = frames;
         clipIndex = 0;
         clipStep = ticksPerFrame;
         clipNext = gameTime(mc);
         DevilFruits.LOGGER.info("[devilfruits autotest] clip {} started ({} frames)", name, frames);
      }
   }

   public static void exactWindow(int width, int height) {
      long handle = Minecraft.getInstance().getWindow().getWindow();
      GLFW.glfwSetWindowAttrib(handle, 131077, 0);
      GLFW.glfwSetWindowPos(handle, 0, 0);
      GLFW.glfwSetWindowSize(handle, width, height);
   }

   public static boolean recording() {
      return clipName != null;
   }

   public static void fps(String label) {
      fpsLabel = label;
      fpsStart = fpsLast = System.nanoTime();
      fpsWorst = 0L;
      fpsFrames = 0;
      fpsPeakParticles = 0;
      fpsShedding = 0;
   }

   public static void fpsEnd() {
      if (fpsLabel != null && fpsFrames != 0) {
         double seconds = (double)(fpsLast - fpsStart) / 1.0E9;
         Minecraft mc = Minecraft.getInstance();
         DevilFruits.LOGGER
            .info(
               "[devilfruits autotest] fps {} avg {} worst-frame {} ms (min {} fps) over {} frames ({}x{}), peak particles {}, load governor on {}% of frames",
               new Object[]{
                  fpsLabel,
                  String.format("%.1f", (double)fpsFrames / seconds),
                  String.format("%.1f", (double)fpsWorst / 1000000.0),
                  String.format("%.1f", 1.0E9 / (double)Math.max(1L, fpsWorst)),
                  fpsFrames,
                  mc.getWindow().getWidth(),
                  mc.getWindow().getHeight(),
                  fpsPeakParticles,
                  fpsShedding * 100 / Math.max(1, fpsFrames)
               }
            );
         fpsLabel = null;
      }
   }

   private static double gameTime(Minecraft mc) {
      return (double)((float)mc.level.getGameTime() + mc.getTimer().getGameTimeDeltaPartialTick(true));
   }

   @SubscribeEvent
   public static void onFrame(Post event) {
      Minecraft mc = Minecraft.getInstance();
      if (fpsLabel != null) {
         long now = System.nanoTime();
         fpsWorst = Math.max(fpsWorst, now - fpsLast);
         if (VfxSettings.underLoad()) {
            fpsShedding++;
         }

         fpsLast = now;
         fpsFrames++;
         fpsPeakParticles = Math.max(fpsPeakParticles, VfxParticles.count());
      }

      if (clipName != null && mc.level != null) {
         if (gameTime(mc) + 0.001 >= clipNext) {
            String file = String.format("%s_%04d.png", clipName, clipIndex);
            Screenshot.grab(mc.gameDirectory, file, mc.getMainRenderTarget(), message -> {
            });
            clipIndex++;
            clipNext = clipNext + clipStep;
            if (clipIndex >= clipFrames) {
               DevilFruits.LOGGER.info("[devilfruits autotest] clip {} done", clipName);
               clipName = null;
            }
         }
      }
   }

   @SubscribeEvent
   public static void onTick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
      if (CaptureRig.active()) {
         CaptureRig.tick();
      }
   }

   private VfxCapture() {
   }
}
