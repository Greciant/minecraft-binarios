package com.devilfruits.fruits.uo_uo_seiryu.client.dev;

import com.devilfruits.client.dev.CaptureStage;
import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.form.FormManager;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuAbilities;
import com.devilfruits.fruits.uo_uo_seiryu.UoUoSeiryu;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public abstract class GpoCapture extends CaptureStage {
   protected GpoCapture(String prefix) {
      super(prefix);
   }

   protected static void cast(ServerPlayer p, String ability, int hold) {
      castAbility(p, "devilfruits:uo_uo_seiryu/" + ability, hold);
   }

   protected static void veil(ServerPlayer p, boolean on) {
      Ability veil = SeiryuAbilities.THUNDERCLAD_VEIL;
      if (!on) {
         AbilityManager.stopToggle(p, veil);
      } else {
         cast(p, "thunderclad_veil", 1);
      }
   }

   protected String form() {
      return "hybrid";
   }

   @Override
   protected ResourceLocation fruit() {
      return UoUoSeiryu.ID;
   }

   @Override
   protected void setup(ServerPlayer p) {
      FormManager.request(p, this.form(), FormManager.Reason.COMMAND);
   }

   @Override
   protected void onReset(ServerPlayer p) {
      veil(p, false);
   }
}
