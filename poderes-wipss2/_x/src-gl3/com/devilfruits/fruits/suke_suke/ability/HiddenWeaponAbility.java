package com.devilfruits.fruits.suke_suke.ability;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.fruits.suke_suke.SukeSounds;
import com.devilfruits.fruits.suke_suke.SukeSuke;
import com.devilfruits.fruits.suke_suke.clear.Clear;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

public class HiddenWeaponAbility extends Ability {
   public HiddenWeaponAbility() {
      super(
         SukeSuke.id("hidden_weapon"),
         CastType.TOGGLE,
         AbilityStats.builder()
            .energy(0)
            .cooldown(20)
            .mastery(2)
            .damage(0.0)
            .range(1.0)
            .knockback(0.0)
            .extra("drainPerSecond", 0.4, 0.0, 50.0, "Energy Hidden Weapon drains every second.")
            .build()
      );
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.NONE;
   }

   @Override
   public void onToggle(AbilityContext ctx, boolean active) {
      ServerPlayer player = ctx.player();
      Clear.hideItems(player, active);
      player.level()
         .playSound(
            null, player.getX(), player.getY(), player.getZ(), active ? SukeSounds.VANISH.get() : SukeSounds.APPEAR.get(), SoundSource.PLAYERS, 0.35F, 1.6F
         );
   }

   @Override
   public void toggleTick(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      if (!Clear.hiddenItems(player)) {
         Clear.hideItems(player, true);
      }

      if (!AbilityManager.freeEnergy(player)) {
         ctx.data().setEnergy(Math.max(0.0F, ctx.data().energy() - (float)ctx.extra("drainPerSecond") / 20.0F));
         if (ctx.data().energy() <= 0.0F) {
            AbilityManager.stopToggle(player, this);
         }
      }
   }

   @Override
   public boolean pausesRegeneration() {
      return false;
   }
}
