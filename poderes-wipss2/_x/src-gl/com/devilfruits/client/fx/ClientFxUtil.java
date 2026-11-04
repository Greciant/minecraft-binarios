package com.devilfruits.client.fx;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

public final class ClientFxUtil {
   public static boolean isLocal(@Nullable Entity entity) {
      return entity != null && entity == Minecraft.getInstance().player;
   }

   public static boolean firstPersonCaster(@Nullable Entity caster) {
      return isLocal(caster) && !Minecraft.getInstance().gameRenderer.getMainCamera().isDetached();
   }

   private ClientFxUtil() {
   }
}
