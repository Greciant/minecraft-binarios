package com.devilfruits.fruits.horo_horo.ability;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.fruits.horo_horo.HoroHoro;
import com.devilfruits.fruits.horo_horo.ghost.GhostForm;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class GhostFormAbility extends Ability {
   private final Set<UUID> went = new HashSet<>();

   public GhostFormAbility() {
      super(
         HoroHoro.id("ghost_form"), CastType.TOGGLE, AbilityStats.builder().energy(15).cooldown(300).mastery(4).damage(0.0).range(1.0).knockback(0.0).build()
      );
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.NONE;
   }

   @Nullable
   @Override
   public Component checkRequirements(ServerPlayer player, PlayerFruitData data) {
      return player.isPassenger() ? Component.translatable("message.devilfruits.horo_horo.dismount_first") : null;
   }

   @Override
   public void onToggle(AbilityContext ctx, boolean active) {
      ServerPlayer player = ctx.player();
      if (active) {
         if (GhostForm.start(player)) {
            this.went.add(player.getUUID());
         } else {
            AbilityManager.stopToggle(player, this);
         }
      } else if (GhostForm.active(player)) {
         GhostForm.finish(player, true);
      }
   }

   @Override
   public boolean pressWhileActive(ServerPlayer player) {
      if (!GhostForm.active(player)) {
         return false;
      } else {
         GhostForm.beginHold(player);
         return true;
      }
   }

   @Override
   public void onKeyRelease(ServerPlayer player) {
      GhostForm.endHold(player);
   }

   @Override
   public boolean cooldownOnToggleOff(ServerPlayer player) {
      return this.went.remove(player.getUUID());
   }

   @Override
   public boolean pausesRegeneration() {
      return false;
   }
}
