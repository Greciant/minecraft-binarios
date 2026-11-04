package com.devilfruits.fruits.yomi_yomi.ability;

import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.BlockImpactService;
import com.devilfruits.core.physics.ImpactShape;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.core.util.ServerScheduler;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.fruits.yomi_yomi.YomiFx;
import com.devilfruits.fruits.yomi_yomi.YomiSounds;
import com.devilfruits.fruits.yomi_yomi.YomiYomi;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class AubadeAbility extends BladeAbility {
   public AubadeAbility() {
      super(
         "aubade",
         CastType.INSTANT,
         AbilityStats.builder()
            .energy(12)
            .cooldown(120)
            .mastery(1)
            .damage(9.0)
            .range(8.0)
            .knockback(0.4)
            .extra("slowTicks", 50.0, 0.0, 400.0, "Ticks the thrust slows what it pierced (2.5 s).")
            .extra("width", 0.7, 0.2, 2.0, "Half width of the piercing line in blocks.")
            .build()
      );
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.builder(this.defaults()).impact(PhysicsProfile.Impact.DIRECTIONAL).force(0.4).blockDamage(0.0).manual().build();
   }

   @Override
   public void activate(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      ServerLevel level = ctx.level();
      AbilityManager.setBusy(player, 9);
      ServerFx.animate(player, ctx.animation(YomiYomi.id("aubade")));
      ServerScheduler.later(level, 7, () -> {
         if (present(player, level)) {
            thrust(ctx, ctx.look());
         }
      });
   }

   public static List<Entity> thrust(AbilityContext ctx, Vec3 look) {
      ServerPlayer player = ctx.player();
      ServerLevel level = ctx.level();
      Vec3[] line = line(player, look, (double)ctx.range());
      double width = ctx.extra("width");
      List<Entity> hit = Targeting.inCapsule(level, player, line[0], line[1], width);
      float damage = ctx.damage();
      int slow = (int)ctx.extra("slowTicks");

      for (Entity e : hit) {
         if (ctx.hurt(e, damage) && e instanceof LivingEntity) {
            LivingEntity living = (LivingEntity)e;
            if (slow > 0) {
               living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slow, 1), player);
            }

            Targeting.push(e, look.scale((double)ctx.knockback()).add(0.0, 0.12, 0.0));
         }
      }

      BlockImpactService.strike(ctx, ImpactShape.capsule(line[0], line[1], width), look);
      Vec3 flat = new Vec3(look.x, 0.0, look.z);
      if (flat.lengthSqr() > 1.0E-4) {
         Targeting.moveSelf(player, flat.normalize().scale(0.45).add(0.0, Math.max(0.0, player.getDeltaMovement().y), 0.0));
      }

      ServerFx.send(player, YomiFx.AUBADE, line[0], line[1], (float)width, ctx.seed(), hit.size());
      level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), YomiSounds.THRUST.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
      if (!hit.isEmpty()) {
         level.playSound(null, line[1].x, line[1].y, line[1].z, YomiSounds.BLADE_HIT.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
      }

      return hit;
   }

   public static Vec3[] line(ServerPlayer player, Vec3 look, double range) {
      Vec3 from = player.getEyePosition().add(0.0, -0.35, 0.0).add(look.scale(0.6));
      Vec3 to = from.add(look.scale(range));
      HitResult wall = player.level().clip(new ClipContext(from, to, Block.COLLIDER, Fluid.NONE, player));
      if (wall.getType() != Type.MISS) {
         to = wall.getLocation();
      }

      return new Vec3[]{from, to};
   }
}
