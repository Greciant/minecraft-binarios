package com.awakenedmod.awakened.core.power;

import com.awakenedmod.awakened.Awakened;
import com.awakenedmod.awakened.core.PowerOwnershipRegistry;
import com.awakenedmod.awakened.core.data.PowerData;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.server.level.ServerPlayer;

public final class PowerMigrations {
   private static final Map<String, String> RENAMES = new LinkedHashMap<>();
   private static final Map<String, String> TOGGLE_RENAMES = new LinkedHashMap<>();

   private PowerMigrations() {
   }

   public static void rename(String oldId, String newId) {
      RENAMES.put(oldId, newId);
   }

   public static void renameToggle(String oldKey, PassiveToggle toggle) {
      TOGGLE_RENAMES.put(PassiveToggle.varOf(oldKey), toggle.var());
   }

   public static boolean apply(ServerPlayer player, PowerData data) {
      if (RENAMES.isEmpty()) {
         return false;
      } else {
         RENAMES.forEach((from, to) -> PowerOwnershipRegistry.renameAll(player.serverLevel().getServer(), from, to));
         boolean migrated = false;

         for (Entry<String, String> rename : RENAMES.entrySet()) {
            if (data.migratePowerId(rename.getKey(), rename.getValue())) {
               Awakened.LOGGER.info("Migrated power of {} from '{}' to '{}'", new Object[]{player.getName().getString(), rename.getKey(), rename.getValue()});
               migrated = true;
            } else {
               data.moveRecords(rename.getKey(), rename.getValue());
            }
         }

         TOGGLE_RENAMES.forEach((from, to) -> {
            if (data.hasVar(from)) {
               if (!data.hasVar(to)) {
                  data.setVar(to, data.var(from, 0.0F));
               }

               data.removeVar(from);
            }
         });
         return migrated;
      }
   }
}
