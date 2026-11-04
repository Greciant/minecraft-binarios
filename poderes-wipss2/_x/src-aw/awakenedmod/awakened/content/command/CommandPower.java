package com.awakenedmod.awakened.content.command;

import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.power.Power;
import java.util.Arrays;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class CommandPower extends Power {
   public static final String ID = "command";
   public static final int THEME = -1525686;
   private static final int[] SLOT_UNLOCK = new int[5];
   private static final int[] SLOT_WEDGE = new int[5];
   private static final int[] PARALLEL_SLOTS = new int[]{0, 2, 4};

   public CommandPower() {
      super(
         "command",
         List.of(
            new RuleToggleAbility(0, 0), new RuleToggleAbility(1, 3), new RuleToggleAbility(2, 5), new RuleToggleAbility(3, 7), new RuleToggleAbility(4, 9)
         )
      );
      Arrays.fill(SLOT_WEDGE, -1);
      List<Ability> wedges = this.abilities();

      for (int i = 0; i < wedges.size(); i++) {
         Object var4 = wedges.get(i);
         if (var4 instanceof RuleToggleAbility) {
            RuleToggleAbility toggle = (RuleToggleAbility)var4;
            if (toggle.slotIndex() >= 0 && toggle.slotIndex() < 5) {
               SLOT_UNLOCK[toggle.slotIndex()] = toggle.unlockLevel();
               SLOT_WEDGE[toggle.slotIndex()] = i;
            }
         }
      }
   }

   public static boolean owns(ServerPlayer player) {
      return player != null && "command".equals(PowerService.data(player).powerId());
   }

   public static int slotUnlockLevel(int slotIndex) {
      return slotIndex >= 0 && slotIndex < 5 ? SLOT_UNLOCK[slotIndex] : 0;
   }

   public static int maxOn(int mastery) {
      int on = 0;

      for (int slot : PARALLEL_SLOTS) {
         if (mastery >= slotUnlockLevel(slot)) {
            on++;
         }
      }

      return Math.max(1, Math.min(3, on));
   }

   public static int slotWedge(int slotIndex) {
      return slotIndex >= 0 && slotIndex < 5 ? SLOT_WEDGE[slotIndex] : -1;
   }

   @Override
   public int themeColor() {
      return -1525686;
   }

   @Override
   public int signatureSlot() {
      List<Ability> wedges = this.abilities();

      for (int i = 0; i < wedges.size(); i++) {
         if (wedges.get(i) instanceof RuleToggleAbility toggle && toggle.slotIndex() == 0) {
            return i;
         }
      }

      return 0;
   }

   @Override
   public boolean rollable() {
      return true;
   }

   @Override
   public void passiveTick(ServerPlayer player, PowerData data) {
      super.passiveTick(player, data);
      RuleEngine.tick(player, data);
   }

   @Override
   public float lockedStamina(ServerPlayer player, PowerData data) {
      return CommandStamina.activeSum(player, CommandData.of(player));
   }

   @Override
   public String lockedStaminaLabel(Player viewer) {
      return viewer == null ? "" : CommandSentences.onSlotLabel(viewer);
   }

   @Override
   public List<String> lockedStaminaReturnLabels(Player viewer) {
      return viewer == null ? List.of() : CommandSentences.returnLabels(viewer);
   }

   @Override
   public boolean lockedStaminaLive(Player viewer) {
      return viewer != null && CommandSentences.anyLive(viewer);
   }

   @Override
   public boolean abilityLitOnClient(Player viewer, int slot) {
      if (viewer == null) {
         return false;
      } else {
         for (int i = 0; i < 5; i++) {
            if (SLOT_WEDGE[i] == slot) {
               return CommandData.of(viewer).slot(i).on();
            }
         }

         return false;
      }
   }

   @Override
   public List<Component> staminaBarLines(Player viewer) {
      return viewer == null ? List.of() : CommandSentences.onSentences(viewer);
   }

   @Override
   public int staminaBarLineRoom(int mastery) {
      return maxOn(mastery);
   }

   @Override
   public void onDealDamage(ServerPlayer attacker, LivingEntity victim) {
      RuleEngine.onDealDamage(attacker, victim);
   }

   @Override
   public float onIncomingDamage(ServerPlayer player, DamageSource source, float amount) {
      float chained = super.onIncomingDamage(player, source, amount);
      RuleEngine.onIncomingDamage(player, source);
      return chained;
   }

   @Override
   public void onDamageTaken(ServerPlayer player, DamageSource source, float amount) {
      RuleEngine.onDamageTaken(player, source, amount);
   }

   @Override
   public void onKill(ServerPlayer killer, LivingEntity victim) {
      RuleEngine.onKill(killer, victim);
   }

   @Override
   public void onLogout(ServerPlayer player) {
      RuleEngine.clearCaster(player);
   }

   @Override
   public void onDeath(ServerPlayer victim, DamageSource source) {
      RuleEngine.onCasterDeath(victim);
   }

   @Override
   public void onRespawn(ServerPlayer player, PowerData data) {
      CommandStamina.relock(player, data, CommandData.of(player));
   }

   @Override
   public void onSuppressed(ServerPlayer player, PowerData data) {
      SealedRules.pause(player);
   }

   @Override
   public void onUnsuppressed(ServerPlayer player, PowerData data) {
      SealedRules.resume(player, data);
   }

   @Override
   public void onReplaced(ServerPlayer player) {
      RuleEngine.clearCaster(player);
      CommandData.write(player, CommandData.EMPTY);
   }
}
