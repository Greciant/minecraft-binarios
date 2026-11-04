package com.awakenedmod.awakened.content.pyrokinetic;

import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.caster.PlayerCaster;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class BackdraftAbility extends Ability {
   public static final String ANIMATION = "backdraft";
   private static final int UNLOCK = 10;
   public static final int STARVE_TICKS = 43;
   private static final int CLIP_RECOVER_TICKS = 11;
   private static final int BODY_TICKS = 64;
   private final Map<UUID, BackdraftEngine.Cast> casts = this.activeState();
   private final Map<UUID, Long> lastCast = this.playerState();
   private final Map<UUID, Boolean> gestures = this.playerState();
   private final List<Ability> channels;

   public BackdraftAbility(List<Ability> channels) {
      super("backdraft", 35.0F, 10);
      this.channels = channels;
   }

   @Override
   public boolean ownsBody(ServerPlayer player) {
      if (this.isActive(player)) {
         return true;
      } else {
         Long last = this.lastCast.get(player.getUUID());
         return last != null && player.serverLevel().getGameTime() < last + 64L;
      }
   }

   @Override
   public String activationAnimation(ServerPlayer player) {
      return null;
   }

   @Override
   public boolean cancelsOnDimensionChange() {
      return true;
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().boostsDamage().build();
   }

   @Override
   public boolean activate(ServerPlayer player) {
      if (this.casts.containsKey(player.getUUID())) {
         return this.fail(player, "awakened.fail.busy", new Object[0]);
      } else {
         for (Ability channel : this.channels) {
            if (channel.ownsBody(player)) {
               return this.fail(player, "awakened.fail.busy", new Object[0]);
            }
         }

         PlayerCaster caster = PlayerCaster.of(player);
         if (!BackdraftEngine.hasAir(caster)) {
            return this.fail(player, "awakened.pyrokinetic.backdraft_no_air", new Object[0]);
         } else {
            this.casts.put(player.getUUID(), BackdraftEngine.begin(caster));
            this.lastCast.put(player.getUUID(), player.serverLevel().getGameTime());
            PowerService.playAnimation(player, "backdraft", true);
            this.gestures.put(player.getUUID(), Boolean.TRUE);
            return true;
         }
      }
   }

   @Override
   public void serverTick(ServerPlayer player) {
      this.closeGesture(player);
      BackdraftEngine.Cast cast = this.casts.get(player.getUUID());
      if (cast != null) {
         if (!BackdraftEngine.tick(PlayerCaster.of(player), cast)) {
            this.casts.remove(player.getUUID());
         } else {
            BackdraftEngine.Blast blast = cast.takeBlast();
            if (blast != null) {
               reported(player, blast);
            }
         }
      }
   }

   private static void reported(ServerPlayer player, BackdraftEngine.Blast blast) {
      player.displayClientMessage(
         Component.translatable("awakened.pyrokinetic.backdraft_fires", new Object[]{blast.snuffed(), Math.round(blast.bonus())}), true
      );
      MasterySystem.award(player, MasteryReason.HIT, blast.hit());
      int threats = 0;

      for (LivingEntity victim : blast.hit()) {
         if (Foes.counts(player, victim)) {
            threats++;
         }
      }

      if (threats >= 3) {
         QuestSystem.feat(player, "pyro_backdraft_blast");
      }
   }

   private void closeGesture(ServerPlayer player) {
      UUID id = player.getUUID();
      Long last = this.lastCast.get(id);
      if (this.gestures.containsKey(id) && last != null && player.serverLevel().getGameTime() + 1L >= last + 64L) {
         this.gestures.remove(id);
         PowerService.forgetHeldAnimation(player);
      }
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      boolean casting = this.casts.remove(player.getUUID()) != null;
      boolean gesturing = this.gestures.remove(player.getUUID()) != null;
      if (casting) {
         PowerService.stopAnimation(player);
      } else if (gesturing) {
         PowerService.forgetHeldAnimation(player);
      }

      super.clearPlayer(player);
   }
}
