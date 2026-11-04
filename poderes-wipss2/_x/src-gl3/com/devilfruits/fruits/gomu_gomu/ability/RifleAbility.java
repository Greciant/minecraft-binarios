package com.devilfruits.fruits.gomu_gomu.ability;

import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.fruits.gomu_gomu.GomuSounds;
import com.devilfruits.fruits.gomu_gomu.strike.Breaker;
import com.devilfruits.fruits.gomu_gomu.strike.StrikeKind;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

public class RifleAbility extends StrikeAbility {
   public RifleAbility() {
      super(
         "rifle",
         AbilityStats.builder()
            .energy(22)
            .cooldown(300)
            .mastery(7)
            .damage(36.0)
            .range(12.0)
            .knockback(1.4)
            .extra("rifleMaxPierce", 3.0, 0.0, 16.0, "Bodies the Rifle bores through before the next one stops it.")
            .blockDamage(true)
            .extra("rifleMaxHardness", 2.0, 0.0, 50.0, "The hardest block the corkscrew bores through (2: cobblestone, wood; stone is 1.5).")
            .extra("rifleMaxResistance", 6.0, 0.0, 1200.0, "The toughest block against blasts it bores through (6: stone, cobblestone, bricks).")
            .extra("rifleMaxBlocks", 24.0, 0.0, 256.0, "Blocks one Rifle breaks at most.")
            .build()
      );
   }

   @Override
   protected Breaker breaker(AbilityContext ctx) {
      return new Breaker(ctx.extra("rifleMaxHardness"), ctx.extra("rifleMaxResistance"), 0.6, (int)ctx.extra("rifleMaxBlocks"));
   }

   @Override
   protected StrikeKind kind() {
      return StrikeKind.RIFLE;
   }

   @Override
   protected float ratio() {
      return 6.0F;
   }

   @Override
   protected int pierce(AbilityContext ctx) {
      return (int)ctx.extra("rifleMaxPierce");
   }

   @Override
   public void activate(AbilityContext ctx) {
      super.activate(ctx);
      ServerPlayer p = ctx.player();
      p.level().playSound(null, p.getX(), p.getY() + 1.2, p.getZ(), GomuSounds.RIFLE_TWIST.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
   }

   @Override
   protected ResourceLocation clip(int[] arms) {
      return sided("rifle", arms);
   }

   @Override
   protected SoundEvent hitSound() {
      return GomuSounds.RIFLE_HIT.get();
   }
}
