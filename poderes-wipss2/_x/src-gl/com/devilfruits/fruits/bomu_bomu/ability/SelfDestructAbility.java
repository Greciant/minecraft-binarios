package com.devilfruits.fruits.bomu_bomu.ability;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.form.FormFlags;
import com.devilfruits.core.fx.ExplosionProfile;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.fruits.bomu_bomu.BomuAbilities;
import com.devilfruits.fruits.bomu_bomu.BomuBomu;
import com.devilfruits.fruits.bomu_bomu.BomuFx;
import com.devilfruits.fruits.bomu_bomu.BomuTriggers;
import com.devilfruits.fruits.bomu_bomu.explosion.BombExplosionService;
import com.devilfruits.fruits.bomu_bomu.explosion.ExplosionSpec;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.phys.Vec3;

public class SelfDestructAbility extends Ability {
   private static final ResourceLocation PIN = BomuBomu.id("vanished");
   private static final float[] TIER_COST = new float[]{0.25F, 0.55F, 1.0F};
   private static final float[] TIER_COOLDOWN = new float[]{0.15F, 0.45F, 1.0F};
   private static final Map<UUID, SelfDestructAbility.Vanish> VANISHED = new HashMap<>();
   private static final Map<UUID, Integer> TIER_REACHED = new HashMap<>();

   public SelfDestructAbility() {
      super(
         BomuBomu.id("self_destruct"),
         CastType.CHARGED,
         AbilityStats.builder()
            .energy(130)
            .cooldown(1200)
            .mastery(6)
            .damage(30.0)
            .range(8.0)
            .knockback(2.6)
            .blockDamage(true)
            .extra("chargeSeconds", 2.5, 0.5, 10.0, "Seconds of charging to the full tier.")
            .extra("tier1", 0.1, 0.0, 1.0, "Charge fraction of tier 1 (a small body pulse); let go earlier and it fizzles.")
            .extra("tier2", 0.5, 0.0, 1.0, "Charge fraction of tier 2 (a medium blast).")
            .extra("maxRadius", 12.0, 2.0, 32.0, "Cap of the full tier's blast radius (blocks); the radius is the range, which grows with mastery.")
            .extra("vanishSeconds", 1.5, 0.2, 10.0, "Seconds the body is gone in its own blast before it reforms.")
            .extra("invulnerableSeconds", 0.6, 0.0, 5.0, "Seconds of invulnerability right after the blast.")
            .extra("fireChance", 0.06, 0.0, 1.0, "Chance of each candidate block round a full blast catching fire.")
            .extra("maxPlayerDamage", 40.0, 0.0, 400.0, "Most damage the blast deals to a player (PvP cap).")
            .build()
      );
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.builder(this.defaults()).impact(PhysicsProfile.Impact.RADIAL).force(2.0).blockDamage(1.0).manual().build();
   }

   @Override
   public int maxChargeTicks() {
      return Math.max(1, (int)Math.round(this.config().extra("chargeSeconds") * 20.0));
   }

   public int tier(float charge) {
      if (charge >= 0.999F) {
         return 3;
      } else if ((double)charge >= this.config().extra("tier2")) {
         return 2;
      } else {
         return (double)charge >= this.config().extra("tier1") ? 1 : 0;
      }
   }

   @Override
   public float energyCost(float charge) {
      int t = this.tier(charge);
      return t == 0 ? 0.0F : (float)this.config().energyCost() * TIER_COST[t - 1];
   }

   @Override
   public int cooldownTicks(float charge) {
      int t = this.tier(charge);
      return t == 0 ? 0 : Math.round((float)this.config().cooldownTicks() * TIER_COOLDOWN[t - 1]);
   }

   @Override
   public float masteryXp(float charge) {
      return this.tier(charge) == 0 ? 0.0F : super.masteryXp(charge);
   }

   @Override
   public void onChargeStart(AbilityContext ctx) {
      TIER_REACHED.put(ctx.player().getUUID(), 0);
      ServerFx.animate(ctx.player(), ctx.animation(BomuBomu.id("self_destruct_charge")));
   }

   @Override
   public void onChargeTick(AbilityContext ctx, int ticks) {
      int t = this.tier(Math.min(1.0F, (float)ticks / (float)this.maxChargeTicks()));
      Integer before = TIER_REACHED.put(ctx.player().getUUID(), t);
      if (before != null && t > before) {
         ServerFx.send(ctx.player(), BomuFx.SELF_DESTRUCT_TIER, ctx.player().position(), Vec3.ZERO, (float)t, 0L, t);
      }
   }

   @Override
   public void onChargeCancel(ServerPlayer player) {
      TIER_REACHED.remove(player.getUUID());
   }

