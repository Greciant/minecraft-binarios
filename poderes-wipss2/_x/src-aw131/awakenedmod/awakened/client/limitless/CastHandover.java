package com.awakenedmod.awakened.client.limitless;

import com.awakenedmod.awakened.client.render.limitless.LimitlessFrame;
import com.awakenedmod.awakened.content.limitless.LimitlessCast;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.world.entity.player.Player;

final class CastHandover {
   private static final int WAIT = 4;
   private final LongSet arrived = new LongOpenHashSet();

   void clear() {
      this.arrived.clear();
   }

   void arrived(int casterId, int fireTick) {
      this.arrived.add(key(casterId, fireTick));
   }

   boolean waiting(LimitlessFrame f, Player p, LimitlessCast cast) {
      return cast.state() == 2 && f.since((long)cast.end()) < 4.0F && !this.arrived.contains(key(p.getId(), cast.end()));
   }

   private static long key(int casterId, int tick) {
      return (long)casterId << 32 | (long)tick & 4294967295L;
   }
}
