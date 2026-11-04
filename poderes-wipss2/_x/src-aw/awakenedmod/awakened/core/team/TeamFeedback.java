package com.awakenedmod.awakened.core.team;

import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.power.AbilityFeedback;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

public final class TeamFeedback {
   private static final long COOLDOWN_TICKS = 20L;
   private static final Map<UUID, Long> SPOKEN = new ConcurrentHashMap<>();

   private TeamFeedback() {
   }

   public static void blocked(Entity attacker, Entity victim) {
      if (victim.level() instanceof ServerLevel level) {
         UUID ownerId = EntityOwners.ownerOf(attacker);
         if (ownerId != null) {
            long now = level.getGameTime();
            Long quietUntil = SPOKEN.get(ownerId);
            if (quietUntil == null || now >= quietUntil) {
               SPOKEN.put(ownerId, now + 20L);
               Fx.sound(level, victim.position(), SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 0.5F, 1.5F);
               Fx.burst(level, ParticleTypes.CRIT, victim.position().add(0.0, (double)victim.getBbHeight() * 0.6, 0.0), 4, 0.25, 0.02);
               ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
               if (owner != null) {
                  AbilityFeedback.fail(owner, "awakened.feedback.teammate");
               }
            }
         }
      }
   }

   public static void forget(UUID player) {
      SPOKEN.remove(player);
   }
}
