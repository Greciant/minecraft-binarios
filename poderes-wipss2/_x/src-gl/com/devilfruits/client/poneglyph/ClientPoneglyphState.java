package com.devilfruits.client.poneglyph;

import com.devilfruits.core.poneglyph.Cipher;
import com.devilfruits.core.poneglyph.PoneglyphNetwork;
import com.devilfruits.core.poneglyph.Poneglyphs;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

public final class ClientPoneglyphState {
   static boolean worldbound;
   static int[] permutation = Cipher.identity();
   static final Map<ResourceLocation, PoneglyphNetwork.PoneglyphInfo> PONEGLYPHS = new HashMap<>();
   @Nullable
   static ClientPoneglyphState.Rub rub;
   @Nullable
   static PoneglyphNetwork.DeskStatePayload desk;
   static long deskTime;

   public static boolean worldbound() {
      return worldbound;
   }

   public static int[] permutation() {
      return permutation;
   }

   @Nullable
   public static PoneglyphNetwork.PoneglyphInfo poneglyph(ResourceLocation id) {
      return PONEGLYPHS.get(id);
   }

   public static MutableComponent glyphs(String text) {
      return Component.literal(Cipher.encode(text, permutation)).withStyle(Style.EMPTY.withFont(Cipher.FONT));
   }

   public static boolean readable(@Nullable Player player) {
      return player != null && player.getInventory().hasAnyMatching(s -> s.is((Item)Poneglyphs.PRIMER.get()));
   }

   private ClientPoneglyphState() {
   }

   static record Rub(BlockPos pos, int cell, int carving, int duration, long startTick) {
      float progress(float partialTick) {
         Minecraft mc = Minecraft.getInstance();
         long now = mc.level == null ? this.startTick : mc.level.getGameTime();
         return Math.min(1.0F, ((float)(now - this.startTick) + partialTick) / (float)Math.max(1, this.duration));
      }
   }
}
