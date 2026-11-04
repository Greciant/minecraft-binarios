package com.devilfruits.client.vfx;

import com.devilfruits.DevilFruits;
import com.devilfruits.core.config.CoreClientConfig;
import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.util.RandomSource;
import net.neoforged.fml.ModList;

public final class VfxSettings {
   private static final RandomSource RANDOM = RandomSource.create();
   private static Method irisInstance;
   private static Method irisPackInUse;
   private static boolean irisChecked;
   private static boolean shaderPack;
   private static int shaderPackAge = 1000;
   private static float frameMs = 16.0F;
   private static boolean underLoad;
   private static long lastFrame;

   public static CoreClientConfig.CinematicQuality tier() {
      return shaderPackActive() ? CoreClientConfig.CinematicQuality.LOW : (CoreClientConfig.CinematicQuality)CoreClientConfig.CINEMATIC_QUALITY.get();
   }

   public static boolean legacy() {
      return tier() == CoreClientConfig.CinematicQuality.LOW;
   }

   public static boolean post() {
      CoreClientConfig.CinematicQuality t = tier();
      return t == CoreClientConfig.CinematicQuality.ULTRA || t == CoreClientConfig.CinematicQuality.HIGH;
   }

   public static boolean distortion() {
      return tier() == CoreClientConfig.CinematicQuality.ULTRA;
   }

   public static boolean lensEffects() {
      return tier() == CoreClientConfig.CinematicQuality.ULTRA;
   }

   public static boolean soft() {
      return post();
   }

   public static int bloomMips() {
      int mips = switch (tier()) {
         case ULTRA -> 5;
         case HIGH -> 3;
         default -> 0;
      };
      return underLoad ? Math.min(mips, 3) : mips;
   }

   public static void frame(boolean busy) {
      long now = System.nanoTime();
      float ms = lastFrame == 0L ? 16.0F : Math.min(250.0F, (float)(now - lastFrame) / 1000000.0F);
      lastFrame = now;
      if (busy && (Boolean)CoreClientConfig.ADAPTIVE_LOAD.get()) {
         frameMs = frameMs + (ms - frameMs) * 0.08F;
         if (!underLoad && frameMs > 40.0F) {
            underLoad = true;
         } else if (underLoad && frameMs < 28.0F) {
            underLoad = false;
         }
      } else {
         frameMs = 16.0F;
         underLoad = false;
      }
   }

   public static boolean underLoad() {
      return underLoad;
   }

   public static float bloomStrength() {
      return ((Double)CoreClientConfig.BLOOM_STRENGTH.get()).floatValue();
   }

   public static boolean reduceFlashing() {
      return (Boolean)CoreClientConfig.REDUCE_FLASHING.get();
   }

   public static float flashCap() {
      return reduceFlashing() ? 0.3F : 0.6F;
   }

   public static boolean hitFlashes() {
      return !reduceFlashing();
   }

   public static int maxParticles() {
      return (Integer)CoreClientConfig.MAX_PARTICLES.get();
   }

   public static int maxRibbonPoints() {
      return (Integer)CoreClientConfig.MAX_RIBBON_POINTS.get();
   }

   public static int maxMeshes() {
      return (Integer)CoreClientConfig.MAX_MESHES.get();
   }

   public static float spawnScale() {
      ParticleStatus status = (ParticleStatus)Minecraft.getInstance().options.particles().get();
      float scale = status == ParticleStatus.DECREASED ? 0.5F : (status == ParticleStatus.MINIMAL ? 0.2F : 1.0F);
      scale = underLoad ? scale * 0.6F : scale;
      return tier() == CoreClientConfig.CinematicQuality.MEDIUM ? scale * 0.7F : scale;
   }

   public static int count(float nominal) {
      float exact = nominal * spawnScale();
      int whole = (int)exact;
      return whole + (RANDOM.nextFloat() < exact - (float)whole ? 1 : 0);
   }

   public static RandomSource random() {
      return RANDOM;
   }

   public static boolean shaderPackActive() {
      if (!irisChecked) {
         irisChecked = true;
         if (ModList.get() != null && ModList.get().isLoaded("iris")) {
            try {
               Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
               irisInstance = api.getMethod("getInstance");
               irisPackInUse = api.getMethod("isShaderPackInUse");
            } catch (LinkageError | ReflectiveOperationException var2) {
               DevilFruits.LOGGER.warn("Iris found but its API is not readable; cinematic effects stay on", var2);
            }
         }
      }

      if (irisPackInUse == null) {
         return false;
      } else {
         if (++shaderPackAge >= 40) {
            shaderPackAge = 0;

            try {
               shaderPack = (Boolean)irisPackInUse.invoke(irisInstance.invoke(null));
            } catch (RuntimeException | ReflectiveOperationException var1) {
               shaderPack = false;
            }
         }

         return shaderPack;
      }
   }

   private VfxSettings() {
   }
}
