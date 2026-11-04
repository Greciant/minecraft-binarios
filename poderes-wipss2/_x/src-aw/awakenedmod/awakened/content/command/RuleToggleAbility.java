package com.awakenedmod.awakened.content.command;

import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class RuleToggleAbility extends Ability {
   private final int slotIndex;

   public RuleToggleAbility(int slotIndex, int unlockLevel) {
      super(CommandIds.ruleAbility(slotIndex), 0.0F, unlockLevel);
      this.slotIndex = slotIndex;
   }

   public int slotIndex() {
      return this.slotIndex;
   }

   @Override
   public float staminaCost(ServerPlayer player) {
      return 0.0F;
   }

   @Override
   public String costLabel(Player viewer) {
      if (viewer == null) {
         return "";
      } else {
         RuleSlot slot = CommandData.of(viewer).slot(this.slotIndex);
         return !slot.isEmpty() && slot.rule().resolves()
            ? String.valueOf((int)this.billed((float)CommandCost.points(slot.rule(), CommandMastery.of(viewer))))
            : "";
      }
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().boosts("rule_number_cap").build();
   }

   @Override
   public boolean activate(ServerPlayer player) {
      PowerData data = PowerService.data(player);
      RuleSlot slot = CommandData.of(player).slot(this.slotIndex);
      if (slot.isEmpty()) {
         return this.fail(player, "awakened.feedback.command_no_rule", new Object[0]);
      } else if (slot.on()) {
         RuleEngine.switchOff(player, this.slotIndex);
         return true;
      } else {
         RuleEngine.Refusal refusal = RuleEngine.switchOn(player, data, this.slotIndex);
         return refusal != null ? this.fail(player, refusal.key(), refusal.args()) : true;
      }
   }

   @Override
   public Component displayName() {
      return Component.translatable("ability.awakened." + CommandIds.ruleAbility(this.slotIndex));
   }

   @Override
   public Component displayName(Player viewer) {
      String name = CommandData.of(viewer).name(this.slotIndex);
      return (Component)(name.isEmpty() ? this.displayName() : Component.literal(name));
   }
}
