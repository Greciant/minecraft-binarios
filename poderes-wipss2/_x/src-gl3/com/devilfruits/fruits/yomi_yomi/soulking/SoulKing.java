package com.devilfruits.fruits.yomi_yomi.soulking;

import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.form.FormManager;
import com.devilfruits.core.form.FruitForm;
import com.devilfruits.core.util.ServerScheduler;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.fruits.yomi_yomi.YomiItems;
import com.devilfruits.fruits.yomi_yomi.YomiSounds;
import com.devilfruits.fruits.yomi_yomi.YomiYomi;
import com.devilfruits.registry.DFAttachments;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Post;

public final class SoulKing {
   private static final int AURA_EVERY = 10;
   private static final Map<UUID, Long> RISEN_AT = new HashMap<>();

   public static void register(IEventBus bus) {
      FormManager.onSwitch(SoulKing::onSwitch);
      bus.addListener(Post.class, SoulKing::onTick);
      bus.addListener(PlayerLoggedOutEvent.class, e -> RISEN_AT.remove(e.getEntity().getUUID()));
   }

   public static boolean active(Player player) {
      return YomiYomi.isUser(player) && "soul_king".equals(DFAttachments.data(player).formName());
   }

   public static boolean ready(ServerPlayer player) {
      Long since = RISEN_AT.get(player.getUUID());
      return active(player) && (since == null || player.level().getGameTime() - since >= 40L);
   }

   public static boolean holdsGuitar(Player player) {
      return player.getMainHandItem().is((Item)YomiItems.SOUL_GUITAR.get());
   }

   private static void onSwitch(ServerPlayer player, FruitForm from, FruitForm to, FormManager.Reason reason) {
      if (to.fruit() == YomiYomi.fruit()) {
         if ("soul_king".equals(to.name())) {
            rise(player);
         } else if ("soul_king".equals(from.name())) {
            end(player, from, reason);
         }
      }
   }

   private static void rise(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      RISEN_AT.put(player.getUUID(), level.getGameTime());
      AbilityManager.setBusy(player, 40);
      ServerScheduler.schedule(level, 0, age -> {
         if (player.isAlive() && !player.isRemoved() && player.level() == level && active(player)) {
            if (age == 26) {
               level.playSound(null, player.getX(), player.getY() + 3.0, player.getZ(), YomiSounds.KING_ROAR.get(), SoundSource.PLAYERS, 2.0F, 1.0F);
            }

            return age >= 26;
         } else {
            return true;
         }
      });
   }

   private static void end(ServerPlayer player, FruitForm form, FormManager.Reason reason) {
      RISEN_AT.remove(player.getUUID());
      PlayerFruitData data = DFAttachments.data(player);
      data.startCooldown(PlayerFruitData.FORM_COOLDOWN, player.level().getGameTime(), form.config().switchCooldownTicks());
      if (reason != FormManager.Reason.DEATH && reason != FormManager.Reason.LOGIN && reason != FormManager.Reason.POWER_LOST) {
         player.level().playSound(null, player.getX(), player.getY(), player.getZ(), YomiSounds.KING_VANISH.get(), SoundSource.PLAYERS, 1.5F, 1.0F);
      }
   }

   private static void onTick(Post event) {
      if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 10 == 0 && ready(player) && YomiYomi.canUse(player)) {
         aura(player);
      }
   }

   public static void aura(ServerPlayer player) {
      double radius = YomiYomi.setting("kingAuraRadius");
      Vec3 center = player.position().add(0.0, (double)player.getBbHeight() * 0.5, 0.0);

      for (Entity e : Targeting.inRadius(player.serverLevel(), center, radius, x -> x != player && Targeting.canHurt(player, x))) {
         if (e instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 16, 0, true, false, true), player);
         }
      }
   }

   private SoulKing() {
   }
}
