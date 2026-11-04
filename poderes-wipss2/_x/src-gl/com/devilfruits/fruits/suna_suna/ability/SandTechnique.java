package com.devilfruits.fruits.suna_suna.ability;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.logia.LogiaManager;
import com.devilfruits.fruits.suna_suna.SunaFx;
import com.devilfruits.fruits.suna_suna.SunaSuna;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.phys.Vec3;

public class SandTechnique extends Ability {
   public SandTechnique(String name, CastType type, AbilityStats stats) {
      super(SunaSuna.id(name), type, stats);
   }

   @Nullable
   @Override
   public ResourceKey<DamageType> damageType() {
      return SunaSuna.SAND_DAMAGE;
   }

   protected static void sandArm(ServerPlayer player, int ticks) {
      LogiaManager.partial(player, 4, ticks);
   }

   protected void placeholder(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      sandArm(player, 10);
      Vec3 hand = player.getEyePosition().add(ctx.look().scale(0.8)).subtract(0.0, 0.4, 0.0);
      ServerFx.send(player, SunaFx.SAND_BURST, hand, ctx.look(), 1.0F, ctx.seed(), 0);
   }

   @Override
   public void activate(AbilityContext ctx) {
      this.placeholder(ctx);
   }

   @Override
   public void release(AbilityContext ctx) {
      this.placeholder(ctx);
   }

   @Override
   public void onChannelStart(AbilityContext ctx) {
      this.placeholder(ctx);
   }

   @Override
   public boolean channelTick(AbilityContext ctx, int ticks) {
      return ticks < 20;
   }

   @Override
   public void onToggle(AbilityContext ctx, boolean active) {
      if (active) {
         this.placeholder(ctx);
      }
   }
}
