package com.awakenedmod.awakened.content.regenerator;

import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public final class FrenzyAbility extends Ability {
   private static final float COST = 32.0F;
   private static final int UNLOCK = 9;
   private static final int DURATION = 160;
   private static final List<Entry<Holder<MobEffect>, Integer>> TRANCE = List.of(
      Map.entry(MobEffects.REGENERATION, 1),
      Map.entry(MobEffects.DAMAGE_BOOST, 0),
      Map.entry(MobEffects.DAMAGE_RESISTANCE, 0),
      Map.entry(MobEffects.MOVEMENT_SPEED, 0)
   );
   private static final Map<UUID, Long> UNTIL = new ConcurrentHashMap<>();

   public FrenzyAbility() {
      super("frenzy", 32.0F, 9);
   }

   @Override
   public boolean activate(ServerPlayer player) {
      for (Entry<Holder<MobEffect>, Integer> effect : TRANCE) {
         player.addEffect(new MobEffectInstance(effect.getKey(), 160, effect.getValue(), false, true));
      }

      UNTIL.put(player.getUUID(), player.serverLevel().getGameTime() + 160L);
      ServerLevel level = player.serverLevel();
      Fx.burst(level, ParticleTypes.CRIT, player.position().add(0.0, 1.0, 0.0), 50, 0.7, 0.3);
      Fx.burst(level, ParticleTypes.ANGRY_VILLAGER, player.getEyePosition(), 8, 0.5, 0.1);
      Fx.sound(level, player.position(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.0F, 0.8F);
      player.swing(InteractionHand.MAIN_HAND);
      PowerService.playAnimation(player, "frenzy");
      player.displayClientMessage(Component.translatable("awakened.regenerator.frenzy"), true);
      MasterySystem.award(player, MasteryReason.SURVIVAL);
      return true;
   }

   static void endTrance(ServerPlayer player) {
      Long until = UNTIL.remove(player.getUUID());
      long left = until == null ? 0L : until - player.serverLevel().getGameTime();
      if (left > 0L) {
         for (Entry<Holder<MobEffect>, Integer> effect : TRANCE) {
            MobEffectInstance running = player.getEffect(effect.getKey());
            if (running != null && running.getAmplifier() == effect.getValue() && (long)running.getDuration() <= left + 1L) {
               player.removeEffect(effect.getKey());
            }
         }
      }
   }
}
