package com.awakenedmod.awakened.content.command;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerPlayer;

public final class CommandTrace {
   @Nullable
   private static volatile CommandTrace.Sink sink;

   private CommandTrace() {
   }

   public static void install(CommandTrace.Sink next) {
      sink = next;
   }

   public static void say(ServerPlayer player, String line) {
      CommandTrace.Sink s = sink;
      if (s != null && player != null) {
         s.say(player, line);
      }
   }

   public static String slot(int index) {
      return "slot " + (index + 1);
   }

   static void forget(UUID playerId) {
      CommandTrace.Sink s = sink;
      if (s != null) {
         s.forget(playerId);
      }
   }

   static void forgetAll() {
      CommandTrace.Sink s = sink;
      if (s != null) {
         s.forgetAll();
      }
   }

   public interface Sink {
      void say(ServerPlayer var1, String var2);

      void forget(UUID var1);

      void forgetAll();
   }
}
