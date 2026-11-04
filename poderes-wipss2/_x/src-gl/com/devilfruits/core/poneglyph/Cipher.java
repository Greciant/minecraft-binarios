package com.devilfruits.core.poneglyph;

import java.util.SplittableRandom;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

public final class Cipher {
   public static final ResourceLocation FONT = ResourceLocation.withDefaultNamespace("alt");
   private static final int[] IDENTITY = identity();

   public static int[] permutation(long worldSeed) {
      int[] p = identity();
      SplittableRandom random = new SplittableRandom(worldSeed ^ 6819981342808085044L);

      for (int i = p.length - 1; i > 0; i--) {
         int j = random.nextInt(i);
         int t = p[i];
         p[i] = p[j];
         p[j] = t;
      }

      return p;
   }

   public static int[] identity() {
      int[] p = new int[26];
      int i = 0;

      while (i < 26) {
         p[i] = i++;
      }

      return p;
   }

   public static int[] active(MinecraftServer server) {
      return PoneglyphConfig.CIPHER_MODE.get() == PoneglyphConfig.CipherMode.WORLDBOUND ? permutation(server.overworld().getSeed()) : (int[])IDENTITY.clone();
   }

   public static String encode(String text, int[] perm) {
      StringBuilder sb = new StringBuilder(text.length());

      for (int i = 0; i < text.length(); i++) {
         char c = Character.toLowerCase(text.charAt(i));
         sb.append(c >= 'a' && c <= 'z' ? (char)(97 + perm[c - 'a']) : text.charAt(i));
      }

      return sb.toString();
   }

   public static boolean isBijective(int[] perm) {
      if (perm.length != 26) {
         return false;
      } else {
         boolean[] seen = new boolean[26];

         for (int v : perm) {
            if (v < 0 || v >= 26 || seen[v]) {
               return false;
            }

            seen[v] = true;
         }

         return true;
      }
   }

   private Cipher() {
   }
}
