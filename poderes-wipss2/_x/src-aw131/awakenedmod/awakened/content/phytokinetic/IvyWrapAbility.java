package com.awakenedmod.awakened.content.phytokinetic;

import com.awakenedmod.awakened.content.vfx.BlockFxEntity;
import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.content.vfx.VfxEntity;
import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public final class IvyWrapAbility extends Ability {
   public static final String ID = "ivy_wrap";
   private static final float COST = 12.0F;
   private static final double RANGE = 15.0;
   private static final float DAMAGE = 5.0F;
   private static final double WIDTH = 0.08;
   private static final int WINDUP_TICKS = 3;
   private static final int WRAP_TICKS = 60;
   private static final int HARDEN_AT = 18;
   private static final int COIL_EVERY = 5;
   private static final int COIL_LIFE = 8;
   private static final double COIL_TURNS = 2.6;
   private final Map<UUID, IvyWrapAbility.Lash> lashes = this.playerState();
   private final Map<UUID, IvyWrapAbility.Wrap> wraps = this.playerState();

   public IvyWrapAbility() {
      super("ivy_wrap", 12.0F, 0);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().damage(5.0).durationTicks(60).build();
   }

   @Override
   protected boolean cancelsOnDimensionChange() {
      return true;
   }

   @Override
   public boolean activate(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      LivingEntity target = AimUtil.aimedLivingEntity(player, 15.0, e -> Harm.allowed(player, e));
      if (target == null) {
         return this.fail(player, "awakened.feedback.ivy_wrap_no_target", new Object[0]);
      } else if (!Harm.controls(player, target)) {
         return this.fail(player, "awakened.feedback.pvp_control_off", new Object[0]);
      } else {
         this.lashes.put(player.getUUID(), new IvyWrapAbility.Lash(target.getId()));
         Vec3 hand = AimUtil.handPosition(player);
         PhytoFx.gatherAtHand(level, hand);
         Fx.sound(level, hand, SoundEvents.AZALEA_LEAVES_BREAK, SoundSource.PLAYERS, 0.8F, 1.3F);
         return true;
      }
   }

   @Override
   public void serverTick(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      this.tickLash(player, level);
      this.tickWrap(player, level);
   }

   private void tickLash(ServerPlayer player, ServerLevel level) {
      IvyWrapAbility.Lash lash = this.lashes.get(player.getUUID());
      if (lash != null) {
         lash.tick++;
         if (lash.path == null) {
            if (lash.tick < 3) {
               PhytoFx.gatherAtHand(level, AimUtil.handPosition(player));
            } else {
               this.fire(player, level, lash);
            }
         } else {
            int travelTick = lash.tick - 3;
            double t = Math.min(1.0, (double)travelTick / (double)lash.travelTicks);
            PhytoFx.tipMotes(level, PhytoFx.pointAt(lash.path, t), lash.tick);
            if (travelTick >= lash.travelTicks) {
               this.lashes.remove(player.getUUID());
               this.impact(player, level, lash);
            }
         }
      }
   }

   private void fire(ServerPlayer player, ServerLevel level, IvyWrapAbility.Lash lash) {
      Vec3 hand;
      Vec3 var10000;
      label16: {
         hand = AimUtil.handPosition(player);
         if ((lash.targetId >= 0 ? level.getEntity(lash.targetId) : null) instanceof LivingEntity living && living.isAlive()) {
            var10000 = living.getBoundingBox().getCenter();
            break label16;
         }

         var10000 = AimUtil.aimedPosition(player, 15.0);
      }

      Vec3 to = var10000;
      lash.path = PhytoFx.whipPath(level.getRandom(), hand, to);
      lash.travelTicks = PhytoFx.growVine(level, level.getRandom(), lash.path, 0.08);
      Fx.burst(level, PhytoFx.LEAF_DUST, hand, 8, 0.25, 0.04);
      Fx.sound(level, hand, SoundEvents.FISHING_BOBBER_THROW, SoundSource.PLAYERS, 1.0F, 0.7F);
   }

   private void impact(ServerPlayer player, ServerLevel level, IvyWrapAbility.Lash lash) {
      Vec3 impact = lash.path.get(lash.path.size() - 1);
      Vec3 tangent = impact.subtract(lash.path.get(Math.max(0, lash.path.size() - 3)));
      float travelYaw = (float)Math.toDegrees(Math.atan2(-tangent.x, tangent.z));
      PhytoFx.leafBurst(level, impact, 10);
      Vfx.crescent(level, impact, 1.1, 9234544, 8, -travelYaw, 20.0F);
      PhytoFx.rootBurst(level, level.getRandom(), impact.add(0.0, -0.6, 0.0));
      Fx.sound(level, impact, SoundEvents.VINE_BREAK, SoundSource.PLAYERS, 1.2F, 0.8F);
      if ((lash.targetId >= 0 ? level.getEntity(lash.targetId) : null) instanceof LivingEntity living && living.isAlive()) {
         Dmg.hurt(living, level, Dmg.by(player, DamageTypes.THORNS), MasteryScaling.scaledDamage(5.0F, player, this.balanceKey()));
         int rootTicks = Math.round(MasteryScaling.scaled(60.0F, player));
         Harm.effect(player, living, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, rootTicks, 250, false, false));
         IvyWrapAbility.Wrap previous = this.wraps.get(player.getUUID());
         if (previous != null) {
            releaseCage(previous);
         }

         this.wraps.put(player.getUUID(), new IvyWrapAbility.Wrap(living.getId(), living.position(), rootTicks));
         MasterySystem.award(player, MasteryReason.HIT, living);
         Fx.sound(level, impact, SoundEvents.EVOKER_FANGS_ATTACK, SoundSource.PLAYERS, 0.9F, 1.25F);
         Fx.sound(level, impact, SoundEvents.ROOTED_DIRT_BREAK, SoundSource.PLAYERS, 1.1F, 0.7F);
         Fx.burst(level, PhytoFx.LEAF_DUST, AimUtil.handPosition(player), 6, 0.2, 0.05);
         Fx.sound(level, player.position(), SoundEvents.VINE_BREAK, SoundSource.PLAYERS, 0.7F, 1.5F);
      }
   }

   private void tickWrap(ServerPlayer player, ServerLevel level) {
      IvyWrapAbility.Wrap wrap = this.wraps.get(player.getUUID());
      if (wrap != null) {
         Entity found = level.getEntity(wrap.targetId);
         if (found instanceof LivingEntity target && target.isAlive()) {
            wrap.tick++;
            target.setDeltaMovement(Vec3.ZERO);
            target.setPos(wrap.lock.x, wrap.lock.y, wrap.lock.z);
            target.resetFallDistance();
            target.hurtMarked = true;
            Vec3 feet = wrap.lock;
            double radius = (double)target.getBbWidth() * 0.62 + 0.18;
            if (wrap.tick < 18) {
               if (wrap.tick % 5 == 1) {
                  Vfx.effect(VfxEntity.Kind.HELIX, feet)
                     .scale(radius)
                     .secondary(new Vec3((double)target.getBbHeight() * 0.8, 2.6, 0.0))
                     .color(4103220, 9234544)
                     .lifetime(13)
                     .intensity(0.9F)
                     .spawn(level);
               }

               if (wrap.tick % 3 == 0) {
                  Fx.burst(level, PhytoFx.MOSS_CRUMBS, feet.add(0.0, (double)target.getBbHeight() * 0.5, 0.0), 2, 0.25, 0.03);
               }

               return;
            }

            if (wrap.tick == 18) {
               this.harden(level, wrap, target, feet, wrap.rootTicks);
            }

            if (wrap.tick % 6 == 0) {
               Fx.burst(level, PhytoFx.WOOD_CRUMBS, feet.add(0.0, 0.3, 0.0), 2, 0.3, 0.02);
            }

            if (wrap.tick >= wrap.rootTicks) {
               this.wraps.remove(player.getUUID());
               releaseCage(wrap);
               Fx.burst(level, PhytoFx.WOOD_CRUMBS, feet.add(0.0, (double)target.getBbHeight() * 0.4, 0.0), 18, 0.45, 0.08);
               PhytoFx.leafBurst(level, feet.add(0.0, 0.5, 0.0), 4);
               Fx.sound(level, feet, SoundEvents.WOOD_BREAK, SoundSource.PLAYERS, 1.1F, 0.7F);
               Fx.sound(level, feet, SoundEvents.VINE_BREAK, SoundSource.PLAYERS, 0.9F, 1.0F);
            }

            return;
         }

         if (found instanceof LivingEntity corpse && Foes.defeatedBy(corpse, player)) {
            QuestSystem.feat(player, "phyto_root_kill");
         }

         releaseCage(wrap);
         this.wraps.remove(player.getUUID());
      }
   }

   private void harden(ServerLevel level, IvyWrapAbility.Wrap wrap, LivingEntity target, Vec3 feet, int rootTicks) {
      double w = (double)target.getBbWidth() * 1.15 + 0.15;
      double h = (double)target.getBbHeight();
      int cageLife = rootTicks - 18 + 5;
      wrap.cage = Vfx.blockState(
         level, feet.add(0.0, h * 0.02, 0.0), Blocks.MANGROVE_ROOTS.defaultBlockState(), w, h * 0.85, w, cageLife, target.getYRot(), 2, true
      );
      wrap.cageLegs = Vfx.blockState(
         level, feet, Blocks.MANGROVE_ROOTS.defaultBlockState(), w * 0.8, h * 0.4, w * 0.8, cageLife, target.getYRot() + 45.0F, 2, true
      );
      Fx.sound(level, feet, SoundEvents.AXE_STRIP, SoundSource.PLAYERS, 1.1F, 0.7F);
      Fx.sound(level, feet, SoundEvents.WOOD_PLACE, SoundSource.PLAYERS, 1.0F, 0.6F);
      Fx.burst(level, PhytoFx.WOOD_CRUMBS, feet.add(0.0, h * 0.4, 0.0), 16, 0.4, 0.06);
   }

   private static void releaseCage(IvyWrapAbility.Wrap wrap) {
      if (wrap.cage != null) {
         wrap.cage.discard();
         wrap.cage = null;
      }

      if (wrap.cageLegs != null) {
         wrap.cageLegs.discard();
         wrap.cageLegs = null;
      }
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      IvyWrapAbility.Wrap wrap = this.wraps.get(player.getUUID());
      if (wrap != null) {
         releaseCage(wrap);
      }

      super.clearPlayer(player);
   }

   private static final class Lash {
      final int targetId;
      List<Vec3> path;
      int travelTicks;
      int tick;

      Lash(int targetId) {
         this.targetId = targetId;
      }
   }

   private static final class Wrap {
      final int targetId;
      final Vec3 lock;
      final int rootTicks;
      int tick;
      BlockFxEntity cage;
      BlockFxEntity cageLegs;

      Wrap(int targetId, Vec3 lock, int rootTicks) {
         this.targetId = targetId;
         this.lock = lock;
         this.rootTicks = rootTicks;
      }
   }
}
