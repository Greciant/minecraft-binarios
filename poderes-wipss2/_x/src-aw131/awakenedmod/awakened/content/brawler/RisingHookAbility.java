package com.awakenedmod.awakened.content.brawler;

import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;

public final class RisingHookAbility extends BrawlerAbility {
   private final Map<UUID, RisingHookAbility.Cast> casts = this.playerState();

   public RisingHookAbility() {
      super("brawler_rising_hook", 6.0F, 0);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().damage(4.0).build();
   }

   @Override
   protected boolean press(ServerPlayer player) {
      if (this.casts.containsKey(player.getUUID())) {
         return false;
      } else {
         this.casts.put(player.getUUID(), new RisingHookAbility.Cast(BrawlerLift.wantsLift(player)));
         BrawlerFx.hookStart(player.serverLevel(), player);
         return true;
      }
   }

   @Override
   public void serverTick(ServerPlayer player) {
      RisingHookAbility.Cast cast = this.casts.get(player.getUUID());
      if (cast != null) {
         if (cast.ticks + 1 < 6) {
            cast.ticks++;
         } else {
            this.casts.remove(player.getUUID());
            this.strike(player, cast.lift);
         }
      }
   }

   private void strike(ServerPlayer player, boolean lift) {
      ServerLevel level = player.serverLevel();
      LivingEntity target = AimUtil.aimedLivingSoft(player, 3.6, 35.0, e -> e != player && Harm.allowed(player, e));
      if (target == null) {
         this.whiff(player);
      } else {
         float bonus = MarkBonus.of(MarkBonus.RISING_HOOK, target);
         boolean transition = Marks.consumeTransition(player, target, this.id());
         float damage = MasteryScaling.scaledDamage(4.0F, player, this.balanceKey(), bonus, BrawlerCombo.bonusFraction(player));
         boolean dealt = Dmg.hurtThrough(target, level, Dmg.by(player, DamageTypes.PLAYER_ATTACK), damage);
         BrawlerFx.hookHit(level, target);
         int marked = Marks.apply(player, target, Mark.AIRBORNE, 30, Mark.Flavour.NONE, this.id());
         if (lift) {
            BrawlerLift.rideUp(player, Marks.launchHeight(target) - 0.3, marked);
         }

         if (dealt) {
            BrawlerCombo.abilityHit(player, target, List.of(), transition ? 3 : 2, -1);
         }
      }
   }

   private static final class Cast {
      final boolean lift;
      int ticks;

      Cast(boolean lift) {
         this.lift = lift;
      }
   }
}
