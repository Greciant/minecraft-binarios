package com.awakenedmod.awakened.content.pocket;

import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.PowerSuppression;
import com.awakenedmod.awakened.core.TeleportGuard;
import com.awakenedmod.awakened.core.power.Power;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class RiftPower extends Power {
   public RiftPower() {
      super("pocket", List.of(new RiftAbility(), new RiftBlinkAbility(), new RecursiveDropAbility(), new SurfaceAbility(), new DomainExpansionAbility()));
      PowerSuppression.register(DomainManager::isSealedByForeignDome);
      TeleportGuard.register(DomainManager::crossesDomeShell);
   }

   @Override
   public int themeColor() {
      return -7712769;
   }

   @Override
   public int signatureSlot() {
      return 0;
   }

   @Override
   public void onKill(ServerPlayer killer, LivingEntity victim) {
      if (RiftSpace.isExiledBy(victim, killer.getUUID()) && Foes.defeatedBy(victim, killer)) {
         QuestSystem.feat(killer, "rift_exile");
      }
   }

   @Override
   public void onAwaken(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      Vec3 pos = player.position().add(0.0, 1.0, 0.0);
      Fx.burst(level, ParticleTypes.PORTAL, player.getEyePosition(), 120, 1.0, 0.8);
      Fx.burst(level, ParticleTypes.REVERSE_PORTAL, pos, 40, 0.8, 0.3);
      Fx.sound(level, pos, SoundEvents.PORTAL_TRIGGER, SoundSource.PLAYERS, 0.45F, 0.7F);
      Fx.sound(level, pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6F, 0.8F);
   }
}
