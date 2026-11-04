package com.devilfruits.fruits.yomi_yomi.ability;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.fruits.yomi_yomi.YomiYomi;
import com.devilfruits.fruits.yomi_yomi.item.SoulCaneItem;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public abstract class BladeAbility extends Ability {
   protected BladeAbility(String name, CastType castType, AbilityStats stats) {
      super(YomiYomi.id(name), castType, stats);
   }

   @Override
   public boolean coatable() {
      return true;
   }

   @Nullable
   @Override
   public Component checkRequirements(ServerPlayer player, PlayerFruitData data) {
      return SoulCaneItem.ready(player) ? null : Component.translatable("message.devilfruits.yomi_yomi.need_blade");
   }

   protected static boolean present(ServerPlayer player, ServerLevel level) {
      return player.isAlive() && !player.isRemoved() && player.level() == level;
   }
}
