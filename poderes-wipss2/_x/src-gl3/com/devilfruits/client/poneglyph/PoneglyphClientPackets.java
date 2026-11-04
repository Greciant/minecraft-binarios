package com.devilfruits.client.poneglyph;

import com.devilfruits.core.poneglyph.Cipher;
import com.devilfruits.core.poneglyph.DecipherDeskMenu;
import com.devilfruits.core.poneglyph.PoneglyphNetwork;
import net.minecraft.client.Minecraft;

public final class PoneglyphClientPackets {
   public static void rubState(PoneglyphNetwork.RubStatePayload p) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null) {
         switch (p.state()) {
            case 0:
               ClientPoneglyphState.rub = new ClientPoneglyphState.Rub(p.pos(), p.cell(), p.carving(), p.duration(), mc.level.getGameTime());
               break;
            case 1:
               ClientPoneglyphState.rub = null;
               RubbingClient.finished(p.pos());
               break;
            default:
               ClientPoneglyphState.rub = null;
         }
      }
   }

   public static void deskState(PoneglyphNetwork.DeskStatePayload p) {
      ClientPoneglyphState.desk = p;
      ClientPoneglyphState.deskTime = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0L;
      if (Minecraft.getInstance().screen instanceof DecipherDeskScreen screen && ((DecipherDeskMenu)screen.getMenu()).containerId == p.containerId()) {
         screen.onState(p);
      }
   }

   public static void sync(PoneglyphNetwork.PoneglyphSyncPayload p) {
      ClientPoneglyphState.worldbound = p.worldbound();
      int[] perm = p.permutationArray();
      ClientPoneglyphState.permutation = Cipher.isBijective(perm) ? perm : Cipher.identity();
      ClientPoneglyphState.PONEGLYPHS.clear();
      p.poneglyphs().forEach(info -> ClientPoneglyphState.PONEGLYPHS.put(info.id(), info));
   }

   public static void toast(PoneglyphNetwork.KnowledgeToastPayload p) {
      Minecraft.getInstance().getToasts().addToast(new KnowledgeToast(p.nameKey(), p.icon()));
   }

   private PoneglyphClientPackets() {
   }
}
