package com.awakenedmod.awakened.content.exosavant;

import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.ExoGear;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.AbilityFeedback;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class ExoSuit {
   private static final double SUIT_ATTACK_DAMAGE = 4.0;
   private static final double SUIT_SPEED_PENALTY = -0.15;
   private static final int POWERUP_LEVEL = 13;
   private static final double POWERUP_SCALE = 1.25;
   private static final ResourceLocation ATTACK_ID = id("exo_attack");
   private static final ResourceLocation SPEED_ID = id("exo_speed");
   private static final int STATE_STOWED = 0;
   private static final int STATE_SUITED = 1;
   private static final int STATE_POWER = 2;
   private static final Map<UUID, Integer> LAST_STATE = new ConcurrentHashMap<>();
   private static final int ASSEMBLY_WINDOW_TICKS = 25;
   private static final int ASSEMBLY_SLOW_AMPLIFIER = 5;
   private static final Map<UUID, Long> ASSEMBLING_UNTIL = new ConcurrentHashMap<>();
   private static final String K_STOWED = "exo_suit_stowed";
   private static final String K_MIGRATED = "exo_gear_migrated";

   private ExoSuit() {
   }

   private static ResourceLocation id(String path) {
      return ResourceLocation.fromNamespaceAndPath("awakened", path);
   }

   public static boolean isSuited(ServerPlayer player) {
      return ExoLoadout.anyEquipped(player);
   }

   public static boolean isAssembling(ServerPlayer player) {
      Long until = ASSEMBLING_UNTIL.get(player.getUUID());
      return until != null && player.serverLevel().getGameTime() < until;
   }

   public static boolean isStowed(ServerPlayer player) {
      return PowerService.data(player).var("exo_suit_stowed", 0.0F) != 0.0F;
   }

   static void setStowed(ServerPlayer player, boolean stowed) {
      PowerService.data(player).setVar("exo_suit_stowed", stowed ? 1.0F : 0.0F);
   }

   public static void toggle(ServerPlayer player) {
      if (player != null) {
         if (isAssembling(player)) {
            AbilityFeedback.fail(player, "awakened.exosavant.assembling");
         } else {
            if (isSuited(player)) {
               dismiss(player);
            } else {
               summon(player);
            }
         }
      }
   }

   public static void summon(ServerPlayer player) {
      if (player != null) {
         PowerData data = PowerService.data(player);
         if ("exosavant".equals(data.powerId())) {
            if (!ExoLoadout.chipInstalled(player)) {
               AbilityFeedback.fail(player, "awakened.exosavant.no_chip");
            } else {
               boolean wasStowed = isStowed(player);
               int placed = collectAndEquip(player);
               if (!isSuited(player)) {
                  AbilityFeedback.fail(player, "awakened.exosavant.summon_empty");
               } else {
                  if (placed > 0 || wasStowed) {
                     update(player, data, true);
                     openAssemblyWindow(player);
                     summonFx(player);
                  }
               }
            }
         }
      }
   }

   private static void openAssemblyWindow(ServerPlayer player) {
      ASSEMBLING_UNTIL.put(player.getUUID(), player.serverLevel().getGameTime() + 25L);
      player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 5, false, false, false));
   }

   public static void dismiss(ServerPlayer player) {
      if (player != null) {
         PowerData data = PowerService.data(player);
         if ("exosavant".equals(data.powerId()) && isSuited(player)) {
            if (!ExoLoadout.chipInstalled(player)) {
               AbilityFeedback.fail(player, "awakened.exosavant.no_chip");
            } else {
               ExoGear store = ExoGear.get(player).copy();
               boolean storeChanged = false;

               for (ExoPiece piece : ExoPiece.values()) {
                  EquipmentSlot slot = piece.armorSlot();
                  ItemStack worn;
                  if (slot != null) {
                     worn = player.getItemBySlot(slot);
                     if (!worn.is(piece.item())) {
                        continue;
                     }

                     worn = worn.copy();
                     player.setItemSlot(slot, ItemStack.EMPTY);
                  } else {
                     worn = ExoCurios.take(player, piece);
                     if (worn.isEmpty()) {
                        continue;
                     }
                  }

                  if (store.get(piece.ordinal()).isEmpty()) {
                     store.with(piece.ordinal(), worn);
                     storeChanged = true;
                  } else {
                     giveOrDrop(player, worn);
                  }
               }

               if (storeChanged) {
                  ExoGear.write(player, store);
               }

               setStowed(player, true);
               update(player, data);
               dismissFx(player);
            }
         }
      }
   }

   static void callSuitOn(ServerPlayer player, PowerData data) {
      collectAndEquip(player);
      update(player, data, true);
   }

   private static int collectAndEquip(ServerPlayer player) {
      setStowed(player, false);
      ExoGear store = ExoGear.get(player).copy();
      boolean storeChanged = false;
      int placed = 0;

      for (ExoPiece piece : ExoPiece.values()) {
         if (!inPlace(player, piece)) {
            ItemStack found = ItemStack.EMPTY;
            if (store.get(piece.ordinal()).is(piece.item())) {
               found = store.get(piece.ordinal());
               store.with(piece.ordinal(), ItemStack.EMPTY);
               storeChanged = true;
            }

            if (found.isEmpty()) {
               found = takeFromInventory(player, piece);
            }

            if (!found.isEmpty()) {
               EquipmentSlot slot = piece.armorSlot();
               if (slot != null) {
                  ItemStack existing = player.getItemBySlot(slot);
                  if (!existing.isEmpty()) {
                     giveOrDrop(player, existing.copy());
                  }

                  player.setItemSlot(slot, found);
               } else if (!ExoCurios.equip(player, piece, found)) {
                  giveOrDrop(player, found);
               }

               placed++;
            }
         }
      }

      if (storeChanged) {
         ExoGear.write(player, store);
      }

      return placed;
   }

   private static boolean inPlace(ServerPlayer player, ExoPiece piece) {
      EquipmentSlot slot = piece.armorSlot();
      return slot != null ? player.getItemBySlot(slot).is(piece.item()) : ExoCurios.holds(player, piece);
   }

   static ItemStack takeFromInventory(ServerPlayer player, ExoPiece piece) {
      Inventory inv = player.getInventory();

      for (int i = 0; i < inv.getContainerSize(); i++) {
         if (inv.getItem(i).is(piece.item())) {
            return inv.removeItemNoUpdate(i);
         }
      }

      return ItemStack.EMPTY;
   }

   private static boolean hasInInventory(ServerPlayer player, ExoPiece piece) {
      Inventory inv = player.getInventory();

      for (int i = 0; i < inv.getContainerSize(); i++) {
         if (inv.getItem(i).is(piece.item())) {
            return true;
         }
      }

      return false;
   }

   public static boolean hasSummonablePlate(ServerPlayer player) {
      ExoGear store = ExoGear.get(player);

      for (ExoPiece piece : ExoPiece.values()) {
         if (piece.countsAsSuit() && (inPlace(player, piece) || store.get(piece.ordinal()).is(piece.item()) || hasInInventory(player, piece))) {
            return true;
         }
      }

      return false;
   }

   private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
      if (!stack.isEmpty()) {
         if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
         }
      }
   }

   private static void summonFx(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      Vec3 at = player.position().add(0.0, 1.0, 0.0);
      level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_RIPTIDE_3, SoundSource.PLAYERS, 0.9F, 0.7F);
      level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NETHERITE_BLOCK_PLACE, SoundSource.PLAYERS, 1.0F, 0.6F);
      level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.IRON_GOLEM_REPAIR, SoundSource.PLAYERS, 1.0F, 0.9F);
      level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.6F, 1.7F);
      Fx.burst(level, ExoFx.dust(), at, 10, 0.5, 0.06);
   }

   private static void dismissFx(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      Vec3 base = player.position();
      level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_RIPTIDE_2, SoundSource.PLAYERS, 0.8F, 1.15F);
      level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NETHERITE_BLOCK_BREAK, SoundSource.PLAYERS, 0.9F, 0.8F);
      level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.6F, 1.5F);
      level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.7F, 1.2F);
      Fx.burst(level, ExoFx.dust(), base.add(0.0, 1.6, 0.0), 10, 0.4, 0.08);
   }

   public static void update(ServerPlayer player, PowerData data) {
      update(player, data, false);
   }

   public static void update(ServerPlayer player, PowerData data, boolean suppressClank) {
      if (player.isAlive()) {
         if (isStowed(player) && ExoLoadout.anyEquipped(player)) {
            setStowed(player, false);
         }

         if (!ExoLoadout.chipInstalled(player) && storeHasPlates(player)) {
            collapseCloud(player);
         }

         int state = desiredState(player, data);
         int count = state == 0 ? 0 : ExoLoadout.equippedCount(player);
         if (count == 0) {
            state = 0;
         }

         int want = state == 0 ? 0 : state * 8 + count;
         Integer last = LAST_STATE.get(player.getUUID());
         if (last == null || last != want) {
            int prevCount = last != null && last != 0 ? Math.floorMod(last, 8) : 0;
            LAST_STATE.put(player.getUUID(), want);
            if (state == 0) {
               clearBuffs(player);
            } else {
               applyBuffs(player, state, count);
               if (!suppressClank && last != null && count > prevCount) {
                  playAssemblyClank(player);
               }
            }
         }
      }
   }

   private static int desiredState(ServerPlayer player, PowerData data) {
      if (!isSuited(player)) {
         return 0;
      } else {
         return data.masteryLevel() >= 13 ? 2 : 1;
      }
   }

   private static void playAssemblyClank(ServerPlayer player) {
      player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NETHERITE_BLOCK_PLACE, SoundSource.PLAYERS, 1.0F, 0.7F);
      player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.IRON_GOLEM_REPAIR, SoundSource.PLAYERS, 0.9F, 0.9F);
   }

   private static void applyBuffs(ServerPlayer player, int state, int count) {
      double powerScale = state == 2 ? 1.25 : 1.0;
      setModifier(player, Attributes.ATTACK_DAMAGE, ATTACK_ID, 4.0 * powerScale, Operation.ADD_VALUE);
      setModifier(player, Attributes.MOVEMENT_SPEED, SPEED_ID, -0.15, Operation.ADD_MULTIPLIED_TOTAL);
   }

   private static void clearBuffs(ServerPlayer player) {
      removeModifier(player, Attributes.ATTACK_DAMAGE, ATTACK_ID);
      removeModifier(player, Attributes.MOVEMENT_SPEED, SPEED_ID);
   }

   static void setModifier(ServerPlayer player, Holder<Attribute> attr, ResourceLocation id, double amount, Operation op) {
      AttributeInstance inst = player.getAttribute(attr);
      if (inst != null) {
         inst.removeModifier(id);
         if (Math.abs(amount) > 1.0E-4) {
            inst.addTransientModifier(new AttributeModifier(id, amount, op));
         }
      }
   }

   static void removeModifier(ServerPlayer player, Holder<Attribute> attr, ResourceLocation id) {
      AttributeInstance inst = player.getAttribute(attr);
      if (inst != null) {
         inst.removeModifier(id);
      }
   }

   public static void clear(ServerPlayer player) {
      clearBuffs(player);
      LAST_STATE.remove(player.getUUID());
      ASSEMBLING_UNTIL.remove(player.getUUID());
      ExoVisualState.clear(player);
   }

   private static boolean storeHasPlates(ServerPlayer player) {
      ExoGear store = ExoGear.get(player);

      for (ExoPiece piece : ExoPiece.values()) {
         if (piece.countsAsSuit() && store.get(piece.ordinal()).is(piece.item())) {
            return true;
         }
      }

      return false;
   }

   public static void migrateLegacyStore(ServerPlayer player) {
      PowerData data = PowerService.data(player);
      if (data.var("exo_gear_migrated", 0.0F) == 0.0F) {
         data.setVar("exo_gear_migrated", 1.0F);
         if (!isStowed(player) && !ExoGear.get(player).isEmpty()) {
            drainStoreToInventory(player);
            setStowed(player, false);
            player.displayClientMessage(Component.translatable("awakened.exosavant.gear_returned"), false);
         }
      }
   }

   private static void collapseCloud(ServerPlayer player) {
      setStowed(player, false);
      ExoGear store = ExoGear.get(player).copy();
      boolean changed = false;

      for (ExoPiece piece : ExoPiece.values()) {
         if (piece.countsAsSuit() && store.get(piece.ordinal()).is(piece.item())) {
            giveOrDrop(player, store.get(piece.ordinal()).copy());
            store.with(piece.ordinal(), ItemStack.EMPTY);
            changed = true;
         }
      }

      if (changed) {
         ExoGear.write(player, store);
      }
   }

   public static void drainStoreToInventory(ServerPlayer player) {
      ExoGear store = ExoGear.get(player);

      for (int i = 0; i < 6; i++) {
         giveOrDrop(player, store.get(i).copy());
      }

      ExoGear.clear(player);
   }
}
