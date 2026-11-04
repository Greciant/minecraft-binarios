package com.awakenedmod.awakened.core.rules;

import com.awakenedmod.awakened.core.AwakenedConfigs;
import com.awakenedmod.awakened.core.PowerSuppression;
import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.npc.PoweredNpc;
import com.awakenedmod.awakened.core.power.Power;
import com.awakenedmod.awakened.core.power.PowerRegistry;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

final class PowerAvailability {
   private PowerAvailability() {
   }

   static void register() {
      PowerSuppression.register(PowerAvailability::holdsDisabledPower, "awakened.rules.power_disabled");
      PowerSuppression.register(PowerAvailability::inDisabledDimension, "awakened.rules.dimension_disabled");
      AwakenedConfigs.choices("powers.disabled", (Function<CommandSourceStack, Stream<String>>)(source -> PowerRegistry.all().stream().map(Power::id)));
      AwakenedConfigs.choices(
         "powers.disabledDimensions",
         (Function<CommandSourceStack, Stream<String>>)(source -> source.getServer().levelKeys().stream().map(PowerAvailability::id))
      );
   }

   private static boolean holdsDisabledPower(LivingEntity body) {
      if (body instanceof PoweredNpc npc) {
         return ServerRules.powerDisabled(npc.kit().powerId());
      } else {
         if (body instanceof Player player && ServerRules.powerDisabled(AwakenedAttachments.get(player).powerId())) {
            return true;
         }

         return false;
      }
   }

   private static boolean inDisabledDimension(LivingEntity body) {
      return ServerRules.anyDimensionDisabled()
         && (body instanceof Player || body instanceof PoweredNpc)
         && ServerRules.dimensionDisabled(id(body.level().dimension()));
   }

   private static String id(ResourceKey<Level> dimension) {
      return dimension.location().toString();
   }
}
