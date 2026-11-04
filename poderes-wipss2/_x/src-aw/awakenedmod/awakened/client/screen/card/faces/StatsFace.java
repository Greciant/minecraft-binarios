package com.awakenedmod.awakened.client.screen.card.faces;

import com.awakenedmod.awakened.client.ClientPowerState;
import com.awakenedmod.awakened.client.screen.card.CardContext;
import com.awakenedmod.awakened.client.screen.card.CardFace;
import com.awakenedmod.awakened.client.screen.card.CardGfx;
import com.awakenedmod.awakened.client.screen.card.CardStyle;
import com.awakenedmod.awakened.core.net.AllocateStatPayload;
import com.awakenedmod.awakened.core.power.PowerProgress;
import com.awakenedmod.awakened.core.skill.SkillFormula;
import com.awakenedmod.awakened.core.skill.Stat;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

public final class StatsFace implements CardFace {
   private static final int ROWS = 34;
   private static final int PITCH = 17;
   private static final int PIP = 7;
   private static final int BAR_W = 120;
   private static final int HEAD = 11;
   private static final int CT_HEAD = 20;
   private static final int TILE_TOP = 20;
   private static final int GAP = 4;
   private static final int TILE_COLS = 4;

   @Override
   public int height(CardContext ctx, int w, boolean compact) {
      return 34 + Stat.count() * 17 + 3 + (compact ? 0 : 17 + descReserve(ctx, w) + 3) + countersH(ctx, progress(ctx), w);
   }

   @Override
   public void draw(CardGfx gfx, CardContext ctx, int x, int y, int w, boolean compact) {
      this.header(gfx, ctx, x, y, w);
      Stat focus = Stat.values()[0];

      for (Stat s : Stat.values()) {
         if (gfx.hover(rowKey(s))) {
            focus = s;
         }
      }

      this.rows(gfx, ctx, focus, x, y + 34, w);
      int yy = y + 34 + Stat.count() * 17 + 3;
      if (!compact) {
         yy = this.description(gfx, ctx, focus, x, yy, w);
      }

      this.counters(gfx, ctx, progress(ctx), x, yy, w);
   }

   private void header(CardGfx gfx, CardContext ctx, int x, int y, int w) {
      ClientPowerState st = ctx.state;
      int lw = gfx.scaled(
         CardGfx.bold(Component.translatableWithFallback("awakened.card.stats.level", "Level %s", new Object[]{st.skillLevel()})), x, y, -1185569, 2
      );
      int pts = st.skillPointsAvailable();
      if (pts > 0) {
         gfx.textBold(
            pts == 1
               ? Component.translatableWithFallback("awakened.card.stats.points_one", "1 point to spend")
               : Component.translatableWithFallback("awakened.card.stats.points", "%s points to spend", new Object[]{pts}),
            x + lw + 8,
            y + 7,
            gfx.band
         );
      } else {
         gfx.text(Component.translatableWithFallback("awakened.card.stats.no_points", "No points"), x + lw + 8, y + 7, -3554376);
      }

      boolean spent = pts < SkillFormula.pointsForLevel(st.skillLevel());
      gfx.chip(x + w, y + 3, Component.translatable("awakened.screen.respec").getString(), 0, true, false, false, !spent, "stat/respec", () -> allocate(-1, 0));
      gfx.bar(x, y + 21, 120, SkillFormula.fraction(st.skillLevel(), (double)st.skillXp()), gfx.band);
      gfx.text(
         Component.translatableWithFallback("awakened.card.stats.xp", "%s / %s XP", new Object[]{(int)st.skillXp(), (int)st.skillXpForNext()}),
         x + 120 + 8,
         y + 22,
         -3554376
      );
   }

