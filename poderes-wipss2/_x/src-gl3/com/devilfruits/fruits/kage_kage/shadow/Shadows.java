package com.devilfruits.fruits.kage_kage.shadow;

import com.devilfruits.core.util.Targeting;
import com.devilfruits.fruits.kage_kage.KageKage;
import com.devilfruits.fruits.kage_kage.KageSounds;
import com.devilfruits.fruits.kage_kage.KageTags;
import com.devilfruits.fruits.kage_kage.KageTriggers;
import com.devilfruits.registry.DFAttachments;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.Tags.EntityTypes;

public final class Shadows {
   public static final Supplier<AttachmentType<Shadows.Held>> HELD = DFAttachments.ATTACHMENTS
      .register(
         "kage_kage/held_shadows",
         () -> AttachmentType.builder(() -> Shadows.Held.NONE)
               .serialize(Shadows.Held.CODEC)
               .sync((holder, to) -> holder == to, Shadows.Held.STREAM_CODEC)
               .build()
      );
   public static final Supplier<AttachmentType<Boolean>> SHADOWLESS = DFAttachments.ATTACHMENTS
      .register(
         "kage_kage/shadowless", () -> AttachmentType.builder(() -> Boolean.FALSE).serialize(Codec.BOOL).sync((holder, to) -> true, ByteBufCodecs.BOOL).build()
      );
   private static final ResourceLocation HEALTH_BONUS = KageKage.id("shadows_health");
   private static final ResourceLocation SPEED_BONUS = KageKage.id("shadows_speed");
   private static final List<Consumer<ServerPlayer>> RELEASE_HOOKS = new ArrayList<>();

   public static void init() {
   }

   public static void onRelease(Consumer<ServerPlayer> hook) {
      RELEASE_HOOKS.add(hook);
   }

   public static List<StolenShadow> held(Player user) {
      return user.hasData(HELD) ? ((Shadows.Held)user.getData(HELD)).shadows() : List.of();
   }

   public static int count(Player user) {
      return held(user).size();
   }

   public static boolean isShadowless(Entity e) {
      return e.hasData(SHADOWLESS) && (Boolean)e.getData(SHADOWLESS);
   }

   @Nullable
   public static Component refusal(ServerPlayer user, Entity target) {
      if (!(target instanceof LivingEntity living) || !living.isAlive() || target == user) {
         return msg("no_target");
      }

      if (target.getType().is(KageTags.NO_SHADOW)) {
         return msg("no_shadow");
      } else if (isShadowless(target)) {
         return msg("already_shadowless");
      } else if (target.getType().is(EntityTypes.BOSSES) && !KageKage.flag("takeFromBosses")) {
         return msg("too_strong");
      } else {
         if (target instanceof Player p && (p.isCreative() || p.isSpectator() || !KageKage.flag("takeFromPlayers"))) {
            return msg("no_target");
         }

         if (!Targeting.canHurt(user, target)) {
            return msg("no_target");
         } else {
            return count(user) >= (int)KageKage.setting("maxShadows") ? msg("full") : null;
         }
      }
   }

