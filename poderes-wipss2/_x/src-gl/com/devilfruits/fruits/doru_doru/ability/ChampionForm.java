package com.devilfruits.fruits.doru_doru.ability;

import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.form.FormManager;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.EntityMass;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.fruits.doru_doru.DoruDoru;
import com.devilfruits.fruits.doru_doru.DoruFx;
import com.devilfruits.fruits.doru_doru.DoruSounds;
import com.devilfruits.fruits.doru_doru.DoruTags;
import com.devilfruits.fruits.doru_doru.DoruTriggers;
import com.devilfruits.fruits.doru_doru.wax.WaxHeatService;
import com.devilfruits.registry.DFAttachments;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public final class ChampionForm {
   private static final Set<UUID> SUITED = new HashSet<>();
   private static boolean splashing;

   public static boolean in(Player player) {
      if (!player.hasData(DFAttachments.PLAYER_DATA)) {
         return false;
      } else {
         PlayerFruitData data = DFAttachments.data(player);
         return DoruDoru.ID.equals(data.fruitId()) && "champion".equals(data.formName());
      }
   }

   public static void init() {
      EntityMass.register(e -> {
         if (e instanceof Player p && in(p)) {
            return 250.0;
         }

         return Double.NaN;
      });
   }

   public static void tick(ServerPlayer player) {
      boolean in = in(player);
      boolean was = SUITED.contains(player.getUUID());
      if (in && !was) {
         SUITED.add(player.getUUID());
         player.setAbsorptionAmount((float)DoruDoru.setting("championIntegrity"));
         DoruTriggers.CHAMPION.get().trigger(player);
      } else if (!in) {
         if (was) {
            SUITED.remove(player.getUUID());
            player.setAbsorptionAmount(0.0F);
         }
      } else if (player.getAbsorptionAmount() <= 0.01F) {
         player.displayClientMessage(Component.translatable("message.devilfruits.doru_doru.suit_melted").withStyle(ChatFormatting.GOLD), true);
         FormManager.revert(player, FormManager.Reason.DEPLETED);
      } else {
         ServerLevel level = player.serverLevel();
         if (player.tickCount % 10 == 0) {
            float heat = WaxHeatService.heatAt(level, player.blockPosition()) + (player.isOnFire() ? 1.0F : 0.0F) + (player.isInLava() ? 3.0F : 0.0F);
            if (heat > 0.05F) {
               player.setAbsorptionAmount(Math.max(0.0F, player.getAbsorptionAmount() - heat * 1.5F));
               ServerFx.send(player, DoruFx.HEAT_WARNING, player.position(), Vec3.ZERO, 1.0F, 0L, 0);
            }
         }

         if (player.onGround() && player.getDeltaMovement().horizontalDistanceSqr() > 0.004 && player.tickCount % 9 == 0) {
            level.playSound(
               null,
               player.getX(),
               player.getY(),
               player.getZ(),
               DoruSounds.STEP.get(),
               SoundSource.PLAYERS,
               0.7F,
               0.8F + player.getRandom().nextFloat() * 0.2F
            );
         }
      }
   }

   public static void onDamage(LivingIncomingDamageEvent event) {
      LivingEntity target = event.getEntity();
      if (target instanceof ServerPlayer p && in(p) && event.getSource().is(DoruTags.HEAT_DAMAGE)) {
         event.setAmount(event.getAmount() * 3.0F);
      }

      if (!splashing
         && event.getSource().getEntity() instanceof ServerPlayer p
         && in(p)
         && event.getSource().is(DamageTypes.PLAYER_ATTACK)
         && event.getSource().getDirectEntity() == p) {
         ServerLevel level = p.serverLevel();
         Vec3 at = target.position().add(0.0, (double)target.getBbHeight() * 0.55, 0.0);
         Vec3 push = new Vec3(target.getX() - p.getX(), 0.0, target.getZ() - p.getZ());
         push = push.lengthSqr() < 1.0E-4 ? p.getLookAngle().multiply(1.0, 0.0, 1.0) : push.normalize();
         float splash = event.getAmount() * 0.6F;
         splashing = true;

         try {
            for (LivingEntity other : level.getEntitiesOfClass(
               LivingEntity.class, target.getBoundingBox().inflate(3.0, 1.5, 3.0), e -> e != p && e != target && e.isAlive() && Targeting.canHurt(p, e)
            )) {
               if (!(other.distanceTo(target) > 3.0F)) {
                  Targeting.hurt(p, other, splash, DoruDoru.WAX_DAMAGE);
                  Vec3 away = new Vec3(other.getX() - p.getX(), 0.0, other.getZ() - p.getZ());
                  Targeting.launch(other, (away.lengthSqr() < 1.0E-4 ? push : away.normalize()).scale(0.7).add(0.0, 0.3, 0.0));
               }
            }
         } finally {
            splashing = false;
         }

         Targeting.launch(target, push.scale(0.5).add(0.0, 0.2, 0.0));
         level.playSound(null, at.x, at.y, at.z, DoruSounds.PUNCH.get(), SoundSource.PLAYERS, 1.0F, 0.8F + p.getRandom().nextFloat() * 0.2F);
         ServerFx.sendNear(level, DoruFx.CHAMPION_PUNCH, target.getId(), at, push, 1.0F, p.getRandom().nextLong(), 0, 64.0);
      }
   }

   public static void onFall(LivingFallEvent event) {
      if (event.getEntity() instanceof ServerPlayer p && in(p)) {
         float dist = event.getDistance();
         event.setDamageMultiplier(0.0F);
         if (dist < 3.0F) {
            return;
         }

         ServerLevel level = p.serverLevel();
         double r = Math.min(6.0, 2.0 + (double)dist * 0.3);
         float dmg = Math.min(16.0F, 4.0F + dist * 0.8F);
         splashing = true;

         try {
            for (LivingEntity t : level.getEntitiesOfClass(
               LivingEntity.class, p.getBoundingBox().inflate(r, 1.5, r), e -> e != p && e.isAlive() && Targeting.canHurt(p, e)
            )) {
               double d = (double)t.distanceTo(p);
               if (!(d > r)) {
                  Targeting.hurt(p, t, dmg * (float)(1.0 - 0.5 * d / r), DoruDoru.WAX_DAMAGE);
                  Vec3 out = new Vec3(t.getX() - p.getX(), 0.0, t.getZ() - p.getZ());
                  out = out.lengthSqr() < 1.0E-4 ? Vec3.ZERO : out.normalize();
                  Targeting.launch(t, out.scale(0.3 + 0.8 * (1.0 - d / r)).add(0.0, 0.45, 0.0));
               }
            }
         } finally {
            splashing = false;
         }

         level.playSound(null, p.getX(), p.getY(), p.getZ(), DoruSounds.STOMP.get(), SoundSource.PLAYERS, 1.4F, 0.8F);
         ServerFx.send(p, DoruFx.CHAMPION_STOMP, p.position(), Vec3.ZERO, (float)r, p.getRandom().nextLong(), 0);
         ServerFx.animate(p, DoruDoru.id("champion/stomp"));
         return;
      }
   }

   public static void forget(UUID player) {
      SUITED.remove(player);
   }

   private ChampionForm() {
   }
}
