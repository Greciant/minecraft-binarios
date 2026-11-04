package com.devilfruits.fruits.horo_horo.ability;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.haki.HakiCombat;
import com.devilfruits.core.haki.HakiConfig;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.core.rules.Mastery;
import com.devilfruits.core.util.ServerScheduler;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.fruits.horo_horo.HollowFlight;
import com.devilfruits.fruits.horo_horo.HoroAbilities;
import com.devilfruits.fruits.horo_horo.HoroFx;
import com.devilfruits.fruits.horo_horo.HoroHoro;
import com.devilfruits.fruits.horo_horo.HoroSounds;
import com.devilfruits.fruits.horo_horo.HoroTriggers;
import com.devilfruits.fruits.kage_kage.KageAim;
import com.devilfruits.registry.DFAttachments;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class MiniHollowAbility extends Ability {
   public static final int EMERGE_TICKS = 5;
   public static final int CLAP_TICKS = 6;
   public static final int MAX_FLIGHT = 80;
   private static final Map<UUID, MiniHollowAbility.Rap> RAPS = new HashMap<>();

   public MiniHollowAbility() {
      super(
         HoroHoro.id("mini_hollow"),
         CastType.TOGGLE,
         AbilityStats.builder()
            .energy(22)
            .cooldown(200)
            .mastery(2)
            .damage(14.0)
            .range(24.0)
            .knockback(0.45)
            .extra("ghosts", 4.0, 1.0, 12.0, "Little ghosts sent out (the damage is shared between them).")
            .extra("attachSeconds", 8.0, 1.0, 60.0, "Seconds the ghosts cling before they pop by themselves.")
            .extra("timeoutFactor", 0.5, 0.0, 1.0, "Share of the damage the ghosts deal when they pop by themselves.")
            .extra("speed", 1.0, 0.2, 4.0, "Blocks per tick the little ghosts fly.")
            .extra("aimConeDegrees", 8.0, 0.0, 45.0, "Aim assist: half-angle of the cone round the crosshair a target may stand in.")
            .build()
      );
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.NONE;
   }

   @Override
   public boolean coatable() {
      return true;
   }

   @Override
   public boolean pausesRegeneration() {
      return false;
   }

   @Nullable
   public static MiniHollowAbility.Rap rap(ServerPlayer player) {
      return RAPS.get(player.getUUID());
   }

   @Nullable
   @Override
   public Component checkRequirements(ServerPlayer player, PlayerFruitData data) {
      if (RAPS.containsKey(player.getUUID())) {
         return Component.translatable("message.devilfruits.horo_horo.ghosts_out");
      } else {
         double range = this.config().range() * (double)Mastery.rangeMultiplier(data.mastery());
         return KageAim.aimed(player, range, this.config().extra("aimConeDegrees")) == null
            ? Component.translatable("message.devilfruits.horo_horo.no_target")
            : null;
      }
   }

   @Override
   public void onToggle(AbilityContext ctx, boolean active) {
      ServerPlayer player = ctx.player();
      if (active) {
         if (!this.launch(ctx)) {
            AbilityManager.stopToggle(player, this);
         }
      } else {
         MiniHollowAbility.Rap rap = RAPS.get(player.getUUID());
         if (rap != null && rap.pop == 0) {
            clap(rap, 1);
         }
      }
   }

   @Override
   public boolean cooldownOnToggleOff(ServerPlayer player) {
      return true;
   }

   private boolean launch(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      LivingEntity target = KageAim.aimed(player, (double)ctx.range(), ctx.extra("aimConeDegrees"));
      if (target == null) {
         return false;
      } else {
         ServerLevel level = ctx.level();
         int n = (int)ctx.extra("ghosts");
         boolean coat = HakiCombat.coatsTechnique(player);
         MiniHollowAbility.Rap rap = new MiniHollowAbility.Rap(player, target, n, ctx.damage() / (float)n, ctx.knockback(), coat);
         rap.launched = level.getGameTime();
         Vec3 look = ctx.look();
         Vec3 chest = chest(player, look);
         long seed = ctx.seed() & -16L | (long)Math.min(n, 15);
         double speed = ctx.extra("speed");
         layout(seed, n, chest, look, speed, rap.flights, rap.offsets);
         RAPS.put(player.getUUID(), rap);
         ServerFx.animate(player, HoroHoro.id("throw_rap"));
         ServerFx.send(player, HoroFx.MINI_LAUNCH, chest, look, (float)speed, seed, target.getId());
         level.playSound(null, chest.x, chest.y, chest.z, HoroSounds.GIGGLE.get(), SoundSource.PLAYERS, 0.9F, 1.3F);
         ServerScheduler.schedule(level, 5, age -> tick(level, rap));
         return true;
      }
   }

   public static Vec3 chest(Player player, Vec3 look) {
      return player.getEyePosition().add(look.scale(0.7)).add(0.0, -0.45, 0.0);
   }

   public static void layout(long seed, int n, Vec3 chest, Vec3 look, double speed, HollowFlight[] flights, Vec3[] offsets) {
      Vec3 right = new Vec3(-look.z, 0.0, look.x);
      right = right.lengthSqr() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : right.normalize();
      RandomSource r = RandomSource.create(seed);

      for (int i = 0; i < n; i++) {
         double side = (double)(i % 2 == 0 ? 1 : -1) * (0.45 + 0.1 * (double)(i / 2));
         Vec3 heading = look.add(right.scale(side * 1.4)).add(0.0, 0.35 + r.nextDouble() * 0.4, 0.0);
         flights[i] = new HollowFlight(chest.add(right.scale(side * 0.6)), heading, speed * (0.9 + 0.2 * r.nextDouble()), 0.16);
         double a = (Math.PI * 2) * (double)i / (double)n + r.nextDouble() * 0.6;
         offsets[i] = new Vec3(Math.cos(a), 0.2 + r.nextDouble() * 0.6, Math.sin(a));
      }
   }

   public static Vec3 clingPoint(Entity target, Vec3 offset) {
      double w = (double)target.getBbWidth() * 0.5 + 0.12;
      return target.position().add(offset.x * w, offset.y * (double)target.getBbHeight(), offset.z * w);
   }

   private static boolean tick(ServerLevel level, MiniHollowAbility.Rap rap) {
      long now = level.getGameTime();
      LivingEntity target = rap.target;
      if (!rap.owner.isRemoved() && target.isAlive() && !target.isRemoved() && target.level() == level && now - rap.launched <= 1280L) {
         for (int i = 0; i < rap.flights.length; i++) {
            if (!rap.popped[i]) {
               Vec3 cling = clingPoint(target, rap.offsets[i]);
               if (!rap.clung[i]) {
                  if (rap.flights[i].step(cling) < 0.35 || now - rap.launched > 80L) {
                     rap.clung[i] = true;
                     rap.flights[i].pos = cling;
                  }
               } else {
                  rap.flights[i].pos = cling;
               }

               if (rap.clung[i] && rap.pop != 0 && now >= rap.popAt) {
                  popOne(level, rap, i);
               }
            }
         }

         if (rap.pop == 0 && now - rap.launched >= (long)attachTicks(rap)) {
            rap.pop = 2;
            rap.popAt = now;
            AbilityManager.stopToggle(rap.owner, HoroAbilities.MINI_HOLLOW);
         }

         if (rap.done()) {
            finish(rap);
            return true;
         } else {
            return false;
         }
      } else {
         for (int ix = 0; ix < rap.popped.length; ix++) {
            if (!rap.popped[ix]) {
               rap.popped[ix] = true;
               ServerFx.send(target, HoroFx.MINI_POP, rap.flights[ix].pos, Vec3.ZERO, 0.0F, 0L, ix);
            }
         }

         finish(rap);
         return true;
      }
   }

   private static int attachTicks(MiniHollowAbility.Rap rap) {
      return (int)(HoroAbilities.MINI_HOLLOW.config().extra("attachSeconds") * 20.0) + 5;
   }

   private static void clap(MiniHollowAbility.Rap rap, int how) {
      ServerPlayer player = rap.owner;
      rap.pop = how;
      rap.popAt = player.level().getGameTime() + (long)(how == 1 ? 6 : 0);
      if (how == 1) {
         ServerFx.animate(player, HoroHoro.id("clap"));
         ServerFx.send(player, HoroFx.CLAP, player.position(), Vec3.ZERO, 1.0F, 0L, 0);
         player.level().playSound(null, player.getX(), player.getY() + 1.0, player.getZ(), HoroSounds.CLAP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
      }
   }

   private static void popOne(ServerLevel level, MiniHollowAbility.Rap rap, int i) {
      rap.popped[i] = true;
      LivingEntity target = rap.target;
      float factor = rap.pop == 2 ? (float)HoroAbilities.MINI_HOLLOW.config().extra("timeoutFactor") : 1.0F;
      Vec3 at = rap.flights[i].pos;
      ServerFx.send(target, HoroFx.MINI_POP, at, Vec3.ZERO, factor, 0L, i);
      level.playSound(null, at.x, at.y, at.z, HoroSounds.POP.get(), SoundSource.PLAYERS, 0.9F, 0.9F + 0.1F * (float)i);
      ServerPlayer owner = rap.owner;
      if (Targeting.canHurt(owner, target) && !(factor <= 0.0F)) {
         float amount = rap.damagePer * factor;
         if (rap.coat) {
            float coated = amount * ((Double)HakiConfig.ABILITY_MULTIPLIER.get()).floatValue();
            HakiCombat.techniqueHurt(owner, () -> Targeting.hurt(owner, target, coated, HoroHoro.HOLLOW_DAMAGE));
         } else {
            Targeting.hurt(owner, target, amount, HoroHoro.HOLLOW_DAMAGE);
         }

         target.invulnerableTime = 0;
         Vec3 away = target.position().subtract(at).multiply(1.0, 0.0, 1.0);
         if (away.lengthSqr() > 1.0E-4 && Targeting.canPush(owner, target)) {
            Targeting.push(target, away.normalize().scale((double)(rap.knockback * factor)).add(0.0, 0.12 * (double)factor, 0.0));
         }
      }
   }

   private static void finish(MiniHollowAbility.Rap rap) {
      RAPS.remove(rap.owner.getUUID(), rap);
      if (rap.pop == 1 && rap.owner.isAlive()) {
         HoroTriggers.GHOST_RAP.get().trigger(rap.owner);
      }

      AbilityManager.stopToggle(rap.owner, HoroAbilities.MINI_HOLLOW);
   }

   public static void forget(UUID player) {
      RAPS.remove(player);
   }

   public static void rewind(ServerPlayer player, int ticks) {
      MiniHollowAbility.Rap rap = RAPS.get(player.getUUID());
      if (rap != null) {
         rap.launched -= (long)ticks;
      }
   }

   public static boolean out(ServerPlayer player) {
      return DFAttachments.data(player).isToggleActive(HoroHoro.id("mini_hollow"));
   }

   public static final class Rap {
      final ServerPlayer owner;
      final LivingEntity target;
      final HollowFlight[] flights;
      final boolean[] clung;
      final boolean[] popped;
      final Vec3[] offsets;
      final float damagePer;
      final float knockback;
      final boolean coat;
      long launched;
      int pop;
      long popAt;

      Rap(ServerPlayer owner, LivingEntity target, int n, float damagePer, float knockback, boolean coat) {
         this.owner = owner;
         this.target = target;
         this.flights = new HollowFlight[n];
         this.clung = new boolean[n];
         this.popped = new boolean[n];
         this.offsets = new Vec3[n];
         this.damagePer = damagePer;
         this.knockback = knockback;
         this.coat = coat;
      }

      boolean done() {
         for (boolean p : this.popped) {
            if (!p) {
               return false;
            }
         }

         return true;
      }

      public int clinging() {
         int n = 0;

         for (int i = 0; i < this.clung.length; i++) {
            if (this.clung[i] && !this.popped[i]) {
               n++;
            }
         }

         return n;
      }
   }
}
