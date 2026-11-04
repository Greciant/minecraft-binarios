package com.awakenedmod.awakened.content.command;

import com.awakenedmod.awakened.content.command.vocab.Clash;
import com.awakenedmod.awakened.content.command.vocab.Effect;
import com.awakenedmod.awakened.content.command.vocab.RuleContext;
import com.awakenedmod.awakened.content.command.vocab.Target;
import com.awakenedmod.awakened.content.command.vocab.TargetKind;
import com.awakenedmod.awakened.content.command.vocab.Trigger;
import com.awakenedmod.awakened.content.command.vocab.Vocabulary;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.AbilityFeedback;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public final class RuleEngine {
   private static final int MASTERY_INTERVAL = 20;
   private static final int FOLD_TICKS = 20;
   private static final int AURA_GRACE_TICKS = 60;
   private static final long REFUSAL_INTERVAL = 20L;
   private static final Map<UUID, RuleEngine.CasterState> STATE = new ConcurrentHashMap<>();
   private static final Map<UUID, Long> LAST_REFUSAL = new ConcurrentHashMap<>();
   private static boolean sharingPain;
   private static boolean applyingRule;
   private static final String CONTROL_REFUSED = "a player: this server keeps holds off other players";

   private RuleEngine() {
   }

   private static RuleEngine.CasterState state(ServerPlayer caster) {
      return STATE.computeIfAbsent(caster.getUUID(), id -> new RuleEngine.CasterState());
   }

   public static void tick(ServerPlayer player, PowerData data) {
      CommandData rules = CommandData.of(player);
      if (rules.anyOn() || !Landings.of(player.getUUID()).isEmpty()) {
         for (Trigger word : Vocabulary.triggers()) {
            word.observe(player);
         }

         landBySelect(player);
         RuleSlot[] work = new RuleSlot[5];

         for (int i = 0; i < 5; i++) {
            work[i] = rules.slot(i);
         }

         RuleEngine.CasterState state = state(player);

         for (int i = 0; i < 5; i++) {
            tickSlot(player, data, work, i, state);
         }

         tickLandings(player, data, work, state);
         foldHolds(player, work, state);
         CommandData.write(player, new CommandData(List.of(work), rules.names()));
      }
   }

   private static void tickSlot(ServerPlayer player, PowerData data, RuleSlot[] work, int index, RuleEngine.CasterState state) {
      RuleSlot slot = work[index];
      if (slot.on()) {
         Rule rule = slot.rule();
         if (!rule.resolves()) {
            switchOff(player, index);
            work[index] = CommandData.of(player).slot(index);
         } else {
            Target target = rule.target();
            Effect effect = rule.effect();
            Trigger trigger = rule.trigger();
            if (!target.kind().lands()) {
               boolean live = trigger.live(player);
               CommandTrace.say(
                  player,
                  CommandTrace.slot(index) + ": trigger " + trigger.id() + (live ? " is live" : " is NOT live (the rule is on, but nothing is being imposed)")
               );
               if (live) {
                  RuleContext ctx = context(rule, index);
                  if (target.kind() == TargetKind.SELF) {
                     applySelf(player, data, rule, effect, state, index, ctx.level());
                  } else {
                     applyAura(player, data, target, effect, state, index, ctx, rule);
                  }

                  work[index] = advanceTimer(player, work[index], index, state);
               } else {
                  boolean tookHold = state.selfHeld[index] || holdsInside(state, index);
                  releaseSelf(player, rule, effect, state, index);
                  parkAura(player, rule, effect, state, index);
                  if (tookHold && rule.instant()) {
                     work[index] = runOut(player, index, state);
                  }
               }
            }
         }
      }
   }

   private static void applySelf(ServerPlayer player, PowerData data, Rule rule, Effect effect, RuleEngine.CasterState state, int index, int level) {
      int age = state.selfHeld[index] ? state.selfAge[index] : 0;
      boolean worked = applyEffect(effect, player, player, level, age);
      if (age == 0) {
         CommandTrace.say(
            player, CommandTrace.slot(index) + ": " + effect.id() + " applied to you at level " + level + (worked ? "" : " (it reported no work)")
         );
      }

      state.selfHeld[index] = true;
      state.selfAge[index] = age + 1;
      award(player, data, state, index, worked, player);
   }

   private static void applyAura(
      ServerPlayer player, PowerData data, Target target, Effect effect, RuleEngine.CasterState state, int index, RuleContext ctx, Rule rule
   ) {
      Map<UUID, RuleEngine.AuraHold> held = state.auraHeld.get(index);
      Set<UUID> done = state.auraDone.get(index);
      Set<UUID> inside = new LinkedHashSet<>();
      boolean worked = false;
      boolean firstWork = false;
      ServerLevel serverLevel = CommandSupport.level(player);
      long now = serverLevel.getGameTime();
      int effectLevel = ctx.level();

      for (LivingEntity body : target.select(player, ctx)) {
         UUID id = body.getUUID();
         if (body.isAlive() && !done.contains(id) && whyNotHold(player, target, effect, body) == null) {
            int ownTicks = rule.infinite() ? -1 : effect.durationTicks(body, rule.durationTicks());
            RuleEngine.AuraHold hold = held.get(id);
            boolean takesHold = false;
            if (hold == null) {
               hold = new RuleEngine.AuraHold();
               held.put(id, hold);
               takesHold = true;
               refreshHolds(player);
               CommandTrace.say(player, CommandTrace.slot(index) + ": picked up " + body.getName().getString() + " (aura, held by the switch-on price)");
            } else if (!hold.inside()) {
               if (ownTicks > 0 && hold.age >= ownTicks) {
                  done.add(id);
                  held.remove(id);
                  continue;
               }

               hold.graceUntil = Long.MIN_VALUE;
               takesHold = true;
               refreshHolds(player);
            }

            if (ownTicks > 0 && hold.age >= ownTicks) {
               done.add(id);
               held.remove(id);
               releaseBody(CommandSupport.server(player), effect, id, rule);
               refreshHolds(player);
            } else {
               boolean bodyWorked = applyEffect(effect, player, body, effectLevel, takesHold ? 0 : hold.age);
               worked |= bodyWorked;
               firstWork |= bodyWorked && !hold.worked;
               hold.worked |= bodyWorked;
               hold.age++;
               inside.add(id);
            }
         }
      }

      boolean dropped = false;

      for (Entry<UUID, RuleEngine.AuraHold> entry : List.copyOf(held.entrySet())) {
         RuleEngine.AuraHold holdx = entry.getValue();
         if (!inside.contains(entry.getKey())) {
            if (holdx.inside()) {
               releaseBody(CommandSupport.server(player), effect, entry.getKey(), rule);
               holdx.graceUntil = now + 60L;
               dropped = true;
            } else if (now >= holdx.graceUntil) {
               held.remove(entry.getKey());
            }
         }
      }

      if (dropped) {
         refreshHolds(player);
      }

      int foes = 0;

      for (UUID id : inside) {
         Entity var32 = CommandSupport.byUuid(CommandSupport.server(player), id);
         if (var32 instanceof LivingEntity) {
            LivingEntity bodyx = (LivingEntity)var32;
            if (Foes.counts(player, bodyx)) {
               foes++;
            }
         }
      }

      CommandFeats.aura(player, index, CommandData.of(player).slot(index).armedAt(), foes);
      award(player, data, state, index, worked && (firstWork || !rule.infinite()), null);
   }

   private static boolean applyEffect(Effect effect, ServerPlayer caster, LivingEntity body, int level, int age) {
      boolean outer = applyingRule;
      applyingRule = true;

      boolean var6;
      try {
         var6 = effect.apply(caster, body, level, age);
      } finally {
         applyingRule = outer;
      }

      return var6;
   }

   private static RuleSlot advanceTimer(ServerPlayer player, RuleSlot slot, int index, RuleEngine.CasterState state) {
      if (slot.ticksLeft() < 0) {
         return slot;
      } else {
         int ran = ++state.sinceFold[index];
         int left = slot.ticksLeft() - ran;
         if (left > 0) {
            if (ran < 20) {
               return slot;
            } else {
               state.sinceFold[index] = 0;
               return slot.withTicksLeft(left);
            }
         } else {
            return runOut(player, index, state);
         }
      }
   }

   private static RuleSlot runOut(ServerPlayer player, int index, RuleEngine.CasterState state) {
      state.sinceFold[index] = 0;
      player.displayClientMessage(Component.translatable("awakened.command.ran_out", new Object[]{index + 1}), true);
      CommandTrace.say(player, CommandTrace.slot(index) + ": timer ran out");
      switchOff(player, index);
      return CommandData.of(player).slot(index);
   }

   private static void tickLandings(ServerPlayer player, PowerData data, RuleSlot[] work, RuleEngine.CasterState state) {
      List<Landings.Landing> landings = Landings.of(player.getUUID());
      if (!landings.isEmpty()) {
         MinecraftServer server = CommandSupport.server(player);
         ServerLevel casterLevel = CommandSupport.level(player);

         for (Landings.Landing landing : List.copyOf(landings)) {
            RuleSlot slot = work[landing.slot];
            if (slot.on() && slot.rule().equals(landing.rule)) {
               Entity found = CommandSupport.byUuid(server, landing.target);
               if (found instanceof LivingEntity) {
                  LivingEntity body = (LivingEntity)found;
                  if (!body.isAlive()) {
                     end(server, player, work, landing);
                  } else {
                     if (landing.bound() && slot.boundDormant()) {
                        work[landing.slot] = slot.withBind(landing.target, false);
                     }

                     if (landing.entityId != body.getId()) {
                        landing.entityId = body.getId();
                        landing.age = 0;
                     }

                     Effect effect = landing.rule.effect();
                     if (effect == null) {
                        end(server, player, work, landing);
                     } else {
                        int level = landing.rule.level();
                        boolean worked = applyEffect(effect, player, body, level, landing.age);
                        if (landing.age == 0) {
                           CommandTrace.say(
                              player,
                              CommandTrace.slot(landing.slot)
                                 + ": "
                                 + effect.id()
                                 + " applied to "
                                 + body.getName().getString()
                                 + " at level "
                                 + level
                                 + (worked ? "" : " (it reported no work)")
                           );
                        }

                        landing.age++;
                        if (!landing.bound()) {
                           award(player, data, state, landing.slot, worked, body);
                        } else if (worked && !landing.masteryPaid) {
                           landing.masteryPaid = award(player, data, state, landing.slot, true, body);
                        }

                        if (!landing.bound() && --landing.ticksLeft <= 0) {
                           end(server, player, work, landing);
                        }
                     }
                  }
               } else if (landing.bound()) {
                  work[landing.slot] = slot.withBind(landing.target, true);
               } else {
                  end(server, player, work, landing);
               }
            } else {
               end(server, player, work, landing);
            }
         }
      }
   }

   private static void end(@Nullable MinecraftServer server, ServerPlayer player, @Nullable RuleSlot[] work, Landings.Landing landing) {
      CommandTrace.say(
         player,
         CommandTrace.slot(landing.slot) + ": let go of a held body after " + landing.age + " ticks (" + Math.round(landing.paid) + " regenerates from here)"
      );
      Landings.expire(server, landing);
      Landings.drop(player.getUUID(), landing);
      CommandStamina.release(player);
      refreshHolds(player);
      if (landing.bound()) {
         if (work != null) {
            if (work[landing.slot].bound()) {
               work[landing.slot] = work[landing.slot].withBind(null, false);
            }
         } else {
            RuleSlot slot = CommandData.of(player).slot(landing.slot);
            if (slot.bound()) {
               CommandData.writeSlot(player, landing.slot, slot.withBind(null, false));
            }
         }
      }
   }

   public static void onDealDamage(ServerPlayer attacker, LivingEntity victim) {
      if (!sharingPain && !applyingRule) {
         land(attacker, victim, TargetKind.HIT);

         for (Trigger word : Vocabulary.triggers()) {
            word.onCasterHit(attacker, victim);
         }
      }
   }

   public static void onKill(ServerPlayer killer, LivingEntity victim) {
      CommandFeats.killed(killer, victim);

      for (Trigger word : Vocabulary.triggers()) {
         word.onCasterKill(killer, victim);
      }
   }

   private static void landBySelect(ServerPlayer player) {
      CommandData rules = CommandData.of(player);

      for (int i = 0; i < 5; i++) {
         RuleSlot slot = rules.slot(i);
         if (slot.on() && slot.rule().resolves()) {
            Rule rule = slot.rule();
            Target target = rule.target();
            if (target.kind().landsBySelect() && rule.trigger().live(player)) {
               for (LivingEntity body : target.select(player, context(rule, i))) {
                  if (body != player && body.isAlive() && !Landings.holds(player.getUUID(), i, body.getUUID())) {
                     String why = landSlot(player, i, body, target.kind(), false);
                     if (why != null) {
                        CommandTrace.say(player, CommandTrace.slot(i) + ": no landing on " + body.getName().getString() + " (" + why + ")");
                     }
                  }
               }
            }
         }
      }
   }

   public static void onIncomingDamage(ServerPlayer player, DamageSource source) {
      if (source.getEntity() instanceof LivingEntity living) {
         land(player, living, TargetKind.HITS_ME);
      }
   }

   public static boolean shieldsFrom(ServerPlayer player, DamageSource source) {
      Entity attacker = source.getEntity();
      if (attacker != null && heldUnder(player, Effect::nullifiesDamageToCaster).contains(attacker.getUUID())) {
         MasterySystem.award(player, MasteryReason.SURVIVAL, attacker);
         return true;
      } else {
         return false;
      }
   }

   public static void onDamageTaken(ServerPlayer player, DamageSource source, float amount) {
      if (!sharingPain && !(amount <= 0.0F)) {
         Set<UUID> sharers = heldUnder(player, Effect::sharesCasterDamage);
         if (!sharers.isEmpty()) {
            MinecraftServer server = CommandSupport.server(player);
            DamageSource share = Dmg.by(player, DamageTypes.MAGIC);
            sharingPain = true;

            try {
               for (UUID id : sharers) {
                  Entity var10 = CommandSupport.byUuid(server, id);
                  if (var10 instanceof LivingEntity) {
                     LivingEntity body = (LivingEntity)var10;
                     if (body != player && body.isAlive()) {
                        Level var14 = body.level();
                        if (var14 instanceof ServerLevel) {
                           ServerLevel bodyLevel = (ServerLevel)var14;
                           if (Dmg.hurtThrough(body, bodyLevel, share, ruleDamage(amount))) {
                              MasterySystem.award(player, MasteryReason.HIT, body);
                           }
                        }
                     }
                  }
               }
            } finally {
               sharingPain = false;
            }
         }
      }
   }

   private static Set<UUID> heldUnder(ServerPlayer player, Predicate<Effect> yes) {
      Set<UUID> out = new LinkedHashSet<>();
      forEachHold(player, (body, effect, level) -> {
         if (yes.test(effect) && !body.equals(player.getUUID())) {
            out.add(body);
         }
      });
      return out;
   }

   private static void forEachHold(ServerPlayer player, RuleEngine.HoldVisitor visitor) {
      for (Landings.Landing landing : Landings.of(player.getUUID())) {
         Effect effect = landing.rule.effect();
         if (effect != null) {
            visitor.visit(landing.target, effect, landing.rule.level());
         }
      }

      RuleEngine.CasterState state = STATE.get(player.getUUID());
      if (state != null) {
         CommandData rules = CommandData.of(player);

         for (int i = 0; i < 5; i++) {
            RuleSlot slot = rules.slot(i);
            Rule rule = slot.rule();
            if (slot.on() && rule.resolves()) {
               Effect effect = rule.effect();
               int level = rule.level();
               if (state.selfHeld[i]) {
                  visitor.visit(player.getUUID(), effect, level);
               }

               for (Entry<UUID, RuleEngine.AuraHold> entry : state.auraHeld.get(i).entrySet()) {
                  if (entry.getValue().inside()) {
                     visitor.visit(entry.getKey(), effect, level);
                  }
               }
            }
         }
      }
   }

   public static RuleContext context(Rule rule, int slot) {
      return new RuleContext(slot, rule.level(), rule.radius());
   }

   public static float incomingDamageMultiplier(LivingEntity body) {
      MinecraftServer server = body.level().getServer();
      if (server != null && (!STATE.isEmpty() || Landings.any())) {
         UUID id = body.getUUID();
         float[] best = new float[]{1.0F};
         ServerPlayer[] by = new ServerPlayer[]{null};

         for (ServerPlayer caster : server.getPlayerList().getPlayers()) {
            forEachHold(caster, (held, effect, level) -> {
               if (held.equals(id)) {
                  float factor = effect.incomingDamageMultiplier(level);
                  if (factor > best[0]) {
                     best[0] = factor;
                     by[0] = caster;
                  }
               }
            });
         }

         if (by[0] != null && by[0] != body) {
            MasterySystem.award(by[0], MasteryReason.HIT, body);
         }

         return best[0];
      } else {
         return 1.0F;
      }
   }

   private static void land(ServerPlayer player, LivingEntity body, TargetKind kind) {
      if (body != player && body.isAlive()) {
         for (int i = 0; i < 5; i++) {
            boolean running = CommandData.of(player).slot(i).on();
            String why = landSlot(player, i, body, kind, true);
            if (why != null && running) {
               CommandTrace.say(player, CommandTrace.slot(i) + ": no landing on " + body.getName().getString() + " (" + why + ")");
            }
         }
      }
   }

   @Nullable
   private static String landSlot(ServerPlayer player, int index, LivingEntity body, @Nullable TargetKind requiredKind, boolean sayRefusal) {
      CommandData rules = CommandData.of(player);
      PowerData data = PowerService.data(player);
      RuleSlot slot = rules.slot(index);
      if (!slot.on()) {
         return slot.isEmpty() ? "nothing written" : "switched off";
      } else if (!slot.rule().resolves()) {
         return "sentence does not resolve";
      } else {
         Rule rule = slot.rule();
         if (!rule.target().kind().lands()) {
            return "not a landing rule (" + rule.target().id() + ")";
         } else if (requiredKind != null && rule.target().kind() != requiredKind) {
            return "rule is " + rule.target().id() + ", not " + requiredKind;
         } else if (!rule.trigger().live(player)) {
            return "trigger " + rule.trigger().id() + " is not live";
         } else {
            String notHeld = whyNotHold(player, rule.target(), rule.effect(), body);
            if (notHeld != null) {
               if (sayRefusal && "a player: this server keeps holds off other players".equals(notHeld)) {
                  sayThrottled(player, "awakened.feedback.pvp_control_off");
               }

               return notHeld;
            } else if (rule.infinite() && slot.bound() && !slot.boundTarget().equals(body.getUUID())) {
               if (sayRefusal) {
                  sayThrottled(player, "awakened.feedback.command_already_bound", index + 1);
               }

               return "already bound to another body";
            } else if (Landings.holds(player.getUUID(), index, body.getUUID())) {
               return "already holding this one";
            } else {
               float price = billed((float)CommandCost.points(rule, data.masteryLevel()));
               if (!CommandStamina.canAfford(data, price)) {
                  if (sayRefusal) {
                     sayThrottled(player, "awakened.feedback.no_stamina");
                  }

                  return "no room on the bar for " + Math.round(price) + " on this body";
               } else {
                  float paid = CommandStamina.spend(player, data, price);
                  int ticks = rule.infinite() ? -1 : Math.max(1, rule.effect().durationTicks(body, rule.durationTicks()));
                  Landings.add(player.getUUID(), index, body.getUUID(), rule, ticks, paid);
                  if (rule.infinite()) {
                     CommandData.writeSlot(player, index, rules.slot(index).withBind(body.getUUID(), false));
                  }

                  CommandFeats.landed(player, rule, body, rule.infinite());
                  CommandStamina.hold(player);
                  refreshHolds(player);
                  CommandTrace.say(
                     player,
                     CommandTrace.slot(index)
                        + ": landed on "
                        + body.getName().getString()
                        + " for "
                        + (ticks < 0 ? "ever" : ticks + " ticks")
                        + ", cost "
                        + Math.round(paid)
                  );
                  return null;
               }
            }
         }
      }
   }

   private static void sayThrottled(ServerPlayer player, String key, Object... args) {
      long now = CommandSupport.level(player).getGameTime();
      Long last = LAST_REFUSAL.get(player.getUUID());
      if (last == null || now - last >= 20L) {
         LAST_REFUSAL.put(player.getUUID(), now);
         AbilityFeedback.fail(player, key, args);
      }
   }

   @Nullable
   public static String landOn(ServerPlayer player, LivingEntity body, int index) {
      if (body == player) {
         return "that is you";
      } else {
         return !body.isAlive() ? "that body is dead" : landSlot(player, index, body, null, true);
      }
   }

   @Nullable
   private static String whyNotHold(ServerPlayer player, Target target, Effect effect, LivingEntity body) {
      if (!effect.appliesTo(body)) {
         return effect.id() + " does nothing on that body";
      } else if (!effect.hostile()) {
         return null;
      } else if (!Harm.allowed(player, body)) {
         return "a teammate: only a kind word lands on them";
      } else if (!target.sparesOwnSide() || !body.hasIndirectPassenger(player) && !Foes.fightsFor(player, body)) {
         return effect.holds() && !Harm.controls(player, body) ? "a player: this server keeps holds off other players" : null;
      } else {
         return "the caster's own mount, pet or summon";
      }
   }

   @Nullable
   public static RuleEngine.Refusal switchOn(ServerPlayer player, PowerData data, int index) {
      RuleEngine.Refusal refusal = switchOnChecked(player, data, index);
      if (refusal != null) {
         CommandTrace.say(player, CommandTrace.slot(index) + ": refused (" + refusal.key() + ")");
      }

      return refusal;
   }

   @Nullable
   private static RuleEngine.Refusal switchOnChecked(ServerPlayer player, PowerData data, int index) {
      CommandData rules = CommandData.of(player);
      RuleSlot slot = rules.slot(index);
      if (slot.isEmpty() || !slot.rule().resolves()) {
         return new RuleEngine.Refusal("awakened.feedback.command_no_rule");
      } else if (slot.on()) {
         return null;
      } else {
         int maxOn = CommandPower.maxOn(data.masteryLevel());
         if (rules.atOnLimit(maxOn)) {
            return new RuleEngine.Refusal("awakened.feedback.command_too_many_on", maxOn);
         } else {
            long now = CommandSupport.level(player).getGameTime();
            Rule rule = slot.rule();
            int mastery = data.masteryLevel();
            RuleEngine.Refusal locked = checkWritable(rule, mastery, MasteryScaling.abilityPower(player));
            if (locked != null) {
               return locked;
            } else {
               int points = CommandCost.points(rule, mastery);
               boolean paysOnSwitchOn = CommandCost.paysOnSwitchOn(rule.target().kind());
               float spent = 0.0F;
               if (paysOnSwitchOn) {
                  float price = billed((float)points);
                  if (!CommandStamina.canAfford(data, price)) {
                     return new RuleEngine.Refusal("awakened.feedback.no_stamina");
                  }

                  spent = CommandStamina.spend(player, data, price);
               }

               int ticks = !rule.target().kind().lands() && !rule.infinite() ? rule.durationTicks() : -1;
               state(player).sinceFold[index] = 0;
               CommandData.writeSlot(player, index, slot.switchedOn(now, ticks, spent));
               if (spent > 0.0F) {
                  CommandStamina.hold(player);
               }

               CommandTrace.say(
                  player,
                  CommandTrace.slot(index)
                     + ": ON \""
                     + rule.describe().getString()
                     + "\" costs "
                     + points
                     + (paysOnSwitchOn ? " (spent now)" : " per body it lands on")
                     + ", timer "
                     + (ticks < 0 ? "none (lands / forever)" : ticks + " ticks")
               );
               return null;
            }
         }
      }
   }

   static boolean resume(ServerPlayer player, PowerData data, int index, RuleSlot was) {
      CommandData rules = CommandData.of(player);
      RuleSlot slot = rules.slot(index);
      if (!slot.on()
         && !slot.isEmpty()
         && slot.rule().equals(was.rule())
         && slot.rule().resolves()
         && was.ticksLeft() != 0
         && !rules.atOnLimit(CommandPower.maxOn(data.masteryLevel()))
         && checkWritable(slot.rule(), data.masteryLevel(), MasteryScaling.abilityPower(player)) == null) {
         state(player).sinceFold[index] = 0;
         CommandData.writeSlot(player, index, slot.switchedOn(CommandSupport.level(player).getGameTime(), was.ticksLeft(), was.locked()));
         CommandTrace.say(player, CommandTrace.slot(index) + ": resumed after the seal, timer " + (was.ticksLeft() < 0 ? "none" : was.ticksLeft() + " ticks"));
         return true;
      } else {
         return false;
      }
   }

   public static void switchOff(ServerPlayer player, int index) {
      CommandData rules = CommandData.of(player);
      RuleSlot slot = rules.slot(index);
      if (slot.on()) {
         float wasHolding = heldBySlot(player, index);
         releaseSlotHolds(player, index);
         CommandData.writeSlot(player, index, slot.switchedOff(CommandSupport.level(player).getGameTime()));
         CommandStamina.release(player);
         CommandTrace.say(player, CommandTrace.slot(index) + ": OFF (" + Math.round(wasHolding) + " regenerates from here)");
      }
   }

   private static float heldBySlot(ServerPlayer player, int index) {
      return heldBySlot(player, index, CommandData.of(player).slot(index));
   }

   private static float heldBySlot(ServerPlayer player, int index, RuleSlot slot) {
      float total = slot.on() ? slot.locked() : 0.0F;

      for (Landings.Landing landing : Landings.of(player.getUUID())) {
         if (landing.slot == index) {
            total += landing.paid;
         }
      }

      return total;
   }

   private static void foldHolds(ServerPlayer player, RuleSlot[] work, RuleEngine.CasterState state) {
      if (++state.sinceHoldFold >= 20) {
         state.sinceHoldFold = 0;

         for (int i = 0; i < 5; i++) {
            RuleSlot slot = work[i];
            if (!slot.on()) {
               work[i] = slot.withHold(0.0F, 0, 0);
            } else {
               work[i] = slot.withHold(heldBySlot(player, i, slot), bodiesHeld(player, i, state), holdEndsIn(player, i, slot, state));
            }
         }
      }
   }

   private static void refreshHolds(ServerPlayer player) {
      RuleEngine.CasterState state = STATE.get(player.getUUID());
      if (state != null) {
         state.sinceHoldFold = 20;
      }
   }

   private static int bodiesHeld(ServerPlayer player, int index, RuleEngine.CasterState state) {
      int bodies = 0;

      for (Landings.Landing landing : Landings.of(player.getUUID())) {
         if (landing.slot == index) {
            bodies++;
         }
      }

      for (RuleEngine.AuraHold hold : state.auraHeld.get(index).values()) {
         if (hold.inside()) {
            bodies++;
         }
      }

      return bodies + (state.selfHeld[index] ? 1 : 0);
   }

   private static int holdEndsIn(ServerPlayer player, int index, RuleSlot slot, RuleEngine.CasterState state) {
      if (slot.rule().resolves() && slot.rule().target().kind().lands()) {
         int last = 0;
         boolean any = false;

         for (Landings.Landing landing : Landings.of(player.getUUID())) {
            if (landing.slot == index) {
               if (landing.bound()) {
                  return -1;
               }

               any = true;
               last = Math.max(last, landing.ticksLeft);
            }
         }

         return any ? Math.max(1, last) : 0;
      } else if (heldBySlot(player, index, slot) <= 0.0F) {
         return 0;
      } else {
         return slot.ticksLeft() < 0 ? -1 : Math.max(1, slot.ticksLeft() - state.sinceFold[index]);
      }
   }

   private static void releaseSlotHolds(ServerPlayer player, int index) {
      RuleEngine.CasterState state = STATE.get(player.getUUID());
      Rule rule = CommandData.of(player).slot(index).rule();
      Effect effect = rule.effect();
      if (state != null) {
         state.sinceFold[index] = 0;
         if (effect != null) {
            releaseSelf(player, rule, effect, state, index);
            releaseAura(player, rule, effect, state, index);
         }
      }

      MinecraftServer server = CommandSupport.server(player);

      for (Landings.Landing landing : List.copyOf(Landings.of(player.getUUID()))) {
         if (landing.slot == index) {
            Landings.expire(server, landing);
            Landings.drop(player.getUUID(), landing);
         }
      }
   }

   @Nullable
   public static RuleEngine.Refusal checkWritable(Rule rule, ServerPlayer player) {
      return checkWritable(rule, PowerService.data(player).masteryLevel(), MasteryScaling.abilityPower(player));
   }

   @Nullable
   public static RuleEngine.Refusal checkWritable(Rule rule, int masteryLevel, float abilityPower) {
      if (!rule.resolves()) {
         return new RuleEngine.Refusal("awakened.feedback.command_no_rule");
      } else if (rule.target().unlockLevel() > masteryLevel) {
         return new RuleEngine.Refusal("awakened.feedback.locked", rule.target().unlockLevel());
      } else if (rule.effect().unlockLevel() > masteryLevel) {
         return new RuleEngine.Refusal("awakened.feedback.locked", rule.effect().unlockLevel());
      } else if (rule.trigger().unlockLevel() > masteryLevel) {
         return new RuleEngine.Refusal("awakened.feedback.locked", rule.trigger().unlockLevel());
      } else if (!rule.effect().supports(rule.target().kind())) {
         return new RuleEngine.Refusal("awakened.feedback.command_mismatch");
      } else {
         Clash clash = Clash.of(rule.target(), rule.effect(), rule.trigger());
         if (clash != null) {
            return new RuleEngine.Refusal(clash.sayKey());
         } else {
            if (rule.infinite()) {
               if (!rule.allowsInfinite()) {
                  return new RuleEngine.Refusal("awakened.feedback.command_no_forever");
               }

               if (!CommandCost.allowsInfinite(masteryLevel)) {
                  return new RuleEngine.Refusal("awakened.feedback.locked", 11);
               }
            } else {
               if (rule.seconds() <= 0) {
                  return new RuleEngine.Refusal("awakened.feedback.command_no_seconds");
               }

               if (rule.seconds() > CommandCost.durationCap(masteryLevel)) {
                  return new RuleEngine.Refusal("awakened.feedback.command_over_cap", CommandCost.durationCap(masteryLevel));
               }
            }

            RuleEngine.Refusal level = checkNumber(
               rule.levelled(), rule.level(), CommandCost.strengthCap(masteryLevel, abilityPower), "awakened.feedback.command_over_strength"
            );
            return level != null ? level : checkNumber(rule.radial(), rule.radius(), CommandCost.radiusCap(masteryLevel), "awakened.feedback.command_over_cap");
         }
      }
   }

   @Nullable
   private static RuleEngine.Refusal checkNumber(boolean present, int number, int cap, String overKey) {
      if (!present) {
         return null;
      } else if (number < 1) {
         return new RuleEngine.Refusal("awakened.feedback.command_number_below_one");
      } else {
         return number > cap ? new RuleEngine.Refusal(overKey, cap) : null;
      }
   }

   private static void releaseSelf(ServerPlayer player, Rule rule, Effect effect, RuleEngine.CasterState state, int index) {
      if (state.selfHeld[index]) {
         state.selfHeld[index] = false;
         state.selfAge[index] = 0;
         effect.release(player);
      }
   }

   private static void releaseAura(ServerPlayer player, Rule rule, Effect effect, RuleEngine.CasterState state, int index) {
      Map<UUID, RuleEngine.AuraHold> held = state.auraHeld.get(index);
      state.auraDone.get(index).clear();
      if (!held.isEmpty()) {
         MinecraftServer server = CommandSupport.server(player);

         for (Entry<UUID, RuleEngine.AuraHold> entry : List.copyOf(held.entrySet())) {
            if (entry.getValue().inside()) {
               releaseBody(server, effect, entry.getKey(), rule);
            }
         }

         held.clear();
         refreshHolds(player);
      }
   }

   private static void parkAura(ServerPlayer player, Rule rule, Effect effect, RuleEngine.CasterState state, int index) {
      Map<UUID, RuleEngine.AuraHold> held = state.auraHeld.get(index);
      if (!held.isEmpty()) {
         MinecraftServer server = CommandSupport.server(player);
         long now = CommandSupport.level(player).getGameTime();
         boolean dropped = false;

         for (Entry<UUID, RuleEngine.AuraHold> entry : List.copyOf(held.entrySet())) {
            RuleEngine.AuraHold hold = entry.getValue();
            if (hold.inside()) {
               releaseBody(server, effect, entry.getKey(), rule);
               hold.graceUntil = now + 60L;
               dropped = true;
            }
         }

         if (dropped) {
            refreshHolds(player);
         }
      }
   }

   private static boolean holdsInside(RuleEngine.CasterState state, int index) {
      for (RuleEngine.AuraHold hold : state.auraHeld.get(index).values()) {
         if (hold.inside()) {
            return true;
         }
      }

      return false;
   }

   private static void releaseBody(@Nullable MinecraftServer server, Effect effect, UUID id, Rule rule) {
      if (CommandSupport.byUuid(server, id) instanceof LivingEntity body) {
         effect.release(body);
      } else {
         effect.forget(id);
      }
   }

   public static void clearCaster(ServerPlayer player) {
      for (int i = 0; i < 5; i++) {
         switchOff(player, i);
      }

      CommandState.clearCaster(CommandSupport.server(player), player.getUUID());
   }

   static void forgetCaster(UUID casterId) {
      STATE.remove(casterId);
      LAST_REFUSAL.remove(casterId);
   }

   static void forgetAll() {
      STATE.clear();
      LAST_REFUSAL.clear();
   }

   public static void onCasterDeath(ServerPlayer player) {
      RuleEngine.CasterState state = STATE.get(player.getUUID());
      CommandData rules = CommandData.of(player);
      if (state != null) {
         for (int i = 0; i < 5; i++) {
            Rule rule = rules.slot(i).rule();
            Effect effect = rule.effect();
            if (effect != null) {
               releaseSelf(player, rule, effect, state, i);
               releaseAura(player, rule, effect, state, i);
            }
         }
      }

      MinecraftServer server = CommandSupport.server(player);

      for (Landings.Landing landing : List.copyOf(Landings.of(player.getUUID()))) {
         if (landing.bound()) {
            Landings.expire(server, landing);
            landing.age = 0;
         } else {
            end(server, player, null, landing);
         }
      }
   }

   public static void onTargetDestroyed(@Nullable MinecraftServer server, UUID targetId) {
      if (server != null) {
         for (ServerPlayer caster : server.getPlayerList().getPlayers()) {
            for (Landings.Landing landing : List.copyOf(Landings.of(caster.getUUID()))) {
               if (landing.bound() && landing.target.equals(targetId)) {
                  end(server, caster, null, landing);
               }
            }
         }
      }
   }

   public static void onTargetGone(@Nullable MinecraftServer server, UUID targetId) {
      if (server != null) {
         for (ServerPlayer caster : server.getPlayerList().getPlayers()) {
            CommandData rules = CommandData.of(caster);

            for (int i = 0; i < 5; i++) {
               RuleSlot slot = rules.slot(i);
               if (slot.on() && targetId.equals(slot.boundTarget()) && !slot.boundDormant()) {
                  CommandData.writeSlot(caster, i, slot.withBind(targetId, true));
               }
            }
         }
      }
   }

   public static void clearAll(@Nullable MinecraftServer server) {
      CommandState.clearAll(server);
   }

   private static boolean award(ServerPlayer player, PowerData data, RuleEngine.CasterState state, int index, boolean worked, @Nullable LivingEntity victim) {
      if (!worked) {
         return false;
      } else {
         long now = CommandSupport.level(player).getGameTime();
         if (state.lastAward[index] != Long.MIN_VALUE && now - state.lastAward[index] < 20L) {
            return false;
         } else {
            state.lastAward[index] = now;
            if (victim != null && victim != player) {
               MasterySystem.award(player, MasteryReason.HIT, victim);
            } else {
               MasterySystem.awardUse(player);
            }

            return true;
         }
      }
   }

   public static void restoreAfterCrash(ServerPlayer player) {
      CommandData rules = CommandData.of(player);
      int mastery = PowerService.data(player).masteryLevel();

      for (int i = 0; i < 5; i++) {
         RuleSlot slot = rules.slot(i);
         if (slot.on() && slot.bound() && slot.rule().resolves() && !Landings.holds(player.getUUID(), i, slot.boundTarget())) {
            Landings.add(player.getUUID(), i, slot.boundTarget(), slot.rule(), -1, billed((float)CommandCost.points(slot.rule(), mastery)));
            CommandData.writeSlot(player, i, slot.withBind(slot.boundTarget(), true));
         }
      }
   }

   static boolean holds(ServerPlayer player, UUID body) {
      for (Landings.Landing landing : Landings.of(player.getUUID())) {
         if (landing.target.equals(body)) {
            return true;
         }
      }

      RuleEngine.CasterState state = STATE.get(player.getUUID());
      if (state != null) {
         for (Map<UUID, RuleEngine.AuraHold> aura : state.auraHeld) {
            RuleEngine.AuraHold hold = aura.get(body);
            if (hold != null && hold.inside()) {
               return true;
            }
         }
      }

      return false;
   }

   public static List<LivingEntity> affectedNow(ServerPlayer player) {
      Map<Integer, LivingEntity> out = new LinkedHashMap<>();
      MinecraftServer server = CommandSupport.server(player);

      for (Landings.Landing landing : Landings.of(player.getUUID())) {
         Entity var6 = CommandSupport.byUuid(server, landing.target);
         if (var6 instanceof LivingEntity) {
            LivingEntity body = (LivingEntity)var6;
            if (body.isAlive()) {
               out.put(body.getId(), body);
            }
         }
      }

      RuleEngine.CasterState state = STATE.get(player.getUUID());
      if (state != null) {
         CommandData rules = CommandData.of(player);

         for (int i = 0; i < 5; i++) {
            if (rules.slot(i).on()) {
               if (state.selfHeld[i]) {
                  out.put(player.getId(), player);
               }

               for (Entry<UUID, RuleEngine.AuraHold> entry : state.auraHeld.get(i).entrySet()) {
                  if (entry.getValue().inside()) {
                     Entity var9 = CommandSupport.byUuid(server, entry.getKey());
                     if (var9 instanceof LivingEntity) {
                        LivingEntity body = (LivingEntity)var9;
                        if (body.isAlive()) {
                           out.put(body.getId(), body);
                        }
                     }
                  }
               }
            }
         }
      }

      return new ArrayList<>(out.values());
   }

   private static float billed(float points) {
      return StaminaSystem.effectiveCost(points, RuleToggleAbility.class.getSimpleName());
   }

   public static float ruleDamage(float raw) {
      return MasteryScaling.configScaledDamage(raw, RuleToggleAbility.class.getSimpleName());
   }

   private static final class AuraHold {
      int age;
      boolean worked;
      long graceUntil = Long.MIN_VALUE;

      boolean inside() {
         return this.graceUntil == Long.MIN_VALUE;
      }
   }

   private static final class CasterState {
      final boolean[] selfHeld = new boolean[5];
      final int[] selfAge = new int[5];
      final long[] lastAward = new long[5];
      final int[] sinceFold = new int[5];
      int sinceHoldFold = 20;
      final List<Map<UUID, RuleEngine.AuraHold>> auraHeld = new ArrayList<>(5);
      final List<Set<UUID>> auraDone = new ArrayList<>(5);

      CasterState() {
         for (int i = 0; i < 5; i++) {
            this.auraHeld.add(new LinkedHashMap<>());
            this.auraDone.add(new LinkedHashSet<>());
            this.lastAward[i] = Long.MIN_VALUE;
         }
      }
   }

   @FunctionalInterface
   private interface HoldVisitor {
      void visit(UUID var1, Effect var2, int var3);
   }

   public static record Refusal(String key, Object... args) {
   }
}
