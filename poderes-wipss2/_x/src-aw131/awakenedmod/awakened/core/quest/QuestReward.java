package com.awakenedmod.awakened.core.quest;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public interface QuestReward {
   ItemStack preview();

   Component label();

   boolean grant(ServerPlayer var1);
}
