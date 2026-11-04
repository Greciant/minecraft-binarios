package com.devilfruits.fruits.suna_suna.ability;

import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.AimHelper;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.BlockImpactService;
import com.devilfruits.core.physics.ImpactShape;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.core.physics.PhysicsWorld;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.fruits.suna_suna.SunaEffects;
import com.devilfruits.fruits.suna_suna.SunaFx;
import com.devilfruits.fruits.suna_suna.SunaSuna;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class DesertEncierroAbility extends SandTechnique {
   private static final Map<UUID, DesertEncierroAbility.Grip> GRIPS = new HashMap<>();

   public DesertEncierroAbility() {
      super(
         "desert_encierro",
         CastType.HELD,
         AbilityStats.builder()
            .energy(20)
            .cooldown(200)
            .mastery(6)
            .damage(3.0)
            .range(6.5)
            .knockback(0.0)
            .extra("maxSeconds", 5.0, 1.0, 30.0, "Longest hold.")
            .extra("drainPerSecond", 6.0, 0.0, 100.0, "Energy drained per second while held.")
            .extra("dehydrateSeconds", 6.0, 0.0, 60.0, "Seconds of Dehydrated added every second of the hold (up to level V).")
            .extra("hold", 1.7, 0.8, 4.0, "Distance in front of the user the victim is held at.")
            .build()
      );
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.builder(this.defaults()).impact(PhysicsProfile.Impact.DIRECTIONAL).force(0.4).blockDamage(0.0).manual().build();
   }

   @Override
   public int maxChannelTicks() {
      return (int)(this.config().extra("maxSeconds") * 20.0);
   }

   @Nullable
   @Override
   public Component checkRequirements(ServerPlayer player, PlayerFruitData data) {
      return victim(player, this.config().range()) == null ? Component.translatable("message.devilfruits.suna_suna.nobody_in_reach") : null;
   }

   @Nullable
   public static LivingEntity victim(ServerPlayer player, double range) {
      Vec3 eye = player.getEyePosition();
      Vec3 end = eye.add(AimHelper.look(player).scale(range));
      HitResult wall = PhysicsWorld.clip(player.level(), new ClipContext(eye, end, Block.COLLIDER, Fluid.NONE, player));
      if (wall.getType() != Type.MISS) {
         end = wall.getLocation();
      }

      AABB box = player.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1.0);
      EntityHitResult hit = ProjectileUtil.getEntityHitResult(
         player, eye, end, box, e -> e instanceof LivingEntity && e.isAlive() && Targeting.canHurt(player, e), eye.distanceToSqr(end)
      );
      return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
   }

   @Override
   public void onChannelStart(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      LivingEntity victim = victim(player, (double)ctx.range());
      if (victim != null) {
         GRIPS.put(player.getUUID(), new DesertEncierroAbility.Grip(victim.getId(), ctx.seed()));
         ServerFx.animate(player, ctx.animation(SunaSuna.id("desert_encierro")));
         sandArm(player, 15);
         ServerFx.sendNear(
            ctx.level(), SunaFx.ENCIERRO, player.getId(), victim.position(), Vec3.ZERO, (float)this.maxChannelTicks(), ctx.seed(), victim.getId(), 96.0
         );
      }
   }

   @Override
   public boolean channelTick(AbilityContext ctx, int ticks) {
      ServerPlayer player = ctx.player();
      DesertEncierroAbility.Grip grip = GRIPS.get(player.getUUID());
      ServerLevel level = ctx.level();
      if ((grip == null ? null : level.getEntity(grip.victim)) instanceof LivingEntity victim
         && victim.isAlive()
         && !(victim.distanceToSqr(player) > Math.pow((double)(ctx.range() + 3.0F), 2.0))) {
         PlayerFruitData data = ctx.data();
         if (!AbilityManager.freeEnergy(player)) {
            data.setEnergy(Math.max(0.0F, data.energy() - (float)ctx.extra("drainPerSecond") / 20.0F));
            if (data.energy() <= 0.0F) {
               return false;
            }
         }

         Vec3 spot = player.position().add(ctx.horizontalLook().scale(ctx.extra("hold") + (double)victim.getBbWidth() * 0.5)).add(0.0, 0.35, 0.0);
         Targeting.launch(victim, spot.subtract(victim.position()).scale(0.45));
         victim.fallDistance = 0.0F;
         if (ticks % 10 == 0) {
            sandArm(player, 15);
         }

         victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 4, false, false, false));
         if (ticks > 0 && ticks % 20 == 0) {
            victim.invulnerableTime = 0;
            Targeting.hurt(player, victim, ctx.damage(), SunaSuna.DEHYDRATION_DAMAGE);
            int seconds = (int)ctx.extra("dehydrateSeconds");
            if (seconds > 0) {
               SunaEffects.dehydrate(victim, seconds, 1, 4, player);
            }
         }

         return victim.isAlive();
      }

      return false;
   }

   @Override
   public void onChannelEnd(AbilityContext ctx, int ticks, boolean interrupted) {
      ServerPlayer player = ctx.player();
      DesertEncierroAbility.Grip grip = GRIPS.remove(player.getUUID());
      if (grip != null) {
         Entity victim = ctx.level().getEntity(grip.victim);
         Vec3 at = victim != null ? victim.position() : player.position();
         if (victim != null && victim.isAlive()) {
            Targeting.launch(victim, ctx.horizontalLook().scale(0.35).add(0.0, 0.1, 0.0));
         }

         BlockImpactService.push(ctx, ImpactShape.sphere(at, 1.6), ctx.horizontalLook(), 0.6);
         ServerFx.sendNear(ctx.level(), SunaFx.ENCIERRO_END, player.getId(), at, Vec3.ZERO, 1.0F, grip.seed, grip.victim, 96.0);
      }

      ServerFx.stopAnimation(player);
   }

   public static int victimId(ServerPlayer player) {
      DesertEncierroAbility.Grip grip = GRIPS.get(player.getUUID());
      return grip == null ? -1 : grip.victim;
   }

   private static record Grip(int victim, long seed) {
   }
}
