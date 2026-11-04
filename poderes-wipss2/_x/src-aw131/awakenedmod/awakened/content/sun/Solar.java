package com.awakenedmod.awakened.content.sun;

import com.awakenedmod.awakened.content.AwakenedItems;
import com.awakenedmod.awakened.content.AwakenedParticles;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Headroom;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.net.SolarHealthFxPayload;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class Solar {
   public static final float NORMAL_DAY = 1.0F;
   public static final float THE_ONE = 2.0F;
   public static final float MAX_POWER = 2.3F;
   public static final float ONE_THRESHOLD = 1.7F;
   public static final float GROWTH_START = 1.0F;
   public static final float MAX_SCALE_BONUS = 1.0F;
   private static final double SIZE_STEP = 0.1;
   private static final float PSUN_FLOOR = 0.8F;
   private static final double PSUN_RANGE = 18.0;
   private static final float MIDDAY_PLATEAU = 0.8F;
   public static final float STEP = 0.25F;
   public static final int MAX_STEPS = 4;
   private static final float AXE_BASE = 0.3F;
   private static final float DRAIN_PER_STEP = 0.14F;
   private static final float RISE = 0.06F;
   private static final float FALL = 0.04F;
   private static final double HEALTH_PER_POWER = 6.0;
   private static final double SPEED_PER_POWER = 0.18;
   private static final float SPEED_FROM = 0.6F;
   private static final double ATTACK_PER_POWER = 4.0;
   private static final double AXE_ATTACK = 8.0;
   private static final double REACH_PER_SIZE = 2.0;
   private static final float SHRINK_AT_ZERO = 0.8F;
   private static final float WEAK_BELOW = 0.25F;
   private static final double NIGHT_ATTACK = 2.0;
   private static final float RESIST_FROM = 0.6F;
   private static final String K_POWER = "sun_power";
   private static final String K_STEPS = "sun_steps";
   private static final String K_PSUN = "sun_psun_until";
   private static final String K_PSX = "sun_psun_x";
   private static final String K_PSY = "sun_psun_y";
   private static final String K_PSZ = "sun_psun_z";
   private static final String K_ONE_DAY = "sun_one_day";
   private static final float NO_DAY = -1.0F;
   private static final long DAY_TICKS = 24000L;
   private static final ResourceLocation SCALE_ID = id("sun_scale");
   private static final ResourceLocation SPEED_ID = id("sun_speed");
   private static final ResourceLocation ATTACK_ID = id("sun_attack");
   private static final ResourceLocation REACH_ID = id("sun_reach");
   private static final ResourceLocation BLOCK_REACH_ID = id("sun_block_reach");
   private static final ResourceLocation HEALTH_ID = id("sun_health");
   private static final Map<UUID, Float> LAST_APPLIED = new ConcurrentHashMap<>();
   private static final Map<UUID, Integer> LAST_HP = new ConcurrentHashMap<>();
   private static final Map<UUID, Float> LAST_CEILING = new ConcurrentHashMap<>();

   private Solar() {
   }

   private static ResourceLocation id(String path) {
      return ResourceLocation.fromNamespaceAndPath("awakened", path);
   }

   public static float level(PowerData data) {
      return data.var("sun_power", 0.0F);
   }

   public static float normalized(PowerData data) {
      return Mth.clamp(level(data) / 1.0F, 0.0F, 2.3F);
   }

   public static boolean isOne(PowerData data) {
      return level(data) >= 1.7F;
   }

   public static int steps(PowerData data) {
      return Math.round(data.var("sun_steps", 0.0F));
   }

   public static float scale(PowerData data, float dim, float bright) {
      return dim + (bright - dim) * level(data);
   }

   public static float scale(ServerPlayer player, float dim, float bright) {
      return scale(PowerService.data(player), dim, bright);
   }

   public static float ascensionMark(ServerPlayer player, PowerData data) {
      float active = (float)steps(data) * 0.25F;
      float ascension = (hasAxe(player) ? 0.3F : 0.0F) + active;
      if (ascension <= 0.0F) {
         return 0.0F;
      } else {
         float activeShare = level(data) * (active / (1.0F + ascension));
         return Mth.clamp(activeShare / 1.0F, 0.0F, 2.3F);
      }
   }

   public static boolean personalSun(ServerPlayer player, PowerData data) {
      return player.serverLevel().getGameTime() < (long)data.var("sun_psun_until", 0.0F);
   }

   public static boolean personalSunOnYou(ServerPlayer player, PowerData data) {
      if (!personalSun(player, data)) {
         return false;
      } else {
         Vec3 sun = personalSunPos(data);
         double dx = player.getX() - sun.x;
         double dz = player.getZ() - sun.z;
         return dx * dx + dz * dz <= 324.0;
      }
   }

   public static Vec3 personalSunPos(PowerData data) {
      return new Vec3((double)data.var("sun_psun_x", 0.0F), (double)data.var("sun_psun_y", 0.0F), (double)data.var("sun_psun_z", 0.0F));
   }

   public static boolean hasAxe(Player player) {
      return isAxe(player.getMainHandItem()) || isAxe(player.getOffhandItem());
   }

   public static boolean hasSunPower(ServerPlayer player) {
      return "sun".equals(PowerService.data(player).powerId());
   }

   private static boolean isAxe(ItemStack stack) {
      return AwakenedItems.SUN_AXE != null && stack.is((Item)AwakenedItems.SUN_AXE.get());
   }

   public static double attackBonus(float power, boolean axe) {
      return 4.0 * (double)Math.max(0.0F, power) + (axe ? axeAttack(power) : 0.0) - nightPenalty(power);
   }

   private static double nightPenalty(float power) {
      return power >= 0.25F ? 0.0 : 2.0 * (1.0 - (double)(Math.max(0.0F, power) / 0.25F));
   }

   private static double axeAttack(float power) {
      return 8.0 * (double)Mth.clamp(power / 1.0F, 0.0F, 1.0F);
   }

   public static void ascend(PowerData data, int delta) {
      data.setVar("sun_steps", (float)Mth.clamp(steps(data) + delta, 0, 4));
   }

   public static void lightPersonalSun(ServerPlayer player, PowerData data, int durationTicks, Vec3 pos) {
      data.setVar("sun_psun_until", (float)(player.serverLevel().getGameTime() + (long)durationTicks));
      data.setVar("sun_psun_x", (float)pos.x);
      data.setVar("sun_psun_y", (float)pos.y);
      data.setVar("sun_psun_z", (float)pos.z);
   }

   public static void extinguishPersonalSun(PowerData data) {
      data.setVar("sun_psun_until", 0.0F);
   }

   public static void update(ServerPlayer player, PowerData data) {
      if (player.isAlive()) {
         float exposure = exposure(player, data);
         int steps = steps(data);
         if (exposure <= 0.02F && steps > 0) {
            steps = 0;
            data.setVar("sun_steps", 0.0F);
         }

         float active = (float)steps * 0.25F;
         float ascension = (hasAxe(player) ? 0.3F : 0.0F) + active;
         float target = exposure * (1.0F + ascension);
         float cur = approach(level(data), target, level(data) < target ? 0.06F : 0.04F);
         if (cur < 0.001F) {
            cur = 0.0F;
         }

         data.setVar("sun_power", cur);
         creditZenith(player, data, cur);
         if (steps > 0 && exposure > 0.02F && !data.infiniteStamina()) {
            float left = StaminaSystem.drainSustained(
               data, player.serverLevel().getGameTime(), StaminaSystem.effectiveCost((float)steps * 0.14F, SolarAscensionAbility.class.getSimpleName())
            );
            if (left <= 0.0F) {
               data.setVar("sun_steps", 0.0F);
            }
         }

         applyAttributes(player, cur);
         applyEffects(player, cur);
         emitGlow(player, cur);
      }
   }

   private static float approach(float from, float to, float rate) {
      return from < to ? Math.min(to, from + rate) : Math.max(to, from - rate);
   }

   private static void creditZenith(ServerPlayer player, PowerData data, float power) {
      if (!(power < 1.7F)) {
         float day = (float)(player.serverLevel().getGameTime() / 24000L);
         if (data.var("sun_one_day", -1.0F) != day) {
            data.setVar("sun_one_day", day);
            QuestSystem.feat(player, "sun_zenith");
         }
      }
   }

   public static float realAmbient(ServerPlayer player) {
      Level level = player.serverLevel();
      return !level.dimensionType().hasSkyLight() ? 0.0F : dayCurve(level);
   }

   private static float dayCurve(Level level) {
      float frac = (float)(level.getDayTime() % 24000L) / 24000.0F;
      float h = (float)Math.sin((double)frac * Math.PI * 2.0);
      if (h <= 0.0F) {
         return 0.0F;
      } else {
         float day = Math.min(0.8F, h * 1.6F);
         float noonBump = 0.19999999F * Mth.clamp((h - 0.85F) / 0.15F, 0.0F, 1.0F);
         return Mth.clamp(day + noonBump, 0.0F, 1.0F);
      }
   }

   public static float openSunlight(ServerPlayer player) {
      float day = realAmbient(player);
      if (day <= 0.0F) {
         return 0.0F;
      } else {
         Level level = player.serverLevel();
         BlockPos pos = player.blockPosition().above();
         float skyAccess = (float)level.getBrightness(LightLayer.SKY, pos) / 15.0F;
         float weather = level.isThundering() ? 0.35F : (level.isRaining() ? 0.7F : 1.0F);
         return Mth.clamp(day * weather * skyAccess, 0.0F, 1.0F);
      }
   }

   public static float exposure(ServerPlayer player, PowerData data) {
      float real = realAmbient(player);
      return personalSunOnYou(player, data) ? Math.max(real, 0.8F) : real;
   }

   private static void applyAttributes(ServerPlayer player, float power) {
      boolean axe = hasAxe(player);
      double grow = growth(player, power);
      float q = (float)Math.round(power * 10.0F) / 10.0F;
      float key = q + (axe ? 2000.0F : 0.0F) + (float)Math.round(grow * 10.0) * 100.0F;
      Float last = LAST_APPLIED.get(player.getUUID());
      if (last == null || last != key) {
         LAST_APPLIED.put(player.getUUID(), key);
         double speed = (double)Math.max(0.0F, power - 0.6F) * 0.18;
         setModifier(player, Attributes.MOVEMENT_SPEED, SPEED_ID, speed, Operation.ADD_MULTIPLIED_TOTAL);
         setModifier(player, Attributes.SCALE, SCALE_ID, grow, Operation.ADD_VALUE);
         setModifier(player, Attributes.ATTACK_DAMAGE, ATTACK_ID, attackBonus(power, axe), Operation.ADD_VALUE);
         double reach = Math.max(0.0, grow) * 2.0;
         setModifier(player, Attributes.ENTITY_INTERACTION_RANGE, REACH_ID, reach, Operation.ADD_VALUE);
         setModifier(player, Attributes.BLOCK_INTERACTION_RANGE, BLOCK_REACH_ID, reach, Operation.ADD_VALUE);
         adjustMaxHealth(player, power);
      }
   }

   private static double growth(ServerPlayer player, float power) {
      if (power < 0.8F) {
         return -0.19999999F * (1.0 - (double)(power / 0.8F));
      } else {
         double grow = (double)(Mth.clamp((power - 1.0F) / 1.0F, 0.0F, 1.0F) * 1.0F);
         if (grow <= 0.0) {
            return grow;
         } else {
            double capped = Math.min(grow, ceiling(player) - 1.0);
            return Math.floor(capped / 0.1 + 1.0E-6) * 0.1;
         }
      }
   }

   private static double ceiling(ServerPlayer player) {
      if (player.onGround()) {
         double measured = Headroom.standingCeiling(player, 2.0);
         LAST_CEILING.put(player.getUUID(), (float)measured);
         return measured;
      } else {
         Float held = LAST_CEILING.get(player.getUUID());
         return held != null ? (double)held.floatValue() : 2.0;
      }
   }

   private static void adjustMaxHealth(ServerPlayer player, float power) {
      int target = (int)(Math.round(Math.max(0.0, (double)power) * 6.0 / 2.0) * 2L);
      int last = LAST_HP.getOrDefault(player.getUUID(), 0);
      if (target != last) {
         int delta = target - last;
         notifyBenignHealth(player);
         float before = player.getHealth();
         setModifier(player, Attributes.MAX_HEALTH, HEALTH_ID, (double)target, Operation.ADD_VALUE);
         player.setHealth(Mth.clamp(before + (float)delta, 1.0F, player.getMaxHealth()));
         LAST_HP.put(player.getUUID(), target);
      }
   }

   private static void notifyBenignHealth(ServerPlayer player) {
      PacketDistributor.sendToPlayer(player, new SolarHealthFxPayload(), new CustomPacketPayload[0]);
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

   private static void applyEffects(ServerPlayer player, float power) {
      if (player.tickCount % 40 == 0) {
         int amp = power >= 1.7F ? 2 : (power >= 1.0F ? 1 : (power >= 0.6F ? 0 : -1));
         if (amp >= 0) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, amp, true, false, false));
         }

         if (power >= 1.0F) {
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 60, 0, true, false, false));
         }
      }
   }

   private static void emitGlow(ServerPlayer player, float power) {
      if (!(power < 0.25F)) {
         ServerLevel level = player.serverLevel();
         if (player.tickCount % Math.max(1, (int)(5.0F - power * 2.0F)) == 0) {
            int count = 1 + Math.round(power * 3.0F);
            double spread = 0.22 + (double)power * 0.18;
            Fx.burst(level, (ParticleOptions)AwakenedParticles.SUN_MOTE.get(), player.position().add(0.0, 1.0, 0.0), count, spread, 0.01);
            if (power >= 1.2F) {
               Fx.burst(level, ParticleTypes.FLAME, player.position().add(0.0, 1.0, 0.0), 1, 0.25, 0.0);
            }
         }
      }
   }

   public static void clear(ServerPlayer player) {
      removeModifier(player, Attributes.MOVEMENT_SPEED, SPEED_ID);
      removeModifier(player, Attributes.SCALE, SCALE_ID);
      removeModifier(player, Attributes.ATTACK_DAMAGE, ATTACK_ID);
      removeModifier(player, Attributes.ENTITY_INTERACTION_RANGE, REACH_ID);
      removeModifier(player, Attributes.BLOCK_INTERACTION_RANGE, BLOCK_REACH_ID);
      LAST_APPLIED.remove(player.getUUID());
      LAST_CEILING.remove(player.getUUID());
      dropBonusHearts(player);
      player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
      player.removeEffect(MobEffects.FIRE_RESISTANCE);
      PowerData data = PowerService.data(player);
      data.setVar("sun_power", 0.0F);
      data.setVar("sun_steps", 0.0F);
   }

   private static void dropBonusHearts(ServerPlayer player) {
      Integer lent = LAST_HP.remove(player.getUUID());
      AttributeInstance hp = player.getAttribute(Attributes.MAX_HEALTH);
      if (hp != null) {
         boolean owed = lent != null && lent > 0 && hp.hasModifier(HEALTH_ID) && player.isAlive();
         if (lent != null && player.connection != null) {
            notifyBenignHealth(player);
         }

         float before = player.getHealth();
         hp.removeModifier(HEALTH_ID);
         if (owed) {
            player.setHealth(Mth.clamp(before - (float)lent.intValue(), 1.0F, player.getMaxHealth()));
         }
      }
   }

   private static void removeModifier(ServerPlayer player, Holder<Attribute> attr, ResourceLocation id) {
      AttributeInstance inst = player.getAttribute(attr);
      if (inst != null) {
         inst.removeModifier(id);
      }
   }
}
