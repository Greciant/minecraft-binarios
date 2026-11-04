package com.devilfruits.fruits.suke_suke;

import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.fruits.suke_suke.clear.Clear;
import com.devilfruits.fruits.suke_suke.clear.Cloak;
import com.devilfruits.fruits.suke_suke.clear.ShipCloak;
import com.devilfruits.fruits.suke_suke.clear.Steps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.PlayLevelSoundEvent.AtPosition;
import net.neoforged.neoforge.event.entity.living.LivingEvent.LivingVisibilityEvent;

public final class SukeEvents {
   public static void register(IEventBus bus) {
      bus.addListener(SukeEvents::visibility);
      bus.addListener(SukeEvents::steps);
      bus.addListener(e -> {
         if (e.getEntity() instanceof ServerPlayer p && Clear.invisible(p) && SukeSuke.flag("breakOnAttack")) {
            endFor(p);
         }
      });
      bus.addListener(e -> {
         if (e.getEntity() instanceof ServerPlayer p) {
            endFor(p);
         }
      });
      bus.addListener(e -> {
         if (e.getEntity() instanceof ServerPlayer p) {
            endFor(p);
         }
      });
      bus.addListener(e -> {
         if (e.getEntity() instanceof ServerPlayer p) {
            endFor(p);
            ShipCloak.syncTo(p);
         }
      });
      bus.addListener(e -> {
         if (e.getEntity() instanceof ServerPlayer p) {
            ShipCloak.syncTo(p);
         }
      });
      bus.addListener(e -> {
         if (e.getEntity() instanceof ServerPlayer p) {
            ShipCloak.syncTo(p);
         }
      });
      bus.addListener(e -> ShipCloak.tick(e.getServer()));
      bus.addListener(e -> {
         Cloak.clear();
         ShipCloak.clear();
      });
   }

   private static void endFor(ServerPlayer p) {
      if (SukeSuke.isUser(p)) {
         AbilityManager.stopToggle(p, SukeAbilities.INVISIBILITY);
         AbilityManager.stopToggle(p, SukeAbilities.GROUP_INVISIBILITY);
      }

      Cloak.end(p);
      Cloak.leave(p);
   }

   private static void visibility(LivingVisibilityEvent e) {
      if (Clear.invisible(e.getEntity())) {
         Entity looker = e.getLookingEntity();
         if (looker == null || !Clear.sees(looker, e.getEntity(), false)) {
            double factor = SukeSuke.setting("mobVisibility");
            double now = e.getVisibilityModifier();
            if (now > factor && now > 0.0) {
               e.modifyVisibility(factor / now);
            }
         }
      }
   }

   private static void steps(AtPosition e) {
      Entity stepping = Steps.current();
      if (stepping != null && Clear.invisible(stepping)) {
         e.setNewVolume(e.getNewVolume() * (float)SukeSuke.setting("muffleSteps"));
      }
   }

   private SukeEvents() {
   }
}
