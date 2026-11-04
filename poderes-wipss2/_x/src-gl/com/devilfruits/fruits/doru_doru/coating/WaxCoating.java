package com.devilfruits.fruits.doru_doru.coating;

import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.fruits.doru_doru.DoruDoru;
import com.devilfruits.fruits.doru_doru.DoruFx;
import com.devilfruits.fruits.doru_doru.DoruSounds;
import com.devilfruits.fruits.doru_doru.DoruTags;
import com.devilfruits.fruits.doru_doru.DoruTriggers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.Tags.EntityTypes;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class WaxCoating {
   public static final int MAX = 10;
   public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, "devilfruits");
   public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, "devilfruits");
   private static final ResourceLocation SLOW = DoruDoru.id("coating_slow");
   private static final ResourceLocation STUCK = DoruDoru.id("coating_encased");
   private static final ResourceLocation STUCK_JUMP = DoruDoru.id("coating_encased_jump");
   public static final long UNTIL_SHAKEN = Long.MAX_VALUE;
   private static final Map<UUID, Long> LAST_SHAKE = new HashMap<>();
   public static final Supplier<AttachmentType<WaxCoating.Data>> TYPE = ATTACHMENTS.register(
      "doru_doru/coating",
      () -> AttachmentType.builder(() -> WaxCoating.Data.NONE)
            .serialize(WaxCoating.Data.CODEC)
            .sync((holder, to) -> true, WaxCoating.Data.STREAM_CODEC)
            .build()
   );
   public static final DeferredHolder<MobEffect, MobEffect> EFFECT = EFFECTS.register(
      "doru_doru/wax_coating", () -> new MobEffect(MobEffectCategory.HARMFUL, 15919832) {
         }
   );
   public static final int SHAKE_GAP = 2;

   public static void init(IEventBus modBus) {
      ATTACHMENTS.register(modBus);
      EFFECTS.register(modBus);
   }

   public static WaxCoating.Data get(Entity e) {
      return e.hasData(TYPE) ? (WaxCoating.Data)e.getData(TYPE) : WaxCoating.Data.NONE;
   }

   private static double setting(String name) {
      return DoruDoru.setting(name);
   }

   public static boolean canCoat(@Nullable ServerPlayer owner, Entity target) {
      if (!(target instanceof LivingEntity living) || !living.isAlive() || target == owner) {
         return false;
      }

      if (target instanceof Player p && (p.isCreative() || p.isSpectator() || !DoruDoru.flag("coatPlayers"))) {
         return false;
      }

      return owner == null || Targeting.canHurt(owner, target);
   }

   public static void add(Entity target, int stacks, @Nullable ServerPlayer owner) {
      if (target.level() instanceof ServerLevel level && canCoat(owner, target)) {
         WaxCoating.Data d = get(target);
         set(
            (LivingEntity)target,
            level,
            new WaxCoating.Data(Math.min(10, d.stacks() + stacks), level.getGameTime(), d.encasedUntil(), d.immuneUntil(), 0.0F, d.shaken()),
            owner
         );
         return;
      }
   }

   public static void expose(LivingEntity target, int ticks, @Nullable ServerPlayer owner) {
      if (target.level() instanceof ServerLevel level && canCoat(owner, target)) {
         WaxCoating.Data d = get(target);
         if (d.encased(level.getGameTime())) {
            return;
         }

         Vec3 v = target.getDeltaMovement();
         double speed = Math.sqrt(v.x * v.x + v.z * v.z);
         double still = Math.max(0.0, Math.min(1.0, 1.0 - speed / 0.2));
         double perStack = setting("coatingSecondsMoving") * 20.0 * (1.0 - still) + setting("coatingSecondsStill") * 20.0 * still;
         float progress = d.progress() + (float)((double)ticks / Math.max(1.0, perStack));

         int stacks;
         for (stacks = d.stacks(); progress >= 1.0F && stacks < 10; stacks++) {
            progress--;
         }

         set(target, level, new WaxCoating.Data(stacks, level.getGameTime(), d.encasedUntil(), d.immuneUntil(), progress, d.shaken()), owner);
         return;
      }
   }

   private static void set(LivingEntity target, ServerLevel level, WaxCoating.Data d, @Nullable ServerPlayer owner) {
      long now = level.getGameTime();
      WaxCoating.Data before = get(target);
      if (d.stacks() >= 10 && !d.encased(now) && d.immuneUntil() <= now) {
         boolean boss = target.getType().is(EntityTypes.BOSSES);
         int ticks = (int)(setting(boss ? "bossEncaseSeconds" : "encaseSeconds") * 20.0);
         d = target instanceof Player
            ? new WaxCoating.Data(10, now, Long.MAX_VALUE, 0L, 0.0F, 0.0F)
            : new WaxCoating.Data(10, now, now + (long)ticks, now + (long)ticks + (long)((int)(setting("encaseImmunitySeconds") * 20.0)), 0.0F, 0.0F);
         level.playSound(null, target.getX(), target.getY(), target.getZ(), DoruSounds.ENCASE.get(), SoundSource.PLAYERS, 1.0F, 0.9F);
         ServerFx.sendNear(level, DoruFx.ENCASE, target.getId(), target.position(), Vec3.ZERO, (float)ticks, 0L, 0, 64.0);
         if (owner != null) {
            DoruTriggers.ENCASE.get().trigger(owner);
         }
      } else if (d.stacks() > before.stacks()) {
         level.playSound(null, target.getX(), target.getY(), target.getZ(), DoruSounds.COAT.get(), SoundSource.PLAYERS, 0.6F, 0.8F + (float)d.stacks() * 0.06F);
      }

      target.setData(TYPE, d);
      apply(target, d, now);
   }

   static void apply(LivingEntity target, WaxCoating.Data d, long now) {
      double slow = (double)d.stacks() * setting("coatingSlowPerStack");
      modifier(target, Attributes.MOVEMENT_SPEED, SLOW, slow > 0.0 ? -Math.min(0.9, slow) : 0.0);
      boolean stuck = d.encased(now);
      modifier(target, Attributes.MOVEMENT_SPEED, STUCK, stuck ? -1.0 : 0.0);
      modifier(target, Attributes.JUMP_STRENGTH, STUCK_JUMP, stuck ? -1.0 : 0.0);
      if (d.stacks() > 0) {
         int duration = target instanceof Player ? -1 : 100;
         MobEffectInstance cur = target.getEffect(EFFECT);
         if (cur != null && cur.getAmplifier() != d.stacks() - 1) {
            target.removeEffect(EFFECT);
            cur = null;
         }

         if (cur == null || cur.isInfiniteDuration() != duration < 0 || duration > 0 && cur.getDuration() < 40) {
            target.addEffect(new MobEffectInstance(EFFECT, duration, d.stacks() - 1, false, false, true));
         }
      } else {
         target.removeEffect(EFFECT);
      }
   }

   private static void modifier(LivingEntity e, Holder<Attribute> attribute, ResourceLocation id, double amount) {
      AttributeInstance inst = e.getAttribute(attribute);
      if (inst != null) {
         if (amount == 0.0) {
            inst.removeModifier(id);
         } else {
            inst.addOrUpdateTransientModifier(new AttributeModifier(id, amount, Operation.ADD_MULTIPLIED_TOTAL));
         }
      }
   }

   public static void tick(LivingEntity e) {
      if (e.level() instanceof ServerLevel level) {
         WaxCoating.Data var10 = get(e);
         long now = level.getGameTime();
         if (var10.encased(now)) {
            e.setDeltaMovement(0.0, Math.min(0.0, e.getDeltaMovement().y), 0.0);
         }

         if (now % 5L == 0L) {
            if (var10.encasedUntil() != 0L && var10.encasedUntil() <= now) {
               breakFree(e, level, var10.immuneUntil());
            } else if (var10.stacks() != 0
               || var10.encased(now)
               || var10.immuneUntil() > now
               || !(var10.progress() <= 0.0F) && !((double)(now - var10.lastGain()) > setting("coatingDecayDelaySeconds") * 20.0)) {
               long idle = now - var10.lastGain();
               long every = Math.max(5L, (long)(setting("coatingDecaySeconds") * 20.0));
               if (!(e instanceof Player)
                  && !var10.encased(now)
                  && var10.stacks() > 0
                  && (double)idle > setting("coatingDecayDelaySeconds") * 20.0
                  && idle % every < 5L) {
                  WaxCoating.Data out = new WaxCoating.Data(var10.stacks() - 1, var10.lastGain(), var10.encasedUntil(), var10.immuneUntil(), 0.0F, 0.0F);
                  e.setData(TYPE, out);
                  apply(e, out, now);
               } else {
                  apply(e, var10, now);
               }
            } else {
               e.removeData(TYPE);
               apply(e, WaxCoating.Data.NONE, now);
            }
         }
      }
   }

   public static void onDamage(LivingEntity e, DamageSource source) {
      if (e.level() instanceof ServerLevel level && (get(e).stacks() != 0 || get(e).encased(level.getGameTime()))) {
         if (source.is(DoruTags.HEAT_DAMAGE)) {
            strip(e, level);
         }

         return;
      }
   }

   public static void strip(LivingEntity e, ServerLevel level) {
      WaxCoating.Data d = get(e);
      if (d.stacks() != 0 || d.encased(level.getGameTime())) {
         long now = level.getGameTime();
         WaxCoating.Data out = new WaxCoating.Data(0, now, 0L, d.encasedUntil() == Long.MAX_VALUE ? immunityFrom(now) : d.immuneUntil(), 0.0F, 0.0F);
         e.setData(TYPE, out);
         apply(e, out, now);
         ServerFx.sendNear(level, DoruFx.MELT_OFF, e.getId(), e.position(), Vec3.ZERO, 1.0F, 0L, 0, 64.0);
         level.playSound(null, e.getX(), e.getY(), e.getZ(), DoruSounds.MELT.get(), SoundSource.PLAYERS, 0.8F, 1.1F);
      }
   }

   private static void breakFree(LivingEntity e, ServerLevel level, long immuneUntil) {
      long now = level.getGameTime();
      WaxCoating.Data out = new WaxCoating.Data(0, now, 0L, immuneUntil, 0.0F, 0.0F);
      e.setData(TYPE, out);
      apply(e, out, now);
      ServerFx.sendNear(level, DoruFx.BREAK_FREE, e.getId(), e.position(), Vec3.ZERO, 1.0F, 0L, 0, 64.0);
      level.playSound(null, e.getX(), e.getY(), e.getZ(), DoruSounds.BREAK_FREE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
   }

   private static long immunityFrom(long now) {
      return now + (long)(setting("encaseImmunitySeconds") * 20.0);
   }

   public static void struggle(ServerPlayer player) {
      if (get(player).encased(player.level().getGameTime())) {
         shake(player);
      }
   }

   public static void shake(ServerPlayer player) {
      WaxCoating.Data d = get(player);
      ServerLevel level = player.serverLevel();
      long now = level.getGameTime();
      boolean encased = d.encased(now);
      if (d.stacks() > 0 || encased) {
         Long last = LAST_SHAKE.get(player.getUUID());
         if (last == null || now - last >= 2L || now < last) {
            LAST_SHAKE.put(player.getUUID(), now);
            float shaken = d.shaken() + 1.0F;
            ServerFx.sendNear(level, DoruFx.STRUGGLE, player.getId(), player.position(), Vec3.ZERO, 1.0F, 0L, 0, 32.0);
            if (encased) {
               if ((double)shaken >= setting("encaseShakes")) {
                  breakFree(player, level, immunityFrom(now));
               } else {
                  player.setData(TYPE, new WaxCoating.Data(d.stacks(), d.lastGain(), d.encasedUntil(), d.immuneUntil(), d.progress(), shaken));
               }
            } else {
               WaxCoating.Data out = (double)shaken >= setting("coatingShakesPerStack")
                  ? new WaxCoating.Data(d.stacks() - 1, d.lastGain(), d.encasedUntil(), d.immuneUntil(), d.progress(), 0.0F)
                  : new WaxCoating.Data(d.stacks(), d.lastGain(), d.encasedUntil(), d.immuneUntil(), d.progress(), shaken);
               player.setData(TYPE, out);
               if (out.stacks() < d.stacks()) {
                  level.playSound(
                     null, player.getX(), player.getY(), player.getZ(), DoruSounds.COAT.get(), SoundSource.PLAYERS, 0.5F, 1.3F + (float)out.stacks() * 0.03F
                  );
                  apply(player, out, now);
               }
            }
         }
      }
   }

   public static void forget(UUID player) {
      LAST_SHAKE.remove(player);
   }

   private WaxCoating() {
   }

   public static record Data(int stacks, long lastGain, long encasedUntil, long immuneUntil, float progress, float shaken) {
      public static final WaxCoating.Data NONE = new WaxCoating.Data(0, 0L, 0L, 0L, 0.0F, 0.0F);
      public static final Codec<WaxCoating.Data> CODEC = RecordCodecBuilder.create(
         i -> i.group(
                  Codec.INT.fieldOf("stacks").forGetter(WaxCoating.Data::stacks),
                  Codec.LONG.fieldOf("last").forGetter(WaxCoating.Data::lastGain),
                  Codec.LONG.fieldOf("encased").forGetter(WaxCoating.Data::encasedUntil),
                  Codec.LONG.fieldOf("immune").forGetter(WaxCoating.Data::immuneUntil),
                  Codec.FLOAT.fieldOf("progress").forGetter(WaxCoating.Data::progress),
                  Codec.FLOAT.optionalFieldOf("shaken", 0.0F).forGetter(WaxCoating.Data::shaken)
               )
               .apply(i, WaxCoating.Data::new)
      );
      public static final StreamCodec<ByteBuf, WaxCoating.Data> STREAM_CODEC = StreamCodec.composite(
         ByteBufCodecs.VAR_INT,
         WaxCoating.Data::stacks,
         ByteBufCodecs.VAR_LONG,
         WaxCoating.Data::lastGain,
         ByteBufCodecs.VAR_LONG,
         WaxCoating.Data::encasedUntil,
         ByteBufCodecs.VAR_LONG,
         WaxCoating.Data::immuneUntil,
         ByteBufCodecs.FLOAT,
         WaxCoating.Data::progress,
         ByteBufCodecs.FLOAT,
         WaxCoating.Data::shaken,
         WaxCoating.Data::new
      );

      public boolean encased(long now) {
         return this.encasedUntil > now;
      }
   }
}
