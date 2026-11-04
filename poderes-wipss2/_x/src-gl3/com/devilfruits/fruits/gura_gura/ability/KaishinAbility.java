package com.devilfruits.fruits.gura_gura.ability;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.BlockImpactService;
import com.devilfruits.core.physics.ImpactShape;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.core.util.DeterministicRandom;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.core.world.TerrainDestruction;
import com.devilfruits.fruits.gura_gura.GuraFx;
import com.devilfruits.fruits.gura_gura.GuraGura;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class KaishinAbility extends Ability {
   public KaishinAbility() {
      super(
         GuraGura.id("kaishin"),
         CastType.CHARGED,
         AbilityStats.builder()
            .energy(14)
            .cooldown(22)
            .mastery(1)
            .damage(11.0)
            .range(7.0)
            .knockback(1.5)
            .extra("chargeTicks", 36.0, 5.0, 200.0, "Ticks to reach a full charge.")
            .extra("fullChargeExtraEnergy", 22.0, 0.0, 500.0, "Additional energy at full charge.")
            .extra("fullChargeExtraCooldown", 60.0, 0.0, 2000.0, "Additional cooldown ticks at full charge.")
            .extra("coneAngle", 28.0, 5.0, 80.0, "Half-angle of the shockwave cone in degrees.")
            .extra("blockDamageCharge", 0.5, 0.0, 1.0, "Minimum charge (0-1) at which the punch tears out blocks.")
            .build()
      );
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.builder(this.defaults()).impact(PhysicsProfile.Impact.DIRECTIONAL).force(1.2).blockDamage(0.6).manual().build();
   }

   @Override
   public int maxChargeTicks() {
      return (int)this.config().extra("chargeTicks");
   }

   @Override
   public float energyCost(float charge) {
      return (float)((double)this.config().energyCost() + this.config().extra("fullChargeExtraEnergy") * (double)charge);
   }

   @Override
   public int cooldownTicks(float charge) {
      return (int)((double)this.config().cooldownTicks() + this.config().extra("fullChargeExtraCooldown") * (double)charge);
   }

   @Override
   public void onChargeStart(AbilityContext ctx) {
      ServerFx.animate(ctx.player(), ctx.animation(GuraGura.anim("kaishin_charge")));
   }

   @Override
   public void release(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      ServerLevel level = ctx.level();
      float charge = ctx.charge();
      Vec3 dir = ctx.look();
      Vec3 eye = ctx.eye();
      Vec3 fist = eye.add(0.0, -0.3, 0.0).add(dir.scale(0.7));
      float length = ctx.range() * (0.75F + charge);
      double halfAngle = Math.toRadians(ctx.extra("coneAngle") * (1.0 + 0.35 * (double)charge));
      float damage = ctx.damage() * (1.0F + 1.4F * charge);
      float knockback = ctx.knockback() * (0.8F + 1.2F * charge);
      ServerFx.animate(player, ctx.animation(GuraGura.anim("kaishin_release")));
      ServerFx.send(player, GuraFx.KAISHIN, fist, dir, charge, ctx.seed(), Mth.floor(length * 16.0F));

      for (Entity target : Targeting.inCone(level, eye, dir, (double)length, halfAngle, e -> Targeting.canPush(player, e))) {
         double dist = target.position().distanceTo(eye);
         float falloff = (float)Mth.clamp(1.0 - 0.55 * dist / (double)length, 0.35, 1.0);
         ctx.hurt(target, damage * falloff);
         Vec3 push = dir.scale((double)(knockback * falloff)).add(0.0, 0.25 + 0.25 * (double)charge, 0.0);
         Targeting.launch(target, push);
      }

      BlockImpactService.push(ctx, ImpactShape.cone(eye, dir, (double)length, halfAngle), dir, 0.5 + (double)charge);
      QuakeDeflect.sweep(player, ImpactShape.cone(eye, dir, (double)length, halfAngle), dir);
      if ((double)charge >= ctx.extra("blockDamageCharge") && TerrainDestruction.destructionAllowed(level)) {
         float power = GuraGura.destruction(ctx.mastery());
         ctx.budget(GuraGura.blockBudget(power));
         float reach = length * (float)Math.cbrt((double)power);
         this.shatterCone(ctx, eye, dir, reach, halfAngle, charge);
         BlockImpactService.damage(ctx, ImpactShape.cone(eye, dir, (double)reach, halfAngle), dir, 0.25 + 0.55 * (double)charge);
      }
   }

   private void shatterCone(AbilityContext ctx, Vec3 eye, Vec3 dir, float length, double halfAngle, float charge) {
      DeterministicRandom random = new DeterministicRandom(ctx.seed() ^ 24301L);
      Vec3 up = Math.abs(dir.y) > 0.95 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
      Vec3 side = dir.cross(up).normalize();
      Vec3 up2 = side.cross(dir).normalize();
      float chance = 0.25F + 0.55F * charge;

      for (float t = 1.5F; t <= length && ctx.budget().hasBlocks(); t += 0.6F) {
         double radius = (double)t * Math.tan(halfAngle) * 0.85;
         int samples = Mth.ceil(radius * radius * 1.4) + 1;

         for (int i = 0; i < samples; i++) {
            if (random.chance(chance)) {
               double r = Math.sqrt((double)random.nextFloat()) * radius;
               double a = (double)random.nextFloat() * Math.PI * 2.0;
               Vec3 point = eye.add(dir.scale((double)t)).add(side.scale(Math.cos(a) * r)).add(up2.scale(Math.sin(a) * r));
               BlockPos pos = BlockPos.containing(point);
               Vec3 velocity = dir.scale(0.5 + 0.6 * (double)charge * (double)(1.0F - t / length))
                  .add((double)random.signed(0.15F), 0.2 + (double)random.nextFloat() * 0.3, (double)random.signed(0.15F));
               TerrainDestruction.tearOut(ctx.level(), ctx.player(), pos, velocity, ctx.budget(), random.nextInt(80), true);
            }
         }
      }
   }
}
