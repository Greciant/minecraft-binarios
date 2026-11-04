package com.awakenedmod.awakened.content.gamma;

import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.net.SolarHealthFxPayload;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.neoforged.neoforge.network.PacketDistributor;

public final class Rage {
   public static final String K_RAGE = "rage";
   private static final String K_LAST_COMBAT = "gamma_last_combat";
   private static final String K_GROWN = "gamma_grown";
   private static final float RAGE_PER_DAMAGE_TAKEN = 0.04F;
   private static final float RAGE_PER_HIT_DEALT = 0.02F;
   private static final float KILL_RAGE = 0.1F;
   private static final float RAGE_MAX = 1.0F;
   public static final float PEAK = 1.0F;
   private static final int COMBAT_WINDOW = 100;
   private static final float DECAY_PER_TICK = 0.006F;
   private static final float GROWTH_EASE = 0.12F;
   private static final float GROWTH_SNAP = 0.005F;
   private static final float TANK_RAGE = 0.7F;
   private static final float ENRAGED_KILL_RAGE = 0.45F;
   private static final double HEALTH_MAX = 16.0;
   private static final double ATTACK_MAX = 1.4;
   private static final double ATTACK_KB_MAX = 2.0;
   private static final double KB_RESIST_MAX = 1.0;
   private static final double SPEED_MAX = 0.15;
   private static final double STEP_MAX = 0.5;
   private static final double SAFE_FALL_MAX = 6.0;
   private static final double FALL_MULT_MAX = -0.6;
   private static final float DAMAGE_TAKEN_REDUCTION_MAX = 0.5F;
   private static final ResourceLocation HEALTH_ID = AwId.of("gamma_health");
   private static final ResourceLocation ATTACK_ID = AwId.of("gamma_attack");
   private static final ResourceLocation ATTACK_KB_ID = AwId.of("gamma_attack_knockback");
   private static final ResourceLocation KB_RESIST_ID = AwId.of("gamma_knockback_resist");
   private static final ResourceLocation SPEED_ID = AwId.of("gamma_speed");
   private static final ResourceLocation STEP_ID = AwId.of("gamma_step_height");
   private static final ResourceLocation SAFE_FALL_ID = AwId.of("gamma_safe_fall");
   private static final ResourceLocation FALL_MULT_ID = AwId.of("gamma_fall_mult");
   private static final Map<UUID, Integer> LAST_APPLIED = new ConcurrentHashMap<>();
   private static final Map<UUID, Integer> LAST_HP = new ConcurrentHashMap<>();

   private Rage() {
   }

   public static float rage(PowerData data) {
      return data.var("rage", 0.0F);
   }

   public static float incomingMultiplier(PowerData data) {
      float r = rage(data);
      return 1.0F - 0.5F * r * (2.0F - r);
   }

   public static void onDamageTaken(ServerPlayer player, PowerData data, DamageSource source, float amount) {
      if (!(amount <= 0.0F)) {
         add(data, 0.04F * amount, 1.0F);
         stampCombat(player, data);
         if (rage(data) >= 0.7F && Foes.threat(source, player)) {
            MasterySystem.award(player, MasteryReason.SURVIVAL);
         }

         broadcast(player, data);
      }
   }

   public static void onDamageDealt(ServerPlayer player, PowerData data) {
      add(data, 0.02F, 1.0F);
      stampCombat(player, data);
      broadcast(player, data);
   }

   public static void onKill(ServerPlayer player, PowerData data) {
      boolean enraged = rage(data) >= 0.45F;
      add(data, 0.1F, 1.0F);
      stampCombat(player, data);
      if (enraged) {
         MasterySystem.award(player, MasteryReason.KILL);
      }

      broadcast(player, data);
   }

   public static float provoke(ServerPlayer player, PowerData data, float amount, float cap) {
      float before = rage(data);
      float added = 0.0F;
      if (before < cap) {
         add(data, amount, cap);
         added = rage(data) - before;
      }

      stampCombat(player, data);
      broadcast(player, data);
      return added;
   }

   private static void add(PowerData data, float delta, float cap) {
      data.setVar("rage", Math.min(cap, Math.max(0.0F, rage(data) + delta)));
   }

   private static void stampCombat(ServerPlayer player, PowerData data) {
      data.setVar("gamma_last_combat", (float)player.serverLevel().getGameTime());
   }

   public static boolean tick(ServerPlayer player, PowerData data) {
      float rage = rage(data);
      float before = rage;
      long now = player.serverLevel().getGameTime();
      long lastCombat = (long)data.var("gamma_last_combat", 0.0F);
      if (rage > 0.0F && now - lastCombat > 100L) {
         rage = Math.max(0.0F, rage - 0.006F);
         data.setVar("rage", rage);
      }

      float grown = data.var("gamma_grown", 0.0F);
      grown += (rage - grown) * 0.12F;
      if (Math.abs(rage - grown) < 0.005F) {
         grown = rage;
      }

      data.setVar("gamma_grown", grown);
      applyAttributes(player, rage, grown);
      return Math.abs(rage - before) > 1.0E-4F;
   }

