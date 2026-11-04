package com.awakenedmod.awakened.client.screen.card.faces;

import com.awakenedmod.awakened.client.ClientConfig;
import com.awakenedmod.awakened.client.ExoClientState;
import com.awakenedmod.awakened.client.ExoWheel;
import com.awakenedmod.awakened.client.PassiveToggleClient;
import com.awakenedmod.awakened.client.screen.HudPlacementScreen;
import com.awakenedmod.awakened.client.screen.card.CardContext;
import com.awakenedmod.awakened.client.screen.card.CardFace;
import com.awakenedmod.awakened.client.screen.card.CardGfx;
import com.awakenedmod.awakened.client.voidview.VoidIntroMode;
import com.awakenedmod.awakened.core.power.PassiveToggle;
import com.awakenedmod.awakened.core.power.Power;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;

public final class SettingsFace implements CardFace {
   private static final int HEAD = 13;
   private static final int PITCH = 14;
   private static final int GAP = 4;
   private static final double[] SHAKE_STEPS = new double[]{0.0, 0.5, 1.0, 1.5};
   private static final String[] SHAKE_KEYS = new String[]{"off", "low", "normal", "strong"};
   private static final String[] SHAKE_NAMES = new String[]{"Off", "Low", "Normal", "Strong"};
   private static final String HUD_PREFIX = "HUD: ";

   private static int descH(CardContext ctx, List<SettingsFace.Group> groups, int w, boolean compact) {
      int lines = 1;

      for (SettingsFace.Group g : groups) {
         for (SettingsFace.Row r : g.rows()) {
            lines = Math.max(lines, ctx.lines(r.desc(), w));
         }
      }

      return 11 + lines * 9;
   }

   private static int groupsH(List<SettingsFace.Group> groups) {
      int h = 0;

      for (SettingsFace.Group g : groups) {
         h += 13 + g.rows().size() * 14 + 4;
      }

      return h - 4 + 2;
   }

   @Override
   public int height(CardContext ctx, int w, boolean compact) {
      List<SettingsFace.Group> groups = groups(ctx);
      return groupsH(groups) + 6 + descH(ctx, groups, w, compact);
   }

   @Override
   public void draw(CardGfx gfx, CardContext ctx, int x, int y, int w, boolean compact) {
      List<SettingsFace.Group> groups = groups(ctx);
      int chipW = chipWidth(gfx, groups);
      int yy = y;
      int id = 0;
      SettingsFace.Row focus = null;

      for (SettingsFace.Group g : groups) {
         gfx.textBold(g.name(), x, yy, -1185569);
         yy += 13;

         for (SettingsFace.Row r : g.rows()) {
            if (row(gfx, r, "setting/" + id++, x, yy, w, chipW)) {
               focus = r;
            }

            yy += 14;
         }

         yy += 4;
      }

      yy -= 2;
      gfx.rule(x, yy, w);
      describe(gfx, focus, x, yy + 6, w);
   }

   private static int chipWidth(CardGfx gfx, List<SettingsFace.Group> groups) {
      int cw = 0;

      for (SettingsFace.Group g : groups) {
         for (SettingsFace.Row r : g.rows()) {
            for (String l : r.labels()) {
               cw = Math.max(cw, gfx.width(l) + 10);
            }
         }
      }

      return cw;
   }

   private static boolean row(CardGfx gfx, SettingsFace.Row r, String key, int x, int y, int w, int chipW) {
      boolean hov = gfx.hover(key);
      if (hov) {
         gfx.fill(x - 4, y - 2, w + 8, 14, -13225168);
      }

      gfx.hit(x - 4, y - 2, w + 8, 14, key, r.action());
      gfx.text(gfx.clip(r.name().getString(), w - chipW - 6), x, y + 1, -1185569);
      gfx.chip(x + w, y - 1, r.label(), chipW, true, r.lit(), false, false, key + "/chip", r.action());
      return hov;
   }

