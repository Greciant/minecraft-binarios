package com.awakenedmod.awakened.content.command.vocab.trigger;

import com.awakenedmod.awakened.content.command.vocab.Trigger;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public abstract class RecentEventTrigger implements Trigger {
   private final Map<UUID, Long> last = new ConcurrentHashMap<>();
   private final int windowTicks;

   protected RecentEventTrigger(int windowTicks) {
      this.windowTicks = windowTicks;
   }

   protected final void mark(ServerPlayer caster) {
      this.last.put(caster.getUUID(), caster.level().getGameTime());
   }

   @Override
   public boolean live(ServerPlayer caster) {
      Long at = this.last.get(caster.getUUID());
      if (at == null) {
         return false;
      } else {
         long since = caster.level().getGameTime() - at;
         return since >= 0L && since <= (long)this.windowTicks;
      }
   }

   @Override
   public boolean liveOnClient(Player caster) {
      return false;
   }

   @Override
   public void forget(UUID casterId) {
      this.last.remove(casterId);
   }

   @Override
   public void forgetAll() {
      this.last.clear();
   }
}
