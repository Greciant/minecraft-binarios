package com.awakenedmod.awakened.content.kineticcharger;

import com.awakenedmod.awakened.content.AwakenedEntities;
import com.awakenedmod.awakened.content.entity.projectile.KineticProjectile;
import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class KineticThrowAbility extends KineticHoldAbility {
   public static final String ID = "kinetic_throw";
   private static final int MAX_CHARGE_TICKS = 24;
   private static final int SAFETY_TICKS = 1200;
   private static final double AIM_DISTANCE = 64.0;
   private static final float INACCURACY = 0.5F;
   private static final float VELOCITY_TAP = 1.9F;
   private static final float VELOCITY_FULL = 2.9F;
   private static final float IMPACT_TAP = 4.0F;
   private static final float IMPACT_FULL = 9.0F;
   private static final float AOE_TAP = 1.5F;
   private static final float AOE_FULL = 4.0F;
   private static final float RADIUS_TAP = 2.0F;
   private static final float RADIUS_FULL = 3.5F;
   private static final float KNOCK_TAP = 0.5F;
   private static final float KNOCK_FULL = 1.0F;
   private static final float POUR_MULTIPLIER = 1.5F;
   private static final float DAMAGE_HEAVINESS_SHARE = 0.5F;
   private static final float BASE_COST = 8.0F;
   private static final float FEAT_CHARGE = 0.9F;
   private static final String CHARGE_CLIP = "kinetic_throw_charge";
   private static final String RELEASE_CLIP = "kinetic_throw_release";
   private static final String FLICK_CLIP = "kinetic_throw_flick";

   public KineticThrowAbility() {
      super("kinetic_throw", 8.0F, 0, 1200);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().boostsDamage().build();
   }

   @Override
   public float staminaCost(ServerPlayer player) {
      return this.cost(player);
   }

   @Override
   public String costLabel(Player viewer) {
      return viewer == null ? this.costLabel() : String.valueOf(Math.round(this.billed(this.cost(viewer))));
   }

   private float cost(Player player) {
      ItemStack ammo = KineticAmmo.pick(player);
      return ammo.isEmpty() ? this.staminaCost() : this.staminaCost() * KineticAmmo.heaviness(ammo);
   }

   @Override
   public String activationAnimation(ServerPlayer player) {
      return "kinetic_throw_charge";
   }

   @Override
   protected int pourFromTick() {
      return 24;
   }

   @Override
   protected KineticHoldAbility.Hold newHold() {
      return new KineticThrowAbility.Windup();
   }

   @Override
   protected boolean begin(ServerPlayer player, KineticHoldAbility.Hold hold) {
      ItemStack ammo = KineticAmmo.pick(player);
      if (ammo.isEmpty()) {
         return this.fail(
            player,
            KineticAmmo.MAIN_HAND_ONLY.enabled(player) ? "awakened.kinetic_charger.no_ammo_main_hand" : "awakened.kinetic_charger.no_ammo",
            new Object[0]
         );
      } else {
         KineticThrowAbility.Windup windup = (KineticThrowAbility.Windup)hold;
         windup.item = ammo.copyWithCount(1);
         windup.heaviness = KineticAmmo.heaviness(windup.item);
         if (!player.getAbilities().instabuild) {
            ammo.shrink(1);
         }

         KineticFx.chargeStart(player);
         return true;
      }
   }

   @Override
   protected void tick(ServerPlayer player, KineticHoldAbility.Hold hold) {
      KineticFx.chargeTick(player, hold.ticks, chargeFraction(hold), KineticBank.pourFraction(hold.poured));
   }

   @Override
   protected void end(ServerPlayer player, KineticHoldAbility.Hold hold, KineticHoldAbility.End reason) {
      if (reason != KineticHoldAbility.End.TORN_DOWN && reason != KineticHoldAbility.End.CANCELLED) {
         this.release(player, (KineticThrowAbility.Windup)hold);
      }
   }

   private void release(ServerPlayer player, KineticThrowAbility.Windup windup) {
      ServerLevel level = player.serverLevel();
      float f = chargeFraction(windup);
      float pour = windup.poured / 60.0F;
      float boost = 1.0F + pour * 1.5F;
      float h = windup.heaviness;
      float damageWeight = 1.0F + (h - 1.0F) * 0.5F;
      KineticProjectile projectile = new KineticProjectile(AwakenedEntities.KINETIC_CHARGE.get(), player, level, windup.item);
      Vec3 hand = KineticFx.fist(player);
      projectile.setPos(hand.x, hand.y, hand.z);
      float radius = lerp(2.0F, 3.5F, f) * boost;
      projectile.configure(
         KineticBank.empoweredDamage(player, lerp(4.0F, 9.0F, f) * damageWeight * boost, this.balanceKey()),
         KineticBank.empoweredDamage(player, lerp(1.5F, 4.0F, f) * damageWeight * boost, this.balanceKey()),
         radius,
         lerp(0.5F, 1.0F, f) * boost
      );
      projectile.setVisualStyle(1);
      projectile.setOnTick(pos -> KineticFx.wake(level, pos, projectile.getDeltaMovement(), projectile.tickCount, pour));
      projectile.setCustomDetonation(true);
      projectile.setOnDetonateSurface((pos, normal) -> KineticFx.impact(level, pos, normal, (double)radius));
      if (f >= 0.9F) {
         armFullHurlFeat(player, projectile);
      }

      Vec3 dir = AimUtil.throwDirection(player, hand, 64.0);
      projectile.shoot(dir.x, dir.y, dir.z, lerp(1.9F, 2.9F, f), 0.5F);
      level.addFreshEntity(projectile);
      player.swing(InteractionHand.MAIN_HAND);
      PowerService.playAnimation(player, windup.ticks <= 24 ? "kinetic_throw_flick" : "kinetic_throw_release");
      KineticFx.throwRelease(player, Math.min(1.0F, (f + pour) * 0.5F));
   }

   private static float chargeFraction(KineticHoldAbility.Hold hold) {
      return Math.min(1.0F, (float)hold.ticks / 24.0F);
   }

   private static void armFullHurlFeat(ServerPlayer player, KineticProjectile projectile) {
      projectile.setOnDirectHit(struck -> {
         if (Foes.whole(struck) instanceof LivingEntity victim && Foes.defeatedBy(victim, player)) {
            QuestSystem.feat(player, "kinetic_full_hurl");
         }
      });
   }

   private static float lerp(float a, float b, float f) {
      return a + (b - a) * f;
   }

   private static final class Windup extends KineticHoldAbility.Hold {
      private ItemStack item = ItemStack.EMPTY;
      private float heaviness = 1.0F;
   }
}
