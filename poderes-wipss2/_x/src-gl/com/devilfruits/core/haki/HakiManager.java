package com.devilfruits.core.haki;

import com.devilfruits.DevilFruits;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.registry.DFAttachments;
import com.devilfruits.registry.DFTriggers;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class HakiManager {
   public static PlayerHakiData data(Player player) {
      return (PlayerHakiData)player.getData(DFAttachments.HAKI);
   }

   public static boolean unlocked(Player player) {
      return player.hasData(DFAttachments.HAKI) && data(player).armament();
   }

   public static int level(Player player) {
      return unlocked(player) ? data(player).level() : 0;
   }

   public static int mask(Player player) {
      return player.hasData(DFAttachments.HAKI) ? data(player).mask() : 0;
   }

   public static float maxSpirit(int level) {
      return (float)((Double)HakiConfig.SPIRIT_BASE.get() + (Double)HakiConfig.SPIRIT_PER_LEVEL.get() * (double)level);
   }

   public static float xpToNext(int level) {
      return (float)((Double)HakiConfig.XP_BASE.get() * Math.pow((double)level, (Double)HakiConfig.XP_EXPONENT.get()));
   }

   public static float logiaFraction(int level) {
      return (float)Math.min(1.0, (Double)HakiConfig.LOGIA_FRACTION_BASE.get() + (Double)HakiConfig.LOGIA_FRACTION_PER_LEVEL.get() * (double)level);
   }

   public static int wanted(PlayerHakiData data) {
      int allowed = HakiParts.allowed(data.tier());
      int mask = HakiParts.normalize(data.selected()) & allowed;
      return mask != 0 ? mask : HakiParts.defaults(data.tier());
   }

   public static boolean unlock(ServerPlayer player) {
      PlayerHakiData data = data(player);
      if (data.armament()) {
         return false;
      } else {
         data.setArmament(true);
         data.setLevel(1);
         data.setXp(0.0F);
         data.setSpirit(maxSpirit(1));
         sync(player);
         ServerFx.sendNear(player.serverLevel(), HakiFx.UNLOCK, player.getId(), player.position(), Vec3.ZERO, 1.0F, 0L, 1, 48.0);
         player.sendSystemMessage(Component.translatable("haki.devilfruits.message.unlocked").withStyle(ChatFormatting.GRAY));
         DFTriggers.HAKI_UNLOCKED.get().trigger(player);
         return true;
      }
   }

   public static void setLevel(ServerPlayer player, int level) {
      PlayerHakiData data = data(player);
      if (!data.armament()) {
         unlock(player);
      }

      int before = data.level();
      data.setLevel(level);
      data.setXp(Math.min(data.xp(), xpToNext(data.level()) - 1.0F));
      data.setSpirit(Math.min(Math.max(0.0F, data.spirit()), maxSpirit(data.level())));
      if (data.active()) {
         data.setCoat(true, data.mask() & HakiParts.allowed(data.tier()));
      }

      if (data.level() > before) {
         leveledUp(player, data);
      }

      sync(player);
   }

   public static void addXp(ServerPlayer player, float amount) {
      PlayerHakiData data = data(player);
      if (data.armament() && !(amount <= 0.0F) && data.level() < 10) {
         float xp = data.xp() + amount;

         boolean up;
         for (up = false; data.level() < 10 && xp >= xpToNext(data.level()); up = true) {
            xp -= xpToNext(data.level());
            data.setLevel(data.level() + 1);
         }

         data.setXp(data.level() >= 10 ? 0.0F : xp);
         if (up) {
            leveledUp(player, data);
            sync(player);
         }
      }
   }

   private static void leveledUp(ServerPlayer player, PlayerHakiData data) {
      player.displayClientMessage(
         Component.translatable("haki.devilfruits.message.level_up", new Object[]{data.level(), Component.translatable(data.tier().translationKey())}), true
      );
      ServerFx.sendNear(player.serverLevel(), HakiFx.LEVEL_UP, player.getId(), player.position(), Vec3.ZERO, (float)data.level(), 0L, 0, 48.0);
      if (data.level() >= 10) {
         DFTriggers.HAKI_MASTER.get().trigger(player);
      }
   }

   public static void reset(ServerPlayer player) {
      data(player).reset();
      sync(player);
   }

   public static void toggle(ServerPlayer player) {
      PlayerHakiData data = data(player);
      if (data.active()) {
         coatOff(player, false);
      } else {
         coatOn(player);
      }
   }

   public static void select(ServerPlayer player, int mask) {
      PlayerHakiData data = data(player);
      if (data.armament()) {
         data.setSelected(HakiParts.normalize(mask) & HakiParts.allowed(data.tier()));
         if (data.active()) {
            data.setCoat(true, wanted(data));
         }

         sync(player);
      }
   }

   public static boolean coatOn(ServerPlayer player) {
      PlayerHakiData data = data(player);
      if (data.armament() && !data.active() && !player.isSpectator()) {
         float cost = ((Double)HakiConfig.ACTIVATION_COST.get()).floatValue();
         if (spirit(data) < cost) {
            player.displayClientMessage(Component.translatable("haki.devilfruits.message.no_spirit"), true);
            return false;
         } else {
            data.setSpirit(spirit(data) - cost);
            data.setCoat(true, wanted(data));
            ServerFx.animate(player, DevilFruits.id("haki/coat"));
            ServerFx.sendNear(player.serverLevel(), HakiFx.COAT, player.getId(), player.position(), Vec3.ZERO, (float)data.level(), 0L, data.mask(), 48.0);
            sync(player);
            return true;
         }
      } else {
         return false;
      }
   }

   public static void coatOff(ServerPlayer player, boolean flicker) {
      PlayerHakiData data = data(player);
      if (data.active()) {
         int mask = data.mask();
         data.setCoat(false, 0);
         data.setRyuoCharge(0);
         data.setRegenDelay((Integer)HakiConfig.REGEN_DELAY.get());
         ServerFx.sendNear(player.serverLevel(), flicker ? HakiFx.FLICKER : HakiFx.UNCOAT, player.getId(), player.position(), Vec3.ZERO, 1.0F, 0L, mask, 48.0);
         if (flicker) {
            player.displayClientMessage(Component.translatable("haki.devilfruits.message.spent"), true);
         }

         sync(player);
      }
   }

   public static void ryuoStart(ServerPlayer player) {
      PlayerHakiData data = data(player);
      if (data.armament() && data.tier().ryuo() && data.ryuoCharge() <= 0 && !player.isSpectator()) {
         if ((double)spirit(data) < (Double)HakiConfig.RYUO_COST.get()) {
            player.displayClientMessage(Component.translatable("haki.devilfruits.message.no_spirit"), true);
         } else if (data.active() || coatOn(player)) {
            data.setRyuoCharge(1);
            ServerFx.animate(player, DevilFruits.id("haki/ryuo_charge"));
            ServerFx.chargeStart(player, HakiFx.RYUO_CHARGE, (float)((Integer)HakiConfig.RYUO_CHARGE_TICKS.get()).intValue(), player.getRandom().nextLong());
            sync(player);
         }
      }
   }

   public static void ryuoRelease(ServerPlayer player) {
      PlayerHakiData data = data(player);
      int charge = data.ryuoCharge();
      if (charge > 0) {
         data.setRyuoCharge(0);
         float fraction = Math.min(1.0F, (float)charge / (float)((Integer)HakiConfig.RYUO_CHARGE_TICKS.get()).intValue());
         ServerFx.chargeStop(player, HakiFx.RYUO_CHARGE, fraction >= 0.2F);
         if (fraction >= 0.2F && data.active()) {
            data.setSpirit(spirit(data) - ((Double)HakiConfig.RYUO_COST.get()).floatValue());
            HakiCombat.ryuo(player, data, fraction);
         } else {
            ServerFx.stopAnimation(player);
         }

         sync(player);
      }
   }

   public static void tick(ServerPlayer player) {
      if (player.hasData(DFAttachments.HAKI)) {
         PlayerHakiData data = data(player);
         if (data.armament()) {
            float max = maxSpirit(data.level());
            float before = data.spirit();
            float spirit = spirit(data);
            if (data.active()) {
               int allowed = data.mask() & HakiParts.allowed(data.tier());
               if (allowed != data.mask()) {
                  data.setCoat(allowed != 0, allowed);
                  sync(player);
               }

               float share = Math.max(0.2F, HakiParts.coverage(data.mask()) + ((data.mask() & 256) != 0 ? 0.05F : 0.0F));
               spirit -= (float)(HakiConfig.DRAIN_PER_SECOND.get() * (double)share * drainFactor(data.tier()) / 20.0);
               if (data.ryuoCharge() > 0 && data.ryuoCharge() < (Integer)HakiConfig.RYUO_CHARGE_TICKS.get()) {
                  data.setRyuoCharge(data.ryuoCharge() + 1);
               }

               if (spirit <= 0.0F) {
                  data.setSpirit(0.0F);
                  coatOff(player, true);
                  return;
               }
            } else if (data.regenDelay() > 0) {
               data.setRegenDelay(data.regenDelay() - 1);
            } else {
               spirit = Math.min(max, spirit + ((Double)HakiConfig.REGEN_PER_SECOND.get()).floatValue() / 20.0F);
            }

            data.setSpirit(spirit);
            if (spirit != before && data.syncDue()) {
               sync(player);
            }
         }
      }
   }

   private static float spirit(PlayerHakiData data) {
      if (data.spirit() < 0.0F) {
         data.setSpirit(maxSpirit(data.level()));
      }

      return data.spirit();
   }

   private static double drainFactor(HakiTier tier) {
      return switch (tier) {
         case NOVICE -> HakiConfig.DRAIN_NOVICE.get();
         case ADEPT -> HakiConfig.DRAIN_ADEPT.get();
         case EXPERT -> HakiConfig.DRAIN_EXPERT.get();
         case MASTER -> HakiConfig.DRAIN_MASTER.get();
      };
   }

   public static void sync(ServerPlayer player) {
      player.syncData(DFAttachments.HAKI);
   }

   private HakiManager() {
   }
}
