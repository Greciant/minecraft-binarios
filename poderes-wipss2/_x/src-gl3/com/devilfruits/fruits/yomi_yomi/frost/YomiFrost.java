package com.devilfruits.fruits.yomi_yomi.frost;

import com.devilfruits.core.combat.Stun;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.fruits.yomi_yomi.YomiFx;
import com.devilfruits.fruits.yomi_yomi.YomiSounds;
import com.devilfruits.fruits.yomi_yomi.YomiYomi;
import com.devilfruits.registry.DFDamageTypes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class YomiFrost {
   public static final int MAX_CHILL = 4;
   public static final int CHILL_HOLD = 80;
   public static final int CHILL_MELT = 30;
   public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, "devilfruits");
   public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, "devilfruits");
   public static final ResourceKey<DamageType> HELLFIRE_COLD = DFDamageTypes.key("yomi_yomi/hellfire_cold");
   public static final Supplier<AttachmentType<YomiFrost.Data>> TYPE = ATTACHMENTS.register(
      "yomi_yomi/frost",
      () -> AttachmentType.builder(() -> YomiFrost.Data.NONE).serialize(YomiFrost.Data.CODEC).sync((holder, to) -> true, YomiFrost.Data.STREAM_CODEC).build()
   );
   public static final DeferredHolder<MobEffect, MobEffect> FROSTBITE = EFFECTS.register(
      "yomi_yomi/frostbite",
      () -> (new MobEffect(MobEffectCategory.HARMFUL, 7334143) {
            })
            .addAttributeModifier(Attributes.MOVEMENT_SPEED, YomiYomi.id("frostbite_speed"), -0.1, Operation.ADD_MULTIPLIED_TOTAL)
            .addAttributeModifier(Attributes.ATTACK_SPEED, YomiYomi.id("frostbite_attack"), -0.1, Operation.ADD_MULTIPLIED_TOTAL)
   );

   public static void init(IEventBus modBus) {
      ATTACHMENTS.register(modBus);
      EFFECTS.register(modBus);
   }

   public static YomiFrost.Data get(Entity e) {
      return e.hasData(TYPE) ? (YomiFrost.Data)e.getData(TYPE) : YomiFrost.Data.NONE;
   }

   public static int chill(Entity e) {
      return get(e).chillAt(e.level().getGameTime());
   }

   public static boolean frozen(Entity e) {
      return get(e).frozen(e.level().getGameTime());
   }

   public static int addChill(LivingEntity target, ServerPlayer owner, float burstDamage, int slowTicks) {
      if (target.level() instanceof ServerLevel level && target.isAlive() && Targeting.canHurt(owner, target)) {
         long now = level.getGameTime();
         YomiFrost.Data d = get(target);
         int stacks = d.chillAt(now) + 1;
         if (stacks < 4) {
            target.setData(TYPE, new YomiFrost.Data(stacks, now, d.frozenUntil(), d.coldUntil()));
            return stacks;
         }

         target.setData(TYPE, new YomiFrost.Data(0, now, d.frozenUntil(), Math.max(d.coldUntil(), now + (long)slowTicks)));
         target.invulnerableTime = 0;
         Targeting.hurt(owner, target, burstDamage, HELLFIRE_COLD);
         target.addEffect(new MobEffectInstance(FROSTBITE, slowTicks, 4, false, true, true), owner);
         Vec3 at = target.getBoundingBox().getCenter();
         ServerFx.send(target, YomiFx.CHILL_BURST, at, Vec3.ZERO, target.getBbHeight(), level.getRandom().nextLong(), slowTicks);
         level.playSound(null, at.x, at.y, at.z, YomiSounds.CHILL_BURST.get(), SoundSource.PLAYERS, 1.1F, 1.0F);
         return 0;
      }

      return 0;
   }

   public static void freeze(LivingEntity target, int ticks) {
      if (target.level() instanceof ServerLevel level && target.isAlive() && ticks > 0) {
         long now = level.getGameTime();
         YomiFrost.Data d = get(target);
         target.setData(TYPE, new YomiFrost.Data(d.chillAt(now), d.lastChill(), Math.max(d.frozenUntil(), now + (long)ticks), d.coldUntil()));
         Stun.apply(target, ticks);
         target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze() - 1));
         ServerFx.send(target, YomiFx.FREEZE, target.position(), Vec3.ZERO, target.getBbHeight(), 0L, ticks);
         level.playSound(null, target.getX(), target.getY(), target.getZ(), YomiSounds.FREEZE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
         return;
      }
   }

   public static void cold(LivingEntity target, @Nullable ServerPlayer owner, int ticks, int level) {
      if (target.level() instanceof ServerLevel world && target.isAlive()) {
         long now = world.getGameTime();
         YomiFrost.Data d = get(target);
         target.setData(TYPE, new YomiFrost.Data(d.chillAt(now), d.lastChill(), d.frozenUntil(), Math.max(d.coldUntil(), now + (long)ticks)));
         target.addEffect(new MobEffectInstance(FROSTBITE, ticks, Math.max(0, level - 1), false, true, true), owner);
         return;
      }
   }

   public static void clear(LivingEntity target) {
      target.removeData(TYPE);
      target.removeEffect(FROSTBITE);
   }

   private YomiFrost() {
   }

   public static record Data(int chill, long lastChill, long frozenUntil, long coldUntil) {
      public static final YomiFrost.Data NONE = new YomiFrost.Data(0, 0L, 0L, 0L);
      public static final Codec<YomiFrost.Data> CODEC = RecordCodecBuilder.create(
         i -> i.group(
                  Codec.INT.fieldOf("chill").forGetter(YomiFrost.Data::chill),
                  Codec.LONG.fieldOf("last").forGetter(YomiFrost.Data::lastChill),
                  Codec.LONG.fieldOf("frozen").forGetter(YomiFrost.Data::frozenUntil),
                  Codec.LONG.fieldOf("cold").forGetter(YomiFrost.Data::coldUntil)
               )
               .apply(i, YomiFrost.Data::new)
      );
      public static final StreamCodec<ByteBuf, YomiFrost.Data> STREAM_CODEC = StreamCodec.composite(
         ByteBufCodecs.VAR_INT,
         YomiFrost.Data::chill,
         ByteBufCodecs.VAR_LONG,
         YomiFrost.Data::lastChill,
         ByteBufCodecs.VAR_LONG,
         YomiFrost.Data::frozenUntil,
         ByteBufCodecs.VAR_LONG,
         YomiFrost.Data::coldUntil,
         YomiFrost.Data::new
      );

      public int chillAt(long now) {
         long idle = now - this.lastChill - 80L;
         return idle < 0L ? this.chill : Math.max(0, this.chill - 1 - (int)(idle / 30L));
      }

      public boolean frozen(long now) {
         return this.frozenUntil > now;
      }

      public boolean cold(long now) {
         return this.coldUntil > now;
      }
   }
}
