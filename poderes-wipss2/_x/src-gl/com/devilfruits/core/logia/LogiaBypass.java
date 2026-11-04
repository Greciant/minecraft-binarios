package com.devilfruits.core.logia;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

public final class LogiaBypass {
   private static final List<LogiaBypass.Provider> PROVIDERS = new CopyOnWriteArrayList<>();

   public static void register(LogiaBypass.Provider provider) {
      PROVIDERS.add(provider);
   }

   @Nullable
   public static LogiaBypass.Result check(ServerPlayer victim, DamageSource source) {
      for (LogiaBypass.Provider provider : PROVIDERS) {
         LogiaBypass.Result result = provider.check(victim, source);
         if (result != null) {
            return result;
         }
      }

      return null;
   }

   private LogiaBypass() {
   }

   @FunctionalInterface
   public interface Provider {
      @Nullable
      LogiaBypass.Result check(ServerPlayer var1, DamageSource var2);
   }

   public static record Result(LogiaBypassSource source, float fraction) {
   }
}
