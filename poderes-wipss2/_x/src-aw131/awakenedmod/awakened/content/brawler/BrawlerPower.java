package com.awakenedmod.awakened.content.brawler;

import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.MeleeProfile;
import com.awakenedmod.awakened.core.power.Power;
import com.awakenedmod.awakened.core.power.PowerMelee;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class BrawlerPower extends Power {
   public static final String ID = "brawler";

   public BrawlerPower() {
      super("brawler", List.of(new RisingHookAbility(), new SledgeDropAbility(), new ShoulderBargeAbility(), new ReelInAbility(), new FullCounterAbility()));
   }

   @Override
   public boolean usesRadialActivate() {
      return true;
   }

   @Override
   public MeleeProfile meleeProfile(Player player) {
      return BrawlerMelee.INSTANCE;
   }

   @Override
   public int themeColor() {
      return -26066;
   }

   @Override
   public int signatureSlot() {
      return 0;
   }

   @Override
   public void passiveTick(ServerPlayer player, PowerData data) {
      super.passiveTick(player, data);
      BrawlerCombo.tick(player);
      BrawlerLift.noteFooting(player);
   }

   @Override
   public void onKill(ServerPlayer killer, LivingEntity victim) {
      BrawlerCombo.onKill(killer, victim);
   }

   @Override
   public float onIncomingDamage(ServerPlayer player, DamageSource source, float amount) {
      return BrawlerFlow.onIncomingDamage(player, source, super.onIncomingDamage(player, source, amount));
   }

   @Override
   public void onDeath(ServerPlayer victim, DamageSource source) {
      Marks.clearPlayer(victim);
      dropFightState(victim);
   }

   @Override
   public void onSuppressed(ServerPlayer player, PowerData data) {
      dropFightState(player);
   }

   private static void dropFightState(ServerPlayer player) {
      BrawlerFollowUp.clearPlayer(player);
      BrawlerLift.clearPlayer(player);
      BrawlerCombo.reset(player);
      BrawlerMelee.clearPlayer(player);
      BrawlerFlow.clearPlayer(player);
      BrawlerIframes.clearPlayer(player);
      BrawlerGuard.clearPlayer(player);
   }

   @Override
   public void onLogin(ServerPlayer player, PowerData data) {
      BrawlerCombo.resync(player);
   }

   @Override
   public void onReplaced(ServerPlayer player) {
      Marks.clearPlayer(player);
      BrawlerFollowUp.clearPlayer(player);
      BrawlerLift.clearPlayer(player);
      BrawlerCombo.reset(player);
      BrawlerCombo.clearPlayer(player);
      BrawlerMelee.clearPlayer(player);
      BrawlerFlow.clearPlayer(player);
      BrawlerIframes.clearPlayer(player);
      BrawlerGuard.clearPlayer(player);
      PowerMelee.clearPlayer(player);
   }

   @Override
   public List<String> extraBalanceKeys() {
      return List.of("BrawlerPunch");
   }
}
