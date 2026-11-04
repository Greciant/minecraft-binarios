package com.awakenedmod.awakened.core.relic;

import com.awakenedmod.awakened.content.relic.RelicBeltItem;
import com.awakenedmod.awakened.content.relic.RelicBeltMenu;
import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.ItemGrant;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.curios.CuriosSlots;
import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.data.RelicData;
import com.awakenedmod.awakened.core.skill.Stat;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

public final class RelicSystem {
   private static final ResourceLocation HEALTH_MODIFIER_ID = AwId.of("relic_max_health");
   public static final String BELT_SLOT = "belt";

   private RelicSystem() {
   }

   public static RelicData data(Player player) {
      return AwakenedAttachments.relics(player);
   }

   public static float bonus(Player player, Stat stat) {
      RelicData data = data(player);
      if (data.attuned().isEmpty()) {
         return 0.0F;
      } else {
         RelicContext ctx = new RelicContext(player);
         float sum = 0.0F;

         for (String id : data.attuned()) {
            Relic relic = RelicRegistry.get(id);
            if (relic != null) {
               sum += relic.statBonus(ctx, stat);
            }
         }

         return sum;
      }
   }

   public static void openBelt(ServerPlayer player) {
      if (!equippedBelt(player).isEmpty() || equipBelt(player)) {
         RelicBeltMenu.open(player);
      }
   }

   public static ItemStack equippedBelt(Player player) {
      ICuriosItemHandler inv = CuriosSlots.inventory(player);
      return inv == null
         ? ItemStack.EMPTY
         : inv.findFirstCurio(stack -> stack.getItem() instanceof RelicBeltItem).<ItemStack>map(SlotResult::stack).orElse(ItemStack.EMPTY);
   }

   private static IDynamicStackHandler beltHandler(Player player) {
      return CuriosSlots.stacks(player, "belt");
   }

   public static boolean equipBelt(ServerPlayer player) {
      if (!equippedBelt(player).isEmpty()) {
         return true;
      } else {
         IDynamicStackHandler handler = beltHandler(player);
         int slot = handler == null ? -1 : firstEmptySlot(handler);
         if (slot < 0) {
            player.displayClientMessage(Component.translatable(handler == null ? "awakened.relic.need_belt" : "awakened.relic.belt_slot_taken"), false);
            return false;
         } else {
            ItemStack belt = RelicBeltItem.takeStack(player);
            if (belt.isEmpty()) {
               player.displayClientMessage(Component.translatable("awakened.relic.need_belt"), false);
               return false;
            } else {
               handler.setStackInSlot(slot, belt);
               refresh(player);
               player.displayClientMessage(Component.translatable("awakened.relic.belt_equipped"), true);
               player.serverLevel()
                  .playSound(null, player.blockPosition(), (SoundEvent)SoundEvents.ARMOR_EQUIP_NETHERITE.value(), SoundSource.PLAYERS, 0.8F, 0.9F);
               return true;
            }
         }
      }
   }

   public static void unequipBelt(ServerPlayer player) {
      IDynamicStackHandler handler = beltHandler(player);
      if (handler != null) {
         for (int i = 0; i < handler.getSlots(); i++) {
            if (handler.getStackInSlot(i).getItem() instanceof RelicBeltItem) {
               ItemStack belt = handler.getStackInSlot(i).copy();
               handler.setStackInSlot(i, ItemStack.EMPTY);
               ItemGrant.to(player, belt);
               refresh(player);
               player.displayClientMessage(Component.translatable("awakened.relic.belt_removed"), true);
               player.serverLevel()
                  .playSound(null, player.blockPosition(), (SoundEvent)SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 0.6F, 0.9F);
               return;
            }
         }
      }
   }

   public static void handleBelt(ServerPlayer player, boolean equip) {
      if (player != null) {
         if (equip) {
            equipBelt(player);
         } else {
            unequipBelt(player);
         }
      }
   }

   public static void writeBeltSlots(ServerPlayer player, List<String> relicIds) {
      if (player != null) {
         IDynamicStackHandler handler = beltHandler(player);
         if (handler != null) {
            for (int i = 0; i < handler.getSlots(); i++) {
               if (handler.getStackInSlot(i).getItem() instanceof RelicBeltItem) {
                  handler.setStackInSlot(i, RelicBeltItem.withRelics(relicIds));
                  refresh(player);
                  return;
               }
            }
         }
      }
   }

   private static int firstEmptySlot(IDynamicStackHandler handler) {
      for (int i = 0; i < handler.getSlots(); i++) {
         if (handler.getStackInSlot(i).isEmpty()) {
            return i;
         }
      }

      return -1;
   }

   public static void refresh(ServerPlayer player) {
      ItemStack belt = equippedBelt(player);
      RelicData data = data(player);
      boolean worn = !belt.isEmpty();
      data.setBelt(worn);
      data.setAttuned(worn ? RelicBeltItem.relicIds(belt) : List.of());
      applyAttributes(player);
      player.setData(AwakenedAttachments.RELIC_DATA, data);
      float max = StaminaSystem.max(player);
      if (PowerService.data(player).stamina() > max) {
         PowerService.data(player).setStamina(max);
      }

      PowerService.sync(player);
      PowerService.syncRelics(player);
   }

   public static void applyAttributes(ServerPlayer player) {
      AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
      if (health != null) {
         health.removeModifier(HEALTH_MODIFIER_ID);
         float bonus = bonus(player, Stat.MAX_HEALTH);
         if (bonus != 0.0F) {
            health.addTransientModifier(new AttributeModifier(HEALTH_MODIFIER_ID, (double)bonus, Operation.ADD_VALUE));
         }

         if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
         }
      }
   }
}
