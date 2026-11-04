package com.awakenedmod.awakened.client.screen;

import com.awakenedmod.awakened.client.AwakenedKeybinds;
import com.awakenedmod.awakened.client.ClientPowerState;
import com.awakenedmod.awakened.client.ClientTeamState;
import com.awakenedmod.awakened.client.nominate.NominationCampaign;
import com.awakenedmod.awakened.client.nominate.NominationCommand;
import com.awakenedmod.awakened.client.nominate.NominationLedger;
import com.awakenedmod.awakened.client.screen.card.CardChrome;
import com.awakenedmod.awakened.client.screen.card.CardContext;
import com.awakenedmod.awakened.client.screen.card.CardFace;
import com.awakenedmod.awakened.client.screen.card.CardGfx;
import com.awakenedmod.awakened.client.screen.card.CardHits;
import com.awakenedmod.awakened.client.screen.card.CardStyle;
import com.awakenedmod.awakened.client.screen.card.faces.PowerFace;
import com.awakenedmod.awakened.client.screen.card.faces.RecipesFace;
import com.awakenedmod.awakened.client.screen.card.faces.RelicsFace;
import com.awakenedmod.awakened.client.screen.card.faces.SettingsFace;
import com.awakenedmod.awakened.client.screen.card.faces.SkinsFace;
import com.awakenedmod.awakened.client.screen.card.faces.StatsFace;
import com.awakenedmod.awakened.client.screen.card.faces.TeamFace;
import com.awakenedmod.awakened.client.screen.card.faces.TrialsFace;
import com.awakenedmod.awakened.core.power.Power;
import com.awakenedmod.awakened.core.power.PowerRegistry;
import com.awakenedmod.awakened.core.quest.TrialConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class CharacterScreen extends AwakenedScreen {
   public static final int POWER_TAB = 0;
   public static final int STATS_TAB = 2;
   public static final int TRIALS_TAB = 3;
   public static final int RELICS_TAB = 4;
   public static final int RECIPES_TAB = 5;
   public static final int SETTINGS_TAB = 6;
   public static final int TEAM_TAB = 7;
   public static final int SKINS_TAB = 8;
   private static final int[] INDEX_TABS = new int[]{0, 2, 4, 3, 5, 8, 7};
   private static final String[] TAB_KEYS = new String[]{
      "awakened.screen.tab_power",
      "awakened.screen.tab_progress",
      "awakened.screen.tab_skills",
      "awakened.screen.tab_quests",
      "awakened.screen.tab_relics",
      "awakened.screen.tab_recipes",
      "awakened.screen.tab_settings",
      "awakened.screen.tab_team",
      "awakened.screen.tab_skins"
   };
   private static final String[] COG = new String[]{
      "....###....",
      ".##.###.##.",
      ".#########.",
      "..#######..",
      "####...####",
      "####...####",
      "####...####",
      "..#######..",
      ".#########.",
      ".##.###.##.",
      "....###...."
   };
   private static final int LINK_H = 26;
   private static final int NOMINATE_H = 21;
   private static final int THANKS_H = 11;
   private static final int AWARD = -871100;
   private static final int AWARD_HOV = -13466;
   private static final int AWARD_EDGE = -4619734;
   private static final int AWARD_INK = -14016758;
   private static final ItemStack NOMINATE_ICON = new ItemStack(Items.NETHER_STAR);
   private static final Map<Integer, CardFace> FACES = Map.of(
      0,
      new PowerFace(),
      2,
      new StatsFace(),
      4,
      new RelicsFace(),
      3,
      new TrialsFace(),
      5,
      new RecipesFace(),
      6,
      new SettingsFace(),
      7,
      new TeamFace(),
      8,
      new SkinsFace()
   );
   private int tab;
   private final CardHits hits = new CardHits();
   private final Map<String, Object> memory = CardContext.newMemory();
   private float uiScale = 1.0F;

   public CharacterScreen() {
      this(0);
   }

   public CharacterScreen(int initialTab) {
      super(Component.translatable("awakened.screen.title"));
      this.tab = initialTab == 1 ? 0 : initialTab;
   }

   private List<Integer> indexTabs() {
      List<Integer> out = new ArrayList<>();
      Power power = currentPower(ClientPowerState.get());

      for (int t : INDEX_TABS) {
         if ((t != 7 || ClientTeamState.enabled()) && (t != 3 || TrialConfig.enabled()) && (t != 8 || power != null && power.hasSkins())) {
            out.add(t);
         }
      }

      return out;
   }

   private static Power currentPower(ClientPowerState state) {
      return state.awakened() ? PowerRegistry.get(state.powerId()) : null;
   }

   private Component tabLabel(int t) {
      return Component.translatable(TAB_KEYS[t]);
   }

   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      super.render(g, mouseX, mouseY, partialTick);
      ClientPowerState state = ClientPowerState.get();
      Power power = currentPower(state);
      if (power == null) {
         this.renderEmpty(g);
      } else {
         List<Integer> tabs = this.indexTabs();
         if (!tabs.contains(this.tab) && this.tab != 6) {
            this.tab = 0;
         }

         CardContext ctx = new CardContext(this.minecraft, this.font, state, power, this, this.memory, tx -> this.tab = tx);
         int w = Math.min(400, this.width - 16);
         int cw = w - 4 - 40;
         int bandH = 32;
         int topH = 2 + bandH + 2;
         int tabMax = 0;

         for (int t : tabs) {
            tabMax = Math.max(tabMax, this.font.width(CardGfx.bold(this.tabLabel(t))) + 14 + this.font.width("00") + 5);
         }

         boolean compact = this.height < 300;
         int tabsNeed = topH + 2 + tabs.size() * 20 + 6;
         int need = Math.max(this.needOf(this.tab, ctx, cw, topH, compact), tabsNeed);
         int tallest = tabsNeed;

         for (int t : tabs) {
            tallest = Math.max(tallest, this.needOf(t, ctx, cw, topH, compact));
         }

         tallest = Math.max(tallest, this.needOf(6, ctx, cw, topH, compact));
         int below = !NominationCampaign.linkVisible() ? 0 : (NominationLedger.get().clicked() ? 37 : 26);
         this.uiScale = Math.min(1.0F, Math.min(((float)this.height - 8.0F) / (float)(tallest + below), ((float)this.width - 8.0F) / (float)(w + tabMax)));
         int vw = Math.round((float)this.width / this.uiScale);
         int vh = Math.round((float)this.height / this.uiScale);
         int mx = Math.round((float)mouseX / this.uiScale);
         int my = Math.round((float)mouseY / this.uiScale);
         this.hits.begin((double)mx, (double)my);
         if (this.uiScale < 1.0F) {
            g.pose().pushPose();
            g.pose().scale(this.uiScale, this.uiScale, 1.0F);
         }

         int band = CardStyle.band(power.themeColor());
         CardGfx gfx = new CardGfx(g, this.font, this.hits, band, mx, my, this.uiScale);
         int x0 = Math.max((vw - w) / 2, tabMax + 4);
         int y0 = vh / 2 - (tallest + below) / 2 - 2;
         CardChrome.face(gfx, x0, y0, w, need);
         CardChrome.titleCentred(gfx, power.displayName(), x0, y0, w);
         this.drawGear(gfx, x0, y0, w);
         CardChrome.frame(gfx, x0, y0, w, need);
         if (below > 0) {
            this.drawNominateLink(gfx, x0, y0 + need, w);
         }

         int faceTop = y0 + bandH + 12;
         CardFace face = FACES.get(this.tab);
         face.draw(gfx, ctx, x0 + 2 + 20, faceTop, cw, compact);
         int ty0 = y0 + bandH + 6;

         for (int row = 0; row < tabs.size(); row++) {
            int t = tabs.get(row);
            this.drawIndexTab(gfx, state, t, x0, ty0 + row * 20);
         }

         if (this.uiScale < 1.0F) {
            g.pose().popPose();
         }
      }
   }

   private int needOf(int t, CardContext ctx, int cw, int topH, boolean compact) {
      return topH + 8 + FACES.get(t).height(ctx, cw, compact) + 10;
   }

   private void drawGear(CardGfx gfx, int x0, int y0, int w) {
      int gs = 20;
      int gx = CardChrome.buttonX(x0, w);
      int gy = CardChrome.buttonY(y0);
      int ink = CardChrome.button(gfx, x0, y0, w, this.tab == 6, this.hits.hover("tab/6"));

      for (int cy = 0; cy < COG.length; cy++) {
         for (int cx = 0; cx < COG[cy].length(); cx++) {
            if (COG[cy].charAt(cx) == '#') {
               gfx.fill(gx + 4 + cx, gy + 4 + cy, 1, 1, ink);
            }
         }
      }

      gfx.hit(gx, gy, gs, gs, "tab/6", () -> this.tab = 6);
   }

   private void drawNominateLink(CardGfx gfx, int x0, int y, int w) {
      Component label = CardGfx.bold(Component.translatable("awakened.nominate.card"));
      boolean hov = this.hits.hover("nominate");
      int bw = 26 + this.font.width(label) + 8;
      int bx = x0 + (w - bw) / 2;
      int by = y + 2;
      gfx.fill(bx, by, bw, 21, -15461872);
      gfx.fill(bx + 1, by + 1, bw - 2, 19, hov ? -13466 : -871100);
      gfx.fill(bx + 1, by + 21 - 3, bw - 2, 2, -4619734);
      gfx.item(NOMINATE_ICON, bx + 6, by + 2);
      gfx.text(label, bx + 6 + 16 + 4, by + 6, -14016758);
      gfx.hit(bx, by, bw, 21, "nominate", NominationCommand::open);
      if (NominationLedger.get().clicked()) {
         Component thanks = Component.translatable("awakened.nominate.thanks");
         gfx.text(thanks, x0 + (w - this.font.width(thanks)) / 2, by + 21 + 3, -5659750);
      }
   }

   private void drawIndexTab(CardGfx gfx, ClientPowerState state, int t, int x0, int ty) {
      boolean open = this.tab == t;
      boolean hov = this.hits.hover("tab/" + t);
      Component label = open ? CardGfx.bold(this.tabLabel(t)) : this.tabLabel(t);
      String badge = t == 2 && state.skillPointsAvailable() > 0 ? Integer.toString(state.skillPointsAvailable()) : null;
      int bw = badge != null ? this.font.width(CardGfx.bold(badge)) + 5 : 0;
      int lw = this.font.width(label) + 14 + (open ? 2 : 0) + bw;
      int lx = x0 + 2 - lw;
      gfx.fill(lx, ty, lw, 18, !open && !hov ? -14277856 : -13751512);
      gfx.fill(lx, ty, lw, 2, -15461872);
      gfx.fill(lx, ty + 18 - 2, lw, 2, -15461872);
      gfx.fill(lx, ty, 2, 18, -15461872);
      if (!open) {
         gfx.fill(x0, ty, 2, 18, -15461872);
      }

      int textX = x0 - 6 - this.font.width(label);
      gfx.text(label, textX, ty + 5, open ? -1185569 : -5659750);
      if (badge != null) {
         gfx.textBold(badge, textX - bw, ty + 5, gfx.band);
      }

      gfx.hit(lx, ty, lw, 18, "tab/" + t, () -> this.tab = t);
   }

   private void renderEmpty(GuiGraphics g) {
      int w = 240;
      int x = (this.width - w) / 2;
      CardGfx gfx = new CardGfx(g, this.font, this.hits, CardStyle.band(3720959), 0, 0, 1.0F);
      List<String> lines = gfx.wrap(Component.translatable("awakened.screen.none"), w - 40);
      int h = 40 + lines.size() * 9 + 12;
      int y = (this.height - h) / 2;
      gfx.fill(x, y, w, h, -13751512);
      gfx.fill(x + 2, y + 2, w - 4, 20, gfx.band);
      gfx.textBold(this.title, x + 20, y + 8, CardStyle.titleInk(gfx.band));

      for (int i = 0; i < lines.size(); i++) {
         gfx.text(lines.get(i), x + 20, y + 32 + i * 9, -3554376);
      }

      CardChrome.frame(gfx, x, y, w, h);
   }

   private boolean handleClick(double mouseX, double mouseY) {
      return this.hits.click(mouseX / (double)this.uiScale, mouseY / (double)this.uiScale);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      return button == 0 && this.handleClick(mouseX, mouseY) ? true : super.mouseClicked(mouseX, mouseY, button);
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      return this.handleKey(keyCode) ? true : super.keyPressed(keyCode, scanCode, modifiers);
   }

   private boolean handleKey(int key) {
      if (key == 258) {
         List<Integer> tabs = this.indexTabs();
         int i = tabs.indexOf(this.tab);
         this.tab = tabs.get((i + 1) % tabs.size());
         return true;
      } else if (key == AwakenedKeybinds.OPEN_SCREEN.getKey().getValue()) {
         this.onClose();
         return true;
      } else {
         return false;
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      ClientPowerState state = ClientPowerState.get();
      Power power = currentPower(state);
      if (power != null) {
         CardContext ctx = new CardContext(this.minecraft, this.font, state, power, this, this.memory, t -> this.tab = t);
         if (FACES.get(this.tab).scrolled(ctx, scrollY)) {
            return true;
         }
      }

      return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
   }

   public boolean isPauseScreen() {
      return false;
   }
}
