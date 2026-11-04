package com.devilfruits.fruits.yomi_yomi.ability;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.fruits.yomi_yomi.YomiYomi;
import com.devilfruits.fruits.yomi_yomi.soul.SoulPhase;
import com.devilfruits.fruits.yomi_yomi.soul.SoulProjection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class SoulProjectionAbility extends Ability {
   private final Set<UUID> went = new HashSet<>();

   public SoulProjectionAbility() {
      super(
         YomiYomi.id("soul_projection"),
         CastType.TOGGLE,
         AbilityStats.builder().energy(20).cooldown(600).mastery(1).damage(0.0).range(1.0).knockback(0.0).build()
      );
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.NONE;
   }

   @Nullable
   @Override
   public Component checkRequirements(ServerPlayer player, PlayerFruitData data) {
      if (SoulPhase.deathPhase(player)) {
         return Component.translatable("message.devilfruits.yomi_yomi.no_body_to_leave");
      } else {
         return SoulProjection.recoiling(player) ? Component.translatable("message.devilfruits.yomi_yomi.soul_returning") : null;
      }
   }

   @Override
   public void onToggle(AbilityContext ctx, boolean active) {
      ServerPlayer player = ctx.player();
      if (active) {
         if (SoulProjection.start(player)) {
            this.went.add(player.getUUID());
         } else {
            AbilityManager.stopToggle(player, this);
         }
      } else {
         SoulProjection.beginRecoil(player);
      }
   }

   @Override
   public boolean cooldownOnToggleOff(ServerPlayer player) {
      return this.went.remove(player.getUUID());
   }

   @Override
   public boolean pausesRegeneration() {
      return true;
   }
}
