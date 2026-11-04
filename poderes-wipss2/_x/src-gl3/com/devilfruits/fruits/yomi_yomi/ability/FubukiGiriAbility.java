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
import com.devilfruits.core.world.TerrainDestruction;
import com.devilfruits.fruits.yomi_yomi.YomiFx;
import com.devilfruits.fruits.yomi_yomi.YomiSounds;
import com.devilfruits.fruits.yomi_yomi.YomiYomi;
import com.devilfruits.fruits.yomi_yomi.frost.YomiFrost;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

public class FubukiGiriAbility extends BladeAbility {
   private static final double STEP = 0.25;

   public FubukiGiriAbility() {
      super(
         "fubuki_giri",
         CastType.INSTANT,
         AbilityStats.builder()
            .energy(24)
            .cooldown(280)
            .mastery(6)
            .damage(12.0)
            .range(5.0)
            .knockback(0.0)
            .extra("freezeTicks", 60.0, 0.0, 400.0, "Ticks the ice blades freeze what they cut (3 s of full paralysis).")
            .extra("pathRadius", 1.6, 0.5, 4.0, "Half width of the strip the ice blades burst from.")
            .build()
      );
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.builder(this.defaults())
         .impact(PhysicsProfile.Impact.DIRECTIONAL)
         .force(0.5)
         .reaction(PhysicsProfile.Reaction.DASH)
         .blockDamage(0.0)
         .manual()
         .build();
   }

