package com.devilfruits.fruits.yomi_yomi.ability;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.data.PlayerFruitData;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class PendingAbility extends Ability {
   public PendingAbility(ResourceLocation id, CastType castType, AbilityStats stats) {
      super(id, castType, stats);
   }

   @Nullable
   @Override
   public Component checkRequirements(ServerPlayer player, PlayerFruitData data) {
      return Component.translatable("message.devilfruits.yomi_yomi.not_yet", new Object[]{this.displayName()});
   }
}
