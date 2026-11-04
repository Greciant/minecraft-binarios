package com.devilfruits.core.ability;

import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.form.FormManager;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.registry.DFAttachments;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class FormSwitchAbility extends Ability {
   private final String targetForm;

   public FormSwitchAbility(ResourceLocation id, String targetForm) {
      this(id, targetForm, AbilityStats.builder().energy(0).cooldown(0).damage(0.0).range(1.0).knockback(0.0).build());
   }

   public FormSwitchAbility(ResourceLocation id, String targetForm, AbilityStats stats) {
      super(id, CastType.FORM_SWITCH, stats);
      this.targetForm = targetForm;
   }

   public String targetForm() {
      return this.targetForm;
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.NONE;
   }

   public boolean trigger(ServerPlayer player) {
      PlayerFruitData data = DFAttachments.data(player);
      FormManager.Result result = this.targetForm.equals(data.formName())
         ? FormManager.revert(player, FormManager.Reason.PLAYER)
         : FormManager.request(player, this.targetForm, FormManager.Reason.PLAYER);
      return result == FormManager.Result.SWITCHED || result == FormManager.Result.WINDUP || result == FormManager.Result.CANCELLED;
   }
}
