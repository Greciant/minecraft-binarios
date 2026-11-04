package com.devilfruits.mobs.pirate;

import com.devilfruits.core.npc.Faction;
import com.devilfruits.core.npc.FactionFighter;
import com.devilfruits.core.poneglyph.PoneglyphApiEvents;
import com.devilfruits.core.poneglyph.PoneglyphConfig;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(
   modid = "devilfruits"
)
public final class PirateAmbush {
   @SubscribeEvent
   public static void onRubbed(PoneglyphApiEvents.PoneglyphRubbedEvent event) {
      if ((Boolean)PoneglyphConfig.RUB_NOISE_AMBUSH.get() && !event.getPlayer().isCreative()) {
         double r = (double)((Integer)PoneglyphConfig.AMBUSH_RADIUS.get()).intValue();

         for (FactionFighter pirate : event.getPlayer()
            .level()
            .getEntitiesOfClass(FactionFighter.class, new AABB(event.getPos()).inflate(r), f -> f.faction() == Faction.PIRATE && f.getTarget() == null)) {
            pirate.setTarget(event.getPlayer());
         }
      }
   }

   private PirateAmbush() {
   }
}
