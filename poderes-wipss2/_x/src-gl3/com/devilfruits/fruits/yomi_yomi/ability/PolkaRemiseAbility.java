package com.devilfruits.fruits.yomi_yomi.ability;

import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.combat.Stun;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.BlockImpactService;
import com.devilfruits.core.physics.ImpactShape;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.fruits.yomi_yomi.YomiFx;
import com.devilfruits.fruits.yomi_yomi.YomiSounds;
import com.devilfruits.fruits.yomi_yomi.YomiYomi;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class PolkaRemiseAbility extends BladeAbility {
   public static final int POLKA_BEAT = 4;
   private static final Map<UUID, PolkaRemiseAbility.Flurry> FLURRIES = new HashMap<>();

   public PolkaRemiseAbility() {
      super(
         "polka_remise",
         CastType.HELD,
         AbilityStats.builder()
            .energy(20)
            .cooldown(220)
            .mastery(4)
            .damage(1.0)
            .range(5.0)
            .knockback(0.0)
            .extra("coneDegrees", 45.0, 10.0, 120.0, "Full angle of the cone the thrusts fill.")
            .extra("slide", 1.1, 0.0, 3.0, "Speed of the slide before the thrusts (about 2.4 blocks).")
            .extra("lockTicks", 6.0, 0.0, 40.0, "Ticks each landed thrust staggers its target (the hit-lock).")
            .build()
      );
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.builder(this.defaults()).impact(PhysicsProfile.Impact.DIRECTIONAL).force(0.2).blockDamage(0.0).manual().build();
   }

   @Override
   public int maxChannelTicks() {
      return 81;
   }

   @Override
   public int minChannelTicks() {
      return 10;
   }

   @Override
   public void onChannelStart(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      FLURRIES.put(player.getUUID(), new PolkaRemiseAbility.Flurry());
      ServerFx.animate(player, ctx.animation(YomiYomi.id("polka_remise")));
      Vec3 flat = ctx.horizontalLook();
      ServerFx.send(player, YomiFx.POLKA, player.position(), flat, (float)ctx.extra("coneDegrees"), ctx.seed(), 38);
      Targeting.moveSelf(player, flat.scale(ctx.extra("slide")).add(0.0, Math.max(0.0, player.getDeltaMovement().y), 0.0));
      ctx.level().playSound(null, player.getX(), player.getY(), player.getZ(), YomiSounds.DASH.get(), SoundSource.PLAYERS, 0.6F, 1.3F);
   }

   @Override
   public boolean channelTick(AbilityContext ctx, int ticks) {
      PolkaRemiseAbility.Flurry flurry = FLURRIES.get(ctx.player().getUUID());
      if (flurry == null) {
         return false;
      } else {
         int due = ticks < 4 ? 0 : Math.min(38, (ticks - 4) / 2 + 1);

         while (flurry.thrusts < due) {
            flurry.thrusts++;
            thrust(ctx, flurry);
         }

         return flurry.thrusts < 38 || ticks < 80;
      }
   }

   @Override
   public void onChannelEnd(AbilityContext ctx, int ticks, boolean interrupted) {
      ServerPlayer player = ctx.player();
      FLURRIES.remove(player.getUUID());
      if (!interrupted) {
         ServerFx.animate(player, YomiYomi.id("polka_finish"));
      }

      ServerFx.send(player, YomiFx.POLKA_END, player.position(), Vec3.ZERO, 0.0F, 0L, interrupted ? 1 : 0);
   }

   private static void thrust(AbilityContext ctx, PolkaRemiseAbility.Flurry flurry) {
      ServerPlayer player = ctx.player();
      ServerLevel level = ctx.level();
      Vec3 eye = player.getEyePosition();
      Vec3 look = ctx.look();
      double half = Math.toRadians(ctx.extra("coneDegrees")) * 0.5;
      List<Entity> hit = Targeting.inCone(level, eye, look, (double)ctx.range(), half, ex -> ex != player && Targeting.canHurt(player, ex));
      float damage = ctx.damage();
      int lock = (int)ctx.extra("lockTicks");

      for (Entity e : hit) {
         e.invulnerableTime = 0;
         if (ctx.hurt(e, damage) && e instanceof LivingEntity living) {
            e.setDeltaMovement(e.getDeltaMovement().multiply(0.2, 1.0, 0.2));
            Stun.apply(living, lock, false);
            int n = flurry.hits.merge(e.getUUID(), 1, Integer::sum);
            Vec3 at = e.getBoundingBox().getCenter();
            Vec3 point = at.add(eye.subtract(at).normalize().scale(Math.min(0.45, (double)e.getBbWidth() * 0.6)));
            ServerFx.send(e, YomiFx.POLKA_HIT, point, look, damage, level.getRandom().nextLong(), n % 4 == 0 ? 1 : 0);
         }
      }

      BlockImpactService.strike(ctx, ImpactShape.cone(eye, look, (double)ctx.range(), half), look, 0.15);
      level.playSound(null, eye.x, eye.y, eye.z, YomiSounds.FLURRY.get(), SoundSource.PLAYERS, 0.55F, Mth.nextFloat(level.getRandom(), 0.9F, 1.15F));
      if (!hit.isEmpty()) {
         level.playSound(null, eye.x, eye.y, eye.z, YomiSounds.BLADE_HIT.get(), SoundSource.PLAYERS, 0.45F, Mth.nextFloat(level.getRandom(), 1.1F, 1.4F));
      }
   }

   public static int thrusts(ServerPlayer player) {
      PolkaRemiseAbility.Flurry flurry = FLURRIES.get(player.getUUID());
      return flurry == null ? -1 : flurry.thrusts;
   }

   private static final class Flurry {
      int thrusts;
      final Map<UUID, Integer> hits = new HashMap<>();
   }
}
