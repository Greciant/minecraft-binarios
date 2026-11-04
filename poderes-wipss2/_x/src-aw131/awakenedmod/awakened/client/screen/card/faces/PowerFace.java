package com.awakenedmod.awakened.client.screen.card.faces;

import com.awakenedmod.awakened.client.AbilityIcons;
import com.awakenedmod.awakened.client.AwakenedKeybinds;
import com.awakenedmod.awakened.client.ClientPowerState;
import com.awakenedmod.awakened.client.screen.GuiPanels;
import com.awakenedmod.awakened.client.screen.card.CardContext;
import com.awakenedmod.awakened.client.screen.card.CardFace;
import com.awakenedmod.awakened.client.screen.card.CardGfx;
import com.awakenedmod.awakened.client.screen.card.CardStyle;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.power.Power;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryFormula;
import com.awakenedmod.awakened.core.skill.Stat;
import java.util.Locale;
import net.minecraft.network.chat.Component;

public final class PowerFace implements CardFace {
   private static final int ICON = 32;
   private static final int BOX = 40;

   private static int identityH(boolean compact) {
      return compact ? 50 : 60;
   }

   private static int kitH(boolean compact) {
      return 40 + (compact ? 14 : 26);
   }

   private static int apH(Ability ab) {
      AbilityScaling s = ab.scalingInfo();
      return 10 + (s.isEmpty() ? 1 : s.entries().size()) * 11;
   }

   private static int detailH(CardContext ctx, Ability ab, int w) {
      return 24 + ctx.lines(ab.description(), w) * 9 + 6 + apH(ab);
   }

   private static int aboutH(CardContext ctx, int w) {
      return 24 + ctx.lines(ctx.power.description(), w) * 9;
   }

   @Override
   public int height(CardContext ctx, int w, boolean compact) {
      int reserve = aboutH(ctx, w);

      for (int i = 0; i < ctx.power.abilityCount(); i++) {
         Ability ab = ctx.power.ability(i);
         if (ab != null) {
            reserve = Math.max(reserve, detailH(ctx, ab, w));
         }
      }

      return identityH(compact) + kitH(compact) + 6 + reserve;
   }

   @Override
   public void draw(CardGfx gfx, CardContext ctx, int x, int y, int w, boolean compact) {
      ClientPowerState st = ctx.state;
      int yy = y + this.identity(gfx, ctx, x, y, w, compact);
      yy += this.kit(gfx, ctx, x, yy, w, compact);
      int focus = -1;

      for (int i = 0; i < ctx.power.abilityCount(); i++) {
         if (gfx.hover("ability/" + i)) {
            focus = i;
         }
      }

      if (focus < 0) {
         about(gfx, ctx, x, yy + 6, w);
      } else {
         this.detail(gfx, ctx, focus, x, yy + 6, w);
      }
   }

   private static void about(CardGfx gfx, CardContext ctx, int x, int y, int w) {
      gfx.rule(x, y, w);
      int hy = y + 8;
      gfx.textBold(ctx.power.displayName(), x, hy, -1185569);
      gfx.paragraph(ctx.power.description(), x, hy + 16, w, -1185569, Integer.MAX_VALUE);
   }

   private int identity(CardGfx gfx, CardContext ctx, int x, int y, int w, boolean compact) {
      ClientPowerState st = ctx.state;
      int pw = 48;
      int ph = compact ? 44 : 50;
      gfx.fill(x, y, pw, ph, GuiPanels.blend(gfx.band, 3025704, 0.45F));
      if (ctx.mc.player != null) {
         gfx.entity(ctx.mc.player, x + 2, y + 2, x + pw - 2, y + ph - 2, compact ? 18 : 21);
      }

      gfx.frame(x, y, pw, ph, -15461872);
      int tx = x + pw + 12;
      int bw = 120;
      int ty = y + (compact ? 2 : 4);
      int need = (int)Math.round(MasteryFormula.progressForNext(st.masteryLevel()));
      int prog = Math.round(st.masteryProgress());
      gfx.text(Component.translatable("awakened.screen.mastery_label", new Object[]{st.masteryLevel()}), tx, ty, -1185569);
      gfx.bar(tx, ty + 11, bw, MasteryFormula.fraction(st.masteryLevel(), (double)st.masteryProgress()), gfx.band);
      gfx.text(prog + " / " + need, tx + bw + 8, ty + 12, -3554376);
      gfx.text(Component.translatable("awakened.screen.stamina_label"), tx, ty + 25, -1185569);
      float frac = st.staminaMax() > 0.0F ? st.stamina() / st.staminaMax() : 0.0F;
      gfx.bar(tx, ty + 36, bw, st.infiniteStamina() ? 1.0F : frac, gfx.band);
      gfx.text(st.infiniteStamina() ? "Infinite" : (int)st.stamina() + " / " + (int)st.staminaMax(), tx + bw + 8, ty + 37, -3554376);
      return identityH(compact);
   }

