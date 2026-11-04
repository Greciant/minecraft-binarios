package com.awakenedmod.awakened.content.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public final class ArbiterVerses {
   public static final ArbiterVerses.Verse V0 = verse("v0", "Verdict", ArbiterVerses.Bar.VERDICT, ArbiterVerses.Bar.WHIP);
   public static final ArbiterVerses.Verse V1 = verse("v1", "Lash, lash, sweep", ArbiterVerses.Bar.WHIP, ArbiterVerses.Bar.WHIP, ArbiterVerses.Bar.SWEEP);
   public static final ArbiterVerses.Verse V2 = verse("v2", "Sweep, lash", ArbiterVerses.Bar.SWEEP, ArbiterVerses.Bar.WHIP);
   public static final ArbiterVerses.Verse V3 = verse("v3", "Lash, rest, lash", ArbiterVerses.Bar.WHIP, ArbiterVerses.Bar.REST, ArbiterVerses.Bar.WHIP);
   public static final ArbiterVerses.Verse V4 = verse("v4", "Roots, lash, sweep", ArbiterVerses.Bar.ROOT, ArbiterVerses.Bar.WHIP, ArbiterVerses.Bar.SWEEP);
   public static final ArbiterVerses.Verse V5 = verse("v5", "Lash, rest, roots", ArbiterVerses.Bar.WHIP, ArbiterVerses.Bar.REST, ArbiterVerses.Bar.ROOT);
   public static final ArbiterVerses.Verse V6 = verse(
      "v6", "Lash, roots, lash, sweep", ArbiterVerses.Bar.WHIP, ArbiterVerses.Bar.ROOT, ArbiterVerses.Bar.WHIP, ArbiterVerses.Bar.SWEEP
   );
   public static final ArbiterVerses.Verse V7 = verse("v7", "Sweep, roots, lash", ArbiterVerses.Bar.SWEEP, ArbiterVerses.Bar.ROOT, ArbiterVerses.Bar.WHIP);
   public static final ArbiterVerses.Verse V8 = verse("v8", "Roots, lash, roots", ArbiterVerses.Bar.ROOT, ArbiterVerses.Bar.WHIP, ArbiterVerses.Bar.ROOT);
   public static final ArbiterVerses.Verse V9 = verse("v9", "Sweep, roots", ArbiterVerses.Bar.SWEEP, ArbiterVerses.Bar.ROOT);
   public static final ArbiterVerses.Verse V10 = verse("v10", "Roots, sweep, roots", ArbiterVerses.Bar.ROOT, ArbiterVerses.Bar.SWEEP, ArbiterVerses.Bar.ROOT);
   public static final List<ArbiterVerses.Verse> ALL = List.of(V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10);
   private static final List<List<ArbiterVerses.Verse>> DECKS = List.of(List.of(V1, V2, V3), List.of(V4, V5, V8, V9), List.of(V5, V6, V7, V10));
   private static final int[] BAR_TICKS = new int[]{80, 64, 48};
   private static final int[] SWEEP_BARS = new int[]{3, 4, 5};
   public static final int BREATH_MIN = 20;
   public static final int LEAD_IN = 20;

   private ArbiterVerses() {
   }

   private static ArbiterVerses.Verse verse(String id, String name, ArbiterVerses.Bar... bars) {
      return new ArbiterVerses.Verse(id, name, List.of(bars));
   }

   public static int barTicks(int tier) {
      return BAR_TICKS[Mth.clamp(tier - 1, 0, BAR_TICKS.length - 1)];
   }

   @Nullable
   public static ArbiterVerses.Verse byId(String id) {
      for (ArbiterVerses.Verse verse : ALL) {
         if (verse.id().equals(id)) {
            return verse;
         }
      }

      return null;
   }

   public static enum Bar {
      WHIP(1),
      ROOT(1),
      SWEEP(3),
      REST(1),
      VERDICT(2);

      private final int bars;

      private Bar(int bars) {
         this.bars = bars;
      }

      public int bars(int tier) {
         return this == SWEEP ? ArbiterVerses.SWEEP_BARS[Mth.clamp(tier - 1, 0, ArbiterVerses.SWEEP_BARS.length - 1)] : this.bars;
      }
   }

   public static final class Runner {
      private final List<ArbiterVerses.Verse> deck = new ArrayList<>();
      @Nullable
      private ArbiterVerses.Verse verse;
      private int index = -1;
      private int barTick;
      @Nullable
      private String lastId;
      private boolean held;
      private int freeTicks;

      public void reset() {
         this.verse = null;
         this.index = -1;
         this.barTick = 0;
         this.held = false;
         this.freeTicks = 0;
         this.deck.clear();
      }

      public void seed(int tier, RandomSource random, @Nullable ArbiterVerses.Verse opener, boolean now) {
         this.verse = opener != null ? opener : this.draw(tier, random);
         this.index = -1;
         this.held = false;
         int bar = ArbiterVerses.barTicks(tier);
         this.barTick = now ? bar : Math.max(0, bar - 20);
         if (now) {
            this.freeTicks = 20;
         }
      }

      public void interject() {
         this.held = true;
      }

      @Nullable
      public ArbiterVerses.Bar tick(int tier, RandomSource random, boolean busy) {
         if (this.verse == null) {
            this.seed(tier, random, null, false);
         }

         this.freeTicks = busy ? 0 : this.freeTicks + 1;
         if (this.held) {
            if (busy) {
               return null;
            } else {
               this.held = false;
               this.barTick = Math.max(this.barTick, this.barLength(tier) - 20);
               return null;
            }
         } else {
            this.barTick++;
            if (this.barTick < this.barLength(tier)) {
               return null;
            } else if (!busy && this.freeTicks >= 20) {
               this.barTick = 0;
               this.index++;
               if (this.verse == null || this.index >= this.verse.bars().size()) {
                  this.lastId = this.verse != null ? this.verse.id() : this.lastId;
                  this.verse = this.draw(tier, random);
                  this.index = 0;
               }

               return this.verse.bars().get(this.index);
            } else {
               return null;
            }
         }
      }

      private int barLength(int tier) {
         int bars = this.verse != null && this.index >= 0 && this.index < this.verse.bars().size() ? this.verse.bars().get(this.index).bars(tier) : 1;
         return ArbiterVerses.barTicks(tier) * bars;
      }

      private ArbiterVerses.Verse draw(int tier, RandomSource random) {
         if (this.deck.isEmpty()) {
            this.deck.addAll(ArbiterVerses.DECKS.get(Mth.clamp(tier - 1, 0, ArbiterVerses.DECKS.size() - 1)));

            for (int i = this.deck.size() - 1; i > 0; i--) {
               this.deck.set(i, this.deck.set(random.nextInt(i + 1), this.deck.get(i)));
            }

            if (this.deck.size() > 1 && this.deck.get(0).id().equals(this.lastId)) {
               this.deck.set(0, this.deck.set(1, this.deck.get(0)));
            }
         }

         ArbiterVerses.Verse drawn = this.deck.remove(0);
         this.lastId = drawn.id();
         return drawn;
      }

      public String readout(int tier) {
         if (this.verse == null) {
            return "verse -- (idle)";
         } else {
            ArbiterVerses.Bar bar = this.index >= 0 && this.index < this.verse.bars().size() ? this.verse.bars().get(this.index) : null;
            StringBuilder deckLine = new StringBuilder();

            for (ArbiterVerses.Verse queued : this.deck) {
               deckLine.append(deckLine.isEmpty() ? "" : ",").append(queued.id());
            }

            return String.format(
               Locale.ROOT,
               "verse %s \"%s\" bar %d/%d (%s) %d/%d%s | deck [%s]",
               this.verse.id(),
               this.verse.name(),
               Math.max(this.index, 0) + 1,
               this.verse.bars().size(),
               bar != null ? bar.name() : "opening",
               this.barTick,
               this.barLength(tier),
               this.held ? " HELD" : "",
               deckLine
            );
         }
      }

      public String shortReadout(int tier) {
         if (this.verse == null) {
            return "idle";
         } else {
            ArbiterVerses.Bar bar = this.index >= 0 && this.index < this.verse.bars().size() ? this.verse.bars().get(this.index) : null;
            return this.verse.id()
               + " bar "
               + (Math.max(this.index, 0) + 1)
               + "/"
               + this.verse.bars().size()
               + " ("
               + (bar != null ? bar.name() : "opening")
               + ") "
               + this.barTick
               + "/"
               + this.barLength(tier);
         }
      }
   }

   public static record Verse(String id, String name, List<ArbiterVerses.Bar> bars) {
   }
}
