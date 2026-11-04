package com.devilfruits.fruits.doru_doru.ability;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.config.AbilityConfig;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.form.FormFlags;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.fruits.doru_doru.DoruAbilities;
import com.devilfruits.fruits.doru_doru.DoruDoru;
import com.devilfruits.fruits.doru_doru.atelier.BuildEngine;
import com.devilfruits.fruits.doru_doru.atelier.WaxIntegrity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class WaxAtelierAbility extends Ability {
   public WaxAtelierAbility() {
      super(
         DoruDoru.id("wax_atelier"),
         CastType.TOGGLE,
         AbilityStats.builder()
            .energy(0)
            .cooldown(10)
            .mastery(2)
            .damage(0.0)
            .range(24.0)
            .extra("reachAtMastery2", 24.0, 4.0, 128.0, "Sculpting reach in blocks at mastery 2.")
            .extra("reachAtMastery10", 40.0, 4.0, 128.0, "Sculpting reach in blocks at mastery 10.")
            .extra("jobSizeAtMastery2", 64.0, 1.0, 100000.0, "Largest job (blocks of wax) at mastery 2.")
            .extra("jobSizeAtMastery10", 4096.0, 1.0, 100000.0, "Largest job (blocks of wax) at mastery 10.")
            .extra("voxelsPerTickAtMastery2", 64.0, 1.0, 100000.0, "Sub-voxels (64 per block) a job pours per tick at mastery 2.")
            .extra("voxelsPerTickAtMastery10", 512.0, 1.0, 100000.0, "Sub-voxels a job pours per tick at mastery 10.")
            .extra("maxBuildSeconds", 12.0, 1.0, 600.0, "Longest a job may take: bigger jobs pour faster to finish in this time.")
            .extra("minBuildTicks", 16.0, 1.0, 200.0, "Shortest a job takes (ticks), so even a small pour is seen.")
            .extra("baseCantilever", 3.0, 0.0, 64.0, "Blocks a thin wax part may reach out from its support (plus twice its thickness).")
            .extra("maxCantilever", 24.0, 1.0, 256.0, "Cap of any part's reach from support (doubled for a part held at both ends).")
            .extra("maxStructureCheck", 8192.0, 64.0, 1000000.0, "Most blocks one structure check visits (larger structures count as supported).")
            .extra("undoDepth", 20.0, 0.0, 200.0, "Operations one player can undo.")
            .extra("carveRefundShare", 0.5, 0.0, 1.0, "Share of the wax's energy carving and dissolving give back.")
            .build()
      );
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.builder(this.defaults()).manual().build();
   }

   @Override
   public Component checkRequirements(ServerPlayer player, PlayerFruitData data) {
      return DoruDoru.heatBlock(player);
   }

   @Override
   public void onToggle(AbilityContext ctx, boolean active) {
      FormFlags.set(ctx.player(), DoruDoru.SCULPTING, active);
      if (!active) {
         BuildEngine.cancel(ctx.player(), false);
      }
   }

   @Override
   public void toggleTick(AbilityContext ctx) {
      if (DoruDoru.heatBlock(ctx.player()) != null) {
         AbilityManager.stopToggle(ctx.player(), this);
      }
   }

   private static AbilityConfig cfg() {
      return DoruAbilities.ATELIER.config();
   }

   public static double scaled(double lo, double hi, int mastery) {
      double t = Math.max(0.0, Math.min(1.0, (double)(mastery - 2) / 8.0));
      return !(lo <= 0.0) && !(hi <= 0.0) ? lo * Math.pow(hi / lo, t) : lo + (hi - lo) * t;
   }

   public static double reach(int mastery) {
      return scaled(cfg().extra("reachAtMastery2"), cfg().extra("reachAtMastery10"), mastery);
   }

   public static double jobSize(int mastery) {
      return scaled(cfg().extra("jobSizeAtMastery2"), cfg().extra("jobSizeAtMastery10"), mastery);
   }

   public static int voxelsPerTick(int mastery) {
      return (int)Math.round(scaled(cfg().extra("voxelsPerTickAtMastery2"), cfg().extra("voxelsPerTickAtMastery10"), mastery));
   }

   public static int maxBuildTicks() {
      return (int)(cfg().extra("maxBuildSeconds") * 20.0);
   }

   public static int minBuildTicks() {
      return (int)cfg().extra("minBuildTicks");
   }

   public static WaxIntegrity.Rules rules() {
      return new WaxIntegrity.Rules(cfg().extra("baseCantilever"), cfg().extra("maxCantilever"), (int)cfg().extra("maxStructureCheck"));
   }
}
