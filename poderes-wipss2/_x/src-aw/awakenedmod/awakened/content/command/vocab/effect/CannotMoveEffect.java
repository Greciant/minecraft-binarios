package com.awakenedmod.awakened.content.command.vocab.effect;

import com.awakenedmod.awakened.content.chronokinetic.ChronoSupport;
import com.awakenedmod.awakened.content.command.vocab.Effect;
import com.awakenedmod.awakened.content.command.vocab.TargetKind;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class CannotMoveEffect implements Effect {
   private static final Map<UUID, Vec3> ANCHORS = new ConcurrentHashMap<>();

   @Override
   public String id() {
      return "cannot_move";
   }

   @Override
   public int unlockLevel() {
      return 4;
   }

   @Override
   public double defaultWeight() {
      return 60.0;
   }

   @Override
   public boolean supports(TargetKind kind) {
      return kind != TargetKind.SELF;
   }

   @Override
   public boolean holds() {
      return true;
   }

   @Override
   public int durationTicks(LivingEntity target, int written) {
      return target instanceof Player ? Math.max(1, written / 2) : written;
   }

   @Override
   public boolean apply(ServerPlayer caster, LivingEntity target, int level, int age) {
      if (age == 0) {
         ChronoSupport.freeze(target);
         if (target instanceof Player) {
            ANCHORS.put(target.getUUID(), target.position());
         }
      }

      if (target instanceof Player) {
         ChronoSupport.hold(target, ANCHORS.computeIfAbsent(target.getUUID(), id -> target.position()));
         return true;
      } else {
         return target.isAlive();
      }
   }

   @Override
   public void release(LivingEntity target) {
      ChronoSupport.release(target);
      ANCHORS.remove(target.getUUID());
   }
}
