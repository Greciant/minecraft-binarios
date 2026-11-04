package com.awakenedmod.awakened.content.command.vocab.effect;

import com.awakenedmod.awakened.content.command.CommandSupport;
import com.awakenedmod.awakened.content.command.vocab.Clash;
import com.awakenedmod.awakened.content.command.vocab.Effect;
import com.awakenedmod.awakened.content.command.vocab.Posture;
import com.awakenedmod.awakened.content.command.vocab.Target;
import com.awakenedmod.awakened.content.command.vocab.TargetKind;
import com.awakenedmod.awakened.content.command.vocab.Trigger;
import com.awakenedmod.awakened.core.Attrs;
import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.phys.Vec3;

public final class MovesWhereILookEffect implements Effect {
   private static final double SPEED_PER_LEVEL = 0.5;
   private static final double TURN_RATE = 0.35;
   private static final double WORKED_DISTANCE = 0.1;
   private static final double OTHERS_COST_FACTOR = 2.0;
   private static final ResourceLocation GRAVITY_ID = AwId.of("command_moves_where_i_look");
   private static final String K_HEAD_X = "cmd_head_x";
   private static final String K_HEAD_Y = "cmd_head_y";
   private static final String K_HEAD_Z = "cmd_head_z";

   @Override
   public String id() {
      return "moves_where_i_look";
   }

   @Override
   public boolean holds() {
      return true;
   }

   @Override
   public int unlockLevel() {
      return 6;
   }

   @Override
   public double defaultWeight() {
      return 40.0;
   }

   @Override
   public boolean hasLevel() {
      return true;
   }

   @Override
   public String numberUnit() {
      return "awakened.command.unit.speed";
   }

   @Override
   public boolean supports(TargetKind kind) {
      return true;
   }

   @Override
   public double costFactor(TargetKind kind) {
      return kind == TargetKind.SELF ? 1.0 : 2.0;
   }

   @Override
   public boolean apply(ServerPlayer caster, LivingEntity target, int level, int age) {
      if (caster != null && !caster.isRemoved()) {
         double speed = 0.5 * (double)Math.max(1, level);
         if (target == caster) {
            return flySelf(caster, speed);
         } else {
            drive(target, caster.getLookAngle(), speed);
            return speed > 0.1;
         }
      } else {
         return false;
      }
   }

   private static boolean flySelf(ServerPlayer pilot, double speed) {
      PowerData data = PowerService.data(pilot);
      if (!CommandSupport.forwardHeld(pilot)) {
         seed(pilot, data);
         weightless(pilot, false);
         return false;
      } else {
         drive(pilot, carve(pilot, data), speed);
         return speed > 0.1;
      }
   }

   private static void drive(LivingEntity body, Vec3 heading, double speed) {
      body.setDeltaMovement(heading.scale(speed));
      body.hurtMarked = true;
      body.resetFallDistance();
      weightless(body, true);
   }

   private static void weightless(LivingEntity body, boolean on) {
      Attrs.set(body, Attributes.GRAVITY, GRAVITY_ID, on ? -1.0 : 0.0, Operation.ADD_MULTIPLIED_TOTAL);
   }

   @Override
   public void release(LivingEntity target) {
      weightless(target, false);
   }

   private static Vec3 carve(ServerPlayer pilot, PowerData data) {
      Vec3 look = pilot.getLookAngle();
      Vec3 dir = new Vec3((double)data.var("cmd_head_x", 0.0F), (double)data.var("cmd_head_y", 0.0F), (double)data.var("cmd_head_z", 0.0F));
      if (dir.lengthSqr() < 1.0E-4) {
         dir = look;
      }

      Vec3 carved = dir.add(look.subtract(dir).scale(0.35));
      if (carved.lengthSqr() < 1.0E-6) {
         carved = look;
      }

      carved = carved.normalize();
      store(data, carved);
      return carved;
   }

   private static void seed(ServerPlayer pilot, PowerData data) {
      store(data, pilot.getLookAngle());
   }

   private static void store(PowerData data, Vec3 dir) {
      data.setVar("cmd_head_x", (float)dir.x);
      data.setVar("cmd_head_y", (float)dir.y);
      data.setVar("cmd_head_z", (float)dir.z);
   }

   @Override
   public Clash clash(Target target, Effect effect, Trigger trigger) {
      if (!target.movesWithCaster()) {
         return null;
      } else if (trigger.postures().contains(Posture.STILL)) {
         return Clash.NEEDS_MOVING;
      } else {
         return trigger.postures().contains(Posture.FALLING) ? Clash.STOPS_THE_FALL : null;
      }
   }
}
