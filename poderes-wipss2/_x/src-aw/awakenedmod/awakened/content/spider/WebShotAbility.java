package com.awakenedmod.awakened.content.spider;

import com.awakenedmod.awakened.content.AwakenedEntities;
import com.awakenedmod.awakened.content.entity.projectile.KineticProjectile;
import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class WebShotAbility extends Ability {
   public static final String ID = "web_shot";
   private static final double AIM_DISTANCE = 48.0;
   private static final double CONTACT = 0.6;
   private static final int FIRE_INTERVAL = 5;
   private static final float IMPACT_DAMAGE = 4.0F;
   public static final float COST_FACTOR = 0.5F;
   private final Map<UUID, int[]> firing = this.activeState();

   public static float perShotCost(WebType type) {
      return type.cost() * 0.5F;
   }

   public WebShotAbility() {
      super("web_shot", WebType.SPLAT.cost(), 3);
   }

   @Override
   public float staminaCost(ServerPlayer player) {
      return perShotCost(SpiderTypeSync.loaded(PowerService.data(player)));
   }

   @Override
   public String activationAnimation(ServerPlayer player) {
      return null;
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().damage(4.0).build();
   }

   @Override
   public boolean activate(ServerPlayer player) {
      PowerData data = PowerService.data(player);
      WebType type = SpiderTypeSync.loaded(data);
      if (!type.isUnlocked(data, SpiderTypeSync.hasGauntlets(player))) {
         return this.fail(player, "awakened.spider.type_locked", new Object[0]);
      } else {
         this.fireBolt(player, data, type);
         this.firing.put(player.getUUID(), new int[]{0});
         SpiderTypeSync.push(player);
         return true;
      }
   }

   @Override
   public void serverTick(ServerPlayer player) {
      int[] state = this.firing.get(player.getUUID());
      if (state != null) {
         if (++state[0] >= 5) {
            state[0] = 0;
            PowerData data = PowerService.data(player);
            WebType type = SpiderTypeSync.loaded(data);
            if (!type.isUnlocked(data, SpiderTypeSync.hasGauntlets(player))) {
               this.firing.remove(player.getUUID());
            } else {
               float cost = this.effectiveCost(player);
               if (!data.infiniteStamina()) {
                  if (!StaminaSystem.canAfford(data, cost)) {
                     this.firing.remove(player.getUUID());
                     this.fail(player, "awakened.feedback.no_stamina", new Object[0]);
                     return;
                  }

                  StaminaSystem.drainSustained(data, player.serverLevel().getGameTime(), cost);
               }

               this.fireBolt(player, data, type);
               PowerService.sync(player);
            }
         }
      }
   }

   @Override
   public void onRelease(ServerPlayer player) {
      this.firing.remove(player.getUUID());
   }

   private void fireBolt(ServerPlayer player, PowerData data, WebType type) {
      ServerLevel level = player.serverLevel();
      float damage = MasteryScaling.scaledDamage(4.0F, player, this.balanceKey());
      boolean left = SpiderAnim.playThrow(player);
      Vec3 hand = AimUtil.handPosition(player, left);
      Vec3 aim = player.pick(48.0, 1.0F, false).getLocation();
      Vec3 dir = aim.subtract(hand);
      KineticProjectile shot = new KineticProjectile(AwakenedEntities.WEB_SHOT.get(), player, level, ItemStack.EMPTY);
      shot.setPos(hand.x, hand.y, hand.z);
      shot.configure(0.0F, 0.0F, 0.0F, 0.0F);
      shot.setCustomDetonation(true);
      shot.setGravity(type.gravity());
      shot.setContactRadius(Math.max(type.contactRadius(), 0.6));
      shot.setOnTick(pos -> type.flightFx(level, pos, shot.getDeltaMovement()));
      shot.setOnDetonate(pos -> {
         LivingEntity var10000;
         label19: {
            if (Foes.whole(shot.directHit()) instanceof LivingEntity living && living != player) {
               var10000 = living;
               break label19;
            }

            var10000 = null;
         }

         LivingEntity victim = var10000;
         if (victim != null) {
            Dmg.hurtRapid(victim, level, Dmg.by(player, DamageTypes.PLAYER_ATTACK), damage);
            Vec3 c = victim.getBoundingBox().getCenter();
            if (victim.isDeadOrDying()) {
               SpiderFx.shotKill(level, c);
            } else {
               SpiderFx.shotHit(level, c);
            }
         }

         type.apply(player, level, victim, pos);
      });
      shot.shoot(dir.x, dir.y, dir.z, type.velocity(), 1.0F);
      level.addFreshEntity(shot);
      SpiderFx.shotLaunch(level, hand);
   }

   @Override
   protected boolean cancelsOnDimensionChange() {
      return true;
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      Webbing.clear(player);
      SpiderDeployables.clear(player);
      SpiderAnim.clear(player);
      super.clearPlayer(player);
   }
}
