package com.awakenedmod.awakened.client.screen.card;

import com.awakenedmod.awakened.client.ClientPowerState;
import com.awakenedmod.awakened.core.power.Power;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

public final class CardContext {
   public final Minecraft mc;
   public final Font font;
   public final ClientPowerState state;
   public final Power power;
   public final Screen screen;
   private final Map<String, Object> memory;
   private final IntConsumer switchTab;

   public CardContext(Minecraft mc, Font font, ClientPowerState state, Power power, Screen screen, Map<String, Object> memory, IntConsumer switchTab) {
      this.mc = mc;
      this.font = font;
      this.state = state;
      this.power = power;
      this.screen = screen;
      this.memory = memory;
      this.switchTab = switchTab;
   }

   public int lines(Component c, int w) {
      List<FormattedText> l = this.font.getSplitter().splitLines(c, Math.max(8, w), Style.EMPTY);
      return l.size();
   }

   public <T> T get(String key, T fallback) {
      Object v = this.memory.get(key);
      return (T)(v == null ? fallback : v);
   }

   public void put(String key, Object value) {
      if (value == null) {
         this.memory.remove(key);
      } else {
         this.memory.put(key, value);
      }
   }

   public void openTab(int tab) {
      this.switchTab.accept(tab);
   }

   public static Map<String, Object> newMemory() {
      return new HashMap<>();
   }
}
