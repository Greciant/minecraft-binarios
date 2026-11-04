package com.devilfruits.fruits.supa_supa.ability;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.haki.HakiCombat;
import com.devilfruits.core.seastone.SeastoneGear;
import com.devilfruits.fruits.supa_supa.BladeLimbs;
import com.devilfruits.fruits.supa_supa.SupaAbilities;
import com.devilfruits.fruits.supa_supa.SupaFx;
import com.devilfruits.fruits.supa_supa.SupaSounds;
import com.devilfruits.fruits.supa_supa.SupaSupa;
import com.devilfruits.fruits.supa_supa.SupaTriggers;
import com.devilfruits.registry.DFAttachments;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.phys.Vec3;

public class SteelBodyAbility extends Ability {
   private static final ResourceLocation SLOW = SupaSupa.id("steel_body_slow");
   private static final ResourceLocation STEADY = SupaSupa.id("steel_body_steady");
   public static final float STEEL_WALL_DAMAGE = 20.0F;

   public SteelBodyAbility() {
      super(
         SupaSupa.id("steel_body"),
         CastType.HELD,
         AbilityStats.builder()
            .energy(10)
            .cooldown(60)
            .mastery(2)
            .extra(
               "damageMultiplier",
               0.05,
               0.0,
               1.0,
               "Share of other blows (fists, falls, explosions) that still reaches a body of steel; blades, bullets and arrows never do."
            )
            .extra("drainPerSecond", 5.0, 0.0, 100.0, "Energy Steel Body drains every second.")
            .extra("drainPerDamage", 1.2, 0.0, 20.0, "Energy drained for every point of damage the steel turns aside.")
            .extra("speedMultiplier", 0.25, 0.0, 1.0, "Walking speed in Steel Body (x normal).")
            .extra("maxSeconds", 30.0, 1.0, 600.0, "Longest Steel Body lasts in one go, seconds.")
            .build()
      );
   }

   @Override
   public int maxChannelTicks() {
      return (int)(this.config().extra("maxSeconds") * 20.0);
   }

