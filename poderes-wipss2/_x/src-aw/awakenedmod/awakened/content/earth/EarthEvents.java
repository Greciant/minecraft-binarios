package com.awakenedmod.awakened.content.earth;

import com.awakenedmod.awakened.core.team.EntityOwners;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public final class EarthEvents {
   private EarthEvents() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(EarthEvents::onIncomingDamage);
      EntityOwners.register("earth", e -> {
         if (e instanceof StoneMechEntity mech) {
            ServerPlayer pilot = StoneMechControl.ownerOf(mech);
            return pilot == null ? null : pilot.getUUID();
         } else {
            return e instanceof HoverRockEntity rock ? rock.ownerId() : null;
         }
      });
   }

   private static void onIncomingDamage(LivingIncomingDamageEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         StoneMechEntity mech = StoneMechControl.mechOf(player);
         if (mech != null) {
            DamageSource source = event.getSource();
            if (!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
               ServerLevel level = player.serverLevel();
               StoneMechEntity.absorbAsStamina(level, player, mech, source, event.getAmount(), mech.position());
               event.setCanceled(true);
            }
         }
      }
   }
}
