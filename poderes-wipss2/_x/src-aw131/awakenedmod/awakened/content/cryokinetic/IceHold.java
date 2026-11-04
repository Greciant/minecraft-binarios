package com.awakenedmod.awakened.content.cryokinetic;

import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.TickFreeze;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

final class IceHold {
   private static final int PLAYER_LOCK = 250;
   private final Map<UUID, List<IceHold.Held>> held;

   IceHold(Map<UUID, List<IceHold.Held>> state) {
      this.held = state;
   }

   void encase(ServerPlayer caster, ServerLevel level, LivingEntity target, int ticks) {
      if (target instanceof Player) {
         Harm.effect(caster, target, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 250, false, false));
         Harm.effect(caster, target, new MobEffectInstance(MobEffects.WEAKNESS, ticks, 4, false, false));
      } else {
         TickFreeze.freeze(target);
      }

      Frostbite.frostAtLeast(target, target.getTicksRequiredToFreeze());
      target.setDeltaMovement(Vec3.ZERO);
      target.hurtMarked = true;
      CryoFx.encase(level, target.position(), (double)target.getBbWidth(), (double)target.getBbHeight(), ticks);
      Frostbite.apply(caster, target, 5);
      this.held.computeIfAbsent(caster.getUUID(), k -> new ArrayList<>()).add(new IceHold.Held(target, level, target.position(), ticks));
   }

   boolean holds(ServerPlayer caster, LivingEntity target) {
      List<IceHold.Held> list = this.held.get(caster.getUUID());
      return list != null && list.stream().anyMatch(h -> h.target == target);
   }

   void tick(ServerPlayer caster) {
      List<IceHold.Held> list = this.held.get(caster.getUUID());
      if (list != null && !list.isEmpty()) {
         Iterator<IceHold.Held> it = list.iterator();

         while (it.hasNext()) {
            IceHold.Held h = it.next();
            boolean gone = !h.target.isAlive() || h.target.isRemoved() || h.target.level() != h.level;
            if (!gone && --h.ticksLeft > 0) {
               h.target.setDeltaMovement(Vec3.ZERO);
               h.target.setPos(h.lockPos.x, h.lockPos.y, h.lockPos.z);
               h.target.resetFallDistance();
               h.target.hurtMarked = true;
               Frostbite.frostAtLeast(h.target, h.target.getTicksRequiredToFreeze());
            } else {
               if (gone && Foes.defeatedBy(h.target, caster)) {
                  QuestSystem.feat(caster, "cryo_shatter");
               }

               release(h);
               it.remove();
            }
         }

         if (list.isEmpty()) {
            this.held.remove(caster.getUUID());
         }
      }
   }

   void releaseAll(ServerPlayer caster) {
      List<IceHold.Held> list = this.held.remove(caster.getUUID());
      if (list != null) {
         list.forEach(IceHold::release);
      }
   }

   private static void release(IceHold.Held h) {
      if (!(h.target instanceof Player)) {
         TickFreeze.release(h.target);
      } else {
         MobEffectInstance lock = h.target.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
         if (lock != null && lock.getAmplifier() == 250) {
            h.target.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
         }
      }
   }

   static final class Held {
      private final LivingEntity target;
      private final ServerLevel level;
      private final Vec3 lockPos;
      private int ticksLeft;

      private Held(LivingEntity target, ServerLevel level, Vec3 lockPos, int ticksLeft) {
         this.target = target;
         this.level = level;
         this.lockPos = lockPos;
         this.ticksLeft = ticksLeft;
      }
   }
}