   @Override
   public void activate(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      ServerLevel level = ctx.level();
      AbilityManager.setBusy(player, 29);
      ServerFx.animate(player, ctx.animation(YomiYomi.id("fubuki_giri")));
      Vec3[] path = new Vec3[2];
      Set<Entity> passed = new LinkedHashSet<>();
      ServerScheduler.schedule(level, 0, age -> {
         if (!present(player, level)) {
            return true;
         } else {
            int dash = age - 6;
            if (dash == 0) {
               Vec3[] p = path(player, ctx.horizontalLook(), ctx.config().range());
               path[0] = p[0];
               path[1] = p[1];
               ServerFx.send(player, YomiFx.FUBUKI_DASH, p[0], p[1], 1.0F, ctx.seed(), 4);
               level.playSound(null, player.getX(), player.getY(), player.getZ(), YomiSounds.DASH.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            }

            if (dash >= 0 && dash < 4) {
               player.fallDistance = 0.0F;
               Vec3 from = player.position();
               Vec3 to = path[0].lerp(path[1], (double)(dash + 1) / 4.0);
               move(player, to);
               passed.addAll(Targeting.inCapsule(level, player, from.add(0.0, 0.9, 0.0), to.add(0.0, 0.9, 0.0), 0.9));
            } else if (dash == 4) {
               Targeting.moveSelf(player, new Vec3(0.0, Math.min(0.0, player.getDeltaMovement().y), 0.0));
               if (player.position().distanceToSqr(path[1]) > 4.0) {
                  place(player, path[1]);
               }
            }

            if (age == 17) {
               level.playSound(null, player.getX(), player.getY(), player.getZ(), YomiSounds.CANE_SLIDE.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
            }

            if (age == 27) {
               burst(ctx, path[0], path[1], passed);
               return true;
            } else {
               return false;
            }
         }
      });
   }

   public static Vec3[] path(ServerPlayer player, Vec3 dir, double range) {
      Vec3 start = player.position();
      Vec3 d = new Vec3(dir.x, 0.0, dir.z);
      d = d.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : d.normalize();
      AABB box = player.getBoundingBox();
      Vec3 at = start;

      for (double s = 0.25; s <= range + 1.0E-6; s += 0.25) {
         Vec3 next = at.add(d.scale(0.25));
         if (free(player, box, start, next)) {
            at = next;
         } else {
            if (!free(player, box, start, at.add(0.0, 1.0, 0.0)) || !free(player, box, start, next.add(0.0, 1.0, 0.0))) {
               break;
            }

            at = next.add(0.0, 1.0, 0.0);
         }
      }

      return new Vec3[]{start, at};
   }

   private static boolean free(ServerPlayer player, AABB box, Vec3 start, Vec3 at) {
      return player.level().noCollision(player, box.move(at.subtract(start)).deflate(0.001));
   }

   private static void move(ServerPlayer player, Vec3 to) {
      if (player instanceof FakePlayer) {
         player.moveTo(to.x, to.y, to.z, player.getYRot(), player.getXRot());
      } else {
         Targeting.moveSelf(player, to.subtract(player.position()));
      }
   }

   private static void place(ServerPlayer player, Vec3 at) {
      if (player instanceof FakePlayer) {
         player.moveTo(at.x, at.y, at.z, player.getYRot(), player.getXRot());
      } else {
         player.teleportTo(at.x, at.y, at.z);
      }
   }

   public static List<LivingEntity> burst(AbilityContext ctx, Vec3 a, Vec3 b, Set<Entity> passed) {
      ServerPlayer player = ctx.player();
      ServerLevel level = ctx.level();
      double radius = ctx.extra("pathRadius");
      Set<Entity> cut = new LinkedHashSet<>(passed);
      cut.addAll(Targeting.inCapsule(level, player, a.add(0.0, 0.9, 0.0), b.add(0.0, 0.9, 0.0), radius));
      int freeze = (int)ctx.extra("freezeTicks");
      float damage = ctx.damage();
      List<LivingEntity> frozen = new ArrayList<>();

      for (Entity e : cut) {
         if (e.isAlive() && Targeting.canHurt(player, e)) {
            e.invulnerableTime = 0;
            if (ctx.hurt(e, damage) && e instanceof LivingEntity) {
               LivingEntity living = (LivingEntity)e;
               if (living.isAlive()) {
                  YomiFrost.freeze(living, freeze);
                  frozen.add(living);
               }
            }
         }
      }

      BlockImpactService.strike(ctx, ImpactShape.capsule(a.add(0.0, 0.6, 0.0), b.add(0.0, 0.6, 0.0), radius), b.subtract(a));
      if (YomiYomi.flag("freezeWater")) {
         freezeWater(ctx, a, b, radius);
      }

      ServerFx.send(player, YomiFx.FUBUKI_BURST, a, b, (float)radius, ctx.seed(), 0);
      level.playSound(null, player.getX(), player.getY(), player.getZ(), YomiSounds.CANE_CLICK.get(), SoundSource.PLAYERS, 1.3F, 1.0F);
      level.playSound(null, player.getX(), player.getY(), player.getZ(), YomiSounds.CANE_SHEATHE.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
      Vec3 mid = a.lerp(b, 0.5);
      level.playSound(null, mid.x, mid.y, mid.z, YomiSounds.ICE_GROW.get(), SoundSource.PLAYERS, 1.4F, 1.0F);
      return frozen;
   }

   private static void freezeWater(AbilityContext ctx, Vec3 a, Vec3 b, double radius) {
      ServerLevel level = ctx.level();
      double len = a.distanceTo(b);
      BlockState ice = Blocks.FROSTED_ICE.defaultBlockState();
      Set<BlockPos> done = new HashSet<>();

      for (double s = 0.0; s <= len + 0.01; s += 0.5) {
         Vec3 p = len < 0.001 ? a : a.lerp(b, s / len);

         for (int dx = (int)(-Math.ceil(radius)); (double)dx <= Math.ceil(radius); dx++) {
            for (int dz = (int)(-Math.ceil(radius)); (double)dz <= Math.ceil(radius); dz++) {
               if (!((double)(dx * dx + dz * dz) > radius * radius + 0.5)) {
                  for (int dy = -2; dy <= 0; dy++) {
                     BlockPos pos = BlockPos.containing(p.x + (double)dx, p.y + (double)dy, p.z + (double)dz);
                     if (done.add(pos)
                        && level.getFluidState(pos).isSourceOfType(Fluids.WATER)
                        && level.getBlockState(pos).is(Blocks.WATER)
                        && level.getBlockState(pos.above()).isAir()
                        && TerrainDestruction.transform(
                           level, ctx.player(), pos, ice, ctx.budget(), 160 + level.getRandom().nextInt(80), level.getBlockState(pos), 2
                        )) {
                        level.scheduleTick(pos, Blocks.FROSTED_ICE, 60 + level.getRandom().nextInt(60));
                     }
                  }
               }
            }
         }
      }
   }
}
