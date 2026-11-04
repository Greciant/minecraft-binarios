package com.awakenedmod.awakened.content.spider;

import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.net.SpiderForceStopPayload;
import com.awakenedmod.awakened.core.net.SpiderZipPayload;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.neoforged.neoforge.network.PacketDistributor;

public final class WebZipAbility extends Ability {
   public static final String ID = "web_zip";
   private static final float COST = 15.0F;
   private static final int UNLOCK = 5;
   private static final int FALL_GRACE = 30;
   private static final double ZIP_VISUAL_RANGE = 48.0;
   private static final double ZIP_VISUAL_AIR_DIST = 14.0;
   private static final double YANK_RANGE = 48.0;
   private static final double YANK_BASE = 1.6;
   private static final double YANK_MIN = 0.5;
   private static final double YANK_MAX = 3.2;
   private static final double YANK_LIFT = 0.25;
   private static final double YANK_PER_LEVEL = 0.12;
   private static final double YANK_MASTERY_CAP = 2.0;
   private static final double YANK_ITEM_SPEED = 1.4;
   private final Map<UUID, Integer> fallGrace = this.playerState();

   public WebZipAbility() {
      super("web_zip", 15.0F, 5);
   }

   public static float cost() {
      return StaminaSystem.effectiveCost(15.0F, WebZipAbility.class.getSimpleName());
   }

   @Override
   public boolean clientDriven() {
      return true;
   }

   @Override
   public boolean activate(ServerPlayer player) {
      return false;
   }

   public static void handleClientZip(ServerPlayer player, SpiderZipPayload payload) {
      WebZipAbility zip = SpiderClientFire.resolve(player, payload.slot(), WebZipAbility.class);
      if (zip != null) {
         if (payload.mode() == 3) {
            PowerService.playAnimation(player, "web_zip_pull");
         } else if (payload.mode() == 4) {
            PowerService.stopAnimation(player);
            SpiderVisualState.clearTether(player);
         } else if (payload.mode() == 2) {
            zip.handleYank(player, payload.targetId());
         } else {
            zip.handleSelf(player, payload.mode() == 1);
         }
      }
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      PowerService.stopAnimation(player);
      SpiderVisualState.clearTether(player);
      super.clearPlayer(player);
   }

   private void handleSelf(ServerPlayer player, boolean launch) {
      PowerData data = PowerService.data(player);
      float cost = this.billed(15.0F);
      if (!StaminaSystem.canAfford(data, cost)) {
         PacketDistributor.sendToPlayer(player, SpiderForceStopPayload.INSTANCE, new CustomPacketPayload[0]);
         this.fail(player, "awakened.feedback.no_stamina", new Object[0]);
      } else {
         if (!data.infiniteStamina()) {
            data.setStamina(data.stamina() - cost);
         }

         data.setLastSpendTime(player.serverLevel().getGameTime());
         MasterySystem.awardUse(player);
         PowerService.playAnimation(player, "web_zip");
         if (!launch) {
            this.fallGrace.put(player.getUUID(), 30);
         }

         SpiderVisualState.setZip(player, false, zipAnchor(player, launch));
         PowerService.sync(player);
      }
   }

   private static Vec3 zipAnchor(ServerPlayer player, boolean launch) {
      Vec3 eye = player.getEyePosition();
      Vec3 look = player.getLookAngle();
      if (launch) {
         return eye.add(look.scale(14.0));
      } else {
         Vec3 end = eye.add(look.scale(48.0));
         BlockHitResult hit = player.serverLevel().clip(new ClipContext(eye, end, Block.COLLIDER, Fluid.NONE, player));
         return hit.getType() == Type.BLOCK ? hit.getLocation() : end;
      }
   }

   private void handleYank(ServerPlayer player, int targetId) {
      ServerLevel level = player.serverLevel();
      Entity entity = level.getEntity(targetId);
      boolean valid = entity != null
         && entity.isAlive()
         && entity != player
         && (entity instanceof LivingEntity || entity instanceof ItemEntity)
         && Harm.allowed(player, entity);
      if (valid && !(player.distanceToSqr(entity) > 2304.0)) {
         PowerData data = PowerService.data(player);
         float cost = this.billed(15.0F);
         if (!StaminaSystem.canAfford(data, cost)) {
            this.fail(player, "awakened.feedback.no_stamina", new Object[0]);
         } else {
            if (!data.infiniteStamina()) {
               data.setStamina(data.stamina() - cost);
            }

            data.setLastSpendTime(level.getGameTime());
            Vec3 from = entity.position();
            Vec3 to = player.position().add(0.0, (double)player.getBbHeight() * 0.4, 0.0);
            Vec3 toPlayer = to.subtract(from);
            Vec3 dir = toPlayer.lengthSqr() < 1.0E-4 ? player.getLookAngle().scale(-1.0).normalize() : toPlayer.normalize();
            double speed;
            if (entity instanceof ItemEntity) {
               speed = 1.4;
            } else {
               speed = yankSpeed((LivingEntity)entity, data.masteryLevel());
            }

            entity.setDeltaMovement(dir.x * speed, dir.y * speed + 0.25, dir.z * speed);
            entity.hurtMarked = true;
            entity.resetFallDistance();
            PowerService.playAnimation(player, "web_yank");
            SpiderFx.yankWeb(level, player, entity.getBoundingBox().getCenter());
            MasterySystem.awardUse(player);
            PowerService.sync(player);
         }
      } else {
         this.fail(player, "awakened.spider.no_yank_target", new Object[0]);
      }
   }

   private static double yankSpeed(LivingEntity target, int masteryLevel) {
      double volume = (double)(target.getBbWidth() * target.getBbWidth() * target.getBbHeight());
      double mass = Math.max(1.0, (double)target.getMaxHealth() * 0.05 + volume);
      double mastery = Math.min(2.0, 1.0 + (double)masteryLevel * 0.12);
      double speed = 1.6 * mastery / Math.sqrt(mass);
      return Math.max(0.5, Math.min(3.2, speed));
   }

   @Override
   public void serverTick(ServerPlayer player) {
      this.tickFallGrace(player);
   }

   private void tickFallGrace(ServerPlayer player) {
      Integer left = this.fallGrace.get(player.getUUID());
      if (left != null) {
         if (!player.onGround() && left > 0) {
            player.resetFallDistance();
            this.fallGrace.put(player.getUUID(), left - 1);
         } else {
            this.fallGrace.remove(player.getUUID());
         }
      }
   }
}
