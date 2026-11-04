package com.awakenedmod.awakened.content.earth;

import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class UpheaveAbility extends Ability {
   private static final double RANGE = 18.0;
   private static final float DAMAGE = 6.0F;
   private static final double FOE_LAUNCH = 1.35;
   private static final double SELF_LAUNCH = 1.25;
   private static final double RIDE_VY = 0.8;
   private static final int PUSH_TICKS = 5;
   private static final int ERUPT_TICKS = 16;
   private static final int SELF_RIDE_MAX_TICKS = 100;
   private static final int CRACK_TICK = 3;
   private static final float PILLAR_WIDTH = 1.0F;
   private static final float PILLAR_HEIGHT = 3.0F;
   private static final int PILLAR_LIFE = 50;
   private static final float LAUNCH_PILLAR_W = 1.2F;
   private static final float LAUNCH_PILLAR_H = 3.6F;
   private static final int LAUNCH_PILLAR_LIFE = 28;
   private static final double VAULT_FORWARD = 0.95;
   private static final double VAULT_UP = 0.5;
   private final Map<UUID, List<UpheaveAbility.Erupt>> erupts = this.playerState();

   public UpheaveAbility() {
      super("upheave", 20.0F, 3);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().damage(6.0).range(18.0).build();
   }

   @Override
   protected boolean cancelsOnDimensionChange() {
      return true;
   }

   @Override
   public boolean requiresFootingWhenRealistic() {
      return true;
   }

   @Override
   public boolean activate(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      double power = Math.min(2.5, Math.max(1.0, (double)MasteryScaling.scaled(1.0F, player)));
      double range = (double)MasteryScaling.scaled(18.0F, player);
      if (player.isShiftKeyDown()) {
         return this.selfLaunch(player, level, power);
      } else {
         LivingEntity foe = AimUtil.aimedLivingSoft(player, range, 12.0, e -> e != player && e.onGround() && Harm.allowed(player, e));
         if (foe != null) {
            EarthMaterial foeMat = EarthMaterial.under(foe);
            Vec3 base = foe.position();
            boolean dealt = Dmg.hurt(foe, level, Dmg.by(player, DamageTypes.FALLING_BLOCK), MasteryScaling.scaledDamage(6.0F, player, this.balanceKey()));
            Vec3 v = foe.getDeltaMovement();
            double lift = 1.35 * power * foeMat.liftFactor();
            foe.setDeltaMovement(v.x * 0.2, Math.max(v.y, lift), v.z * 0.2);
            foe.hurtMarked = true;
            foe.resetFallDistance();
            foeMat.stagger(player, foe);
            spawnLaunchPillar(level, base, foeMat, foe.getYRot());
            this.add(player, new UpheaveAbility.Erupt(foe, base, power, foeMat));
            EarthFx.upheave(level, base, foeMat);
            if (dealt) {
               MasterySystem.award(player, MasteryReason.HIT, foe);
            }

            return true;
         } else if (AimUtil.wantsSelfLaunch(player)) {
            return this.selfLaunch(player, level, power);
         } else {
            EarthMaterial mat = EarthMaterial.under(player);
            Block block = mat.state().getBlock();
            Vec3 eye = player.getEyePosition();
            Vec3 look = player.getLookAngle();
            BlockHitResult hit = level.clip(
               new ClipContext(eye, eye.add(look.scale(range)), net.minecraft.world.level.ClipContext.Block.COLLIDER, Fluid.NONE, player)
            );
            if (hit.getType() != Type.BLOCK) {
               return this.fail(player, "ability.awakened.upheave.no_ground", new Object[0]);
            } else {
               Vec3 ground = hit.getLocation();
               StoneConstructEntity.spawn(level, ground, block, 1.0F, 3.0F, 50, player.getYRot());
               EarthFx.upheave(level, ground, mat);
               return true;
            }
         }
      }
   }

   private boolean selfLaunch(ServerPlayer player, ServerLevel level, double power) {
      if (!player.onGround()) {
         return this.fail(player, "ability.awakened.upheave.airborne", new Object[0]);
      } else {
         EarthMaterial mat = EarthMaterial.under(player);
         Vec3 feet = player.position();
         Vec3 v = player.getDeltaMovement();
         player.setDeltaMovement(v.x * 0.3, 1.25 * power, v.z * 0.3);
         player.hurtMarked = true;
         player.resetFallDistance();
         spawnLaunchPillar(level, feet, mat, player.getYRot());
         this.add(player, new UpheaveAbility.Erupt(null, feet, power, mat));
         EarthFx.upheave(level, feet, mat);
         return true;
      }
   }

   private static void spawnLaunchPillar(ServerLevel level, Vec3 feet, EarthMaterial mat, float yaw) {
      StoneConstructEntity.spawn(level, feet, mat.state().getBlock(), 1.2F, 3.6F, 28, yaw, false);
   }

   @Override
   public void serverTick(ServerPlayer player) {
      List<UpheaveAbility.Erupt> list = this.erupts.get(player.getUUID());
      if (list != null && !list.isEmpty()) {
         ServerLevel level = player.serverLevel();
         Iterator<UpheaveAbility.Erupt> it = list.iterator();

         while (it.hasNext()) {
            UpheaveAbility.Erupt e = it.next();
            boolean self = e.target == null;
            LivingEntity rider = (LivingEntity)(self ? player : e.target);
            if (rider != null && rider.isAlive()) {
               if (e.age < 5) {
                  Vec3 v = rider.getDeltaMovement();
                  rider.setDeltaMovement(v.x * 0.6, Math.max(v.y, 0.8 * e.power), v.z * 0.6);
                  rider.hurtMarked = true;
               }

               if (e.age <= 16) {
                  EarthFx.upheaveColumn(level, e.base, rider.getY() + (double)rider.getBbHeight() * 0.5, e.mat, e.age);
                  if (e.age == 3) {
                     EarthFx.upheaveCrack(level, new Vec3(rider.getX(), rider.getY() + (double)rider.getBbHeight(), rider.getZ()), e.mat);
                  }
               }

               e.age++;
               boolean done = self ? e.age > 16 && player.onGround() || e.age > 100 : e.age > 16;
               if (done) {
                  it.remove();
               }
            } else {
               it.remove();
            }
         }

         if (list.isEmpty()) {
            this.erupts.remove(player.getUUID());
         }
      }
   }

   @Override
   public void onSecondaryAction(ServerPlayer player, int action) {
      if (action == 5) {
         List<UpheaveAbility.Erupt> list = this.erupts.get(player.getUUID());
         if (list != null) {
            for (UpheaveAbility.Erupt e : list) {
               if (e.target == null && !e.vaulted) {
                  Vec3 look = player.getLookAngle();
                  Vec3 lead = new Vec3(look.x, 0.0, look.z);
                  lead = lead.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : lead.normalize();
                  player.setDeltaMovement(lead.x * 0.95, 0.5, lead.z * 0.95);
                  player.hurtMarked = true;
                  player.resetFallDistance();
                  EarthFx.upheaveCrack(player.serverLevel(), player.position().add(0.0, (double)player.getBbHeight() * 0.5, 0.0), e.mat);
                  e.vaulted = true;
                  return;
               }
            }
         }
      }
   }

   private void add(ServerPlayer player, UpheaveAbility.Erupt e) {
      this.erupts.computeIfAbsent(player.getUUID(), k -> new ArrayList<>()).add(e);
   }

   private static final class Erupt {
      private final LivingEntity target;
      private final Vec3 base;
      private final double power;
      private final EarthMaterial mat;
      private int age;
      private boolean vaulted;

      private Erupt(LivingEntity target, Vec3 base, double power, EarthMaterial mat) {
         this.target = target;
         this.base = base;
         this.power = power;
         this.mat = mat;
      }
   }
}