   private void rows(CardGfx gfx, CardContext ctx, Stat focus, int x, int y, int w) {
      ClientPowerState st = ctx.state;
      int nameW = 0;
      int bonusW = 0;
      int maxPips = 0;

      for (Stat s : Stat.values()) {
         nameW = Math.max(nameW, gfx.width(s.displayName()));
         bonusW = Math.max(bonusW, gfx.width(bonus(s, s.maxPoints())));
         maxPips = Math.max(maxPips, s.maxPoints());
      }

      int px0 = x + 20 + nameW + 10;
      int vx = px0 + maxPips * 7 + 8;
      int ax = vx + bonusW + 5;
      int chipsX = x + w - 26;
      int avail = st.skillPointsAvailable();

      for (Stat s : Stat.values()) {
         int ry = y + s.ordinal() * 17;
         int pts = st.statPoints(s);
         int id = s.ordinal();
         boolean focused = s == focus;
         boolean canPlus = avail > 0 && pts < s.maxPoints();
         boolean preview = canPlus && gfx.hover(rowKey(s) + "/plus");
         if (focused) {
            gfx.fill(x - 4, ry - 1, w + 8, 17, -13225168);
         }

         gfx.hit(x - 4, ry - 1, w + 8, 17, rowKey(s));
         gfx.item(icon(s), x, ry);
         gfx.text(s.displayName(), x + 20, ry + 4, focused ? -1185569 : -3554376);
         pips(gfx, px0, ry + 4, pts, s.maxPoints(), preview, focused ? -13225168 : -13751512);
         gfx.text(bonus(s, pts), vx, ry + 4, preview ? -1185569 : -3554376);
         if (preview) {
            gfx.sprite("container/villager/trade_arrow", ax, ry + 4, 10, 9);
            gfx.textBold(gfx.clip(bonus(s, pts + 1), chipsX - 4 - (ax + 14)), ax + 14, ry + 4, gfx.band);
         }

         gfx.chip(chipsX, ry + 2, "-", 12, false, false, false, pts <= 0, rowKey(s) + "/minus", () -> allocate(id, -1));
         gfx.chip(x + w - 12, ry + 2, "+", 12, false, false, false, !canPlus, rowKey(s) + "/plus", () -> allocate(id, 1));
      }
   }

   private static void pips(CardGfx gfx, int x, int y, int pts, int max, boolean preview, int under) {
      for (int p = 0; p < max; p++) {
         int px = x + p * 7;
         if (p < pts) {
            gfx.fill(px, y, 6, 8, CardStyle.opaque(gfx.band));
         } else if (preview && p == pts) {
            gfx.fill(px, y, 6, 8, CardStyle.opaque(gfx.band));
            gfx.fill(px + 1, y + 1, 4, 6, under);
         } else {
            gfx.fill(px, y, 6, 8, -12238791);
         }
      }
   }

   private int description(CardGfx gfx, CardContext ctx, Stat focus, int x, int y, int w) {
      gfx.rule(x, y, w);
      int yy = y + 6;
      gfx.textBold(focus.displayName(), x, yy, -1185569);
      yy += 11;
      gfx.paragraph(focus.description(), x, yy, w, -1185569, Integer.MAX_VALUE);
      return yy + descReserve(ctx, w) + 3;
   }

   private static int descReserve(CardContext ctx, int w) {
      int d = 0;

      for (Stat s : Stat.values()) {
         d = Math.max(d, ctx.lines(s.description(), w) * 9);
      }

      return d;
   }

   private static PowerProgress progress(CardContext ctx) {
      return ctx.mc.player != null && ctx.power != null && ctx.power.progressOnCard()
         ? ctx.power.progress(ctx.mc.player, ctx.state.masteryLevel())
         : PowerProgress.NONE;
   }

   private static int countersH(CardContext ctx, PowerProgress pc, int w) {
      if (pc.isEmpty()) {
         return 0;
      } else {
         int h = 6 + (pc.hasBar() ? 24 : 0);
         List<PowerProgress.Stat> tiles = pc.stats();
         if (!tiles.isEmpty()) {
            h += tileRows(tiles.size()) * (tileH(ctx, tiles, w) + 4);
         }

         return h;
      }
   }

