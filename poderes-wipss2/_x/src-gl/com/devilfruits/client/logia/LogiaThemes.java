package com.devilfruits.client.logia;

import com.devilfruits.core.logia.LogiaManager;
import com.devilfruits.core.logia.LogiaProfile;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public final class LogiaThemes {
   private static final Map<ResourceLocation, LogiaTheme> THEMES = new HashMap<>();

   public static void register(ResourceLocation element, LogiaTheme theme) {
      if (THEMES.putIfAbsent(element, theme) != null) {
         throw new IllegalStateException("Duplicate Logia theme " + element);
      }
   }

   public static void registerDebug(ResourceLocation element) {
      register(element, LogiaTheme.builder().colors(12166872, 7035532).shedding(3.0F, 2.0F).build());
   }

   public static LogiaTheme get(ResourceLocation element) {
      return THEMES.getOrDefault(element, LogiaTheme.DEFAULT);
   }

   public static LogiaTheme of(Player player) {
      LogiaProfile profile = LogiaManager.profile(player);
      return profile == null ? LogiaTheme.DEFAULT : get(profile.element());
   }

   public static LogiaTheme current(Player player) {
      LogiaTheme theme = of(player);
      return theme.hooks().look(player, theme);
   }

   private LogiaThemes() {
   }
}
