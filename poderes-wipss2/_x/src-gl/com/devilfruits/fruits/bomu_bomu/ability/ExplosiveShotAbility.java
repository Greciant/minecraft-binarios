package com.devilfruits.fruits.bomu_bomu.ability;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.fx.ExplosionProfile;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.fruits.bomu_bomu.BomuAbilities;
import com.devilfruits.fruits.bomu_bomu.BomuBomu;
import com.devilfruits.fruits.bomu_bomu.BomuFx;
import com.devilfruits.fruits.bomu_bomu.BomuSounds;
import com.devilfruits.fruits.bomu_bomu.entity.BombBullet;
import com.devilfruits.fruits.bomu_bomu.explosion.ExplosionSpec;
import com.devilfruits.fruits.bomu_bomu.item.BombPistolItem;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class ExplosiveShotAbility extends Ability {
   public ExplosiveShotAbility() {
      super(
         BomuBomu.id("explosive_shot"),
         CastType.INSTANT,
         AbilityStats.builder()
            .energy(14)
            .cooldown(16)
            .mastery(4)
            .damage(16.0)
            .range(4.0)
            .knockback(1.3)
            .blockDamage(true)
            .extra("bulletSpeed", 5.0, 1.0, 12.0, "Speed of the shot of breath (blocks per tick).")
            .extra("maxDistance", 96.0, 8.0, 256.0, "Blocks the breath flies before it goes off in the air.")
            .extra("maxRadius", 6.5, 1.0, 16.0, "Cap of the blast radius (blocks); keep it below the Self-Destruct's.")
            .extra("maxPlayerDamage", 18.0, 0.0, 200.0, "Most damage the blast deals to a player (PvP cap).")
            .build()
      );
   }

   @Override
   public boolean coatable() {
      return true;
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.builder(this.defaults()).impact(PhysicsProfile.Impact.RADIAL).force(0.8).blockDamage(0.8).manual().build();
   }

   @Nullable
   @Override
   public Component checkRequirements(ServerPlayer player, PlayerFruitData data) {
      InteractionHand hand = BombPistolItem.hand(player);
      if (hand == null) {
         return Component.translatable("ability.devilfruits.bomu_bomu.explosive_shot.no_pistol");
      } else {
         ItemStack pistol = player.getItemInHand(hand);
         if (BombPistolItem.rounds(pistol) <= 0) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), BomuSounds.SHOT_CLICK.get(), SoundSource.PLAYERS, 0.6F, 0.8F);
            return Component.translatable("ability.devilfruits.bomu_bomu.explosive_shot.empty");
         } else {
            return !BombPistolItem.bombBreath(pistol) ? Component.translatable("ability.devilfruits.bomu_bomu.explosive_shot.plain_air") : null;
         }
      }
   }

   @Override
   public void activate(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      InteractionHand hand = BombPistolItem.hand(player);
      if (hand != null) {
         ItemStack pistol = player.getItemInHand(hand);
         if (!player.getAbilities().instabuild) {
            BombPistolItem.setRounds(pistol, BombPistolItem.rounds(pistol) - 1);
            pistol.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
         }

         player.getCooldowns().addCooldown(pistol.getItem(), this.cooldownTicks(1.0F));
         Vec3 muzzle = BombPistolItem.muzzle(player, hand);
         Vec3 dir = ctx.aimFrom(muzzle);
         BombBullet.shoot(player, muzzle, dir.scale(ctx.extra("bulletSpeed")), spec(ctx), (int)Math.ceil(ctx.extra("maxDistance") / ctx.extra("bulletSpeed")));
         player.level()
            .playSound(null, muzzle.x, muzzle.y, muzzle.z, BomuSounds.SHOT_FIRE.get(), SoundSource.PLAYERS, 1.3F, 0.95F + player.getRandom().nextFloat() * 0.1F);
         ServerFx.send(player, BomuFx.SHOT_FIRE, muzzle, dir, 1.0F, ctx.seed(), hand == InteractionHand.MAIN_HAND ? 1 : 3);
         ServerFx.animate(player, ctx.animation(BomuBomu.id(hand == InteractionHand.MAIN_HAND ? "pistol_fire" : "pistol_fire_left")));
      }
   }

   public static ExplosionSpec spec(AbilityContext ctx) {
      float radius = (float)Math.min((double)ctx.range(), ctx.extra("maxRadius"));
      return new ExplosionSpec(
         radius, radius, 1.0F, ctx.damage(), ctx.knockback(), 0.04F, radius >= 5.5F ? ExplosionProfile.LARGE : ExplosionProfile.MEDIUM, BomuAbilities.SHOT.id()
      );
   }
}
