package com.awakenedmod.awakened.content.phytokinetic;

import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.Power;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class PhytokineticPower extends Power {
   public static final String ID = "phytokinetic";
   private static final float FIRE_DAMAGE_MULTIPLIER = 1.5F;
   private static final int PHOTOSYNTHESIS_INTERVAL = 20;
   private static final float PHOTOSYNTHESIS_HEAL = 1.0F;
   private static final float SUNLIGHT_STAMINA_REGEN = 4.0F;

   public PhytokineticPower() {
      super(
         "phytokinetic",
         List.of(new IvyWrapAbility(), new SunflowerFieldAbility(), new VineGrappleAbility(), new LifeGraftAbility(), new CradleOfRootsAbility())
      );
   }

   @Override
   public int themeColor() {
      return -10960310;
   }

   @Override
   public int signatureSlot() {
      return 1;
   }

   @Override
   public void onAwaken(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      Vec3 center = player.position().add(0.0, 1.0, 0.0);
      Vfx.dome(level, player.position(), 2.5, 5816906, 18);
      Vfx.ring(level, player.position().add(0.0, 0.1, 0.0), 3.0, 9234544, 16);
      PhytoFx.leafBurst(level, center, 20);
      Fx.burst(level, ParticleTypes.COMPOSTER, center, 30, 1.0, 0.15);
      Fx.sound(level, player.position(), SoundEvents.BONE_MEAL_USE, SoundSource.PLAYERS, 1.2F, 0.8F);
      Fx.sound(level, player.position(), SoundEvents.AZALEA_PLACE, SoundSource.PLAYERS, 1.0F, 0.7F);
   }

   @Override
   public void passiveTick(ServerPlayer player, PowerData data) {
      super.passiveTick(player, data);
      if (player.tickCount % 20 == 0 && inDirectSunlight(player)) {
         if (player.isAlive() && player.getHealth() < player.getMaxHealth()) {
            player.heal(1.0F);
         }

         PhytoFx.sunlitDrift(player.serverLevel(), player.position().add(0.0, 1.0, 0.0));
      }
   }

   @Override
   public float bonusStaminaRegen(Player player) {
      if (player instanceof ServerPlayer sp && inDirectSunlight(sp)) {
         return 4.0F;
      }

      return 0.0F;
   }

   @Override
   public float onIncomingDamage(ServerPlayer player, DamageSource source, float amount) {
      float result = super.onIncomingDamage(player, source, amount);
      return source.is(DamageTypeTags.IS_FIRE) ? result * 1.5F : result;
   }

   private static boolean inDirectSunlight(ServerPlayer player) {
      Level level = player.serverLevel();
      if (level.dimensionType().hasSkyLight() && level.canSeeSky(player.blockPosition())) {
         float frac = (float)(level.getDayTime() % 24000L) / 24000.0F;
         return Math.sin((double)frac * Math.PI * 2.0) > 0.0;
      } else {
         return false;
      }
   }
}
