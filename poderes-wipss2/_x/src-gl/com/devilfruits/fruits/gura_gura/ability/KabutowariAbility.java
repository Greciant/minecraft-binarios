package com.devilfruits.fruits.gura_gura.ability;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.BlockImpactService;
import com.devilfruits.core.physics.ImpactShape;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.core.physics.StructureForces;
import com.devilfruits.core.util.DeterministicRandom;
import com.devilfruits.core.util.ServerScheduler;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.core.world.TerrainDestruction;
import com.devilfruits.fruits.gura_gura.GuraFx;
import com.devilfruits.fruits.gura_gura.GuraGura;
import com.devilfruits.fruits.gura_gura.quake.FissurePattern;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;

public class KabutowariAbility extends Ability {
   private static final Map<UUID, Long> SLAMMING = new HashMap<>();

   @Override
   public boolean coatable() {
      return true;
   }

   public KabutowariAbility() {
      super(
         GuraGura.id("kabutowari"),
         CastType.INSTANT,
         AbilityStats.builder()
            .energy(30)
            .cooldown(160)
            .mastery(2)
            .damage(16.0)
            .range(7.0)
            .knockback(1.1)
            .extra("lines", 7.0, 3.0, 16.0, "Number of main fissure lines.")
            .extra("leapVelocity", 0.95, 0.2, 3.0, "Upward velocity of the leap.")
            .extra("spreadSpeed", 1.6, 0.2, 10.0, "Fissure growth in blocks per tick.")
            .extra("liftChance", 0.7, 0.0, 1.0, "Chance that a block on a fissure line is torn out.")
            .build()
      );
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.builder(this.defaults())
         .impact(PhysicsProfile.Impact.RADIAL)
         .force(1.2)
         .reaction(PhysicsProfile.Reaction.LANDING)
         .blockDamage(0.5)
         .manual()
         .build();
   }

   @Override
   public void activate(final AbilityContext ctx) {
      final ServerPlayer player = ctx.player();
      final ServerLevel level = ctx.level();
      AbilityManager.setBusy(player, 60);
      ServerFx.animate(player, ctx.animation(GuraGura.anim("kabutowari_leap")));
      SLAMMING.put(player.getUUID(), level.getGameTime() + 100L);
      ServerScheduler.later(level, 4, () -> {
         if (player.isAlive()) {
            Vec3 flat = ctx.horizontalLook();
            Vec3 leap = flat.scale(0.35).add(0.0, ctx.extra("leapVelocity"), 0.0);
            StructureForces.takeoff(player, leap);
            Targeting.launch(player, leap);
            ServerFx.send(player, GuraFx.KABUTOWARI_LEAP, player.position(), Vec3.ZERO, 1.0F, ctx.seed(), 0);
         }
      });
      ServerScheduler.schedule(level, 6, new ServerScheduler.Task() {
         boolean diving;

         @Override
         public boolean tick(int age) {
            if (player.isAlive() && player.level() == level) {
               if (!this.diving && age > 3 && player.getDeltaMovement().y < 0.15) {
                  this.diving = true;
                  Targeting.launch(player, new Vec3(player.getDeltaMovement().x * 0.5, -1.7, player.getDeltaMovement().z * 0.5));
               }

               if ((age <= 2 || !player.onGround()) && age <= 40 && !player.isInWater()) {
                  return false;
               } else {
                  KabutowariAbility.this.impact(ctx);
                  return true;
               }
            } else {
               KabutowariAbility.SLAMMING.remove(player.getUUID());
               AbilityManager.clearBusy(player);
               return true;
            }
         }
      });
   }