   private void counters(CardGfx gfx, CardContext ctx, PowerProgress pc, int x, int y, int w) {
      if (!pc.isEmpty()) {
         gfx.rule(x, y, w);
         int yy = y + 6;
         if (pc.hasBar()) {
            int rx = gfx.textRight(pc.barRight(), x + w, yy, -3554376);
            gfx.textBold(gfx.clip(pc.barLeft().getString(), rx - 8 - x), x, yy, gfx.band);
            gfx.bar(x, yy + 11, w, pc.barFraction(), gfx.band);
            yy += 24;
         }

         List<PowerProgress.Stat> tiles = pc.stats();
         if (!tiles.isEmpty()) {
            int cols = tileCols(tiles.size());
            int tw = tileW(w, cols);
            int th = tileH(ctx, tiles, w);

            for (int i = 0; i < tiles.size(); i++) {
               tile(gfx, tiles.get(i), x + i % cols * (tw + 4), yy + i / cols * (th + 4), tw, th);
            }
         }
      }
   }

   private static void tile(CardGfx gfx, PowerProgress.Stat t, int x, int y, int w, int h) {
      gfx.fill(x, y, w, h, -14277856);
      Component v = CardGfx.bold(t.value());
      if (gfx.width(v) * 2 <= w - 8) {
         gfx.scaled(v, x + 4, y + 2, -1185569, 2);
      } else {
         gfx.textBold(gfx.clip(t.value().getString(), w - 8), x + 4, y + 6, -1185569);
      }

      List<String> lines = gfx.wrap(t.label(), w - 8);

      for (int j = 0; j < lines.size(); j++) {
         gfx.text(lines.get(j), x + 4, y + 20 + j * 9, -3554376);
      }
   }

   private static int tileCols(int n) {
      return (int)Math.ceil((double)n / (double)tileRows(n));
   }

   private static int tileRows(int n) {
      return (int)Math.ceil((double)n / 4.0);
   }

   private static int tileW(int w, int cols) {
      return (w - (cols - 1) * 4) / cols;
   }

   private static int tileH(CardContext ctx, List<PowerProgress.Stat> tiles, int w) {
      int tw = tileW(w, tileCols(tiles.size()));
      int ll = 1;

      for (PowerProgress.Stat t : tiles) {
         ll = Math.max(ll, ctx.lines(t.label(), tw - 8));
      }

      return 20 + ll * 9 + 1;
   }

   private static String rowKey(Stat s) {
      return "stat/" + s.ordinal();
   }

   private static ItemStack icon(Stat s) {
      return new ItemStack(switch (s) {
         case MAX_STAMINA -> Items.SUGAR;
         case MAX_HEALTH -> Items.GOLDEN_APPLE;
         case STAMINA_REGEN -> Items.CLOCK;
         case ABILITY_POWER -> Items.BLAZE_POWDER;
      });
   }

   private static String bonus(Stat s, int pts) {
      float v = s.perPoint() * (float)pts;

      return switch (s) {
         case MAX_STAMINA -> Component.translatableWithFallback("awakened.card.stats.bonus.max_stamina", "+%s stamina", new Object[]{num(v)}).getString();
         case MAX_HEALTH -> v / 2.0F == 1.0F
         ? Component.translatableWithFallback("awakened.card.stats.bonus.max_health_one", "+1 heart").getString()
         : Component.translatableWithFallback("awakened.card.stats.bonus.max_health", "+%s hearts", new Object[]{num(v / 2.0F)}).getString();
         case STAMINA_REGEN -> Component.translatableWithFallback("awakened.card.stats.bonus.stamina_regen", "+%s stamina/s", new Object[]{num(v)}).getString();
         case ABILITY_POWER -> "+" + Math.round(v * 100.0F) + "%";
      };
   }

   private static String num(float v) {
      return (double)v == Math.floor((double)v) ? Integer.toString((int)v) : String.format(Locale.ROOT, "%.1f", v);
   }

   private static void allocate(int statId, int delta) {
      PacketDistributor.sendToServer(new AllocateStatPayload(statId, delta), new CustomPacketPayload[0]);
   }
}
