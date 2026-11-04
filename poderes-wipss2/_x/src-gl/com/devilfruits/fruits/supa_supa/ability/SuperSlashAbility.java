package com.devilfruits.fruits.supa_supa.ability;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.core.util.ServerScheduler;
import com.devilfruits.fruits.supa_supa.BladeLimbs;
import com.devilfruits.fruits.supa_supa.SupaSounds;
import com.devilfruits.fruits.supa_supa.SupaSupa;
import com.devilfruits.fruits.supa_supa.slash.SuperSlash;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

public class SuperSlashAbility extends Ability {
   public static final int CUT_BEAT = 2;

   public SuperSlashAbility() {
      super(
         SupaSupa.id("super_slash"),
         CastType.CHARGED,
         AbilityStats.builder()
            .energy(60)
            .cooldown(400)
            .mastery(8)
            .damage(28.0)
            .range(40.0)
            .knockback(0.9)
            .blockDamage(true)
            .extra("chargeTicks", 30.0, 5.0, 200.0, "Ticks to a full charge (the raised overhead pose).")
            .extra("height", 5.0, 1.0, 16.0, "Height of the cut's leading edge, blocks.")
            .extra("speed", 2.0, 0.5, 6.0, "Flight speed of the cut, blocks per tick.")
            .extra("maxBlocks", 400.0, 0.0, 4096.0, "Most terrain blocks one cut removes.")
            .extra("structureGlideSpeed", 0.15, 0.02, 2.0, "Speed of the blade gliding along a structure's cut plane, blocks per tick.")
            .extra("maxGlideTicks", 100.0, 10.0, 600.0, "Longest glide through one structure (long hulls are crossed faster), ticks.")
            .extra("exitDistance", 5.0, 0.0, 40.0, "How far the blade flies on after leaving a structure, blocks.")
            .extra("splitDelayTicks", 40.0, 0.0, 400.0, "Pause between the blade leaving a structure and the structure falling apart, ticks.")
            .extra(
               "cutAnimationSpeed",
               1.5,
               0.25,
               5.0,
               "How fast a cut through a structure plays, from the blade meeting it to the split: the glide and the pause run this many times faster than the timings above."
            )
            .extra("separationSpeed", 6.0, 0.0, 30.0, "Speed the two halves of a cut structure fly apart at, m/s (relative; mass-weighted, momentum kept).")
            .extra("maxSplitBlocks", 8192.0, 1.0, 200000.0, "Largest structure (in blocks) a Super Slash can cut in two.")
            .flag("splitStructures", true, "Super Slash cuts physics structures (Sable) in two; off: it flies through them.")
            .build()
      );
   }

   @Override
   public boolean coatable() {
      return true;
   }

   @Override
   public int maxChargeTicks() {
      return (int)this.config().extra("chargeTicks");
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.builder(this.defaults()).impact(PhysicsProfile.Impact.NONE).blockDamage(1.0).manual().build();
   }

   @Override
   public void onChargeStart(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      BladeLimbs.arms(player, BladeLimbs.ArmMode.X_BLADES, true, true, 0);
      ServerFx.animate(player, ctx.animation(SupaSupa.id("slash_raise")));
      player.level().playSound(null, player.getX(), player.getY() + 1.0, player.getZ(), SupaSounds.SLASH_CHARGE.get(), SoundSource.PLAYERS, 1.2F, 1.0F);
   }

   @Override
   public void release(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      ServerFx.animate(player, ctx.animation(SupaSupa.id("slash_cut")));
      BladeLimbs.arms(player, BladeLimbs.ArmMode.X_BLADES, true, true, 16);
      float charge = ctx.charge();
      ServerScheduler.later(ctx.level(), 2, () -> {
         if (!player.isRemoved() && SupaSupa.active(player)) {
            SuperSlash.launch(ctx, charge);
         }
      });
   }

   @Override
   public void onChargeCancel(ServerPlayer player) {
      BladeLimbs.clearArms(player, BladeLimbs.ArmMode.X_BLADES);
      ServerFx.stopAnimation(player);
   }
}
