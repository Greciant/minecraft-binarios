package com.awakenedmod.awakened.core;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import org.slf4j.Logger;

public final class DevTools {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String CORE = DevTools.class.getPackageName();
   private static final String DEV = CORE.substring(0, CORE.lastIndexOf(46)) + ".dev.";
   private static final String COMMON = DEV + "DevBootstrap";
   private static final String CLIENT = DEV + "client.DevClientBootstrap";

   private DevTools() {
   }

   public static boolean isDevRun() {
      try {
         Class.forName(COMMON, false, DevTools.class.getClassLoader());
         return true;
      } catch (ClassNotFoundException var1) {
         return false;
      }
   }

   public static void register(IEventBus modBus, IEventBus gameBus) {
      boot(COMMON, modBus, gameBus);
   }

   public static void registerClient(IEventBus modBus, IEventBus gameBus) {
      boot(CLIENT, modBus, gameBus);
   }

   private static void boot(String className, IEventBus modBus, IEventBus gameBus) {
      Class<?> bootstrap;
      try {
         bootstrap = Class.forName(className, true, DevTools.class.getClassLoader());
      } catch (ClassNotFoundException var6) {
         return;
      }

      try {
         bootstrap.getMethod("register", IEventBus.class, IEventBus.class).invoke(null, modBus, gameBus);
      } catch (ReflectiveOperationException var5) {
         throw new IllegalStateException("Failed to register dev bootstrap " + className, var5);
      }

      LOGGER.info("Dev tools loaded ({})", bootstrap.getSimpleName());
   }
}