   @Override
   public void onChannelStart(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      if (ctx.data().isToggleActive(SupaAbilities.BLADE_SKATES.id())) {
         AbilityManager.stopToggle(player, SupaAbilities.BLADE_SKATES);
      }

      BladeLimbs.arms(player, BladeLimbs.ArmMode.NONE, false, false, 0);
      BladeLimbs.steel(player, true);
      modifier(player, Attributes.MOVEMENT_SPEED, SLOW, this.config().extra("speedMultiplier") - 1.0, Operation.ADD_MULTIPLIED_TOTAL);
      modifier(player, Attributes.KNOCKBACK_RESISTANCE, STEADY, 1.0, Operation.ADD_VALUE);
      ServerFx.animate(player, ctx.animation(SupaSupa.id("steel_guard")));
      player.level().playSound(null, player.getX(), player.getY() + 1.0, player.getZ(), SupaSounds.STEEL_ON.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
   }

   @Override
   public boolean channelTick(AbilityContext ctx, int ticks) {
      ServerPlayer player = ctx.player();
      PlayerFruitData data = ctx.data();
      if (!AbilityManager.freeEnergy(player)) {
         data.setEnergy(Math.max(0.0F, data.energy() - (float)this.config().extra("drainPerSecond") / 20.0F));
      }

      return data.energy() > 0.0F || AbilityManager.freeEnergy(player);
   }

   @Override
   public void onChannelEnd(AbilityContext ctx, int ticks, boolean interrupted) {
      ServerPlayer player = ctx.player();
      remove(player, Attributes.MOVEMENT_SPEED, SLOW);
      remove(player, Attributes.KNOCKBACK_RESISTANCE, STEADY);
      BladeLimbs.steel(player, false);
      ServerFx.stopAnimation(player);
      player.level().playSound(null, player.getX(), player.getY() + 1.0, player.getZ(), SupaSounds.STEEL_OFF.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
   }

   public static boolean active(Player player) {
      return player instanceof ServerPlayer && AbilityManager.channellingAbility(player) == SupaAbilities.STEEL_BODY;
   }

   public static boolean passes(DamageSource source) {
      if (!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
         && !source.is(DamageTypes.STARVE)
         && !source.is(DamageTypes.DROWN)
         && !source.is(DamageTypes.IN_WALL)
         && !HakiCombat.coatedStrike(source)
         && !HakiCombat.ryuo(source)) {
         ItemStack weapon = source.getWeaponItem();
         if (source.getDirectEntity() == source.getEntity() && weapon != null && weapon.is(SeastoneGear.GEAR)) {
            return true;
         } else {
            if (source.getDirectEntity() instanceof AbstractArrow arrow && SeastoneGear.isSeastoneArrow(arrow)) {
               return true;
            }

            return false;
         }
      } else {
         return true;
      }
   }

   public static float absorb(ServerPlayer player, DamageSource source, float amount) {
      if (!(amount <= 0.0F) && !passes(source)) {
         SteelBodyAbility ability = SupaAbilities.STEEL_BODY;
         float through = steelProof(source) ? 0.0F : amount * (float)ability.config().extra("damageMultiplier");
         float aside = amount - through;
         PlayerFruitData data = DFAttachments.data(player);
         if (!AbilityManager.freeEnergy(player)) {
            data.setEnergy(Math.max(0.0F, data.energy() - aside * (float)ability.config().extra("drainPerDamage")));
         }

         if (aside >= 20.0F) {
            SupaTriggers.STEEL_WALL.get().trigger(player);
         }

         clang(player, strikePoint(player, source), aside);
         return through;
      } else {
         return amount;
      }
   }

   public static boolean steelProof(DamageSource source) {
      if (!source.is(DamageTypeTags.IS_PROJECTILE) && !(source.getDirectEntity() instanceof Projectile)) {
         ItemStack weapon = source.getWeaponItem();
         return weapon != null && !weapon.isEmpty() && source.getDirectEntity() == source.getEntity()
            ? weapon.is(ItemTags.SWORDS)
               || weapon.is(ItemTags.AXES)
               || weapon.getItem() instanceof SwordItem
               || weapon.getItem() instanceof AxeItem
               || weapon.getItem() instanceof TridentItem
            : false;
      } else {
         return true;
      }
   }

   static Vec3 strikePoint(ServerPlayer player, DamageSource source) {
      Vec3 centre = player.getBoundingBox().getCenter();
      Vec3 from = source.getSourcePosition();
      if (from == null && source.getDirectEntity() != null) {
         from = source.getDirectEntity().position();
      }

      if (from == null) {
         return centre;
      } else {
         Vec3 dir = from.subtract(centre);
         Vec3 flat = new Vec3(dir.x, 0.0, dir.z);
         if (flat.lengthSqr() < 1.0E-4) {
            return centre.add(0.0, 0.6, 0.0);
         } else {
            flat = flat.normalize().scale((double)player.getBbWidth() * 0.55);
            double y = Math.max(-0.7, Math.min(0.7, dir.y * 0.4));
            return centre.add(flat).add(0.0, y, 0.0);
         }
      }
   }

   public static void clang(ServerPlayer player, Vec3 at, float damage) {
      ServerFx.sendNear(
         player.serverLevel(), SupaFx.CLANG, player.getId(), at, at.subtract(player.getBoundingBox().getCenter()), damage, (long)player.tickCount, 0, 48.0
      );
      float pitch = 1.3F - Math.min(0.6F, damage * 0.02F);
      player.level().playSound(null, at.x, at.y, at.z, SupaSounds.STEEL_CLANG.get(), SoundSource.PLAYERS, Math.min(1.4F, 0.5F + damage * 0.05F), pitch);
   }

   public static boolean bounce(ServerPlayer player, Projectile projectile) {
      if (projectile instanceof AbstractArrow arrow && SeastoneGear.isSeastoneArrow(arrow)) {
         return false;
      }

      Vec3 v = projectile.getDeltaMovement();
      Vec3 normal = projectile.position().subtract(player.getBoundingBox().getCenter()).multiply(1.0, 0.3, 1.0);
      normal = normal.lengthSqr() < 1.0E-4 ? v.scale(-1.0).normalize() : normal.normalize();
      Vec3 out = v.subtract(normal.scale(2.0 * v.dot(normal)))
         .scale(0.35)
         .add((player.getRandom().nextDouble() - 0.5) * 0.12, 0.08, (player.getRandom().nextDouble() - 0.5) * 0.12);
      projectile.setDeltaMovement(out);
      projectile.hasImpulse = true;
      if (projectile instanceof AbstractArrow arrow) {
         arrow.setBaseDamage(arrow.getBaseDamage() * 0.5);
      }

      clang(player, projectile.position(), (float)v.length() * 2.5F);
      return true;
   }

   private static void modifier(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation id, double amount, Operation op) {
      AttributeInstance instance = player.getAttribute(attribute);
      if (instance != null) {
         instance.removeModifier(id);
         instance.addTransientModifier(new AttributeModifier(id, amount, op));
      }
   }

   private static void remove(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation id) {
      AttributeInstance instance = player.getAttribute(attribute);
      if (instance != null) {
         instance.removeModifier(id);
      }
   }

   public static void clear(ServerPlayer player) {
      remove(player, Attributes.MOVEMENT_SPEED, SLOW);
      remove(player, Attributes.KNOCKBACK_RESISTANCE, STEADY);
   }
}
