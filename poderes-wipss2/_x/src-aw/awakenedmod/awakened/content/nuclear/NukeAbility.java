package com.awakenedmod.awakened.content.nuclear;

import com.awakenedmod.awakened.content.AwakenedEntities;
import com.awakenedmod.awakened.content.AwakenedItems;
import com.awakenedmod.awakened.content.entity.projectile.KineticProjectile;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.LoopSound;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.power.AbilityFeedback;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.CollisionContext;

public final class NukeAbility extends Ability {
   private static final int UNLOCK = 11;
   private static final float COST = 70.0F;
   private static final int DRAIN_SYNC_TICKS = 5;
   private static final int MAX_CHARGE_TICKS = 50;
   public static final int RELEASE_DELAY_TICKS = 3;
   private static final float IMPACT_DAMAGE = 55.0F;
   private static final float AOE_DAMAGE = 34.0F;
   private static final float AOE_RADIUS = 9.0F;
   private static final float KNOCKBACK = 1.6F;
   private static final double MUSHROOM_RADIUS = 8.0;
   private static final double FALLOUT_RADIUS = 7.0;
   private static final int FALLOUT_TICKS = 240;
   private static final float FALLOUT_DAMAGE = 3.5F;
   private static final double LAUNCH_VELOCITY = 1.7;
   private static final double DOWNWARD_REACH = 2.0;
   private static final int PENDING_TIMEOUT_TICKS = 200;
   private final Map<UUID, NukeAbility.Charge> charging = this.activeState();
   private final Map<UUID, int[]> pending = this.activeState();
   private final Map<UUID, int[]> throwing = this.activeState();

   public NukeAbility() {
      super("nuke", 0.0F, 11);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().damage(55.0).build();
   }

