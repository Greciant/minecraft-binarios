package com.devilfruits.fruits.kage_kage.ability;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.combat.AbilityProjectiles;
import com.devilfruits.core.config.AbilityConfig;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.form.FormFlags;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.rules.Weakness;
import com.devilfruits.core.util.ServerScheduler;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.fruits.kage_kage.KageAbilities;
import com.devilfruits.fruits.kage_kage.KageAim;
import com.devilfruits.fruits.kage_kage.KageFx;
import com.devilfruits.fruits.kage_kage.KageKage;
import com.devilfruits.fruits.kage_kage.KageTiming;
import com.devilfruits.registry.DFAttachments;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.phys.Vec3;

public class BrickBatAbility extends Ability {
   private static final double CATCH_RADIUS = 2.4;
   private static final Map<UUID, Long> RAISED = new HashMap<>();
   private static final Map<UUID, long[]> WINDOW = new HashMap<>();
   private static final Set<UUID> IDLE = new HashSet<>();

   public BrickBatAbility() {
      super(
         KageKage.id("brick_bat"),
         CastType.TOGGLE,
         AbilityStats.builder()
            .energy(6)
            .cooldown(240)
            .mastery(1)
            .damage(20.0)
            .range(30.0)
            .knockback(0.15)
            .extra("aegisDrainPerSecond", 2.0, 0.0, 50.0, "Energy the Shadow Aegis drains per second while it is up.")
            .extra("aegisMaxSeconds", 30.0, 1.0, 600.0, "Longest the Shadow Aegis stays up before the swarm leaves by itself.")
            .extra("interceptsPerSecond", 4.0, 1.0, 40.0, "Most projectiles the Aegis takes per second (the rest fly through).")
            .extra("interceptEnergy", 3.0, 0.0, 50.0, "Energy one intercepted projectile costs.")
            .extra("bats", 10.0, 1.0, 40.0, "Bats in the swarm (the damage is shared between them).")
            .extra("swarmEnergy", 12.0, 0.0, 200.0, "Energy the swarm costs when it leaves (nothing when there is no target).")
            .extra("aimConeDegrees", 8.0, 0.0, 45.0, "Aim assist: half-angle of the cone round the crosshair a target may stand in.")
            .extra("autoTargetRange", 20.0, 0.0, 64.0, "With nothing aimed at, the swarm goes for the nearest valid target within this many blocks.")
            .build()
      );
   }

   @Override
   public boolean coatable() {
      return true;
   }

   public static boolean raised(ServerPlayer player) {
      return DFAttachments.data(player).isToggleActive(KageAbilities.BRICK_BAT.id());
   }

   @Override
   public void onToggle(AbilityContext ctx, boolean active) {
      ServerPlayer player = ctx.player();
      UUID id = player.getUUID();
      FormFlags.set(player, KageKage.AEGIS, active);
      if (active) {
         RAISED.put(id, player.level().getGameTime());
         ServerFx.send(player, KageFx.AEGIS_ON, player.position(), Vec3.ZERO, 1.0F, ctx.seed(), 0);
         ServerFx.animate(player, ctx.animation(KageKage.id("aegis_raise")));
      } else {
         RAISED.remove(id);
         WINDOW.remove(id);
         IDLE.remove(id);
         boolean flew = player.isAlive() && !player.hasDisconnected() && !Weakness.isSuppressed(player) && this.loose(ctx);
         if (!flew) {
            IDLE.add(id);
         }

         ServerFx.send(player, KageFx.AEGIS_OFF, player.position(), Vec3.ZERO, 1.0F, 0L, flew ? 1 : 0);
      }
   }

   @Override
   public boolean cooldownOnToggleOff(ServerPlayer player) {
      return !IDLE.remove(player.getUUID());
   }

   @Override
   public void onStartTracking(ServerPlayer user, ServerPlayer tracker) {
      if (raised(user)) {
         ServerFx.sendTo(tracker, KageFx.AEGIS_ON, user, user.position(), Vec3.ZERO, 1.0F, user.getRandom().nextLong(), 0);
      }
   }

   @Override
   public void toggleTick(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      PlayerFruitData data = ctx.data();
      if (!AbilityManager.freeEnergy(player)) {
         data.setEnergy(data.energy() - (float)ctx.extra("aegisDrainPerSecond") / 20.0F);
      }

      long now = player.level().getGameTime();
      long raised = RAISED.computeIfAbsent(player.getUUID(), k -> now);
      if ((double)(now - raised) >= ctx.extra("aegisMaxSeconds") * 20.0) {
         AbilityManager.stopToggle(player, this);
      } else {
         Vec3 centre = player.getBoundingBox().getCenter();
         AbilityProjectiles.shatter(ctx.level(), player, at -> at.distanceToSqr(centre) <= 5.76 && take(player, at, centre.subtract(at)), Vec3.ZERO);
      }
   }

