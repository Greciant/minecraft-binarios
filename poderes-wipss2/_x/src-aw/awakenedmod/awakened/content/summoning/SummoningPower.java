package com.awakenedmod.awakened.content.summoning;

import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.Power;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

public final class SummoningPower extends Power {
   public SummoningPower() {
      super("summoning", List.of(new SummonAbility(), new RecallAbility(), new CommandAbility(), new MassRaiseAbility(), new PossessAbility()));
   }

   @Override
   public int themeColor() {
      return -13049400;
   }

   @Override
   public int signatureSlot() {
      return 0;
   }

   @Override
   public void passiveTick(ServerPlayer player, PowerData data) {
      super.passiveTick(player, data);
      MinionSupport.tick(player, data);
      MinecraftServer server = player.serverLevel().getServer();
      if (server != null) {
         SummonFx.tickAll(server);
      }
   }

   @Override
   public void onKill(ServerPlayer killer, LivingEntity victim) {
      reportHostKill(killer, victim);
      if (SummonSouls.harvestable(victim) && killer.level() instanceof ServerLevel level) {
         DamageSource last = victim.getLastDamageSource();
         if (last == null || !(last.getEntity() instanceof Mob m) || !SummonAttachments.minion(m).isOwnedBy(killer.getUUID())) {
            SummonSouls.dropSoul(level, victim, killer.getUUID());
         }
      }
   }

   private static void reportHostKill(ServerPlayer killer, LivingEntity victim) {
      if (Foes.defeatedBy(victim, killer) && MinionSupport.count(killer) >= 3) {
         QuestSystem.feat(killer, "summon_host_kill");
      }
   }

   @Override
   public void onReplaced(ServerPlayer player) {
      ServerLevel var3 = player.serverLevel();
      if (var3 instanceof ServerLevel) {
         MinionSupport.recallAllIn(var3, player);
      }
   }

   @Override
   public void onSuppressed(ServerPlayer player, PowerData data) {
      this.onReplaced(player);
   }

   @Override
   public void onAwaken(ServerPlayer player) {
      ServerLevel c = player.serverLevel();
      if (c instanceof ServerLevel) {
         Vec3 var4 = player.position().add(0.0, 1.0, 0.0);
         Fx.burst(c, ParticleTypes.SCULK_SOUL, var4, 60, 0.6, 0.05);
         Fx.burst(c, ParticleTypes.SOUL, player.position().add(0.0, 0.1, 0.0), 30, 0.5, 0.1);
         Fx.sound(c, player.position(), (SoundEvent)SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.0F, 0.4F);
         Fx.sound(c, player.position(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.0F, 0.6F);
      }
   }
}
