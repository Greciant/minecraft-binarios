package com.awakenedmod.awakened.client.compat;

import java.lang.reflect.Method;
import net.neoforged.fml.ModList;

public final class ShaderPacks {
   private static final String MOD_ID = "iris";
   private static Object api;
   private static Method inUse;
   private static Method shadowPass;
   private static boolean resolved;
   private static boolean available;

   private ShaderPacks() {
   }

   public static boolean active() {
      if (!wire()) {
         return false;
      } else {
         try {
            return Boolean.TRUE.equals(inUse.invoke(api));
         } catch (Throwable var1) {
            return false;
         }
      }
   }

   public static boolean shadowPass() {
      if (wire() && shadowPass != null) {
         try {
            return Boolean.TRUE.equals(shadowPass.invoke(api));
         } catch (Throwable var1) {
            return false;
         }
      } else {
         return false;
      }
   }

   public static void assign(Object pipeline, String program) {
      if (wire()) {
         try {
            Class<?> programs = Class.forName("net.irisshaders.iris.api.v0.IrisProgram");
            Object value = programs.getMethod("valueOf", String.class).invoke(null, program);

            for (Method m : api.getClass().getMethods()) {
               if (m.getName().equals("assignPipeline") && m.getParameterCount() == 2) {
                  m.invoke(api, pipeline, value);
                  return;
               }
            }
         } catch (Throwable var8) {
         }
      }
   }

   private static boolean wire() {
      if (resolved) {
         return available;
      } else {
         resolved = true;
         if (!ModList.get().isLoaded("iris")) {
            return false;
         } else {
            try {
               Class<?> type = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
               api = type.getMethod("getInstance").invoke(null);
               inUse = type.getMethod("isShaderPackInUse");

               try {
                  shadowPass = type.getMethod("isRenderingShadowPass");
               } catch (NoSuchMethodException var2) {
                  shadowPass = null;
               }

               available = api != null;
            } catch (Throwable var3) {
               available = false;
            }

            return available;
         }
      }
   }
}
