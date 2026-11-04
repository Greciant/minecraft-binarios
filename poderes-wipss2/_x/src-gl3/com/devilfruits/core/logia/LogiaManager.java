package com.devilfruits.core.logia;

import com.devilfruits.DevilFruits;
import com.devilfruits.core.CoreEvents;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.config.CoreConfig;
import com.devilfruits.core.config.LogiaFruitConfig;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.fruit.DevilFruit;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.network.DFNetwork;
import com.devilfruits.core.network.LogiaStatePayload;
import com.devilfruits.core.rules.Weakness;
import com.devilfruits.core.util.ServerScheduler;
import com.devilfruits.registry.DFAttachments;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class LogiaManager {
   private static final ResourceLocation SLOW_ID = DevilFruits.id("logia_counter_slow");
   public static final ResourceLocation FIZZLE = DevilFruits.id("logia/fizzle");
   public static final int AWAKEN_TICKS = 30;
   private static final Map<UUID, Integer> FORCED_SUPPRESS = new HashMap<>();

   @Nullable
   public static LogiaProfile profile(Player player) {
      if (!player.hasData(DFAttachments.PLAYER_DATA)) {
         return null;
      } else {
         DevilFruit fruit = DFAttachments.data(player).fruit();
         return fruit == null ? null : fruit.logia();
      }
   }

   public static boolean isLogia(Player player) {
      return profile(player) != null;
   }

   public static LogiaState state(Player player) {
      return isLogia(player) ? DFAttachments.data(player).logia().state() : LogiaState.SOLID;
   }

   public static boolean bodyReady(Player player) {
      if (!isLogia(player)) {
         return false;
      } else {
         PlayerFruitData data = DFAttachments.data(player);
         LogiaData logia = data.logia();
         return logia.state() != LogiaState.SUPPRESSED && !logia.intangibilitySuppressed() && !Weakness.seaSuppressed(player);
      }
   }

   public static boolean intangible(Player player) {
      return isLogia(player) && DFAttachments.data(player).logia().intangible();
   }

   public static void setIntangible(ServerPlayer player, boolean on) {
      if (isLogia(player)) {
         PlayerFruitData data = DFAttachments.data(player);
         if (data.logia().intangible() != on) {
            data.logia().setIntangible(on);
            data.markDirty();
            CoreEvents.syncNow(player);
            player.displayClientMessage(
               Component.translatable(on ? "message.devilfruits.logia.intangible_on" : "message.devilfruits.logia.intangible_off")
                  .withStyle(on ? ChatFormatting.AQUA : ChatFormatting.GOLD),
               true
            );
         }
      }
   }

   public static boolean abilitiesBlocked(Player player) {
      return player.hasData(DFAttachments.PLAYER_DATA) && DFAttachments.data(player).logia().abilitiesBlocked();
   }

   public static float abilityPower(Player player) {
      return player.hasData(DFAttachments.PLAYER_DATA) ? DFAttachments.data(player).logia().abilityPower() : 1.0F;
   }

   public static double regenMultiplier(Player player) {
      LogiaProfile profile = profile(player);
      if (profile == null) {
         return 1.0;
      } else {
         LogiaFruitConfig config = DFAttachments.data(player).fruit().config().logia();
         double m = 1.0;
         if (profile.environment().atSource(player)) {
            m *= config == null ? profile.environment().sourceRegen() : config.sourceRegen();
         }

         if (profile.environment().opposed(player)) {
            m *= config == null ? profile.environment().opposedRegen() : config.opposedRegen();
         }

         return m;
      }
   }

   public static boolean counterActive(Player player, String id) {
      LogiaProfile profile = profile(player);
      if (profile == null) {
         return false;
      } else {
         int index = profile.counterIndex(id);
         return index >= 0 && (DFAttachments.data(player).logia().activeCounters() & 1 << index) != 0;
      }
   }

   public static void tick(ServerPlayer player, PlayerFruitData data) {
      LogiaData logia = data.logia();
      LogiaProfile profile = profile(player);
      if (profile == null) {
         if (!logia.isDefault() || logia.activeCounters() != 0) {
            logia.reset();
            removeSlow(player);
            sync(player, data);
         }
      } else {
         int before = signature(logia);
         LogiaFruitConfig config = data.fruit().config().logia();
         boolean free = AbilityManager.freeEnergy(player);
         int active = 0;
         boolean suppress = false;
         boolean block = false;
         boolean reform = false;
         float power = 1.0F;
         float drain = 0.0F;
         float slow = 0.0F;
         Map<String, Integer> timers = logia.counterTimers();

         for (int i = 0; i < profile.counters().size(); i++) {
            LogiaCounter counter = profile.counters().get(i);
            LogiaFruitConfig.CounterConfig cc = config == null ? null : config.counter(counter.id());
            if (cc != null && !(Boolean)cc.enabled().get()) {
               timers.remove(counter.id());
            } else {
               int linger = Math.max(1, cc == null ? counter.lingerTicks() : (Integer)cc.lingerTicks().get());

               for (CounterTrigger trigger : counter.triggers()) {
                  if (!trigger.isEvent() && (trigger.kind() != CounterTrigger.Kind.SEASTONE_NEAR || player.tickCount % 10 == 0) && trigger.test(player)) {
                     int keep = trigger.kind() == CounterTrigger.Kind.SEASTONE_NEAR ? Math.max(linger, 10) : linger;
                     timers.merge(counter.id(), keep, Math::max);
                  }
               }

               Integer left = timers.get(counter.id());
               if (left != null && left > 0) {
                  timers.put(counter.id(), left - 1);
                  active |= 1 << i;
                  suppress |= counter.has(LogiaCounter.Action.SUPPRESS_INTANGIBILITY);
                  block |= counter.has(LogiaCounter.Action.BLOCK_ABILITIES);
                  reform |= counter.has(LogiaCounter.Action.FORCE_REFORM);
                  if (counter.has(LogiaCounter.Action.DRAIN_ENERGY)) {
                     drain += cc == null ? counter.energyDrainPerSecond() : ((Double)cc.energyDrain().get()).floatValue();
                  }

                  if (counter.has(LogiaCounter.Action.WEAKEN_ABILITIES)) {
                     power = Math.min(power, cc == null ? counter.abilityPower() : ((Double)cc.abilityPower().get()).floatValue());
                  }

                  if (counter.has(LogiaCounter.Action.SLOW)) {
                     slow = Math.max(slow, cc == null ? counter.speedPenalty() : ((Double)cc.speedPenalty().get()).floatValue());
                  }

                  if (counter.has(LogiaCounter.Action.DEBUFF) && player.tickCount % 10 == 0) {
                     for (Holder<MobEffect> effect : counter.debuffs()) {
                        player.addEffect(new MobEffectInstance(effect, 40, 0, true, false, true));
                     }
                  }
               } else {
                  timers.remove(counter.id());
               }
            }
         }

         if (FORCED_SUPPRESS.getOrDefault(player.getUUID(), 0) > 0) {
            FORCED_SUPPRESS.computeIfPresent(player.getUUID(), (k, v) -> v > 1 ? v - 1 : null);
            suppress = true;
         }

         boolean held = suppress || Weakness.seaSuppressed(player);
         logia.setActiveCounters(active);
         logia.setIntangibilitySuppressed(held);
         logia.setAbilitiesBlocked(block);
         logia.setAbilityPower(power);
         if (drain > 0.0F && !free) {
            data.setEnergy(Math.max(0.0F, data.energy() - drain / 20.0F));
         }

         applySlow(player, slow);
         switch (logia.state()) {
            case ELEMENTAL_BODY:
               LogiaMovement.Session session = LogiaMovement.session(player);
               boolean own = session != null && session.mode.forcesBody();
               if (!free && !own) {
                  double perSecond = config == null ? (double)profile.elementalDrain() : config.elementalDrain();
                  data.setEnergy((float)Math.max(0.0, (double)data.energy() - perSecond / 20.0));
               }

               if (!held && !reform && (!(data.energy() <= 0.0F) || own) && player.isAlive()) {
                  LogiaMovement.tick(player);
               } else {
                  beginReform(player);
               }
               break;
            case REFORMING:
               logia.setReformTicks(logia.reformTicks() - 1);
               if (logia.reformTicks() <= 0) {
                  logia.setState(held ? LogiaState.SUPPRESSED : (logia.limbs() != 0 ? LogiaState.PARTIAL : LogiaState.SOLID));
               }
               break;
            default:
               if (logia.partialTicks() > 0) {
                  logia.setPartialTicks(logia.partialTicks() - 1);
                  if (logia.partialTicks() == 0) {
                     logia.setLimbs(0);
                  }
               }

               if (held) {
                  logia.setLimbs(0);
                  logia.setPartialTicks(0);
               }

               logia.setState(held ? LogiaState.SUPPRESSED : (logia.limbs() != 0 ? LogiaState.PARTIAL : LogiaState.SOLID));
         }

         if (signature(logia) != before) {
            sync(player, data);
         }
      }
   }

   private static int signature(LogiaData logia) {
      return Objects.hash(logia.state(), logia.limbs(), logia.activeCounters(), logia.intangibilitySuppressed(), logia.abilitiesBlocked(), logia.abilityPower());
   }

   public static void fizzle(ServerPlayer player) {
      ServerFx.sendNear(player.serverLevel(), FIZZLE, player.getId(), player.position(), Vec3.ZERO, 1.0F, 0L, 0, 32.0);
   }

   public static boolean setElemental(ServerPlayer player, boolean on) {
      LogiaProfile profile = profile(player);
      if (profile == null) {
         return false;
      } else {
         PlayerFruitData data = DFAttachments.data(player);
         LogiaData logia = data.logia();
         if (on) {
            if (!profile.allowsElementalBody() || logia.state() == LogiaState.ELEMENTAL_BODY || player.isSpectator()) {
               return false;
            } else if (!bodyReady(player)) {
               fizzle(player);
               return false;
            } else if (data.energy() <= 0.0F && !AbilityManager.freeEnergy(player)) {
               return false;
            } else {
               logia.setState(LogiaState.ELEMENTAL_BODY);
               logia.setLimbs(63);
               logia.setPartialTicks(0);
               sync(player, data);
               return true;
            }
         } else if (logia.state() != LogiaState.ELEMENTAL_BODY) {
            return false;
         } else {
            beginReform(player);
            return true;
         }
      }
   }

   public static void awaken(ServerPlayer player) {
      if (isLogia(player)) {
         PlayerFruitData data = DFAttachments.data(player);
         LogiaData logia = data.logia();
         logia.setState(LogiaState.ELEMENTAL_BODY);
         logia.setLimbs(63);
         logia.setPartialTicks(0);
         sync(player, data);
         ServerScheduler.later(player.serverLevel(), 30, () -> {
            if (!player.isRemoved() && isLogia(player) && state(player) == LogiaState.ELEMENTAL_BODY && LogiaMovement.session(player) == null) {
               beginReform(player);
            }
         });
      }
   }

   public static void beginReform(ServerPlayer player) {
      PlayerFruitData data = DFAttachments.data(player);
      LogiaData logia = data.logia();
      LogiaMovement.end(player, false);
      logia.setLimbs(0);
      logia.setReformTicks((Integer)CoreConfig.LOGIA_REFORM_TICKS.get());
      logia.setState(logia.reformTicks() > 0 ? LogiaState.REFORMING : LogiaState.SOLID);
      sync(player, data);
   }

   public static void partial(ServerPlayer player, int limbs, int ticks) {
      if (isLogia(player)) {
         PlayerFruitData data = DFAttachments.data(player);
         LogiaData logia = data.logia();
         if ((logia.state() == LogiaState.SOLID || logia.state() == LogiaState.PARTIAL) && bodyReady(player)) {
            logia.setLimbs(logia.limbs() | limbs);
            logia.setPartialTicks(Math.max(logia.partialTicks(), ticks));
            logia.setState(LogiaState.PARTIAL);
            sync(player, data);
         }
      }
   }

   public static void trigger(ServerPlayer player, CounterTrigger.Kind kind, @Nullable DamageSource source) {
      LogiaProfile profile = profile(player);
      if (profile != null) {
         PlayerFruitData data = DFAttachments.data(player);
         LogiaFruitConfig config = data.fruit().config().logia();

         for (LogiaCounter counter : profile.counters()) {
            LogiaFruitConfig.CounterConfig cc = config == null ? null : config.counter(counter.id());
            if (cc == null || (Boolean)cc.enabled().get()) {
               for (CounterTrigger trigger : counter.triggers()) {
                  boolean match = trigger.kind() == kind
                     && (kind != CounterTrigger.Kind.DAMAGE_TYPE || source != null && trigger.damageTypes() != null && source.is(trigger.damageTypes()));
                  if (match) {
                     int linger = Math.max(1, cc == null ? counter.lingerTicks() : (Integer)cc.lingerTicks().get());
                     data.logia().counterTimers().merge(counter.id(), linger, Math::max);
                     data.markDirty();
                  }
               }
            }
         }
      }
   }

   public static void forceState(ServerPlayer player, LogiaState state, int ticks) {
      if (isLogia(player)) {
         PlayerFruitData data = DFAttachments.data(player);
         LogiaData logia = data.logia();
         switch (state) {
            case ELEMENTAL_BODY:
               logia.setState(LogiaState.ELEMENTAL_BODY);
               logia.setLimbs(63);
               break;
            case REFORMING:
               beginReform(player);
               break;
            case PARTIAL:
               logia.setState(LogiaState.PARTIAL);
               logia.setLimbs(12);
               logia.setPartialTicks(Math.max(1, ticks));
               break;
            case SUPPRESSED:
               FORCED_SUPPRESS.put(player.getUUID(), Math.max(1, ticks));
               LogiaMovement.end(player, false);
               logia.setState(LogiaState.SUPPRESSED);
               logia.setLimbs(0);
               break;
            case SOLID:
               FORCED_SUPPRESS.remove(player.getUUID());
               LogiaMovement.end(player, false);
               logia.setState(LogiaState.SOLID);
               logia.setLimbs(0);
               logia.setPartialTicks(0);
         }

         sync(player, data);
      }
   }

   public static boolean forceCounter(ServerPlayer player, String id, int ticks) {
      LogiaProfile profile = profile(player);
      if (profile != null && profile.counter(id) != null) {
         DFAttachments.data(player).logia().counterTimers().put(id, ticks);
         return true;
      } else {
         return false;
      }
   }

   public static void settle(ServerPlayer player) {
      if (isLogia(player)) {
         PlayerFruitData data = DFAttachments.data(player);
         LogiaData logia = data.logia();
         LogiaMovement.end(player, false);
         if (logia.state() == LogiaState.ELEMENTAL_BODY || logia.state() == LogiaState.REFORMING) {
            LogiaReform.settleSafely(player);
            logia.setState(LogiaState.SOLID);
            logia.setLimbs(0);
            logia.setPartialTicks(0);
         }

         sync(player, data);
      }
   }

   public static void onDeath(ServerPlayer player) {
      PlayerFruitData data = DFAttachments.data(player);
      LogiaMovement.end(player, false);
      FORCED_SUPPRESS.remove(player.getUUID());
      LogiaData logia = data.logia();
      logia.setState(LogiaState.SOLID);
      logia.setLimbs(0);
      logia.setPartialTicks(0);
      logia.counterTimers().clear();
      removeSlow(player);
      data.markUrgent();
   }

   public static void onPowerRemoved(ServerPlayer player) {
      LogiaMovement.end(player, false);
      FORCED_SUPPRESS.remove(player.getUUID());
      removeSlow(player);
      PlayerFruitData data = DFAttachments.data(player);
      boolean was = !data.logia().isDefault();
      data.logia().reset();
      if (was) {
         sync(player, data);
      }
   }

   public static void forget(ServerPlayer player) {
      FORCED_SUPPRESS.remove(player.getUUID());
      LogiaMovement.forget(player);
   }

   public static void sync(ServerPlayer player, PlayerFruitData data) {
      data.markUrgent();
      LogiaData logia = data.logia();
      PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, payload(player, logia), new CustomPacketPayload[0]);
   }

   public static void sendTo(ServerPlayer tracker, ServerPlayer user) {
      if (isLogia(user)) {
         DFNetwork.sendTo(tracker, payload(user, DFAttachments.data(user).logia()));
      }
   }

   private static LogiaStatePayload payload(ServerPlayer player, LogiaData logia) {
      return new LogiaStatePayload(player.getId(), logia.state().ordinal(), logia.limbs(), logia.activeCounters(), LogiaMovement.index(logia.movementMode()));
   }

   private static void applySlow(ServerPlayer player, float penalty) {
      AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
      if (speed != null) {
         if (penalty <= 0.0F) {
            if (speed.getModifier(SLOW_ID) != null) {
               speed.removeModifier(SLOW_ID);
            }
         } else {
            AttributeModifier current = speed.getModifier(SLOW_ID);
            if (current == null || current.amount() != (double)(-penalty)) {
               speed.addOrUpdateTransientModifier(new AttributeModifier(SLOW_ID, (double)(-penalty), Operation.ADD_MULTIPLIED_TOTAL));
            }
         }
      }
   }

   private static void removeSlow(ServerPlayer player) {
      applySlow(player, 0.0F);
   }

   private LogiaManager() {
   }
}
