package com.devilfruits.fruits.gomu_gomu.ability;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.logia.LogiaManager;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.core.rules.Mastery;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.fruits.gomu_gomu.GomuAbilities;
import com.devilfruits.fruits.gomu_gomu.GomuGomu;
import com.devilfruits.fruits.gomu_gomu.GomuSounds;
import com.devilfruits.fruits.gomu_gomu.strike.Breaker;
import com.devilfruits.fruits.gomu_gomu.strike.Strike;
import com.devilfruits.fruits.gomu_gomu.strike.StrikeKind;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.Vec3;

public abstract class StrikeAbility extends Ability {
   private static final Map<UUID, Integer> LAST_ARM = new ConcurrentHashMap<>();

   protected StrikeAbility(String name, AbilityStats stats) {
      super(GomuGomu.id(name), CastType.INSTANT, stats);
   }

   protected abstract StrikeKind kind();

   protected abstract float ratio();

   protected abstract ResourceLocation clip(int[] var1);

   protected abstract SoundEvent hitSound();

   protected int pierce(AbilityContext ctx) {
      return 0;
   }

   @Nullable
   protected Breaker breaker(AbilityContext ctx) {
      return null;
   }

   @Override
   public boolean coatable() {
      return true;
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.builder(this.defaults()).blockDamage(0.0).manual().build();
   }

   @Override
   public void activate(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      int[] arms = this.kind().twoArms() ? new int[]{1, -1} : new int[]{nextArm(player)};
      ServerFx.animate(player, ctx.animation(this.clip(arms)));
      player.level()
         .playSound(
            null,
            player.getX(),
            player.getY() + 1.2,
            player.getZ(),
            GomuSounds.STRETCH.get(),
            SoundSource.PLAYERS,
            0.9F,
            0.9F + player.getRandom().nextFloat() * 0.2F
         );
      Strike.start(ctx, this.kind(), ctx.config().range(), this.damage(ctx), this.pierce(ctx), arms, this::onHit).breaks(this.breaker(ctx));
   }

   public Strike prepare(AbilityContext ctx, int arm) {
      int[] arms = this.kind().twoArms() ? new int[]{1, -1} : new int[]{arm};
      return Strike.create(ctx, this.kind(), ctx.config().range(), this.damage(ctx), this.pierce(ctx), arms, this::onHit).breaks(this.breaker(ctx));
   }

   public float damage(AbilityContext ctx) {
      float scale = Mastery.damageMultiplier(ctx.mastery()) * ctx.modifiers().damage() * LogiaManager.abilityPower(ctx.player());
      return (float)GomuAbilities.PISTOL.config().damage() * this.ratio() * scale;
   }

   protected void onHit(Strike strike, int arm, Vec3 at, @Nullable Entity target) {
      AbilityContext ctx = strike.ctx;
      if (target != null && ctx.hurt(target, strike.damage)) {
         Targeting.launch(target, strike.aim.multiply(1.0, 0.4, 1.0).normalize().scale((double)ctx.knockback()).add(0.0, 0.3, 0.0));
      }

      strike.nudge(at, (double)strike.damage * 0.6 + (double)ctx.knockback());
      ctx.level().playSound(null, at.x, at.y, at.z, this.hitSound(), SoundSource.PLAYERS, 1.0F, 0.9F + ctx.level().random.nextFloat() * 0.2F);
   }

   static int nextArm(ServerPlayer player) {
      int main = player.getMainArm() == HumanoidArm.RIGHT ? 1 : -1;
      Integer last = LAST_ARM.get(player.getUUID());
      int arm = last == null ? main : -last;
      LAST_ARM.put(player.getUUID(), arm);
      return arm;
   }

   public static void forget(UUID player) {
      LAST_ARM.remove(player);
   }

   protected static ResourceLocation sided(String name, int[] arms) {
      return GomuGomu.id(name + (arms[0] > 0 ? "_right" : "_left"));
   }
}
