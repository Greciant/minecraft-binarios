package com.devilfruits.fruits.yomi_yomi;

import com.devilfruits.core.CoreEvents;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.fruits.yomi_yomi.frost.YomiFrost;
import com.devilfruits.registry.DFAttachments;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;

public final class SecondWind {
   public static final ResourceLocation COOLDOWN = YomiYomi.id("second_wind");
   public static final int GRACE_TICKS = 40;
   private static final Map<UUID, Long> GRACE = new HashMap<>();

   public static boolean ready(ServerPlayer player, DamageSource source) {
      return YomiYomi.vessel(player) == Vessel.SKELETON && YomiYomi.canUse(player) && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
         ? !DFAttachments.data(player).isOnCooldown(COOLDOWN, player.level().getGameTime())
         : false;
   }

   public static void rise(ServerPlayer player) {
      long now = player.level().getGameTime();
      PlayerFruitData data = DFAttachments.data(player);
      data.startCooldown(COOLDOWN, now, (int)(YomiYomi.setting("secondWindMinutes") * 1200.0));
      player.setHealth(player.getMaxHealth());
      player.clearFire();
      GRACE.put(player.getUUID(), now + 40L);
      double radius = YomiYomi.setting("secondWindRadius");
      Vec3 center = player.position().add(0.0, 0.9, 0.0);

      for (Entity e : Targeting.inRadius(player.serverLevel(), center, radius, x -> x != player && Targeting.canHurt(player, x))) {
         e.invulnerableTime = 0;
         Targeting.hurt(player, e, 4.0F, YomiFrost.HELLFIRE_COLD);
         Vec3 away = new Vec3(e.getX() - player.getX(), 0.0, e.getZ() - player.getZ());
         away = away.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : away.normalize();
         Targeting.launch(e, away.scale(1.6).add(0.0, 0.45, 0.0));
         if (e instanceof LivingEntity living) {
            YomiFrost.cold(living, player, 60, 3);
         }
      }

      ServerFx.send(player, YomiFx.SECOND_WIND, player.position(), Vec3.ZERO, (float)radius, player.getRandom().nextLong(), 40);
      player.serverLevel().playSound(null, center.x, center.y, center.z, YomiSounds.CHILL_BURST.get(), SoundSource.PLAYERS, 1.4F, 0.6F);
      player.serverLevel().playSound(null, center.x, center.y, center.z, YomiSounds.RATTLE.get(), SoundSource.PLAYERS, 1.2F, 0.8F);
      CoreEvents.syncNow(player);
   }

   public static boolean inGrace(ServerPlayer player) {
      Long until = GRACE.get(player.getUUID());
      return until != null && until > player.level().getGameTime();
   }

   public static void register(IEventBus bus) {
      bus.addListener(EventPriority.HIGHEST, e -> {
         if (e.getEntity() instanceof ServerPlayer player && ready(player, e.getSource())) {
            e.setCanceled(true);
            rise(player);
         }
      });
      bus.addListener(EventPriority.HIGH, e -> {
         if (e.getEntity() instanceof ServerPlayer player && inGrace(player) && !e.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            e.setCanceled(true);
         }
      });
   }

   private SecondWind() {
   }
}
