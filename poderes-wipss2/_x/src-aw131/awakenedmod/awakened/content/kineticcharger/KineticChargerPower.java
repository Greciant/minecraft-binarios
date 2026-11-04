package com.awakenedmod.awakened.content.kineticcharger;

import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.power.PassiveToggle;
import com.awakenedmod.awakened.core.power.Power;
import com.awakenedmod.awakened.core.power.PowerResource;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

public final class KineticChargerPower extends Power {
   public static final String ID = "kinetic_charger";
   private static final int THEME_ARGB = -3703297;

   public KineticChargerPower() {
      super(
         "kinetic_charger",
         List.of(new KineticThrowAbility(), new RecoilShotAbility(), new ImpactStrikeAbility(), new RuptureKickAbility(), new FullReleaseAbility())
      );
   }

   @Override
   public List<PassiveToggle> passiveToggles() {
      return List.of(MagnetPull.TOGGLE, KineticAmmo.MAIN_HAND_ONLY);
   }

   @Override
   public int themeColor() {
      return -3703297;
   }

   @Override
   public int signatureSlot() {
      return 0;
   }

   @Override
   public void passiveTick(ServerPlayer player, PowerData data) {
      super.passiveTick(player, data);
      FullReleaseState.tick(player, data);
      Stockpile.tick(player, this.hasActiveAbility(player));
      MagnetPull.tick(player, data);
      KineticAttrs.passiveFallImmunity(player, data.masteryLevel());
      KineticBank.tick(player, data);
   }

   @Override
   public void onSuppressed(ServerPlayer player, PowerData data) {
      KineticAttrs.passiveFallImmunity(player, 0);
   }

   @Override
   public float onIncomingDamage(ServerPlayer player, DamageSource source, float amount) {
      float reduced = FullReleaseState.resisted(PowerService.data(player), source, super.onIncomingDamage(player, source, amount));
      KineticBank.hitTaken(player, source, reduced);
      return reduced;
   }

   @Override
   protected void onAbilityStateCleared(ServerPlayer player) {
      KineticBank.forget(player);
      Stockpile.forget(player);
      KineticFx.forget(player);
   }

   @Override
   public PowerResource resource() {
      return KineticChargeResource.INSTANCE;
   }

   @Override
   public boolean hasResourceBar() {
      return true;
   }

   @Override
   public int resourceBarColor() {
      return -3703297;
   }

   @Override
   public Component resourceBarLabel() {
      return Component.translatable("awakened.kinetic_charger.bank");
   }

   @Override
   public float resourceBarFraction(ServerPlayer player, PowerData data) {
      return KineticBank.fraction(player, data);
   }

   @Override
   public float resourceBarMark(ServerPlayer player, PowerData data) {
      return KineticBank.markOf(player, this.liveSpend(player));
   }

   @Override
   public boolean resourceBarMarkTrails() {
      return true;
   }

   private float liveSpend(ServerPlayer player) {
      for (Ability ability : this.abilities()) {
         if (ability instanceof KineticHoldAbility hold) {
            float spend = hold.liveSpend(player);
            if (spend > 0.0F) {
               return spend;
            }
         }
      }

      return 0.0F;
   }

   @Override
   public int resourceBarTier(ServerPlayer player, PowerData data) {
      return KineticBank.releaseLevel(data);
   }

   @Override
   public String resourceBarTierLabel(int tier) {
      return tier <= 0 ? null : tier * 10 + "%";
   }

   @Override
   public String resourceBarTierRoom() {
      return "100%";
   }

   @Override
   public void onAwaken(ServerPlayer player) {
      KineticFx.awaken(player);
   }
}
