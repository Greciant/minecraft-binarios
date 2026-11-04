package com.devilfruits.fruits.suke_suke.ability;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.fruits.suke_suke.SukeSuke;
import com.devilfruits.fruits.suke_suke.clear.ShipCloak;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class ShipInvisibilityAbility extends Ability {
   private final Set<UUID> hid = new HashSet<>();

   public ShipInvisibilityAbility() {
      super(
         SukeSuke.id("ship_invisibility"),
         CastType.TOGGLE,
         AbilityStats.builder().energy(120).cooldown(3600).mastery(8).damage(0.0).range(16.0).knockback(0.0).build()
      );
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.NONE;
   }

   @Nullable
   @Override
   public Component checkRequirements(ServerPlayer player, PlayerFruitData data) {
      if (!ShipCloak.world().available()) {
         return Component.translatable("message.devilfruits.suke_suke.needs_sable");
      } else {
         ShipCloak.Ship ship = ShipCloak.target(player);
         if (ship == null) {
            return Component.translatable("message.devilfruits.suke_suke.no_ship");
         } else {
            return ShipCloak.hidden(ship.id()) ? Component.translatable("message.devilfruits.suke_suke.ship_hidden") : null;
         }
      }
   }

   @Override
   public void onToggle(AbilityContext ctx, boolean active) {
      ServerPlayer player = ctx.player();
      if (active) {
         ShipCloak.Ship ship = ShipCloak.target(player);
         if (ship == null) {
            AbilityManager.stopToggle(player, this);
            return;
         }

         this.hid.add(player.getUUID());
         ServerFx.animate(player, ctx.animation(SukeSuke.id("ship_cast")));
         ShipCloak.start(player, ship, (int)Math.round(ShipCloak.seconds(ctx.mastery()) * 20.0));
      } else {
         for (UUID id : ShipCloak.hiddenBy(player)) {
            ShipCloak.stop(player.getServer(), id);
         }
      }
   }

   @Override
   public void toggleTick(AbilityContext ctx) {
      if (ShipCloak.hiddenBy(ctx.player()).isEmpty()) {
         AbilityManager.stopToggle(ctx.player(), this);
      }
   }

   @Override
   public boolean cooldownOnToggleOff(ServerPlayer player) {
      return this.hid.remove(player.getUUID());
   }

   @Override
   public boolean pausesRegeneration() {
      return false;
   }
}
