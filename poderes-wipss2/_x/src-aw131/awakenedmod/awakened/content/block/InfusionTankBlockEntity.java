package com.awakenedmod.awakened.content.block;

import com.awakenedmod.awakened.content.AwakenedBlockEntities;
import com.awakenedmod.awakened.content.regenerator.AdamantiumBonding;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.net.InfusionPosePayload;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class InfusionTankBlockEntity extends BlockEntity {
   public static final int CAPACITY = 4;
   private static final int DURATION = 200;
   private static final int DAMAGE_INTERVAL = 10;
   private static final float DAMAGE = 4.0F;
   private static final float HEAL_PER_TICK = 0.3F;
   private static final float HP_FLOOR = 2.0F;
   private UUID occupant;
   private boolean infusing;
   private int ticksRemaining;

   public InfusionTankBlockEntity(BlockPos pos, BlockState state) {
      super(AwakenedBlockEntities.INFUSION_TANK.get(), pos, state);
   }

   public int charge() {
      return (Integer)this.getBlockState().getValue(InfusionTankBlock.CHARGE);
   }

   public boolean isFull() {
      return this.charge() >= 4;
   }

   public float chargeFraction() {
      return Math.max(0.0F, Math.min(1.0F, (float)this.charge() / 4.0F));
   }

   public boolean isInfusing() {
      return this.infusing;
   }

   public int addCharge(int ingots) {
      int current = this.charge();
      int accepted = Math.min(ingots, 4 - current);
      if (accepted > 0) {
         this.setCharge(current + accepted);
      }

      return accepted;
   }

   private void setCharge(int value) {
      if (this.level != null) {
         BlockState foot = this.getBlockState();
         this.level.setBlock(this.worldPosition, (BlockState)foot.setValue(InfusionTankBlock.CHARGE, value), 3);
         BlockPos head = this.worldPosition.relative((Direction)foot.getValue(InfusionTankBlock.FACING));
         BlockState headState = this.level.getBlockState(head);
         if (headState.is(foot.getBlock())) {
            this.level.setBlock(head, (BlockState)headState.setValue(InfusionTankBlock.CHARGE, value), 3);
         }
      }
   }

   public boolean tryEnter(ServerPlayer player) {
      if (this.infusing) {
         return false;
      } else if (this.occupant != null && !this.occupant.equals(player.getUUID())) {
         return false;
      } else {
         this.occupant = player.getUUID();
         return true;
      }
   }

   public boolean tryBegin(ServerPlayer player) {
      if (this.infusing) {
         return false;
      } else {
         PowerData data = AwakenedAttachments.get(player);
         if (!data.awakened() || !data.powerId().equals("regenerator")) {
            player.displayClientMessage(Component.translatable("awakened.infusion.not_regenerator"), true);
            return false;
         } else if (AdamantiumBonding.isMaxed(data)) {
            player.displayClientMessage(Component.translatable("awakened.adamantium.maxed"), true);
            return false;
         } else if (!this.isFull()) {
            player.displayClientMessage(Component.translatable("awakened.infusion.need_charge"), true);
            return false;
         } else {
            this.occupant = player.getUUID();
            this.infusing = true;
            this.ticksRemaining = 200;
            if (this.level instanceof ServerLevel server) {
               Vec3 c = this.center();
               Fx.burst(server, ParticleTypes.ENCHANT, c, 40, 0.6, 0.3);
               Fx.sound(server, c, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 0.9F, 0.7F);
               Fx.sound(server, c, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 0.7F, 0.6F);
            }

            player.displayClientMessage(Component.translatable("awakened.adamantium.bonding_start"), true);
            return true;
         }
      }
   }

   public void serverTick(ServerLevel level) {
      if (this.occupant != null) {
         if (level.getPlayerByUUID(this.occupant) instanceof ServerPlayer player && player.isAlive() && !player.isRemoved()) {
            player.setForcedPose(Pose.SLEEPING);
            float lyingYaw = InfusionTankBlock.lyingYaw((Direction)this.getBlockState().getValue(InfusionTankBlock.FACING));
            player.setYBodyRot(lyingYaw);
            player.setYHeadRot(lyingYaw);
            Vec3 c = this.center();
            if (player.distanceToSqr(c) > 0.04) {
               player.teleportTo(c.x, c.y, c.z);
            }

            player.setDeltaMovement(Vec3.ZERO);
            player.resetFallDistance();
            if (!this.infusing) {
               if (player.isShiftKeyDown()) {
                  standUp(player);
                  this.release();
               }

               return;
            }

            float progress = (float)(200 - this.ticksRemaining) / 200.0F;
            Fx.burst(level, ParticleTypes.BUBBLE, c.add(0.0, 0.2, 0.0), 4 + Math.round(progress * 12.0F), 0.35, 0.02);
            Fx.burst(level, ParticleTypes.ENCHANT, c.add(0.0, 0.4, 0.0), 2, 0.3, 0.05);
            if (player.getHealth() < player.getMaxHealth()) {
               player.heal(0.3F);
            }

            int elapsed = 200 - this.ticksRemaining;
            if (elapsed > 0 && elapsed % 10 == 0) {
               float dmg = Math.min(4.0F, Math.max(0.0F, player.getHealth() - 2.0F));
               if (dmg > 0.0F) {
                  Dmg.hurt(player, level, level.damageSources().magic(), dmg);
               }

               Fx.burst(level, ParticleTypes.CRIT, c.add(0.0, 0.3, 0.0), 10 + Math.round(progress * 20.0F), 0.4, 0.1);
               Fx.burst(level, ParticleTypes.ELECTRIC_SPARK, c.add(0.0, 0.3, 0.0), 6, 0.4, 0.05);
               Fx.sound(level, c, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.5F, 1.1F + progress * 0.8F);
            }

            this.ticksRemaining--;
            if (this.ticksRemaining <= 0) {
               this.complete(player);
            }

            return;
         }

         this.release();
      }
   }

   private void complete(ServerPlayer player) {
      standUp(player);
      AdamantiumBonding.grantCoating(player, AwakenedAttachments.get(player));
      this.setCharge(0);
      this.release();
   }

   private void release() {
      this.occupant = null;
      this.infusing = false;
      this.ticksRemaining = 0;
   }

   private static void standUp(ServerPlayer player) {
      player.setForcedPose(null);
      PacketDistributor.sendToPlayer(player, new InfusionPosePayload(false, 0.0F), new CustomPacketPayload[0]);
   }

   public void setRemoved() {
      super.setRemoved();
      if (this.occupant != null && this.level instanceof ServerLevel server && server.getPlayerByUUID(this.occupant) instanceof ServerPlayer player) {
         standUp(player);
      }
   }

   private Vec3 center() {
      Direction facing = (Direction)this.getBlockState().getValue(InfusionTankBlock.FACING);
      return new Vec3(
         (double)this.worldPosition.getX() + 0.5 - (double)facing.getStepX() * 0.45,
         (double)this.worldPosition.getY() + 0.35,
         (double)this.worldPosition.getZ() + 0.5 - (double)facing.getStepZ() * 0.45
      );
   }
}