   @Override
   public boolean activate(ServerPlayer player) {
      UUID id = player.getUUID();
      if (!this.charging.containsKey(id) && !this.throwing.containsKey(id) && !this.pending.containsKey(id)) {
         PowerData data = PowerService.data(player);
         float total = StaminaSystem.effectiveCost(70.0F, this.balanceKey());
         if (!data.infiniteStamina() && data.stamina() < total) {
            return this.fail(player, "awakened.feedback.no_stamina", new Object[0]);
         } else {
            this.charging.put(id, new NukeAbility.Charge(total / 50.0F));
            Fx.sound(player.serverLevel(), player.position(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8F, 0.4F);
            return true;
         }
      } else {
         return this.fail(player, "awakened.fail.busy", new Object[0]);
      }
   }

   @Override
   public void serverTick(ServerPlayer player) {
      UUID id = player.getUUID();
      int[] wait = this.pending.get(id);
      if (wait != null && ++wait[0] > 200) {
         this.pending.remove(id);
      }

      int[] heave = this.throwing.get(id);
      if (heave != null && --heave[0] <= 0) {
         this.throwing.remove(id);
         this.launch(player);
      }

      NukeAbility.Charge state = this.charging.get(id);
      if (state != null) {
         PowerData data = PowerService.data(player);
         if (!data.infiniteStamina()) {
            if (data.stamina() < state.perTick) {
               this.charging.remove(id);
               fizzle(player);
               AbilityFeedback.fail(player, "awakened.feedback.no_stamina");
               PowerService.sync(player);
               return;
            }

            StaminaSystem.drainSustained(data, player.serverLevel().getGameTime(), state.perTick);
         }

         state.ticks = Math.min(50, state.ticks + 1);
         if (state.ticks % 5 == 0 || state.ticks >= 50) {
            PowerService.sync(player);
         }

         float f = (float)state.ticks / 50.0F;
         LoopSound.keep(player, "nuke_charge", SoundEvents.CONDUIT_AMBIENT, SoundSource.PLAYERS, 0.6F, 0.4F + f * 0.9F);
         if (state.ticks >= 50) {
            this.charging.remove(id);
            this.heave(player);
         }
      }
   }

   @Override
   public void onRelease(ServerPlayer player) {
      if (this.charging.remove(player.getUUID()) != null) {
         fizzle(player);
      }
   }

   @Override
   public String costLabel() {
      return String.valueOf((int)this.billed(70.0F));
   }

   @Override
   public float onIncomingDamage(ServerPlayer player, DamageSource source, float amount) {
      if (amount > 0.0F && source.getEntity() != null && source.getEntity() != player && this.charging.remove(player.getUUID()) != null) {
         fizzle(player);
      }

      return amount;
   }

   private static void fizzle(ServerPlayer player) {
      LoopSound.stop(player, "nuke_charge");
      PowerService.stopAnimation(player);
      Fx.sound(player.serverLevel(), player.position(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.8F, 0.8F);
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      if (this.charging.containsKey(player.getUUID()) || this.throwing.containsKey(player.getUUID())) {
         LoopSound.stop(player, "nuke_charge");
         PowerService.stopAnimation(player);
      }

      super.clearPlayer(player);
   }

   private void heave(ServerPlayer player) {
      LoopSound.stop(player, "nuke_charge");
      this.pending.put(player.getUUID(), new int[]{0});
      this.throwing.put(player.getUUID(), new int[]{3});
      PowerService.stopAnimation(player);
      PowerService.playAnimation(player, "nuke_throw");
   }

   private void launch(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      Vec3 origin = player.getEyePosition().add(player.getViewVector(1.0F).scale(0.8));
      KineticProjectile shell = new KineticProjectile(
         AwakenedEntities.KINETIC_CHARGE.get(), player, level, new ItemStack((ItemLike)AwakenedItems.NUKE_CHARGE.get())
      );
      shell.setPos(origin.x, origin.y, origin.z);
      shell.configure(MasteryScaling.scaledDamage(55.0F, player, this.balanceKey()), 0.0F, 0.0F, 0.0F);
      shell.setContactRadius(0.8);
      shell.setCustomDetonation(true);
      shell.setTrail(false);
      shell.setOnDetonate(pos -> this.detonate(player, level, pos));
      Vec3 target = player.pick(80.0, 1.0F, false).getLocation();
      Vec3 dir = target.subtract(origin);
      shell.shoot(dir.x, dir.y, dir.z, 1.7F, 0.6F);
      level.addFreshEntity(shell);
      player.swing(InteractionHand.MAIN_HAND);
      Fx.sound(level, player.position(), (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.0F, 0.6F);
   }

   private void detonate(ServerPlayer player, ServerLevel level, Vec3 pos) {
      this.pending.remove(player.getUUID());
      NuclearFx.boom(level, pos, 8.0, NuclearFx.Kind.MUSHROOM);
      NuclearFx.screenShake(level, pos, 48.0, 24);
      Fallout.add(player, level, pos, 7.0, 240, MasteryScaling.scaledDamage(3.5F, player, this.balanceKey()));
      DamageSource src = Dmg.by(player, DamageTypes.MAGIC);
      float dmg = MasteryScaling.scaledDamage(34.0F, player, this.balanceKey());
      AABB box = new AABB(pos, pos).inflate(9.0, 9.0, 9.0);
      boolean hit = false;
      LivingEntity threat = null;
      int finished = 0;

      for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != player)) {
         Vec3 c = victim.position().add(0.0, (double)victim.getBbHeight() * 0.5, 0.0);
         double horiz = Math.hypot(c.x - pos.x, c.z - pos.z);
         double up = c.y - pos.y;
         if (!(horiz > 9.0) && !(up > 9.0) && !(up < -2.0) && (!(up > 0.0) || !(horiz * horiz + up * up > 81.0)) && exposed(level, pos, c)) {
            Dmg.hurt(victim, level, src, dmg);
            if (Foes.defeatedBy(victim, player)) {
               finished++;
            }

            Vec3 push = c.subtract(pos);
            push = push.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 1.0, 0.0) : push.normalize();
            Harm.push(player, victim, push.x * 1.6F, Math.max(0.45, push.y * 1.6F + 0.4), push.z * 1.6F);
            hit = true;
            if (threat == null && Foes.counts(player, victim)) {
               threat = victim;
            }
         }
      }

      if (hit) {
         MasterySystem.award(player, MasteryReason.HIT, threat);
      }

      if (finished >= 3) {
         QuestSystem.feat(player, "nuclear_ground_zero");
      }
   }

   private static boolean exposed(ServerLevel level, Vec3 from, Vec3 target) {
      BlockHitResult clip = level.clip(new ClipContext(from, target, Block.COLLIDER, Fluid.NONE, CollisionContext.empty()));
      return clip.getType() == Type.MISS || clip.getLocation().distanceToSqr(target) < 1.0;
   }

   private static final class Charge {
      int ticks;
      final float perTick;

      Charge(float perTick) {
         this.perTick = perTick;
      }
   }
}
