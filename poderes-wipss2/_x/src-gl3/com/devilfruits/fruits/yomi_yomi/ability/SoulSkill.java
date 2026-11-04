package com.devilfruits.fruits.yomi_yomi.ability;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.fruits.yomi_yomi.YomiYomi;
import com.devilfruits.fruits.yomi_yomi.soul.SoulProjection;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public abstract class SoulSkill extends Ability {
   protected SoulSkill(String name, CastType castType, AbilityStats stats) {
      super(YomiYomi.id(name), castType, stats);
   }

   @Nullable
   @Override
   public Component checkRequirements(ServerPlayer player, PlayerFruitData data) {
      if (!SoulProjection.active(player)) {
         return Component.translatable("message.devilfruits.yomi_yomi.not_projecting");
      } else {
         return SoulProjection.recoiling(player) ? Component.translatable("message.devilfruits.yomi_yomi.soul_returning") : null;
      }
   }
}
