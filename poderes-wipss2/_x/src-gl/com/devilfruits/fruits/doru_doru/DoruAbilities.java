package com.devilfruits.fruits.doru_doru;

import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.FormSwitchAbility;
import com.devilfruits.fruits.doru_doru.ability.CandleServiceAbility;
import com.devilfruits.fruits.doru_doru.ability.CandleWallAbility;
import com.devilfruits.fruits.doru_doru.ability.WaxAtelierAbility;
import com.devilfruits.fruits.doru_doru.ability.WaxLanceAbility;
import com.devilfruits.fruits.doru_doru.ability.WaxToolsAbility;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.entity.player.Player;

public final class DoruAbilities {
   public static final WaxToolsAbility TOOLS = new WaxToolsAbility();
   public static final WaxAtelierAbility ATELIER = new WaxAtelierAbility();
   public static final CandleWallAbility WALL = new CandleWallAbility();
   public static final WaxLanceAbility LANCE = new WaxLanceAbility();
   public static final CandleServiceAbility SERVICE = new CandleServiceAbility();
   public static final FormSwitchAbility CHAMPION = new FormSwitchAbility(
      DoruDoru.id("candle_champion"), "champion", AbilityStats.builder().energy(0).cooldown(0).mastery(10).damage(0.0).range(1.0).knockback(0.0).build()
   );
   private static final Map<UUID, Integer> COLORS = new HashMap<>();

   public static int color(Player player) {
      return COLORS.getOrDefault(player.getUUID(), 0);
   }

   public static void setColor(Player player, int color) {
      COLORS.put(player.getUUID(), Math.floorMod(color, 16));
   }

   public static void forget(UUID player) {
      COLORS.remove(player);
   }

   private DoruAbilities() {
   }
}
