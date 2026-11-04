package com.devilfruits.fruits.uo_uo_seiryu.ability;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.form.FormFlags;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuFx;
import com.devilfruits.fruits.uo_uo_seiryu.UoUoSeiryu;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class ThundercladVeilAbility extends Ability {
   public static final int FLAG = FormFlags.fruit(2);
   public static final int GATHER_TICKS = 8;
   public static final int COCOON_TICKS = 16;
   private static final Map<UUID, Long> STARTED = new HashMap<>();

   public ThundercladVeilAbility() {
      super(
         UoUoSeiryu.id("thunderclad_veil"),
         CastType.TOGGLE,
         AbilityStats.builder()
            .energy(30)
            .cooldown(400)
            .mastery(4)
            .damage(0.0)
            .range(0.0)
            .knockback(0.0)
            .extra("durationSeconds", 30.0, 1.0, 600.0, "How long the veil lasts (pressing again ends it early).")
            .extra("damageBonus", 0.1, 0.0, 2.0, "Extra damage dealt while veiled (fraction).")
            .extra("damageReduction", 0.25, 0.0, 0.9, "Fraction of incoming damage the veil absorbs (after the Hybrid's own reduction).")
            .build()
      );
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.NONE;
   }

   @Override
   public boolean pausesRegeneration() {
      return false;
   }

   @Override
   public void onToggle(AbilityContext ctx, boolean active) {
      ServerPlayer player = ctx.player();
      if (active) {
         STARTED.put(player.getUUID(), player.level().getGameTime());
         int duration = this.durationTicks();
         AbilityManager.setBusy(player, 24);
         HybridCombat.root(player, 24);
         ServerFx.animate(player, UoUoSeiryu.anim("hybrid/thunderclad_veil"));
         FormFlags.set(player, FLAG, true);
         ServerFx.send(player, SeiryuFx.VEIL, player.position(), Vec3.ZERO, 1.0F, ctx.seed(), duration);
      } else {
         STARTED.remove(player.getUUID());
         FormFlags.set(player, FLAG, false);
         ServerFx.send(player, SeiryuFx.VEIL, player.position(), Vec3.ZERO, 0.0F, ctx.seed(), 0);
      }
   }

   @Override
   public void toggleTick(AbilityContext ctx) {
      if (this.remainingTicks(ctx.player()) <= 0) {
         AbilityManager.stopToggle(ctx.player(), this);
      }
   }

   @Override
   public void onStartTracking(ServerPlayer user, ServerPlayer tracker) {
      int left = this.remainingTicks(user);
      if (left > 0) {
         ServerFx.sendTo(tracker, SeiryuFx.VEIL, user, user.position(), Vec3.ZERO, 0.0F, 0L, left);
      }
   }

   public int durationTicks() {
      return (int)Math.round(this.config().extra("durationSeconds") * 20.0);
   }

   public int remainingTicks(ServerPlayer player) {
      Long start = STARTED.get(player.getUUID());
      return start != null && HybridCombat.veiled(player) ? (int)Math.max(0L, start + (long)this.durationTicks() - player.level().getGameTime()) : 0;
   }

   public void backdate(ServerPlayer player, int ticks) {
      STARTED.computeIfPresent(player.getUUID(), (k, v) -> v - (long)ticks);
   }
}
