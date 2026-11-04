package com.awakenedmod.awakened.mixin;

import com.awakenedmod.awakened.client.voidview.VoidView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({GameRenderer.class})
public abstract class VoidPickMixin {
   @Inject(
      method = {"pick(F)V"},
      at = {@At("TAIL")},
      require = 0
   )
   private void awakened$voidPick(float partialTick, CallbackInfo ci) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.hitResult instanceof BlockHitResult block && block.getType() == Type.BLOCK && VoidView.hidesWorld()) {
         Entity viewer = minecraft.getCameraEntity();
         if (viewer instanceof Player body) {
            double reach = body.entityInteractionRange();
            Vec3 eye = viewer.getEyePosition(partialTick);
            Vec3 look = viewer.getViewVector(partialTick);
            Vec3 end = eye.add(look.scale(reach));
            AABB sweep = viewer.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.0);
            EntityHitResult hit = ProjectileUtil.getEntityHitResult(viewer, eye, end, sweep, e -> !e.isSpectator() && e.isPickable(), reach * reach);
            if (hit != null) {
               minecraft.hitResult = hit;
               minecraft.crosshairPickEntity = hit.getEntity();
            } else {
               minecraft.hitResult = BlockHitResult.miss(end, block.getDirection(), BlockPos.containing(end));
            }

            return;
         }

         minecraft.hitResult = BlockHitResult.miss(block.getLocation(), block.getDirection(), block.getBlockPos());
         return;
      }
   }
}