   @Override
   public void release(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      TIER_REACHED.remove(player.getUUID());
      int tier = this.tier(ctx.charge());
      if (tier == 0) {
         ServerFx.stopAnimation(player);
      } else {
         Vec3 at = player.position().add(0.0, (double)player.getBbHeight() * 0.5, 0.0);
         BombExplosionService.Result result = BombExplosionService.detonate(player.serverLevel(), at, spec(ctx, tier), player);
         if (!result.cancelled() && result.blocks() + result.structureBlocks() > 0) {
            BomuTriggers.SELF_DESTRUCTION.get().trigger(player);
         }

         long now = player.level().getGameTime();
         int vanish = (int)Math.round(ctx.extra("vanishSeconds") * 20.0);
         VANISHED.put(
            player.getUUID(), new SelfDestructAbility.Vanish(now + (long)vanish, now + Math.round(ctx.extra("invulnerableSeconds") * 20.0), player.position())
         );
         pin(player, true);
         FormFlags.set(player, BomuBomu.VANISHED, true);
         ServerFx.send(player, BomuFx.SELF_DESTRUCT_VANISH, at, Vec3.ZERO, (float)tier, ctx.seed(), vanish);
         ServerFx.animate(player, ctx.animation(BomuBomu.id("self_destruct_blast")));
      }
   }

   public static ExplosionSpec spec(AbilityContext ctx, int tier) {
      float full = (float)Math.min((double)ctx.range(), ctx.extra("maxRadius"));

      float radius = switch (tier) {
         case 1 -> Math.min(2.5F, full);
         case 2 -> Math.min(5.0F, full);
         default -> full;
      };
      float share = tier == 1 ? 0.3F : (tier == 2 ? 0.6F : 1.0F);
      ExplosionProfile profile = tier == 1
         ? ExplosionProfile.SMALL
         : (tier == 2 ? ExplosionProfile.MEDIUM : (radius >= 10.0F ? ExplosionProfile.MASSIVE : ExplosionProfile.LARGE));
      return new ExplosionSpec(
         radius,
         radius,
         tier == 1 ? 0.5F : 1.0F,
         ctx.damage() * share,
         ctx.knockback() * share,
         tier == 3 ? (float)ctx.extra("fireChance") : 0.0F,
         profile,
         BomuAbilities.SELF_DESTRUCT.id()
      );
   }

   public static boolean vanished(ServerPlayer player) {
      return VANISHED.containsKey(player.getUUID());
   }

   public static boolean invulnerable(ServerPlayer player) {
      SelfDestructAbility.Vanish v = VANISHED.get(player.getUUID());
      return v != null && player.level().getGameTime() < v.invulnerableUntil();
   }

   public static void tick(ServerPlayer player) {
      SelfDestructAbility.Vanish v = VANISHED.get(player.getUUID());
      if (v != null) {
         if (player.level().getGameTime() < v.reformAt() && player.isAlive()) {
            if (player.position().distanceToSqr(v.at()) > 0.04) {
               player.teleportTo(v.at().x, v.at().y, v.at().z);
            }
         } else {
            reform(player);
         }
      }
   }

   public static void reform(ServerPlayer player) {
      if (VANISHED.remove(player.getUUID()) != null) {
         pin(player, false);
         FormFlags.set(player, BomuBomu.VANISHED, false);
         if (player.isAlive()) {
            ServerFx.send(
               player,
               BomuFx.SELF_DESTRUCT_REFORM,
               player.position().add(0.0, (double)player.getBbHeight() * 0.5, 0.0),
               Vec3.ZERO,
               1.0F,
               player.getRandom().nextLong(),
               0
            );
            ServerFx.animate(player, BomuBomu.id("self_destruct_reform"));
         }
      }
   }

   private static void pin(ServerPlayer player, boolean on) {
      modifier(player, Attributes.MOVEMENT_SPEED, on);
      modifier(player, Attributes.JUMP_STRENGTH, on);
      modifier(player, Attributes.GRAVITY, on);
      if (on) {
         player.setDeltaMovement(Vec3.ZERO);
         player.connection.send(new ClientboundSetEntityMotionPacket(player));
      }
   }

   private static void modifier(ServerPlayer player, Holder<Attribute> attribute, boolean on) {
      AttributeInstance a = player.getAttribute(attribute);
      if (a != null) {
         a.removeModifier(PIN);
         if (on) {
            a.addTransientModifier(new AttributeModifier(PIN, -1.0, Operation.ADD_MULTIPLIED_TOTAL));
         }
      }
   }

   public static void forget(ServerPlayer player) {
      reform(player);
      TIER_REACHED.remove(player.getUUID());
   }

   public static void clear() {
      VANISHED.clear();
      TIER_REACHED.clear();
   }

   private static record Vanish(long reformAt, long invulnerableUntil, Vec3 at) {
   }
}
