package com.awakenedmod.awakened.content.pyrokinetic;

import com.awakenedmod.awakened.core.caster.PlayerCaster;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.HumanoidArm;

public final class FireballAbility extends Ability {
   public static final int COOLDOWN_TICKS = 12;
   public static final double HAND_WIDTH = 0.34;
   public static final double HAND_DROP = 0.46;
   public static final double HAND_REACH = 0.6;
   private final Map<UUID, Long> lastCast = this.playerState();
   private final Map<UUID, Long> coolWarned = this.playerState();
   private final Map<UUID, Boolean> nextOffHand = this.playerState();
   private final Map<UUID, FireballEngine.Throw> pending = this.playerState();
   private final List<Ability> channels;

   public FireballAbility(List<Ability> channels) {
      super("fireball", 6.0F, 0);
      this.channels = channels;
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().damage(5.0).build();
   }

   @Override
   public boolean cancelsOnDimensionChange() {
      return true;
   }

   @Override
   public String activationAnimation(ServerPlayer player) {
      boolean offHand = !this.nextOffHand.getOrDefault(player.getUUID(), true);
      boolean rightArm = player.getMainArm() == HumanoidArm.RIGHT != offHand;
      return rightArm ? "fireball_throw_r" : "fireball_throw_l";
   }

   @Override
   public boolean activate(ServerPlayer player) {
      for (Ability channel : this.channels) {
         if (channel.ownsBody(player)) {
            return this.failSilently(player);
         }
      }

      long now = player.serverLevel().getGameTime();
      Long last = this.lastCast.get(player.getUUID());
      if (last == null || now - last >= 12L) {
         boolean offHand = this.nextOffHand.getOrDefault(player.getUUID(), false);
         this.pending.put(player.getUUID(), FireballEngine.windUp(PlayerCaster.of(player), offHand));
         this.lastCast.put(player.getUUID(), now);
         this.nextOffHand.put(player.getUUID(), !offHand);
         return true;
      } else if (last.equals(this.coolWarned.get(player.getUUID()))) {
         return this.failSilently(player);
      } else {
         this.coolWarned.put(player.getUUID(), last);
         return this.fail(player, "awakened.fail.busy", new Object[0]);
      }
   }

   @Override
   public void serverTick(ServerPlayer player) {
      FireballEngine.Throw wound = this.pending.get(player.getUUID());
      if (wound != null) {
         PlayerCaster caster = PlayerCaster.of(player);
         if (FireballEngine.isDue(caster, wound)) {
            this.pending.remove(player.getUUID());
            if (player.isAlive()) {
               FireballEngine.release(caster, wound);
            }
         }
      }
   }
}