   private static void applyAttributes(ServerPlayer player, float rage, float grown) {
      int key = Math.round(rage * 100.0F) * 1000 + Math.round(grown * 200.0F);
      Integer last = LAST_APPLIED.get(player.getUUID());
      if (last == null || last != key) {
         LAST_APPLIED.put(player.getUUID(), key);
         GammaFx.applyGrowth(player, grown);
         adjustMaxHealth(player, rage);
         setModifier(player, Attributes.ATTACK_DAMAGE, ATTACK_ID, 1.4 * (double)rage, Operation.ADD_MULTIPLIED_TOTAL);
         setModifier(player, Attributes.ATTACK_KNOCKBACK, ATTACK_KB_ID, 2.0 * (double)rage, Operation.ADD_VALUE);
         setModifier(player, Attributes.KNOCKBACK_RESISTANCE, KB_RESIST_ID, 1.0 * (double)rage, Operation.ADD_VALUE);
         setModifier(player, Attributes.MOVEMENT_SPEED, SPEED_ID, 0.15 * (double)rage, Operation.ADD_MULTIPLIED_TOTAL);
         setModifier(player, Attributes.STEP_HEIGHT, STEP_ID, 0.5 * (double)rage, Operation.ADD_VALUE);
         setModifier(player, Attributes.SAFE_FALL_DISTANCE, SAFE_FALL_ID, 6.0 * (double)rage, Operation.ADD_VALUE);
         setModifier(player, Attributes.FALL_DAMAGE_MULTIPLIER, FALL_MULT_ID, -0.6 * (double)rage, Operation.ADD_MULTIPLIED_TOTAL);
      }
   }

   private static void adjustMaxHealth(ServerPlayer player, float rage) {
      int target = (int)(Math.round((double)Math.max(0.0F, rage) * 16.0 / 2.0) * 2L);
      int last = LAST_HP.getOrDefault(player.getUUID(), 0);
      if (target != last) {
         int delta = target - last;
         notifyBenignHealth(player);
         float before = player.getHealth();
         setModifier(player, Attributes.MAX_HEALTH, HEALTH_ID, (double)target, Operation.ADD_VALUE);
         if (player.isAlive()) {
            player.setHealth(Math.max(1.0F, Math.min(before + (float)delta, player.getMaxHealth())));
         }

         LAST_HP.put(player.getUUID(), target);
      }
   }

   private static void notifyBenignHealth(ServerPlayer player) {
      if (player.connection != null) {
         PacketDistributor.sendToPlayer(player, new SolarHealthFxPayload(), new CustomPacketPayload[0]);
      }
   }

   private static void setModifier(ServerPlayer player, Holder<Attribute> attr, ResourceLocation id, double amount, Operation op) {
      AttributeInstance inst = player.getAttribute(attr);
      if (inst != null) {
         inst.removeModifier(id);
         if (Math.abs(amount) > 1.0E-4) {
            inst.addTransientModifier(new AttributeModifier(id, amount, op));
         }
      }
   }

   private static void removeModifier(ServerPlayer player, Holder<Attribute> attr, ResourceLocation id) {
      AttributeInstance inst = player.getAttribute(attr);
      if (inst != null) {
         inst.removeModifier(id);
      }
   }

   public static void broadcast(ServerPlayer player, PowerData data) {
      GammaFx.broadcastRage(player, rage(data));
   }

   public static void clear(ServerPlayer player) {
      if (LAST_HP.remove(player.getUUID()) != null) {
         notifyBenignHealth(player);
      }

      removeModifier(player, Attributes.MAX_HEALTH, HEALTH_ID);
      removeModifier(player, Attributes.ATTACK_DAMAGE, ATTACK_ID);
      removeModifier(player, Attributes.ATTACK_KNOCKBACK, ATTACK_KB_ID);
      removeModifier(player, Attributes.KNOCKBACK_RESISTANCE, KB_RESIST_ID);
      removeModifier(player, Attributes.MOVEMENT_SPEED, SPEED_ID);
      removeModifier(player, Attributes.STEP_HEIGHT, STEP_ID);
      removeModifier(player, Attributes.SAFE_FALL_DISTANCE, SAFE_FALL_ID);
      removeModifier(player, Attributes.FALL_DAMAGE_MULTIPLIER, FALL_MULT_ID);
      GammaFx.clearGrowth(player);
      LAST_APPLIED.remove(player.getUUID());
      PowerData data = PowerService.data(player);
      data.setVar("rage", 0.0F);
      data.setVar("gamma_grown", 0.0F);
      data.setVar("gamma_last_combat", 0.0F);
      GammaFx.broadcastRage(player, 0.0F);
   }
}
