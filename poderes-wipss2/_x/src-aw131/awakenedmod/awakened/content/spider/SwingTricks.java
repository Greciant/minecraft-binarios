package com.awakenedmod.awakened.content.spider;

import com.awakenedmod.awakened.core.PowerService;
import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

final class SwingTricks {
   private static final int LATENCY_SLACK = 2;
   private final Map<UUID, SwingTricks.Run> runs;

   SwingTricks(Map<UUID, SwingTricks.Run> runs) {
      this.runs = runs;
   }

   void begin(ServerPlayer player) {
      this.runs.put(player.getUUID(), new SwingTricks.Run(player.hurtTime));
   }

   void start(ServerPlayer player, SpiderTrick trick) {
      SwingTricks.Run run = this.runs.get(player.getUUID());
      long now = player.serverLevel().getGameTime();
      long clientNow = now + 2L;
      if (run != null && !player.onGround() && !run.busy(clientNow)) {
         run.settle(clientNow);
         run.current = trick;
         run.currentStart = now;
         PowerService.playAnimation(player, trick.animation());
         SpiderFx.trick(player.serverLevel(), player.position().add(0.0, (double)player.getBbHeight() * 0.5, 0.0));
      }
   }

   void cancelCurrent(ServerPlayer player) {
      SwingTricks.Run run = this.runs.get(player.getUUID());
      if (run != null) {
         run.current = null;
      }
   }

   void tick(ServerPlayer player) {
      SwingTricks.Run run = this.runs.get(player.getUUID());
      if (run != null) {
         boolean hit = player.hurtTime > run.lastHurtTime;
         run.lastHurtTime = player.hurtTime;
         if (!hit && !player.onGround() && !player.isInWater() && !WebWingsAbility.isGliding(player)) {
            run.settle(player.serverLevel().getGameTime());
         } else {
            this.runs.remove(player.getUUID());
         }
      }
   }

   int cashIn(ServerPlayer player) {
      SwingTricks.Run run = this.runs.remove(player.getUUID());
      if (run == null) {
         return 0;
      } else {
         run.settle(player.serverLevel().getGameTime());
         return Math.min(run.landed.size(), 3);
      }
   }

   static final class Run {
      final EnumSet<SpiderTrick> landed = EnumSet.noneOf(SpiderTrick.class);
      SpiderTrick current;
      long currentStart;
      int lastHurtTime;

      Run(int hurtTime) {
         this.lastHurtTime = hurtTime;
      }

      boolean busy(long now) {
         return this.current != null && now - this.currentStart < 9L;
      }

      void settle(long now) {
         if (this.current != null && !this.busy(now)) {
            this.landed.add(this.current);
            this.current = null;
         }
      }
   }
}
