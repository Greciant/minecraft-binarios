package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.content.exosavant.ExoSkin;
import com.awakenedmod.awakened.content.exosavant.ExoSkins;
import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.data.ExoVisual;
import com.awakenedmod.awakened.core.net.ExoSkinSelectPayload;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

public final class ExoSkinClient {
   private static final long PREVIEW_MS = 150L;
   private static ExoSkin preview;
   private static long previewAt;

   private ExoSkinClient() {
   }

   public static ExoSkin shown(Entity entity) {
      Minecraft mc = Minecraft.getInstance();
      boolean live = preview != null && Util.getMillis() - previewAt < 150L;
      return live && entity == mc.player ? preview : ExoSkins.worn(entity);
   }

   public static void preview(ExoSkin skin) {
      preview = skin;
      previewAt = Util.getMillis();
   }

   public static boolean suited() {
      Minecraft mc = Minecraft.getInstance();
      return mc.player != null && ((ExoVisual)mc.player.getData(AwakenedAttachments.EXO_VISUAL)).suited();
   }

   public static void wear(ExoSkin skin) {
      ExoWheel.send(new ExoSkinSelectPayload(skin.id()));
   }
}
