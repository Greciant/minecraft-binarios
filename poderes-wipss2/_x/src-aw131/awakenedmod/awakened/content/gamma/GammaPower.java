package com.awakenedmod.awakened.content.gamma;

import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.PassiveToggle;
import com.awakenedmod.awakened.core.power.Power;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public final class GammaPower extends Power {
   private static final PassiveToggle RISING_FURY = new PassiveToggle(
      "rising_fury", "awakened.screen.setting_gamma_rising_fury", "awakened.screen.setting_gamma_rising_fury_desc", true
   );

   public GammaPower() {
      super("gamma", List.of(new ProvokeAbility(), new ThunderclapAbility(), new SmashLeapAbility(), new BoulderThrowAbility(), new RagdollSlamAbility()));
   }

   @Override
   public int themeColor() {
      return -8986566;
   }

   @Override
   public int signatureSlot() {
      return 0;
   }

   @Override
   public List<PassiveToggle> passiveToggles() {
      return List.of(RISING_FURY);
   }

   @Override
   public void passiveTick(ServerPlayer player, PowerData data) {
      super.passiveTick(player, data);
      if (!RISING_FURY.enabled(data)) {
         if (Rage.rage(data) != 0.0F) {
            Rage.clear(player);
         }

         GammaBeats.tick(player);
      } else {
         Rage.tick(player, data);
         ServerLevel level = player.serverLevel();
         GammaFx.ambientTick(level, player, Rage.rage(data));
         if (level.getGameTime() % 8L == 0L) {
            Rage.broadcast(player, data);
         }

         GammaBeats.tick(player);
      }
   }

   @Override
   public float onIncomingDamage(ServerPlayer player, DamageSource source, float amount) {
      PowerData data = PowerService.data(player);
      float mult = Rage.incomingMultiplier(data);
      if (amount > 0.0F && RISING_FURY.enabled(data)) {
         float before = Rage.rage(data);
         Rage.onDamageTaken(player, data, source, amount);
         if (before < 1.0F && Rage.rage(data) >= 1.0F && Foes.threat(source, player)) {
            QuestSystem.feat(player, "gamma_unstoppable");
         }
      }

      return super.onIncomingDamage(player, source, amount * mult);
   }

   @Override
   public void onDealDamage(ServerPlayer attacker, LivingEntity victim) {
      PowerData data = PowerService.data(attacker);
      if (RISING_FURY.enabled(data)) {
         Rage.onDamageDealt(attacker, data);
      }
   }

   @Override
   public void onKill(ServerPlayer killer, LivingEntity victim) {
      PowerData data = PowerService.data(killer);
      if (RISING_FURY.enabled(data)) {
         Rage.onKill(killer, data);
      }
   }
}
