package com.devilfruits.fruits.uo_uo_seiryu.client;

import com.devilfruits.client.form.FormMovements;
import com.devilfruits.client.input.InputHandler;
import com.devilfruits.core.form.FormFlags;
import com.devilfruits.core.form.FruitForm;
import com.devilfruits.fruits.uo_uo_seiryu.DragonShells;
import com.devilfruits.fruits.uo_uo_seiryu.UoUoSeiryu;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

final class DragonMovement implements FormMovements.FormMovement {
   private static final double INERTIA = 0.14;

   @Override
   public void input(LocalPlayer player, FruitForm form, Input input) {
      float forward = input.forwardImpulse;
      float strafe = input.leftImpulse;
      boolean up = input.jumping;
      boolean down = input.shiftKeyDown;
      boolean boost = InputHandler.isBoosting();
      input.shiftKeyDown = false;
      if (FormFlags.has(player, 2)) {
         input.forwardImpulse = 0.0F;
         input.leftImpulse = 0.0F;
         input.jumping = false;
         player.getAbilities().flying = true;
         double speed = setting(boost ? "dragonBoostSpeed" : "dragonFlightSpeed");
         if (FormFlags.has(player, DragonShells.TWISTING)) {
            speed *= 0.3;
         }

         Vec3 look = player.getLookAngle();
         double ahead = forward > 0.0F ? speed * (double)forward : (forward < 0.0F ? -0.2 : 0.06);
         Vec3 right = new Vec3(-look.z, 0.0, look.x);
         right = right.lengthSqr() < 1.0E-6 ? Vec3.ZERO : right.normalize();
         double vertical = (up ? 0.65 : 0.0) - (down ? 0.65 : 0.0);
         Vec3 target = look.scale(ahead).add(right.scale((double)(-strafe) * 0.3)).add(0.0, vertical, 0.0);
         Vec3 v = player.getDeltaMovement();
         player.setDeltaMovement(v.add(target.subtract(v).scale(0.14)));
      } else {
         player.getAbilities().flying = false;
         if (forward != 0.0F && player.onGround()) {
            Vec3 look = player.getLookAngle();
            Vec3 flat = new Vec3(look.x, 0.0, look.z);
            if (flat.lengthSqr() > 1.0E-6) {
               flat = flat.normalize();
               double speed = setting("dragonGroundSpeed") * (boost ? 1.6 : 1.0) * (double)Math.signum(forward);
               Vec3 v = player.getDeltaMovement();
               player.setDeltaMovement(v.x + (flat.x * speed - v.x) * 0.3, v.y, v.z + (flat.z * speed - v.z) * 0.3);
               input.forwardImpulse = 0.0F;
            }
         }

         input.leftImpulse *= 0.3F;
      }
   }

   @Override
   public void leave(LocalPlayer player, FruitForm form) {
      if (!player.isCreative() && !player.isSpectator() && player.getAbilities().flying) {
         player.getAbilities().flying = false;
         player.onUpdateAbilities();
      }
   }

   private static double setting(String name) {
      return UoUoSeiryu.fruit().config().setting(name);
   }
}
