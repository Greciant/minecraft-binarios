package com.devilfruits.fruits.suke_suke.ability;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.fruits.suke_suke.SukeAbilities;
import com.devilfruits.fruits.suke_suke.SukeSuke;
import com.devilfruits.fruits.suke_suke.clear.Cloak;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class CloakAbility extends Ability {
   private final boolean shared;
   private static final Set<UUID> HANDOVER = new HashSet<>();

   public CloakAbility(String name, boolean shared, int mastery, int cooldown) {
      super(
         SukeSuke.id(name), CastType.TOGGLE, AbilityStats.builder().energy(0).cooldown(cooldown).mastery(mastery).damage(0.0).range(1.0).knockback(0.0).build()
      );
      this.shared = shared;
   }

   public boolean shared() {
      return this.shared;
   }

   private CloakAbility other() {
      return this.shared ? SukeAbilities.INVISIBILITY : SukeAbilities.GROUP_INVISIBILITY;
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.NONE;
   }

   @Override
   public float masteryXp(float charge) {
      return this.shared ? 6.0F : 3.0F;
   }

   @Nullable
   @Override
   public Component checkRequirements(ServerPlayer player, PlayerFruitData data) {
      return this.shared && Cloak.candidates(player).isEmpty() ? Component.translatable("message.devilfruits.suke_suke.no_one_near") : null;
   }

   @Override
   public void onToggle(AbilityContext ctx, boolean active) {
      ServerPlayer player = ctx.player();
      if (active) {
         if (ctx.data().isToggleActive(this.other().id())) {
            HANDOVER.add(player.getUUID());

            try {
               AbilityManager.stopToggle(player, this.other());
            } finally {
               HANDOVER.remove(player.getUUID());
            }
         }

         Cloak.start(player, this.shared);
      } else if (!HANDOVER.contains(player.getUUID())) {
         Cloak.end(player);
      }
   }

   @Override
   public void toggleTick(AbilityContext ctx) {
      if (!Cloak.tick(ctx.player(), ctx.data(), this.shared)) {
         AbilityManager.stopToggle(ctx.player(), this);
      }
   }

   @Override
   public boolean cooldownOnToggleOff(ServerPlayer player) {
      return !HANDOVER.contains(player.getUUID());
   }
}
