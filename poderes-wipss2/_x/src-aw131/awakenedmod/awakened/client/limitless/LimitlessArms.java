package com.awakenedmod.awakened.client.limitless;

import com.awakenedmod.awakened.client.AwakenedAnimations;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

final class LimitlessArms {
   private LimitlessArms() {
   }

   static boolean firstPerson(Player p) {
      Minecraft mc = Minecraft.getInstance();
      return p == mc.player && mc.options.getCameraType().isFirstPerson();
   }

   static float armPitch(Player p, float partialTick, boolean firstPerson) {
      float look = p.getViewXRot(partialTick);
      return firstPerson ? AwakenedAnimations.firstPersonArmPitch(look) : AwakenedAnimations.aimedChannelArmPitch(p, look, p.isCrouching());
   }
}
