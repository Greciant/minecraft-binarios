package com.devilfruits.core.form;

import com.devilfruits.core.data.PlayerFruitData;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

@FunctionalInterface
public interface FormCondition {
   @Nullable
   Component check(ServerPlayer var1, PlayerFruitData var2);
}
