package com.devilfruits.fruits.horo_horo.negative;

import com.devilfruits.core.combat.Interrupt;
import com.devilfruits.fruits.horo_horo.HoroHoro;
import com.devilfruits.fruits.horo_horo.HoroTriggers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.item.BlockItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.Tags.EntityTypes;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent.Expired;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent.Remove;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.neoforged.neoforge.event.level.BlockEvent.BreakEvent;
import net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class Negative {
   public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, "devilfruits");
   public static final DeferredHolder<MobEffect, MobEffect> NEGATIVE = EFFECTS.register(
      "horo_horo/negative",
      () -> (new MobEffect(MobEffectCategory.HARMFUL, 5987240) {
            })
            .addAttributeModifier(Attributes.MOVEMENT_SPEED, HoroHoro.id("negative_speed"), -0.9, Operation.ADD_MULTIPLIED_TOTAL)
            .addAttributeModifier(Attributes.JUMP_STRENGTH, HoroHoro.id("negative_jump"), -1.0, Operation.ADD_MULTIPLIED_TOTAL)
            .addAttributeModifier(Attributes.FLYING_SPEED, HoroHoro.id("negative_fly"), -0.9, Operation.ADD_MULTIPLIED_TOTAL)
   );
   public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, "devilfruits");
   public static final Supplier<AttachmentType<Negative.View>> VIEW = ATTACHMENTS.register(
      "horo_horo/negative_view", () -> AttachmentType.builder(() -> Negative.View.NONE).sync((holder, to) -> true, Negative.View.STREAM_CODEC).build()
   );

   public static void init(IEventBus modBus) {
      EFFECTS.register(modBus);
      ATTACHMENTS.register(modBus);
   }

   public static Negative.View view(Entity e) {
      return e.hasData(VIEW) ? (Negative.View)e.getData(VIEW) : Negative.View.NONE;
   }

   public static boolean in(@Nullable Entity entity) {
      if (entity instanceof LivingEntity living && living.hasEffect(NEGATIVE)) {
         return true;
      }

      return false;
   }

   public static void apply(LivingEntity target, int ticks, @Nullable ServerPlayer owner) {
      if (target.getType().is(EntityTypes.BOSSES)) {
         ticks = (int)Math.round((double)ticks * HoroHoro.setting("bossNegativeFactor"));
      }

      if (ticks > 0) {
         target.addEffect(new MobEffectInstance(NEGATIVE, ticks, 0, false, false, true), owner);
         long now = target.level().getGameTime();
         Negative.View was = view(target);
         target.setData(VIEW, new Negative.View(was.on(now) ? was.since() : now, now + (long)ticks));
         target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 1, false, false, false), owner);
         target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, ticks, 2, false, false, false), owner);
         Interrupt.apply(target);
         if (target instanceof Mob mob) {
            mob.setTarget(null);
            mob.getNavigation().stop();
         }

         if (owner != null) {
            HoroTriggers.NEGATIVE.get().trigger(owner);
         }
      }
   }

   @Nullable
   public static Component castBlocked(ServerPlayer player) {
      return in(player) && HoroHoro.flag("blocksAbilities") ? Component.translatable("message.devilfruits.horo_horo.in_negative") : null;
   }

   private static void onIncomingDamage(LivingIncomingDamageEvent event) {
      if (in(event.getSource().getEntity())) {
         event.setCanceled(true);
      }
   }

   private static void onAttack(AttackEntityEvent event) {
      if (in(event.getEntity())) {
         event.setCanceled(true);
      }
   }

   private static void onLeftClickBlock(LeftClickBlock event) {
      if (in(event.getEntity())) {
         event.setCanceled(true);
      }
   }

   private static void onBreak(BreakEvent event) {
      if (in(event.getPlayer())) {
         event.setCanceled(true);
      }
   }

   private static void onPlace(EntityPlaceEvent event) {
      if (in(event.getEntity())) {
         event.setCanceled(true);
      }
   }

   private static void onRightClickBlock(RightClickBlock event) {
      if (in(event.getEntity()) && event.getItemStack().getItem() instanceof BlockItem) {
         event.setCanceled(true);
      }
   }

   private static void onChangeTarget(LivingChangeTargetEvent event) {
      if (in(event.getEntity()) && event.getNewAboutToBeSetTarget() != null) {
         event.setCanceled(true);
      }
   }

   private static void onEffectRemoved(Remove event) {
      if (event.getEffect().value() == NEGATIVE.value() && event.getEntity().hasData(VIEW)) {
         event.getEntity().removeData(VIEW);
      }
   }

   private static void onEffectExpired(Expired event) {
      if (event.getEffectInstance() != null && event.getEffectInstance().is(NEGATIVE) && event.getEntity().hasData(VIEW)) {
         event.getEntity().removeData(VIEW);
      }
   }

   public static void register(IEventBus bus) {
      bus.addListener(Negative::onEffectRemoved);
      bus.addListener(Negative::onEffectExpired);
      bus.addListener(Negative::onIncomingDamage);
      bus.addListener(Negative::onAttack);
      bus.addListener(Negative::onLeftClickBlock);
      bus.addListener(Negative::onBreak);
      bus.addListener(Negative::onPlace);
      bus.addListener(Negative::onRightClickBlock);
      bus.addListener(Negative::onChangeTarget);
   }

   private Negative() {
   }

   public static record View(long since, long until) {
      public static final Negative.View NONE = new Negative.View(0L, 0L);
      public static final Codec<Negative.View> CODEC = RecordCodecBuilder.create(
         i -> i.group(Codec.LONG.fieldOf("since").forGetter(Negative.View::since), Codec.LONG.fieldOf("until").forGetter(Negative.View::until))
               .apply(i, Negative.View::new)
      );
      public static final StreamCodec<ByteBuf, Negative.View> STREAM_CODEC = StreamCodec.composite(
         ByteBufCodecs.VAR_LONG, Negative.View::since, ByteBufCodecs.VAR_LONG, Negative.View::until, Negative.View::new
      );

      public boolean on(long now) {
         return this.until > now;
      }
   }
}
