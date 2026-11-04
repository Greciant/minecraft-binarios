package com.devilfruits.fruits.supa_supa.ability;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.PhysicsWorld;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.fruits.supa_supa.BladeLimbs;
import com.devilfruits.fruits.supa_supa.SkateMotion;
import com.devilfruits.fruits.supa_supa.SupaAbilities;
import com.devilfruits.fruits.supa_supa.SupaFx;
import com.devilfruits.fruits.supa_supa.SupaSounds;
import com.devilfruits.fruits.supa_supa.SupaSupa;
import com.devilfruits.registry.DFAttachments;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.phys.Vec3;

public class BladeSkatesAbility extends Ability {
   private static final ResourceLocation GLIDE = SupaSupa.id("blade_skates_glide");
   private static final Map<UUID, BladeSkatesAbility.State> STATES = new HashMap<>();

   public BladeSkatesAbility() {
      super(
         SupaSupa.id("blade_skates"),
         CastType.TOGGLE,
         AbilityStats.builder()
            .energy(6)
            .cooldown(20)
            .mastery(3)
            .damage(5.0)
            .range(3.5)
            .knockback(0.8)
            .extra("maxSpeed", 0.85, 0.2, 3.0, "Top skating speed, blocks per tick (checked on the server).")
            .extra("acceleration", 0.045, 0.005, 0.5, "Speed gained per tick when pushing forward, blocks per tick.")
            .extra("glide", 0.985, 0.8, 1.0, "Share of the speed kept every tick on the blades (momentum).")
            .extra("brake", 0.86, 0.5, 1.0, "Share of the speed kept every tick when braking (sneaking).")
            .extra("turnRate", 0.16, 0.02, 1.0, "How fast the travel turns towards the look (carving), radians per tick.")
            .extra("drainPerSecond", 3.0, 0.0, 100.0, "Energy the skates drain every second.")
            .extra("lungeDamagePerSpeed", 12.0, 0.0, 100.0, "Extra lunge damage per block/tick of speed.")
            .extra("lungeCooldownTicks", 14.0, 0.0, 200.0, "Ticks between two lunges.")
            .build()
      );
   }

   @Override
   public boolean coatable() {
      return true;
   }

   public SkateMotion.Params params() {
      return new SkateMotion.Params(
         this.config().extra("maxSpeed"),
         this.config().extra("acceleration"),
         this.config().extra("glide"),
         this.config().extra("brake"),
         this.config().extra("turnRate")
      );
   }

   @Override
   public void onToggle(AbilityContext ctx, boolean active) {
      ServerPlayer player = ctx.player();
      AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
      if (active) {
         BladeLimbs.legs(player, true);
         if (speed != null) {
            speed.removeModifier(GLIDE);
            speed.addTransientModifier(new AttributeModifier(GLIDE, -0.85, Operation.ADD_MULTIPLIED_TOTAL));
         }

         BladeSkatesAbility.State st = STATES.computeIfAbsent(player.getUUID(), k -> new BladeSkatesAbility.State());
         st.last = player.position();
         st.lastValid = player.position();
         st.speed = 0.0;
         st.strikes = 0;
         player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SupaSounds.SKATES_UNFOLD.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
      } else {
         BladeLimbs.legs(player, false);
         if (speed != null) {
            speed.removeModifier(GLIDE);
         }

         STATES.remove(player.getUUID());
      }
   }

   @Override
   public void toggleTick(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      PlayerFruitData data = ctx.data();
      if (!AbilityManager.freeEnergy(player)) {
         data.setEnergy(Math.max(0.0F, data.energy() - (float)this.config().extra("drainPerSecond") / 20.0F));
      }

      if ((!(data.energy() <= 0.0F) || AbilityManager.freeEnergy(player))
         && !player.isInWater()
         && !player.isInLava()
         && !player.isPassenger()
         && !player.getAbilities().flying) {
         BladeSkatesAbility.State st = STATES.computeIfAbsent(player.getUUID(), k -> new BladeSkatesAbility.State());
         Vec3 now = player.position();
         if (st.last == null) {
            st.last = now;
            st.lastValid = now;
         }

         Vec3 deck = PhysicsWorld.carrierVelocity(player);
         double dx = now.x - st.last.x - deck.x;
         double dz = now.z - st.last.z - deck.z;
         double travel = Math.sqrt(dx * dx + dz * dz);
         st.last = now;
         if (SkateMotion.tooFast(travel, this.config().extra("maxSpeed")) && player.getDeltaMovement().y > -1.5) {
            if (++st.strikes >= 3) {
               player.connection.teleport(st.lastValid.x, st.lastValid.y, st.lastValid.z, player.getYRot(), player.getXRot());
               AbilityManager.stopToggle(player, this);
               return;
            }
         } else {
            st.strikes = 0;
            st.lastValid = now;
            st.speed = st.speed * 0.6 + travel * 0.4;
         }
      } else {
         AbilityManager.stopToggle(player, this);
      }
   }

   public static double speed(ServerPlayer player) {
      BladeSkatesAbility.State st = STATES.get(player.getUUID());
      return st == null ? 0.0 : st.speed;
   }

   public static void lunge(ServerPlayer player) {
      BladeSkatesAbility skates = SupaAbilities.BLADE_SKATES;
      PlayerFruitData data = DFAttachments.data(player);
      BladeSkatesAbility.State st = STATES.get(player.getUUID());
      if (st != null && data.isToggleActive(skates.id()) && SupaSupa.active(player) && player.level().getGameTime() >= st.nextLunge) {
         AbilityContext ctx = SupaSupa.context(player, skates);
         if (ctx != null) {
            st.nextLunge = player.level().getGameTime() + (long)skates.config().extra("lungeCooldownTicks");
            double speed = st.speed;
            BladeLimbs.arms(player, BladeLimbs.ArmMode.FOREARM, true, true, 16);
            ServerFx.animate(player, ctx.animation(SupaSupa.id("skate_lunge")));
            Vec3 eye = ctx.eye();
            Vec3 look = ctx.look();
            float damage = ctx.damage() + (float)(skates.config().extra("lungeDamagePerSpeed") * speed);
            double reach = (double)ctx.range() + speed * 2.0;

            for (Entity e : Targeting.inCapsule(ctx.level(), player, eye.add(0.0, -0.4, 0.0), eye.add(look.scale(reach)).add(0.0, -0.4, 0.0), 1.0)) {
               if (Targeting.canHurt(player, e) && ctx.hurt(e, damage)) {
                  Targeting.launch(e, look.scale((double)ctx.knockback() * (1.0 + speed)).add(0.0, 0.25, 0.0));
                  ServerFx.sendNear(ctx.level(), SupaFx.CUT_HIT, e.getId(), e.getBoundingBox().getCenter(), look, damage, (long)e.getId(), 0, 64.0);
               }
            }

            player.level()
               .playSound(
                  null, player.getX(), player.getY() + 1.0, player.getZ(), SupaSounds.SKATES_LUNGE.get(), SoundSource.PLAYERS, 1.0F, 0.9F + (float)speed * 0.3F
               );
            ServerFx.sendNear(ctx.level(), SupaFx.LUNGE, player.getId(), eye, look, (float)reach, ctx.seed(), (int)(speed * 100.0), 64.0);
         }
      }
   }

   public static void forget(UUID player) {
      STATES.remove(player);
   }

   private static final class State {
      Vec3 last;
      Vec3 lastValid;
      double speed;
      int strikes;
      long nextLunge;
   }
}