   private int kit(CardGfx gfx, CardContext ctx, int x, int y, int w, boolean compact) {
      Power power = ctx.power;
      int n = power.abilityCount();
      int gap = n > 1 ? (w - n * 40) / (n - 1) : 0;

      for (int i = 0; i < n; i++) {
         Ability ab = power.ability(i);
         if (ab != null) {
            int bx = x + i * (40 + gap);
            int cx = bx + 20;
            boolean locked = locked(ctx, ab);
            boolean eq = i == ctx.state.activeSlot();
            boolean hov = gfx.hover("ability/" + i);
            iconBox(gfx, bx, y, ab, locked, eq, eq ? gfx.band : (hov && gap >= 6 ? GuiPanels.blend(gfx.band, 3025704, 0.5F) : 0));
            int col = locked ? -6054250 : (eq ? -1185569 : -3554376);
            int room = 40 + Math.max(0, gap) - 10;
            caption(gfx, ab.displayName(ctx.mc.player), eq, room, cx, x, w, y + 40 + 4, col);
            String cost = locked ? "" : cost(ctx, ab);
            if (!compact && (locked || !cost.isEmpty())) {
               caption(gfx, Component.literal(locked ? "Mastery " + ab.unlockLevel() : "Cost: " + cost), false, room, cx, x, w, y + 40 + 14, col);
            }

            gfx.hit(bx - gap / 2, y - 2, 40 + gap, 66, "ability/" + i);
         }
      }

      return kitH(compact);
   }

   private static void caption(CardGfx gfx, Component c, boolean bold, int room, int cx, int x, int w, int y, int col) {
      String s = c.getString();
      if ((bold ? gfx.widthBold(s) : gfx.width(s)) > room) {
         while (s.length() > 1 && (bold ? gfx.widthBold(s + "...") : gfx.width(s + "...")) > room) {
            s = s.substring(0, s.length() - 1);
         }

         s = s.stripTrailing() + "...";
      }

      Component t = (Component)(bold ? CardGfx.bold(s) : Component.literal(s));
      int tw = gfx.width(t);
      int lx = Math.max(x, Math.min(x + w - tw, cx - tw / 2));
      gfx.text(t, lx, y, col);
   }

   private static void iconBox(CardGfx gfx, int x, int y, Ability ab, boolean locked, boolean sunken, int ring) {
      if (locked) {
         gfx.fill(x, y, 40, 40, -12962254);
         gfx.fill(x + 2, y + 2, 36, 36, -14803942);
         gfx.texture(AbilityIcons.of(ab.id()), x + 4, y + 4, 32);
         gfx.fill(x + 4, y + 4, 32, 32, -1340204006);
      } else {
         gfx.fill(x, y, 40, 40, -15461872);
         edge(gfx, x, y, sunken ? -15987958 : -9541534, sunken ? -9541534 : -15987958);
         gfx.texture(AbilityIcons.of(ab.id()), x + 4, y + 4, 32);
      }

      if (ring != 0) {
         int r = CardStyle.opaque(ring);
         edge(gfx, x, y, r, r);
      }
   }

