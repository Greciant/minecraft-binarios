package com.devilfruits.client.hud;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.logia.LogiaTheme;
import com.devilfruits.client.logia.LogiaThemes;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.fruit.DevilFruit;
import com.devilfruits.core.logia.LogiaCounter;
import com.devilfruits.core.logia.LogiaData;
import com.devilfruits.core.logia.LogiaProfile;
import com.devilfruits.core.logia.LogiaState;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class LogiaHud {
   public static final ResourceLocation WARNING = DevilFruits.id("logia/warning");
   private static final int TANGIBLE_COLOR = 16752474;
   private static final int WARNING_COLOR = 16757575;
   private static final int STATE_COLOR = 14211288;

   public static String counterKey(DevilFruit fruit, LogiaCounter counter) {
      return "logia_counter." + fruit.id().getNamespace() + "." + fruit.id().getPath() + "." + counter.id();
   }

   static boolean render(GuiGraphics gg, Minecraft mc, DevilFruit fruit, PlayerFruitData data, int x, int y, boolean horizontal, float partial) {
      LogiaProfile profile = fruit.logia();
      if (profile == null) {
         return false;
      } else {
         Font font = mc.font;
         LogiaData logia = data.logia();
         LogiaState state = logia.state();
         float time = mc.level == null ? 0.0F : (float)mc.level.getGameTime() + partial;
         boolean tangible = !logia.intangible() && state != LogiaState.ELEMENTAL_BODY && state != LogiaState.SUPPRESSED;
         LogiaTheme theme = LogiaThemes.get(profile.element());
         gg.blitSprite(theme.stateIcon(tangible ? "tangible" : state.serializedName()), x, y, 9, 9);
         int cx = x + 11;
         int cy = y;
         if (horizontal && (state != LogiaState.SOLID || tangible)) {
            Component name = Component.translatable(tangible ? "hud.devilfruits.logia.tangible" : state.translationKey());
            gg.drawString(font, name, cx, y + 1, tangible ? 16752474 : 14211288, true);
            cx += font.width(name) + 5;
         }

         float pulse = 0.55F + 0.45F * Mth.sin(time * 0.45F);
         int active = logia.activeCounters();
         List<LogiaCounter> counters = profile.counters();
         boolean anyCounter = false;

         for (int i = 0; i < counters.size(); i++) {
            if ((active & 1 << i) != 0) {
               anyCounter = true;
               LogiaCounter counter = counters.get(i);
               if (!horizontal) {
                  cx = x;
                  cy -= 11;
               }

               gg.setColor(1.0F, 1.0F, 1.0F, pulse);
               gg.blitSprite(counter.hudIcon() != null ? counter.hudIcon() : WARNING, cx, cy, 9, 9);
               gg.setColor(1.0F, 1.0F, 1.0F, 1.0F);
               if (horizontal) {
                  Component label = Component.translatable(counterKey(fruit, counter));
                  gg.drawString(font, label, cx + 11, cy + 1, 16757575, true);
                  cx += 11 + font.width(label) + 5;
               }
            }
         }

         if (!anyCounter && data.suppressed()) {
            if (!horizontal) {
               cx = x;
               cy -= 11;
            }

            gg.setColor(1.0F, 1.0F, 1.0F, pulse);
            gg.blitSprite(DFHud.SUPPRESSED, cx, cy, 9, 9);
            gg.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            if (horizontal) {
               gg.drawString(font, Component.translatable("hud.devilfruits.suppressed"), cx + 11, cy + 1, 7317759, true);
            }
         }

         return true;
      }
   }

   private LogiaHud() {
   }
}