   private static void describe(CardGfx gfx, SettingsFace.Row focus, int x, int y, int w) {
      if (focus == null) {
         gfx.text(Component.translatableWithFallback("awakened.card.settings.hint", "Point at a setting to see what it does."), x, y, -6054250);
      } else {
         gfx.textBold(focus.name(), x, y, -1185569);
         gfx.paragraph(focus.desc(), x, y + 11, w, -3554376, Integer.MAX_VALUE);
      }
   }

   private static List<SettingsFace.Group> groups(CardContext ctx) {
      List<SettingsFace.Group> out = new ArrayList<>();
      out.add(new SettingsFace.Group(Component.translatableWithFallback("awakened.card.settings.group_hud", "HUD"), hudRows()));
      out.add(new SettingsFace.Group(Component.translatableWithFallback("awakened.card.settings.group_camera", "Camera"), List.of(shakeRow(), voidIntroRow())));
      Power power = ctx.power;
      if (power != null) {
         List<SettingsFace.Row> own = powerRows(power);
         if (!own.isEmpty()) {
            out.add(new SettingsFace.Group(power.displayName(), own));
         }
      }

      return out;
   }

   private static List<SettingsFace.Row> hudRows() {
      List<SettingsFace.Row> rows = new ArrayList<>();
      rows.add(
         configRow(
            Component.translatableWithFallback("awakened.card.settings.number_only", "Slot number only"),
            desc("awakened.screen.setting_hud_number_only"),
            ClientConfig.HUD_ABILITY_NUMBER_ONLY,
            false
         )
      );
      rows.add(
         configRow(
            Component.translatableWithFallback("awakened.card.settings.hide_stamina", "Hide stamina when full"),
            desc("awakened.screen.setting_hud_hide_stamina_full"),
            ClientConfig.HUD_HIDE_STAMINA_WHEN_FULL,
            false
         )
      );
      rows.add(
         configRow(
            Component.translatableWithFallback("awakened.card.settings.ability_tray", "Ability tray"),
            Component.translatableWithFallback(
               "awakened.card.settings.ability_tray_desc", "Show the ability tray beside the hotbar. OFF shows only the current ability's icon."
            ),
            ClientConfig.HUD_ICONS_OFF,
            true
         )
      );
      rows.add(
         actionRow(
            Component.translatableWithFallback("awakened.card.settings.hud_layout", "HUD layout"),
            desc("awakened.screen.setting_hud_layout"),
            Component.translatable("awakened.screen.setting_arrange").getString(),
            () -> Minecraft.getInstance().setScreen(new HudPlacementScreen())
         )
      );
      return rows;
   }

   private static List<SettingsFace.Row> powerRows(Power power) {
      String id = power.id();
      List<SettingsFace.Row> rows = new ArrayList<>();

      for (PassiveToggle t : power.passiveToggles()) {
         rows.add(
            toggleRow(
               Component.translatable(t.nameKey()),
               Component.translatable(t.descKey()),
               () -> PassiveToggleClient.enabled(t),
               () -> PassiveToggleClient.toggle(t)
            )
         );
      }

      if (power.usesRadialActivate()) {
         rows.add(clientRow("awakened.screen.setting_ability_wheel", ClientConfig.ABILITY_WHEEL));
      }

      if ("spider".equals(id)) {
         rows.add(clientRow("awakened.screen.setting_spider_sense", ClientConfig.SPIDER_SENSE));
      }

      if ("brawler".equals(id)) {
         rows.add(clientRow("awakened.screen.setting_brawler_combo_sounds", ClientConfig.BRAWLER_COMBO_SOUNDS));
      }

      if ("exosavant".equals(id)) {
         rows.add(
            toggleRow(
               name("awakened.screen.setting_exo_flight_jet"),
               desc("awakened.screen.setting_exo_flight_jet"),
               ExoClientState::defaultJet,
               ExoWheel::toggleDefaultFlight
            )
         );
      }

      if ("reaper".equals(id)) {
         rows.add(clientRow("awakened.screen.setting_reaper_aura_first", ClientConfig.REAPER_AURA_SELF_FIRST));
         rows.add(clientRow("awakened.screen.setting_reaper_aura_third", ClientConfig.REAPER_AURA_SELF_THIRD));
      }

      return rows;
   }

