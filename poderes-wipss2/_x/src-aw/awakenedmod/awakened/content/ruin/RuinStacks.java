package com.awakenedmod.awakened.content.ruin;

import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.PowerSuppression;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class RuinStacks {
   public static final ResourceKey<DamageType> DECAY_TYPE = ResourceKey.create(Registries.DAMAGE_TYPE, AwId.of("ruin_decay"));
   private static final Map<LivingEntity, RuinStacks.Track> TRACKED = new ConcurrentHashMap<>();
   private static int lastTick = -1;

   private RuinStacks() {
   }

   public static boolean add(LivingEntity target, int n, ServerPlayer caster, Vec3 hitPoint) {
      if (n > 0 && accepts(target, caster) && !PowerSuppression.isSuppressed(caster) && target.level() instanceof ServerLevel level) {
         long var12 = level.getGameTime();
         RuinDecay old = get(target);
         float ox = old.originX();
         float oy = old.originY();
         float oz = old.originZ();
         if (!old.active()) {
            Vec3 local = RuinDecay.toBodyFrame(hitPoint.subtract(target.position()), target.yBodyRot);
            ox = (float)local.x;
            oy = (float)local.y;
            oz = (float)local.z;
         }

         int stacks = (int)Math.min(2147483647L, (long)old.stacks() + (long)n);
         set(target, new RuinDecay(stacks, caster.getUUID(), var12, ox, oy, oz, old.pulseSeq() + 1, (byte)1, var12, n));
         RuinFeats.stacked(caster, target, old.stacks(), stacks);
         TRACKED.computeIfAbsent(target, t -> new RuinStacks.Track(var12 + 20L));
         return true;
      } else {
         return false;
      }
   }

   public static boolean accepts(LivingEntity target, ServerPlayer caster) {
      return canCarry(target) && target != caster && Harm.allowed(caster, target);
   }

   public static float payout(LivingEntity target, float perStack) {
      RuinDecay d = get(target);
      if (d.active() && !(perStack <= 0.0F) && target.level() instanceof ServerLevel level) {
         float var5 = perStack * (float)d.stacks();
         set(target, d.withPulse((byte)2, level.getGameTime(), d.stacks()));
         Dmg.hurtThrough(target, level, decaySource(level, applierOnline(level, d)), var5);
         return var5;
      } else {
         return 0.0F;
      }
   }

   public static RuinDecay get(LivingEntity target) {
      return (RuinDecay)target.getData(RuinAttachments.DECAY);
   }

   public static int stacks(LivingEntity target) {
      return get(target).stacks();
   }

   public static void clear(LivingEntity target) {
      TRACKED.remove(target);
      RuinDecay d = get(target);
      if (d.active() && target.level() instanceof ServerLevel level) {
         set(target, d.withPulse((byte)4, level.getGameTime(), d.stacks()).withStacks(0));
      }
   }

   public static void clearAllBy(UUID applier) {
      for (LivingEntity target : List.copyOf(TRACKED.keySet())) {
         if (applier.equals(get(target).applier())) {
            clear(target);
         }
      }
   }

   public static Vec3 touchPoint(Entity toucher, LivingEntity target) {
      Vec3 eye = toucher.getEyePosition();
      AABB box = target.getBoundingBox();
      double reach = eye.distanceTo(target.position()) + box.getSize() + 1.0;
      Vec3 far = eye.add(toucher.getLookAngle().scale(reach));
      return box.clip(eye, far).orElseGet(() -> new Vec3(clamp(eye.x, box.minX, box.maxX), clamp(eye.y, box.minY, box.maxY), clamp(eye.z, box.minZ, box.maxZ)));
   }

   static void tickAll(MinecraftServer server) {
      int tick = server.getTickCount();
      if (tick != lastTick) {
         lastTick = tick;
         Iterator<Entry<LivingEntity, RuinStacks.Track>> it = TRACKED.entrySet().iterator();

         while (it.hasNext()) {
            Entry<LivingEntity, RuinStacks.Track> e = it.next();
            LivingEntity target = e.getKey();
            if (target.isAlive() && !target.isRemoved() && target.level() instanceof ServerLevel level) {
               RuinDecay d = get(target);
               if (!d.active()) {
                  it.remove();
               } else {
                  long now = level.getGameTime();
                  if (now >= d.expiresAt()) {
                     it.remove();
                     set(target, d.withPulse((byte)3, now, d.stacks()).withStacks(0));
                  } else {
                     RuinStacks.Track track = e.getValue();
                     if (now >= track.nextDot) {
                        track.nextDot = now + 20L;
                        dot(level, target, d, track);
                     }
                  }
               }
            } else {
               it.remove();
            }
         }
      }
   }

   private static void dot(ServerLevel level, LivingEntity target, RuinDecay d, RuinStacks.Track track) {
      ServerPlayer applier = applierOnline(level, d);
      float amount = 0.25F * (float)d.stacks();
      if (applier != null) {
         amount = MasteryScaling.scaledDamage(amount, applier, "RuinDecay");
      } else {
         amount = MasteryScaling.configScaledDamage(amount, "RuinDecay");
      }

      if (Dmg.hurtTick(target, level, decaySource(level, applier), amount)) {
         track.dotTicks++;
         if (applier != null && isRuin(applier) && Integer.bitCount(track.dotTicks) == 1) {
            MasterySystem.award(applier, MasteryReason.HIT, target);
         }
      }
   }

   static void clearAll() {
      TRACKED.clear();
      lastTick = -1;
   }

   private static boolean canCarry(LivingEntity target) {
      if (target.isAlive() && !target.isRemoved() && !(target instanceof ArmorStand)) {
         if (target instanceof Player p && (p.isCreative() || p.isSpectator())) {
            return false;
         }

         return true;
      } else {
         return false;
      }
   }

   static boolean isRuin(ServerPlayer player) {
      PowerData data = PowerService.data(player);
      return data.hasPower() && "ruin".equals(data.powerId());
   }

   @Nullable
   private static ServerPlayer applierOnline(ServerLevel level, RuinDecay d) {
      UUID id = d.applier();
      return id == null ? null : level.getServer().getPlayerList().getPlayer(id);
   }

   private static DamageSource decaySource(ServerLevel level, @Nullable ServerPlayer applier) {
      Holder<DamageType> type = level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DECAY_TYPE);
      return new DamageSource(type, null, applier);
   }

   private static void set(LivingEntity target, RuinDecay next) {
      target.setData(RuinAttachments.DECAY, next);
   }

   private static double clamp(double v, double lo, double hi) {
      return Math.max(lo, Math.min(hi, v));
   }

   private static final class Track {
      long nextDot;
      int dotTicks;

      Track(long nextDot) {
         this.nextDot = nextDot;
      }
   }
}
