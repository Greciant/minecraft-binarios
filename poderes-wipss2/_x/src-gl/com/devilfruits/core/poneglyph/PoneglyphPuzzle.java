package com.devilfruits.core.poneglyph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public final class PoneglyphPuzzle {
   public static long mix(long seed, long player, int draw) {
      long z = seed ^ player * -7046029254386353131L ^ (long)draw * -4417276706812531889L;
      z = (z ^ z >>> 30) * -4658895280553007687L;
      z = (z ^ z >>> 27) * -7723592293110705685L;
      return z ^ z >>> 31;
   }

   public static List<PoneglyphPuzzle.Round> generate(
      long seed, long player, int draw, List<String> words, List<String> otherWords, int rounds, int optionCount
   ) {
      Random rng = new Random(mix(seed, player, draw));
      List<String> clues = new ArrayList<>(words);
      Collections.shuffle(clues, rng);
      List<PoneglyphPuzzle.Round> out = new ArrayList<>(rounds);

      for (int r = 0; r < rounds; r++) {
         String clue = clues.get(r % clues.size());
         List<String> options = new ArrayList<>(decoys(clue, words, otherWords, rng, Math.max(1, optionCount - 1)));
         options.add(clue);
         Collections.shuffle(options, rng);
         out.add(new PoneglyphPuzzle.Round(clue, List.copyOf(options), options.indexOf(clue)));
      }

      return out;
   }

   public static PoneglyphPuzzle.Round current(List<PoneglyphPuzzle.Round> rounds, BlueprintData.Progress progress, long seed) {
      PoneglyphPuzzle.Round round = rounds.get(Math.min(progress.round(), rounds.size() - 1));
      return progress.shuffle() == 0 ? round : round.reshuffled(mix(seed, (long)progress.shuffle(), progress.round() + 31));
   }

   public static List<String> decoys(String word, List<String> ownWords, List<String> otherWords, Random rng, int count) {
      int len = word.length();
      List<String> swaps = new ArrayList<>();

      for (int i = 0; i + 1 < len; i++) {
         char[] c = word.toCharArray();
         char t = c[i];
         c[i] = c[i + 1];
         c[i + 1] = t;
         swaps.add(new String(c));
      }

      List<String> sameLength = new ArrayList<>(otherWords.stream().filter(wx -> wx.length() == len).distinct().toList());
      List<String> substitutions = new ArrayList<>();
      String letters = word + "aeiourstnl";

      for (int i = 0; i < len * 3; i++) {
         char[] c = word.toCharArray();
         c[rng.nextInt(len)] = letters.charAt(rng.nextInt(letters.length()));
         substitutions.add(new String(c));
      }

      List<String> own = new ArrayList<>(ownWords.stream().filter(wx -> wx.length() == len).toList());
      List<String> anagrams = new ArrayList<>();

      for (int i = 0; i < 6; i++) {
         List<Character> c = new ArrayList<>();

         for (char ch : word.toCharArray()) {
            c.add(ch);
         }

         Collections.shuffle(c, rng);
         StringBuilder sb = new StringBuilder();
         c.forEach(sb::append);
         anagrams.add(sb.toString());
      }

      List<String> near = new ArrayList<>(otherWords.stream().filter(wx -> Math.abs(wx.length() - len) == 1).distinct().toList());

      for (List<String> l : List.of(swaps, sameLength, substitutions, own, anagrams, near)) {
         Collections.shuffle(l, rng);
      }

      Set<String> picked = new LinkedHashSet<>();
      List<Iterator<String>> firstChoice = List.of(swaps.iterator(), sameLength.iterator(), substitutions.iterator(), own.iterator(), anagrams.iterator());
      boolean any = true;

      while (picked.size() < count && any) {
         any = false;

         for (Iterator<String> it : firstChoice) {
            while (it.hasNext() && picked.size() < count) {
               String w = it.next();
               if (!w.equals(word) && picked.add(w)) {
                  any = true;
                  break;
               }
            }
         }
      }

      Iterator<String> it = near.iterator();

      while (it.hasNext() && picked.size() < count) {
         String w = it.next();
         if (!w.equals(word)) {
            picked.add(w);
         }
      }

      for (int guard = 0; picked.size() < count && guard < 500; guard++) {
         StringBuilder sb = new StringBuilder();

         for (int i = 0; i < len; i++) {
            sb.append(rng.nextBoolean() ? word.charAt(rng.nextInt(len)) : (char)(97 + rng.nextInt(26)));
         }

         String w = sb.toString();
         if (!w.equals(word)) {
            picked.add(w);
         }
      }

      return List.copyOf(picked);
   }

   public static PoneglyphPuzzle.Step pick(
      BlueprintData.Progress p, boolean right, int rounds, long now, int lockTicks, int maxMistakes, boolean canForgive, boolean hardMode
   ) {
      if (now < p.lockUntil()) {
         return new PoneglyphPuzzle.Step(PoneglyphPuzzle.Outcome.LOCKED, p);
      } else if (right) {
         int round = p.round() + 1;
         BlueprintData.Progress next = new BlueprintData.Progress(p.owner(), p.draw(), round, p.mistakes(), 0L, p.flags(), 0);
         return new PoneglyphPuzzle.Step(round >= rounds ? PoneglyphPuzzle.Outcome.SOLVED : PoneglyphPuzzle.Outcome.CORRECT, next);
      } else if (canForgive && !p.has(4)) {
         return new PoneglyphPuzzle.Step(
            PoneglyphPuzzle.Outcome.FORGIVEN, new BlueprintData.Progress(p.owner(), p.draw(), p.round(), p.mistakes(), 0L, p.flags() | 4, p.shuffle() + 1)
         );
      } else {
         int mistakes = p.mistakes() + 1;
         if (mistakes >= maxMistakes) {
            return hardMode
               ? new PoneglyphPuzzle.Step(PoneglyphPuzzle.Outcome.DESTROYED, p)
               : new PoneglyphPuzzle.Step(PoneglyphPuzzle.Outcome.RESET, new BlueprintData.Progress(p.owner(), p.draw() + 1, 0, 0, now + (long)lockTicks, 0, 0));
         } else {
            return new PoneglyphPuzzle.Step(
               PoneglyphPuzzle.Outcome.WRONG,
               new BlueprintData.Progress(p.owner(), p.draw(), p.round(), mistakes, now + (long)lockTicks, p.flags(), p.shuffle() + 1)
            );
         }
      }
   }

   private PoneglyphPuzzle() {
   }

   public static enum Outcome {
      LOCKED,
      CORRECT,
      SOLVED,
      WRONG,
      FORGIVEN,
      RESET,
      DESTROYED;
   }

   public static record Round(String clue, List<String> options, int correct) {
      public PoneglyphPuzzle.Round reshuffled(long salt) {
         List<String> copy = new ArrayList<>(this.options);
         Collections.shuffle(copy, new Random(salt));
         return new PoneglyphPuzzle.Round(this.clue, List.copyOf(copy), copy.indexOf(this.clue));
      }
   }

   public static record Step(PoneglyphPuzzle.Outcome outcome, BlueprintData.Progress progress) {
   }
}
