package com.awakenedmod.awakened.content.spider;

import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.net.ForcedCameraPayload;
import com.awakenedmod.awakened.core.net.SpiderForceStopPayload;
import com.awakenedmod.awakened.core.net.SpiderStrikePayload;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class WebStrikeAbility extends Ability {
   public static final String ID = "web_strike";
   private static final float COST = 35.0F;
   private static final int UNLOCK = 7;
   private static final int FALL_GRACE = 40;
   private static final double HIT_RANGE = 5.5;
   private static final double MAX_SPEED = 2.2;
   private static final double MIN_SPEED = 0.25;
   private static final float MIN_DAMAGE = 5.0F;
   private static final float MAX_DAMAGE = 20.0F;
   private static final double MIN_KB = 0.6;
   private static final double MAX_KB = 2.4;
   private static final double KB_LIFT = 0.42;
   private static final double MOMENTUM_SPEED = 1.1;
   private static final int ARM_WINDOW = 30;
   private static final int GUARD_WINDOW = 30;
   private static final int STRIKE_CAM_TICKS = 30;
   private final Map<UUID, Integer> fallGrace = this.playerState();
   private final Map<UUID, Long> armed = this.playerState();
   private final Map<UUID, Long> guardUntil = this.playerState();

   public WebStrikeAbility() {
      super("web_strike", 35.0F, 7);
   }

   public static float cost() {
      return StaminaSystem.effectiveCost(35.0F, WebStrikeAbility.class.getSimpleName());
   }

   @Override
   public boolean clientDriven() {
      return true;
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().boostsDamage().build();
   }

   @Override
   protected boolean cancelsOnDimensionChange() {
      return true;
   }

   @Override
   public boolean activate(ServerPlayer player) {
      return false;
   }

   public static void handleClientStrike(ServerPlayer player, SpiderStrikePayload payload) {
      WebStrikeAbility strike = SpiderClientFire.resolve(player, payload.slot(), WebStrikeAbility.class);
      if (strike != null) {
         strike.handle(player, payload);
      }
   }

   private void handle(ServerPlayer player, SpiderStrikePayload payload) {
      switch (payload.action()) {
         case 0:
            this.onStart(player);
            break;
         case 1:
            this.onHit(player, payload);
      }
   }

   private void onStart(ServerPlayer player) {
      PowerData data = PowerService.data(player);
      Long live = this.armed.get(player.getUUID());
      if (live != null && player.serverLevel().getGameTime() <= live) {
         long now = player.serverLevel().getGameTime();
         this.armed.put(player.getUUID(), now + 30L);
         this.guardUntil.put(player.getUUID(), now + 30L);
      } else {
         float cost = this.billed(35.0F);
         if (!StaminaSystem.canAfford(data, cost)) {
            PacketDistributor.sendToPlayer(player, SpiderForceStopPayload.INSTANCE, new CustomPacketPayload[0]);
            this.fail(player, "awakened.feedback.no_stamina", new Object[0]);
         } else {
            if (!data.infiniteStamina()) {
               data.setStamina(data.stamina() - cost);
            }

            long gameTime = player.serverLevel().getGameTime();
            data.setLastSpendTime(gameTime);
            MasterySystem.awardUse(player);
            PowerService.playAnimation(player, "web_strike");
            PacketDistributor.sendToPlayer(player, new ForcedCameraPayload(30), new CustomPacketPayload[0]);
            this.fallGrace.put(player.getUUID(), 40);
            this.armed.put(player.getUUID(), gameTime + 30L);
            this.guardUntil.put(player.getUUID(), gameTime + 30L);
            PowerService.sync(player);
         }
      }
   }

   private void onHit(ServerPlayer player, SpiderStrikePayload payload) {
      ServerLevel level = player.serverLevel();
      Long expiry = this.armed.remove(player.getUUID());
      if (expiry != null && level.getGameTime() <= expiry) {
         if (!(level.getEntity(payload.targetId()) instanceof LivingEntity target) || !target.isAlive() || target == player) {
            return;
         }

         if (!(target.distanceToSqr(player) > 30.25)) {
            double speed = Math.max(0.0, Math.min(payload.speed(), 2.2));
            double t = (speed - 0.25) / 1.9500000000000002;
            t = Math.max(0.0, Math.min(1.0, t));
            float damage = (float)(5.0 + 15.0 * t);
            damage = MasteryScaling.scaledDamage(damage, player, this.balanceKey());
            Dmg.hurt(target, level, Dmg.by(player, DamageTypes.PLAYER_ATTACK), damage);
            Vec3 away = new Vec3(target.getX() - player.getX(), 0.0, target.getZ() - player.getZ());
            away = away.lengthSqr() < 1.0E-4 ? player.getLookAngle() : away.normalize();
            double kb = 0.6 + 1.7999999999999998 * t;
            Harm.setMotion(player, target, away.x * kb, 0.42, away.z * kb);
            SpiderFx.strikeImpact(level, target.position().add(0.0, (double)target.getBbHeight() * 0.5, 0.0), (float)t);
            MasterySystem.award(player, MasteryReason.HIT, target);
            if (speed >= 1.1) {
               MasterySystem.award(player, MasteryReason.HIT, target);
               if (!player.onGround() && Foes.counts(player, target)) {
                  QuestSystem.feat(player, "web_momentum_strike");
               }
            }

            PowerService.sync(player);
         }
      }
   }

   @Override
   public float onIncomingDamage(ServerPlayer player, DamageSource source, float amount) {
      Long until = this.guardUntil.get(player.getUUID());
      return until != null && player.serverLevel().getGameTime() <= until ? 0.0F : amount;
   }

   @Override
   public void serverTick(ServerPlayer player) {
      Integer left = this.fallGrace.get(player.getUUID());
      if (left != null) {
         if (!player.onGround() && left > 0) {
            player.resetFallDistance();
            this.fallGrace.put(player.getUUID(), left - 1);
         } else {
            this.fallGrace.remove(player.getUUID());
         }
      }
   }
}