   @Nullable
   public static StolenShadow take(ServerPlayer user, LivingEntity target) {
      if (refusal(user, target) != null) {
         return null;
      } else {
         float attack = target.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE) ? (float)target.getAttributeValue(Attributes.ATTACK_DAMAGE) : 1.0F;
         StolenShadow shadow = new StolenShadow(
            target.getUUID(), BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()), target.getMaxHealth(), attack, target.getName().getString()
         );
         List<StolenShadow> list = new ArrayList<>(held(user));
         list.add(shadow);
         user.setData(HELD, new Shadows.Held(List.copyOf(list)));
         ShadowVictims.get(user.server).put(target.getUUID(), user.getUUID());
         target.setData(SHADOWLESS, Boolean.TRUE);
         applyBonuses(user);
         KageTriggers.SHADOW_TAKEN.get().trigger(user);
         return shadow;
      }
   }

   @Nullable
   public static StolenShadow spendStrongest(ServerPlayer user) {
      List<StolenShadow> list = new ArrayList<>(held(user));
      if (list.isEmpty()) {
         return null;
      } else {
         StolenShadow best = list.stream().max(Comparator.comparingDouble(StolenShadow::strength)).orElseThrow();
         list.remove(best);
         user.setData(HELD, new Shadows.Held(List.copyOf(list)));
         applyBonuses(user);
         return best;
      }
   }

   public static void releaseAll(ServerPlayer user, Shadows.Release why) {
      MinecraftServer server = user.server;
      List<UUID> victims = ShadowVictims.get(server).releaseAll(user.getUUID());

      for (UUID id : victims) {
         Entity e = find(server, id);
         if (e != null) {
            restore(e);
         }
      }

      boolean had = !held(user).isEmpty() || !victims.isEmpty();
      user.setData(HELD, Shadows.Held.NONE);
      applyBonuses(user);
      RELEASE_HOOKS.forEach(h -> h.accept(user));
      if (had) {
         user.displayClientMessage(msg(why == Shadows.Release.DEATH ? "returned_death" : "returned"), false);
      }
   }

   public static void returnHome(MinecraftServer server, UUID victim) {
      ShadowVictims.get(server).remove(victim);
      Entity e = find(server, victim);
      if (e != null && isShadowless(e)) {
         restore(e);
      }
   }

   private static void restore(Entity e) {
      e.removeData(SHADOWLESS);
      e.level().playSound(null, e.getX(), e.getY(), e.getZ(), KageSounds.SHADOW_RETURN.get(), SoundSource.NEUTRAL, 0.8F, 1.0F);
   }

   @Nullable
   private static Entity find(MinecraftServer server, UUID id) {
      ServerPlayer player = server.getPlayerList().getPlayer(id);
      if (player != null) {
         return player;
      } else {
         for (ServerLevel level : server.getAllLevels()) {
            Entity e = level.getEntity(id);
            if (e != null) {
               return e;
            }
         }

         return null;
      }
   }

   public static void onJoin(Entity e) {
      if (e instanceof LivingEntity && e.level() instanceof ServerLevel level) {
         boolean victim = ShadowVictims.get(level.getServer()).isVictim(e.getUUID());
         if (victim && !isShadowless(e)) {
            e.setData(SHADOWLESS, Boolean.TRUE);
         } else if (!victim && e.hasData(SHADOWLESS)) {
            e.removeData(SHADOWLESS);
         }
      }
   }

   public static void onVictimDeath(LivingEntity e) {
      if (!(e instanceof Player) && e.level() instanceof ServerLevel level && isShadowless(e)) {
         ShadowVictims.get(level.getServer()).remove(e.getUUID());
      }
   }

   public static void applyBonuses(ServerPlayer user) {
      int n = KageKage.isUser(user) ? count(user) : 0;
      AttributeInstance health = user.getAttribute(Attributes.MAX_HEALTH);
      if (health != null) {
         if (n > 0) {
            health.addOrUpdateTransientModifier(new AttributeModifier(HEALTH_BONUS, (double)n * KageKage.setting("shadowHealthBonus"), Operation.ADD_VALUE));
         } else {
            health.removeModifier(HEALTH_BONUS);
         }

         if (user.getHealth() > user.getMaxHealth()) {
            user.setHealth(user.getMaxHealth());
         }
      }

      AttributeInstance speed = user.getAttribute(Attributes.MOVEMENT_SPEED);
      if (speed != null) {
         double bonus = Math.min((double)n * KageKage.setting("shadowSpeedBonus"), KageKage.setting("shadowSpeedCap"));
         if (bonus > 0.0) {
            speed.addOrUpdateTransientModifier(new AttributeModifier(SPEED_BONUS, bonus, Operation.ADD_MULTIPLIED_BASE));
         } else {
            speed.removeModifier(SPEED_BONUS);
         }
      }
   }

   public static float damageMultiplier(Player user) {
      return 1.0F + (float)count(user) * (float)KageKage.setting("shadowDamageBonus");
   }

   private static Component msg(String key) {
      return Component.translatable("message.devilfruits.kage_kage.shadow." + key);
   }

   private Shadows() {
   }

   public static record Held(List<StolenShadow> shadows) {
      public static final Shadows.Held NONE = new Shadows.Held(List.of());
      public static final Codec<Shadows.Held> CODEC = StolenShadow.CODEC.listOf().xmap(Shadows.Held::new, Shadows.Held::shadows);
      public static final StreamCodec<ByteBuf, Shadows.Held> STREAM_CODEC = StolenShadow.STREAM_CODEC
         .apply(ByteBufCodecs.list())
         .map(Shadows.Held::new, Shadows.Held::shadows);
   }

   public static enum Release {
      DEATH,
      POWER_LOST,
      COMMAND;
   }
}
