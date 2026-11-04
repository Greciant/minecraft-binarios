package com.awakenedmod.awakened.content.exosavant;

import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class Failsafe {
   private static final String K_DISABLED = "exo_failsafe_off";
   private static final String K_READY_AT = "exo_failsafe_ready";
   private static final String K_IFRAMES = "exo_failsafe_iframes";
   public static final int UNLOCK = 13;
   private static final float SURVIVE_HP = 4.0F;
   private static final int COOLDOWN_TICKS = 1800;
   private static final double KNOCKBACK_RADIUS = 6.0;
   private static final double KNOCKBACK_STRENGTH = 1.6;
   private static final int IFRAME_GRACE_TICKS = 30;
   private static final int IFRAME_FLOOR = 18;

   private Failsafe() {
   }

   public static boolean enabled(PowerData data) {
      return data.var("exo_failsafe_off", 0.0F) == 0.0F;
   }

   public static void setEnabled(ServerPlayer player, boolean on) {
      PowerService.data(player).setVar("exo_failsafe_off", on ? 0.0F : 1.0F);
   }

   public static void toggle(ServerPlayer player) {
      if (player != null) {
         PowerData data = PowerService.data(player);
         if ("exosavant".equals(data.powerId())) {
            boolean now = !enabled(data);
            setEnabled(player, now);
            player.displayClientMessage(
               Component.translatable(
                  "commands.awakened.failsafe.set",
                  new Object[]{Component.translatable(now ? "commands.awakened.failsafe.on" : "commands.awakened.failsafe.off")}
               ),
               true
            );
         }
      }
   }

   public static boolean unlocked(PowerData data) {
      return data.masteryLevel() >= 13;
   }

   public static boolean ready(ServerPlayer player, PowerData data) {
      return (float)player.serverLevel().getGameTime() >= data.var("exo_failsafe_ready", 0.0F);
   }

   public static float onLethal(ServerPlayer player, DamageSource source, float incoming, PowerData data) {
      if (!(incoming <= 0.0F)
         && unlocked(data)
         && enabled(data)
         && ready(player, data)
         && ExoLoadout.chipInstalled(player)
         && ExoSuit.hasSummonablePlate(player)) {
         float effectiveHealth = player.getHealth() + player.getAbsorptionAmount();
         if (incoming < effectiveHealth) {
            return incoming;
         } else {
            save(player, data);
            return Math.max(0.0F, effectiveHealth - 4.0F);
         }
      } else {
         return incoming;
      }
   }

   private static void save(ServerPlayer player, PowerData data) {
      long now = player.serverLevel().getGameTime();
      data.setVar("exo_failsafe_ready", (float)(now + 1800L));
      data.setVar("exo_failsafe_iframes", 30.0F);
      player.invulnerableTime = Math.max(player.invulnerableTime, 30);
      ExoSuit.callSuitOn(player, data);
      blastAttackers(player);
      player.displayClientMessage(Component.translatable("awakened.exosavant.failsafe"), true);
      ServerLevel level = player.serverLevel();
      Vec3 at = player.position().add(0.0, 1.0, 0.0);
      Fx.burst(level, ParticleTypes.EXPLOSION, at, 1, 0.0, 0.0);
      Fx.sound(level, at, SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.8F, 0.7F);
      Fx.sound(level, at, SoundEvents.NETHERITE_BLOCK_PLACE, SoundSource.PLAYERS, 1.0F, 0.6F);
   }

   private static void blastAttackers(ServerPlayer player) {
      Vec3 center = player.position();

      for (LivingEntity foe : AimUtil.livingAround(player, center, 6.0)) {
         if (ExoTags.isHostileThreat(foe, player)) {
            Harm.knockback(player, foe, 1.6, center.x - foe.getX(), center.z - foe.getZ());
         }
      }
   }

   public static void tick(ServerPlayer player, PowerData data) {
      float grace = data.var("exo_failsafe_iframes", 0.0F);
      if (!(grace <= 0.0F)) {
         data.setVar("exo_failsafe_iframes", grace - 1.0F);
         if (player.invulnerableTime < 18) {
            player.invulnerableTime = 18;
         }
      }
   }
}