   private static void edge(CardGfx gfx, int x, int y, int lt, int rb) {
      gfx.fill(x, y, 40, 2, lt);
      gfx.fill(x, y, 2, 40, lt);
      gfx.fill(x, y + 40 - 2, 40, 2, rb);
      gfx.fill(x + 40 - 2, y, 2, 40, rb);
   }

   private void detail(CardGfx gfx, CardContext ctx, int slot, int x, int y, int w) {
      Ability ab = ctx.power.ability(slot);
      if (ab != null) {
         boolean locked = locked(ctx, ab);
         boolean eq = slot == ctx.state.activeSlot();
         gfx.rule(x, y, w);
         int hy = y + 8;
         gfx.texture(AbilityIcons.of(ab.id()), x, hy - 4, 16);
         if (locked) {
            gfx.fill(x, hy - 4, 16, 16, -1340204006);
         }

         gfx.textBold(ab.displayName(ctx.mc.player), x + 22, hy, locked ? -6054250 : -1185569);
         if (locked) {
            gfx.textRight("Locked", x + w, hy, -3554376);
         } else if (eq) {
            gfx.textRight(Component.literal("Key: ").append(AwakenedKeybinds.ACTIVATE.getTranslatedKeyMessage()), x + w, hy, -3554376);
         }

         int yy = hy + 16;
         if (locked) {
            gfx.text(Component.translatable("awakened.screen.locked_at", new Object[]{ab.unlockLevel()}), x, yy, -6054250);
         } else {
            yy += gfx.paragraph(ab.description(), x, yy, w, -1185569, Integer.MAX_VALUE) * 9 + 6;
            this.abilityPower(gfx, ctx, ab, x, yy);
         }
      }
   }

   private void abilityPower(CardGfx gfx, CardContext ctx, Ability ab, int x, int y) {
      gfx.text(Stat.ABILITY_POWER.displayName(), x, y, -3554376);
      AbilityScaling s = ab.scalingInfo();
      if (s.isEmpty()) {
         gfx.text(Component.translatable("awakened.scaling.awakened.none_reason"), x, y + 11, -6054250);
      } else {
         float ap = ctx.state.abilityPower();
         int mastery = ctx.state.masteryLevel();
         int ry = y + 11;

         for (AbilityScaling.Entry e : s.entries()) {
            int vx = x + gfx.text(e.label().copy().append(": "), x, ry, -1185569);
            if (e.kind() == AbilityScaling.Kind.QUALITATIVE) {
               gfx.text(ap > 0.0F ? "+" + Math.round(ap * 100.0F) + "%" : "0%", vx, ry, -1185569);
            } else {
               String unit = unitSuffix(e.unit());
               String before = fmt(e.before(mastery), e.unit()) + unit;
               if (ap > 0.0F) {
                  vx += gfx.text(before, vx, ry, -1185569) + 4;
                  gfx.sprite("container/villager/trade_arrow", vx, ry - 1, 10, 9);
                  vx += 14;
                  gfx.text(fmt(e.after(mastery, ap), e.unit()) + unit, vx, ry, -1185569);
               } else {
                  gfx.text(before, vx, ry, -1185569);
               }
            }

            ry += 11;
         }
      }
   }

   private static boolean locked(CardContext ctx, Ability ab) {
      return ctx.state.masteryLevel() < ab.unlockLevel();
   }

   private static String cost(CardContext ctx, Ability ab) {
      return ctx.mc.player != null ? ab.costLabel(ctx.mc.player) : ab.costLabel();
   }

   private static String fmt(double v, AbilityScaling.Unit unit) {
      if (unit == AbilityScaling.Unit.COUNT) {
         return Integer.toString((int)Math.round(v));
      } else {
         double r = (double)Math.round(v * 10.0) / 10.0;
         return r == Math.floor(r) ? Integer.toString((int)r) : String.format(Locale.ROOT, "%.1f", r);
      }
   }

   private static String unitSuffix(AbilityScaling.Unit unit) {
      return switch (unit) {
         case BLOCKS -> " " + Component.translatable("awakened.scaling.awakened.unit.blocks").getString();
         case SECONDS -> " " + Component.translatable("awakened.scaling.awakened.unit.seconds").getString();
         default -> "";
      };
   }
}
