package com.awakenedmod.awakened.content.ruin;

import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.TeleportUtil;
import com.awakenedmod.awakened.core.TickFreeze;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

final class RuinDrag {
   final LivingEntity body;
   private final boolean frozen;
   private boolean released;
   Vec3 at;

   private RuinDrag(LivingEntity body) {
      this.body = body;
      this.frozen = !(body instanceof Player);
      this.at = body.position();
   }

   static RuinDrag take(LivingEntity body) {
      if (body.isPassenger()) {
         body.stopRiding();
      }

      RuinDrag drag = new RuinDrag(body);
      if (drag.frozen) {
         TickFreeze.freeze(body);
      }

      return drag;
   }

   boolean lost(ServerPlayer holder) {
      return !this.body.isAlive() || this.body.isRemoved() || this.body.level() != holder.level() || !RuinStacks.accepts(this.body, holder);
   }

   @Nullable
   Vec3 placeAhead(ServerPlayer holder, Vec3 dir) {
      double reach = (double)holder.getBbWidth() * 0.5 + (double)this.body.getBbWidth() * 0.5 + 0.3;
      Vec3 p = holder.position().add(dir.scale(reach));
      ServerLevel level = Dmg.serverLevel(holder);
      double top = holder.getY() + 1.1;
      BlockPos floor = RuinCracks.solidUnder(level, BlockPos.containing(p.x, top, p.z));
      double y = holder.getY();
      if (floor != null) {
         double floorY = (double)floor.getY() + level.getBlockState(floor).getCollisionShape(level, floor).max(Axis.Y);
         if (floorY > top) {
            return null;
         }

         if (floorY >= holder.getY() - 1.5) {
            y = floorY;
         }
      }

      for (double lift = 0.0; lift <= 1.100001 && !(y + lift > top); lift += 0.25) {
         Vec3 feet = new Vec3(p.x, y + lift, p.z);
         if (TeleportUtil.fits(this.body, feet)) {
            return feet;
         }
      }

      return null;
   }

   boolean tallWallAhead(Vec3 dir) {
      Vec3 feet = this.at != null ? this.at : this.body.position();
      Vec3 front = feet.add(dir.scale((double)this.body.getBbWidth() * 0.5 + 0.3 + 0.2));
      BlockPos low = BlockPos.containing(front.x, feet.y + 0.1, front.z);
      return this.solid(low) && this.solid(low.above());
   }

   private boolean solid(BlockPos pos) {
      return !this.body.level().getBlockState(pos).getCollisionShape(this.body.level(), pos).isEmpty();
   }

   void moveTo(ServerPlayer holder, Vec3 feet) {
      this.at = feet;
      this.body.setDeltaMovement(Vec3.ZERO);
      this.body.resetFallDistance();
      if (this.body instanceof ServerPlayer player) {
         RuinGripAbility.lockPlayer(holder, player);
         player.teleportTo(feet.x, feet.y, feet.z);
      } else {
         this.body.setPos(feet.x, feet.y, feet.z);
      }
   }

   void pinTick(ServerPlayer holder) {
      if (this.body instanceof ServerPlayer player && !this.released) {
         RuinGripAbility.lockPlayer(holder, player);
         player.setDeltaMovement(0.0, Math.min(0.0, player.getDeltaMovement().y), 0.0);
         player.hurtMarked = true;
      }
   }

   void driveInto(ServerPlayer holder, Vec3 dir) {
      Vec3 best = this.at;

      for (double s = 0.05; s <= 0.400001; s += 0.05) {
         Vec3 next = this.at.add(dir.scale(s));
         if (!TeleportUtil.fits(this.body, next)) {
            break;
         }

         best = next;
      }

      this.moveTo(holder, best);
   }

   @Nullable
   BlockHitResult wallAhead(Vec3 dir) {
      return wallAhead(this.body, this.at, dir);
   }

   @Nullable
   static BlockHitResult wallAhead(LivingEntity entity, Vec3 feet, Vec3 dir) {
      Vec3 from = feet.add(0.0, (double)entity.getBbHeight() * 0.5, 0.0);
      Vec3 to = from.add(dir.scale((double)entity.getBbWidth() * 0.5 + 1.5));
      BlockHitResult hit = entity.level().clip(new ClipContext(from, to, Block.COLLIDER, Fluid.NONE, entity));
      return hit.getType() == Type.BLOCK ? hit : null;
   }

   void letGo(@Nullable Vec3 motion) {
      if (!this.released) {
         this.released = true;
         if (this.frozen) {
            TickFreeze.release(this.body);
         }

         if (this.body.isAlive() && !this.body.isRemoved()) {
            if (this.body instanceof ServerPlayer player) {
               TeleportUtil.ejectFromWall(player);
            }

            this.body.resetFallDistance();
            if (motion != null) {
               this.body.setDeltaMovement(motion);
               this.body.hurtMarked = true;
            }
         }
      }
   }
}
