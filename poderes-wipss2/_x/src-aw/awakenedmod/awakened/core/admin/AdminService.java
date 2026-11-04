package com.awakenedmod.awakened.core.admin;

import com.awakenedmod.awakened.content.AwakenedEntities;
import com.awakenedmod.awakened.content.entity.ArbiterEntity;
import com.awakenedmod.awakened.content.entity.EnforcerEntity;
import com.awakenedmod.awakened.content.entity.OverseerEntity;
import com.awakenedmod.awakened.content.entity.StalkerEntity;
import com.awakenedmod.awakened.content.entity.SuppressorEntity;
import com.awakenedmod.awakened.content.entity.sentinel.SentinelEntity;
import com.awakenedmod.awakened.content.reaper.HauntControl;
import com.awakenedmod.awakened.content.reaper.ReaperAttachments;
import com.awakenedmod.awakened.content.regenerator.Bleed;
import com.awakenedmod.awakened.content.relic.RelicBeltItem;
import com.awakenedmod.awakened.content.relic.RelicItem;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.PowerRegistry;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import com.awakenedmod.awakened.core.relic.RelicRegistry;
import com.awakenedmod.awakened.core.skill.SkillSystem;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class AdminService {
   private AdminService() {
   }

   public static void handle(ServerPlayer player, AdminAction action, String powerId, int intArg) {
      if (player != null && action != null) {
         if (!isAllowed(player)) {
            player.displayClientMessage(Component.translatable("awakened.admin.denied"), false);
         } else {
            PowerData data = PowerService.data(player);
            switch (action) {
               case SET_POWER:
                  if (PowerRegistry.get(powerId) != null) {
                     PowerService.setPower(player, powerId);
                     QuestSystem.resetForPowerChange(player);
                  }
                  break;
               case REROLL:
                  PowerService.reroll(player);
                  return;
               case CLEAR:
                  PowerService.clearPower(player);
                  break;
               case REFILL_STAMINA:
                  data.setStamina(StaminaSystem.max(player));
                  break;
               case TOGGLE_INFINITE_STAMINA:
                  boolean enabled = !data.infiniteStamina();
                  data.setInfiniteStamina(enabled);
                  if (enabled) {
                     data.setStamina(StaminaSystem.max(player));
                  }
                  break;
               case SET_MASTERY:
                  if (data.hasPower()) {
                     data.currentMastery().set(Math.max(0, intArg), 0.0);
                  }
                  break;
               case GRANT_XP:
                  SkillSystem.award(player, (double)Math.max(1, intArg));
                  return;
               case BLEED_SELF:
                  Bleed.inflictSelf(player, Math.max(1, intArg));
                  return;
               case GRANT_SOULS:
                  ReaperAttachments.add(player, (long)Math.max(1, intArg));
                  return;
               case RESET_SOULS:
                  ReaperAttachments.set(player, 0L);
                  return;
               case HAUNT_SELF:
                  HauntControl.previewSelf(player);
                  return;
               case SPAWN_SUPPRESSOR:
                  spawnInFront(player, new SuppressorEntity(AwakenedEntities.SUPPRESSOR.get(), player.serverLevel()));
                  return;
               case SPAWN_STALKER:
                  spawnInFront(player, new StalkerEntity(AwakenedEntities.STALKER.get(), player.serverLevel()));
                  return;
               case SPAWN_ENFORCER:
                  spawnInFront(player, new EnforcerEntity(AwakenedEntities.ENFORCER.get(), player.serverLevel()));
                  return;
               case SPAWN_OVERSEER:
                  spawnInFront(player, new OverseerEntity(AwakenedEntities.OVERSEER.get(), player.serverLevel()));
                  return;
               case SPAWN_ARBITER:
                  spawnInFront(player, new ArbiterEntity(AwakenedEntities.ARBITER.get(), player.serverLevel()));
                  return;
               case SPAWN_SENTINEL:
                  spawnInFront(player, new SentinelEntity(AwakenedEntities.SENTINEL.get(), player.serverLevel()));
                  return;
               case GRANT_RELIC:
                  List<String> ids = RelicRegistry.ids();
                  if (!ids.isEmpty()) {
                     int idx = intArg >= 0 && intArg < ids.size() ? intArg : player.getRandom().nextInt(ids.size());
                     RelicItem.give(player, ids.get(idx));
                  }

                  return;
               case GRANT_BELT:
                  RelicBeltItem.give(player, List.of());
                  return;
            }

            PowerService.sync(player);
            QuestSystem.sync(player);
         }
      }
   }

   private static void spawnInFront(ServerPlayer player, Entity entity) {
      ServerLevel level = player.serverLevel();
      Vec3 look = player.getLookAngle();
      Vec3 pos = player.position().add(look.x * 4.0, 0.0, look.z * 4.0);
      entity.moveTo(pos.x, pos.y, pos.z, player.getYRot() + 180.0F, 0.0F);
      level.addFreshEntity(entity);
   }

   private static boolean isAllowed(ServerPlayer player) {
      return player.hasPermissions(2);
   }
}
