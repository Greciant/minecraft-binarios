package com.awakenedmod.awakened.content.exosavant;

import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.data.ExoSkinData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public final class ExoSkins {
   private ExoSkins() {
   }

   public static ExoSkin worn(Entity entity) {
      return entity.hasData(AwakenedAttachments.EXO_SKINS)
         ? ExoSkin.byId(((ExoSkinData)entity.getData(AwakenedAttachments.EXO_SKINS)).selected())
         : ExoSkin.DEFAULT;
   }

   public static boolean isUnlocked(Entity entity, ExoSkin skin) {
      return skin.free() || entity.hasData(AwakenedAttachments.EXO_SKINS) && ((ExoSkinData)entity.getData(AwakenedAttachments.EXO_SKINS)).isUnlocked(skin.id());
   }

   public static void unlock(ServerPlayer player, ExoSkin skin) {
      ExoSkinData data = (ExoSkinData)player.getData(AwakenedAttachments.EXO_SKINS);
      if (!skin.free() && data.unlock(skin.id())) {
         player.setData(AwakenedAttachments.EXO_SKINS, data);
         PowerService.announceUnlock(
            player, Component.translatable("awakened.exo_skin.unlocked", new Object[]{skin.displayName()}), PowerService.Unlock.FEATURE
         );
      }
   }

   public static void select(ServerPlayer player, String id) {
      ExoSkin skin = ExoSkin.byId(id);
      if (skin.id().equals(id) && isUnlocked(player, skin)) {
         ExoSkinData data = (ExoSkinData)player.getData(AwakenedAttachments.EXO_SKINS);
         data.select(skin.id());
         player.setData(AwakenedAttachments.EXO_SKINS, data);
      }
   }
}