   public static boolean intercept(ServerPlayer player, Projectile projectile) {
      if (raised(player) && projectile.getOwner() != player) {
         Vec3 velocity = projectile.getDeltaMovement();
         if (!take(player, projectile.position(), velocity)) {
            return false;
         } else {
            if (!(projectile instanceof ThrownTrident) && !(projectile instanceof AbstractHurtingProjectile)) {
               projectile.discard();
            } else {
               projectile.setDeltaMovement(velocity.scale(-0.6));
               projectile.hasImpulse = true;
               if (projectile instanceof AbstractHurtingProjectile hurting) {
                  hurting.setOwner(player);
                  hurting.accelerationPower = 0.1;
               }
            }

            return true;
         }
      } else {
         return false;
      }
   }

   private static boolean take(ServerPlayer player, Vec3 at, Vec3 direction) {
      AbilityConfig config = KageAbilities.BRICK_BAT.config();
      long now = player.level().getGameTime();
      long[] window = WINDOW.computeIfAbsent(player.getUUID(), k -> new long[]{now, 0L});
      if (now - window[0] >= 20L) {
         window[0] = now;
         window[1] = 0L;
      }

      if ((double)window[1] >= config.extra("interceptsPerSecond")) {
         return false;
      } else {
         PlayerFruitData data = DFAttachments.data(player);
         boolean free = AbilityManager.freeEnergy(player);
         float cost = (float)config.extra("interceptEnergy");
         if (!free && data.energy() < cost) {
            return false;
         } else {
            if (!free) {
               data.setEnergy(data.energy() - cost);
            }

            window[1]++;
            data.setLastCombatTick(now);
            Vec3 centre = player.getBoundingBox().getCenter();
            Vec3 off = at.subtract(centre);
            Vec3 spot = off.lengthSqr() < 1.0E-4 ? centre : centre.add(off.normalize().scale(Math.min(1.1, off.length())));
            ServerFx.send(player, KageFx.AEGIS_BLOCK, spot, direction, 1.0F, player.getRandom().nextLong(), 0);
            return true;
         }
      }
   }

   @Nullable
   public static LivingEntity swarmTarget(ServerPlayer player, AbilityConfig config) {
      LivingEntity aimed = KageAim.aimed(player, config.range(), config.extra("aimConeDegrees"));
      return aimed != null ? aimed : KageAim.nearest(player, config.extra("autoTargetRange"));
   }

   private boolean loose(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      LivingEntity target = swarmTarget(player, ctx.config());
      if (target == null) {
         return false;
      } else {
         PlayerFruitData data = ctx.data();
         boolean free = AbilityManager.freeEnergy(player);
         float cost = (float)ctx.extra("swarmEnergy");
         if (!free && data.energy() < cost) {
            return false;
         } else {
            if (!free) {
               data.setEnergy(data.energy() - cost);
            }

            data.setLastCombatTick(player.level().getGameTime());
            int bats = Math.max(1, (int)ctx.extra("bats"));
            float each = ctx.damage() / (float)bats;
            float push = ctx.knockback() / (float)bats;
            double distance = player.getEyePosition().distanceTo(target.getBoundingBox().getCenter());
            ServerFx.send(player, KageFx.SWARM, target.getBoundingBox().getCenter(), Vec3.ZERO, (float)bats, ctx.seed(), target.getId());
            ServerFx.animate(player, ctx.animation(KageKage.id("summon")));

            for (int i = 0; i < bats; i++) {
               ServerScheduler.later(ctx.level(), KageTiming.batArrival(i, distance), () -> bite(ctx, target, each, push));
            }

            return true;
         }
      }
   }

   private static void bite(AbilityContext ctx, LivingEntity target, float damage, float push) {
      ServerPlayer player = ctx.player();
      if (target.isAlive() && !target.isRemoved() && target.level() == player.level() && !player.isRemoved()) {
         target.invulnerableTime = 0;
         if (ctx.hurt(target, damage)) {
            Vec3 away = target.position().subtract(player.position()).multiply(1.0, 0.0, 1.0);
            if (away.lengthSqr() > 1.0E-4 && push > 0.0F) {
               Targeting.push(target, away.normalize().scale((double)push).add(0.0, 0.02, 0.0));
            }
         }
      }
   }
}