   private void impact(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      ServerLevel level = ctx.level();
      AbilityManager.clearBusy(player);
      player.fallDistance = 0.0F;
      SLAMMING.put(player.getUUID(), level.getGameTime() + 10L);
      BlockPos center = player.blockPosition();
      BlockPos ground = TerrainDestruction.findSurface(level, center.getX(), center.getY(), center.getZ(), 1, 4);
      int groundY = ground != null ? ground.getY() : center.getY() - 1;
      Vec3 epicentre = new Vec3(player.getX(), (double)groundY + 1.0, player.getZ());
      float power = GuraGura.destruction(ctx.mastery());
      float grow = (float)Math.sqrt((double)power);
      int lines = Math.round((float)((int)ctx.extra("lines") + (ctx.mastery() - 1) / 3) * grow);
      float length = ctx.range() * grow;
      ctx.budget(GuraGura.blockBudget(power));
      long seed = ctx.seed();
      ServerFx.animate(player, ctx.animation(GuraGura.anim("kabutowari_slam")));
      ServerFx.sendNear(level, GuraFx.KABUTOWARI_SLAM, player.getId(), epicentre, Vec3.ZERO, length, seed, lines, 96.0);

      record Target(float dist, BlockPos pos, float width) {
      }

      for (Entity e : Targeting.inRadius(level, epicentre, 3.2, x -> Targeting.canPush(player, x))) {
         ctx.hurt(e, ctx.damage());
         Vec3 out = e.position().subtract(epicentre).multiply(1.0, 0.0, 1.0);
         Vec3 outward = out.lengthSqr() > 1.0E-4 ? out.normalize() : Vec3.ZERO;
         Targeting.launch(e, outward.scale(0.45).add(0.0, (double)ctx.knockback(), 0.0));
      }

      QuakeDeflect.sweep(player, epicentre.add(0.0, 1.0, 0.0), 3.2 + (double)length * 0.15);
      BlockImpactService.slam(ctx, epicentre, 3.2 + (double)length * 0.15, 1.0);
      BlockImpactService.damage(ctx, ImpactShape.sphere(epicentre, 2.6), new Vec3(0.0, 1.0, 0.0), 1.0);
      List<Target> targets = new ArrayList<>();
      Set<BlockPos> seen = new HashSet<>();
      List<ImpactShape> cracks = new ArrayList<>();

      for (FissurePattern.Line line : FissurePattern.generate(seed, lines, length)) {
         for (int i = 0; i + 2 < line.size(); i += 2) {
            cracks.add(
               ImpactShape.capsule(
                  new Vec3(epicentre.x + (double)line.x()[i], epicentre.y - 0.5, epicentre.z + (double)line.z()[i]),
                  new Vec3(epicentre.x + (double)line.x()[i + 2], epicentre.y - 0.5, epicentre.z + (double)line.z()[i + 2]),
                  0.6 + 0.4 * (double)line.width()
               )
            );
         }

         for (int i = 0; i < line.size(); i++) {
            int bx = Mth.floor(epicentre.x + (double)line.x()[i]);
            int bz = Mth.floor(epicentre.z + (double)line.z()[i]);
            BlockPos surface = TerrainDestruction.findSurface(level, bx, groundY, bz, 2, 4);
            if (surface != null && seen.add(surface)) {
               targets.add(new Target(line.dist()[i], surface, line.width()));
            }
         }
      }

      targets.sort((a, b) -> Float.compare(a.dist, b.dist));
      if (!cracks.isEmpty()) {
         BlockImpactService.damage(ctx, ImpactShape.union(cracks), new Vec3(0.0, 1.0, 0.0), 1.0);
      }

      boolean destruction = TerrainDestruction.destructionAllowed(level);
      float spread = (float)ctx.extra("spreadSpeed");
      float liftChance = (float)ctx.extra("liftChance");
      IntSet hit = new IntOpenHashSet();
      DeterministicRandom random = new DeterministicRandom(seed ^ 61781L);
      int[] cursor = new int[]{0};
      ServerScheduler.schedule(level, 0, age -> {
         float reach = (float)(age + 1) * spread;

         while (cursor[0] < targets.size() && targets.get(cursor[0]).dist <= reach) {
            Target t = targets.get((int)(cursor[0]++));
            Vec3 top = Vec3.atBottomCenterOf(t.pos.above());
            if (destruction && random.chance(liftChance * t.width)) {
               Vec3 velocity = new Vec3((double)random.signed(0.12F), 0.38 + (double)random.nextFloat() * 0.4, (double)random.signed(0.12F));
               TerrainDestruction.tearOut(level, player, t.pos, velocity, ctx.budget(), cursor[0] * 2, true);
            }

            for (Entity e : Targeting.inRadius(level, top.add(0.0, 0.8, 0.0), 1.6, x -> !hit.contains(x.getId()) && Targeting.canPush(player, x))) {
               hit.add(e.getId());
               ctx.hurt(e, ctx.damage() * 0.6F);
               Targeting.launch(e, new Vec3((double)random.signed(0.2F), (double)ctx.knockback() * 0.8 + 0.2, (double)random.signed(0.2F)));
            }
         }

         return cursor[0] >= targets.size() || age > 60;
      });
   }

   public static void onFall(LivingFallEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         Long until = SLAMMING.get(player.getUUID());
         if (until != null) {
            if (until >= player.level().getGameTime()) {
               event.setCanceled(true);
            }

            if (until < player.level().getGameTime() || player.onGround()) {
               SLAMMING.remove(player.getUUID());
            }
         }
      }
   }
}
