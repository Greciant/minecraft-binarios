package com.awakenedmod.awakened.content.limitless;

import com.awakenedmod.awakened.content.AwakenedSounds;
import com.awakenedmod.awakened.content.limitless.blue.BlueAbility;
import com.awakenedmod.awakened.content.limitless.domain.InfiniteVoidAbility;
import com.awakenedmod.awakened.content.limitless.purple.PurpleAbility;
import com.awakenedmod.awakened.content.limitless.rct.RctAbility;
import com.awakenedmod.awakened.content.limitless.red.RedAbility;
import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.power.PassiveToggle;
import com.awakenedmod.awakened.core.power.PassiveToggleService;
import com.awakenedmod.awakened.core.power.Power;
import com.awakenedmod.awakened.core.power.PowerRegistry;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class LimitlessPower extends Power {
   public static final String ID = "limitless";
   public static final int THEME = -11557633;
   public static final PassiveToggle INFINITY = new PassiveToggle(
      "limitless_infinity", "awakened.screen.setting_limitless_infinity", "awakened.screen.setting_limitless_infinity_desc", true
   );
   public static final PassiveToggle SIX_EYES = new PassiveToggle(
      "limitless_six_eyes", "awakened.screen.setting_limitless_six_eyes", "awakened.screen.setting_limitless_six_eyes_desc", true
   );
   private final RctAbility rct;

   public LimitlessPower() {
      this(new BlueAbility(), new RctAbility());
   }

   private LimitlessPower(BlueAbility blue, RctAbility rct) {
      super("limitless", List.of(blue, new RedAbility(blue), new PurpleAbility(), rct, new InfiniteVoidAbility()));
      this.rct = rct;
   }

   public static boolean castingOther(ServerPlayer player, Ability self) {
      Power var3 = PowerRegistry.get(PowerService.data(player).powerId());
      if (var3 instanceof Power) {
         for (Ability ability : var3.abilities()) {
            if (ability != self && !(ability instanceof InfiniteVoidAbility) && ability.isActive(player)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   @Override
   public int themeColor() {
      return -11557633;
   }

   @Override
   public int signatureSlot() {
      return 0;
   }

   @Override
   public List<PassiveToggle> passiveToggles() {
      return List.of(INFINITY, SIX_EYES);
   }

   @Override
   public List<String> extraBalanceKeys() {
      return List.of("LimitlessInfinity");
   }

   @Override
   public boolean handlesSecondaryKey() {
      return true;
   }

   @Override
   public void onSecondaryKey(ServerPlayer player, boolean sneaking, boolean held) {
      PassiveToggleService.toggle(player, SIX_EYES.key());
   }

   @Override
   public List<PassiveToggle.Wedge> secondaryKeyWheel() {
      return List.of(
         INFINITY.wedge(AwId.of("textures/gui/toggle/limitless_infinity.png")), SIX_EYES.wedge(AwId.of("textures/gui/toggle/limitless_six_eyes.png"))
      );
   }

   @Override
   public void passiveTick(ServerPlayer player, PowerData data) {
      super.passiveTick(player, data);
      LimitlessInfinity.tick(player);
   }

   @Override
   protected void onAbilityStateCleared(ServerPlayer player) {
      LimitlessInfinity.releaseAll(player);
   }

   @Override
   public void onLogout(ServerPlayer player) {
      LimitlessInfinity.forget(player);
   }

   @Override
   public void onDamageTaken(ServerPlayer player, DamageSource source, float amount) {
      if (amount > 0.0F && Foes.threat(source, player)) {
         this.rct.noteHarm(player, amount);
      }
   }

   @Override
   public boolean abilityLitOnClient(Player viewer, int slot) {
      return this.ability(slot) instanceof InfiniteVoidAbility && InfiniteVoidAbility.litOnClient(viewer);
   }

   @Override
   public void onAwaken(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      LivingEntity last = player.getLastHurtByMob();
      Vec3 toward = last != null && last.distanceToSqr(player) < 256.0 ? last.position().subtract(player.position()) : player.getLookAngle();
      Vec3 flat = new Vec3(toward.x, 0.0, toward.z);
      flat = flat.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
      LimitlessInfinity.showSwingStopped(player, flat);
      Fx.sound(level, player.position(), (SoundEvent)AwakenedSounds.AWAKEN.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
   }
}