   private static SettingsFace.Row clientRow(String key, BooleanValue value) {
      return configRow(name(key), desc(key), value, false);
   }

   private static SettingsFace.Row configRow(Component name, Component desc, BooleanValue value, boolean inverted) {
      return toggleRow(name, desc, () -> (Boolean)value.get() != inverted, () -> {
         value.set(!(Boolean)value.get());
         ClientConfig.save();
      });
   }

   private static SettingsFace.Row toggleRow(Component name, Component desc, BooleanSupplier on, Runnable flip) {
      return new SettingsFace.Row(name, desc, List.of("OFF", "ON"), () -> on.getAsBoolean() ? 1 : 0, true, flip);
   }

   private static SettingsFace.Row actionRow(Component name, Component desc, String label, Runnable run) {
      return new SettingsFace.Row(name, desc, List.of(label), () -> 0, false, run);
   }

   private static SettingsFace.Row shakeRow() {
      List<String> labels = new ArrayList<>();

      for (int i = 0; i < SHAKE_KEYS.length; i++) {
         labels.add(Component.translatableWithFallback("awakened.card.settings.shake_" + SHAKE_KEYS[i], SHAKE_NAMES[i]).getString());
      }

      return new SettingsFace.Row(
         name("awakened.screen.setting_camera_shake"), desc("awakened.screen.setting_camera_shake"), labels, SettingsFace::shakeStep, false, () -> {
            int next = (shakeStep() + 1) % SHAKE_STEPS.length;
            ClientConfig.SCREEN_SHAKE.set(next > 0);
            ClientConfig.SCREEN_SHAKE_INTENSITY.set(next > 0 ? SHAKE_STEPS[next] : 1.0);
            ClientConfig.save();
         }
      );
   }

   private static SettingsFace.Row voidIntroRow() {
      VoidIntroMode[] modes = VoidIntroMode.values();
      List<String> labels = new ArrayList<>();

      for (VoidIntroMode m : modes) {
         labels.add(Component.translatable("awakened.card.settings.void_intro_" + m.name().toLowerCase(Locale.ROOT)).getString());
      }

      return new SettingsFace.Row(
         name("awakened.screen.setting_void_intro"),
         desc("awakened.screen.setting_void_intro"),
         labels,
         () -> ((VoidIntroMode)ClientConfig.VOID_INTRO.get()).ordinal(),
         false,
         () -> {
            ClientConfig.VOID_INTRO.set(modes[(((VoidIntroMode)ClientConfig.VOID_INTRO.get()).ordinal() + 1) % modes.length]);
            ClientConfig.save();
         }
      );
   }

   private static int shakeStep() {
      if (!(Boolean)ClientConfig.SCREEN_SHAKE.get()) {
         return 0;
      } else {
         double v = (Double)ClientConfig.SCREEN_SHAKE_INTENSITY.get();
         int best = 0;

         for (int i = 1; i < SHAKE_STEPS.length; i++) {
            if (Math.abs(SHAKE_STEPS[i] - v) < Math.abs(SHAKE_STEPS[best] - v)) {
               best = i;
            }
         }

         return best;
      }
   }

   private static Component name(String key) {
      Component c = Component.translatable(key);
      String s = c.getString();
      return (Component)(s.startsWith("HUD: ") ? Component.literal(s.substring("HUD: ".length())) : c);
   }

   private static Component desc(String key) {
      return Component.translatable(key + "_desc");
   }

   private static record Group(Component name, List<SettingsFace.Row> rows) {
   }

   private static record Row(Component name, Component desc, List<String> labels, IntSupplier state, boolean toggle, Runnable action) {
      String label() {
         return this.labels.get(this.state.getAsInt());
      }

      boolean lit() {
         return this.toggle && this.state.getAsInt() == 1;
      }
   }
}
