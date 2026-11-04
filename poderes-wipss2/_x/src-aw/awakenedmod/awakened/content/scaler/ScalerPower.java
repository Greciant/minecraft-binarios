package com.awakenedmod.awakened.content.scaler;

import com.awakenedmod.awakened.content.AwakenedItems;
import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Headroom;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.CraftRecipe;
import com.awakenedmod.awakened.core.power.Power;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import java.util.List;
import java.util.Map;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.phys.Vec3;

public final class ScalerPower extends Power {
   public static final String ID = "scaler";
   private static final int THEME = -10755928;
   private static final int DODGE_WINDOW_TICKS = 10;
   private static final double DODGE_RADIUS = 4.0;
   private static final int DODGE_GATE_TICKS = 200;
   private static final int SWAP_KILL_WINDOW_TICKS = 60;
   private static final int SQUEEZE_GATE_TICKS = 600;
   private static final int SQUEEZE_CHECK_INTERVAL = 10;
   private static final double SQUEEZE_MIN_MOVE_SQR = 1.0E-4;
   private static final String K_LAST_DODGE_AWARD = "scaler_last_dodge";
   private static final String K_LAST_SQUEEZE_AWARD = "scaler_last_squeeze";

   public ScalerPower() {
      super("scaler", List.of(new SizeChangeAbility(), new MassLeapAbility(), new HitchStompAbility(), new ScaleRayAbility(), new ClamberSeizeAbility()));
   }

   @Override
   public int themeColor() {
      return -10755928;
   }

   @Override
   public int signatureSlot() {
      return 0;
   }

   @Override
   public List<CraftRecipe> recipes() {
      return List.of(
         CraftRecipe.shaped(
            (ItemLike)AwakenedItems.CONTAINMENT_JAR.get(),
            List.of("G G", "GSG", "NGN"),
            Map.of('G', Items.GLASS, 'S', Items.SLIME_BALL, 'N', Items.IRON_NUGGET)
         )
      );
   }

   @Override
   public void passiveTick(ServerPlayer player, PowerData data) {
      super.passiveTick(player, data);
      Scaling.update(player, data);
      if (player.tickCount % 4 == 0 && (Math.abs(Scaling.smoothedScale(data) - 1.0F) > 0.01F || Scaling.stage(data) != SizeStage.NORMAL)) {
         PowerService.sync(player);
      }

      tickShrinkDodge(player, data);
      tickSqueeze(player, data);
   }

   private static void tickShrinkDodge(ServerPlayer player, PowerData data) {
      if (!(data.var("scaler_shift_dir", 0.0F) >= 0.0F)) {
         long gameTime = player.serverLevel().getGameTime();
         long lastShift = (long)data.var("scaler_last_shift", 0.0F);
         if (lastShift > 0L && gameTime - lastShift <= 10L) {
            if (gameTime - (long)data.var("scaler_last_dodge", 0.0F) >= 200L) {
               boolean menaced = !player.serverLevel()
                  .getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(4.0), mob -> mob.getTarget() == player)
                  .isEmpty();
               if (menaced) {
                  data.setVar("scaler_last_dodge", (float)gameTime);
                  MasterySystem.award(player, MasteryReason.SURVIVAL);
               }
            }
         }
      }
   }

   private static void tickSqueeze(ServerPlayer player, PowerData data) {
      if (player.tickCount % 10 == 0 && !(Scaling.stage(data).scale() > SizeStage.MITE.scale())) {
         long gameTime = player.serverLevel().getGameTime();
         if (gameTime - (long)data.var("scaler_last_squeeze", 0.0F) >= 600L) {
            if (!(player.getKnownMovement().horizontalDistanceSqr() < 1.0E-4)) {
               if (!Headroom.fits(player, (double)SizeStage.NORMAL.scale())) {
                  data.setVar("scaler_last_squeeze", (float)gameTime);
                  MasterySystem.award(player, MasteryReason.SURVIVAL);
               }
            }
         }
      }
   }

   @Override
   public float onIncomingDamage(ServerPlayer player, DamageSource source, float amount) {
      float current = super.onIncomingDamage(player, source, amount);
      return current * Scaling.stage(PowerService.data(player)).damageTakenMult();
   }

   @Override
   public void onDealDamage(ServerPlayer attacker, LivingEntity victim) {
      super.onDealDamage(attacker, victim);
      if (Scaling.stage(PowerService.data(attacker)).scale() >= SizeStage.BRUTE.scale()) {
         MasterySystem.award(attacker, MasteryReason.HIT, victim);
      }
   }

   @Override
   public void onKill(ServerPlayer killer, LivingEntity victim) {
      super.onKill(killer, victim);
      if (Foes.countsAsKill(killer, victim)) {
         PowerData data = PowerService.data(killer);
         long lastShift = (long)data.var("scaler_last_shift", 0.0F);
         if (lastShift > 0L && killer.level().getGameTime() - lastShift <= 60L) {
            MasterySystem.award(killer, MasteryReason.KILL);
         }
      }
   }

   @Override
   public void onAwaken(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      Vec3 feet = player.position();
      Vfx.ring(level, feet.add(0.0, 0.1, 0.0), 2.6, 6021288, 16);
      Vfx.ring(level, feet.add(0.0, 1.0, 0.0), 1.6, 12582880, 12);
      Fx.burst(level, ParticleTypes.CLOUD, feet.add(0.0, 0.6, 0.0), 30, 0.9, 0.05);
      Fx.sound(level, feet, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2F, 0.7F);
      player.displayClientMessage(Component.translatable("awakened.scaler.hint"), true);
   }

   @Override
   public boolean hasResourceBar() {
      return true;
   }

   @Override
   public int resourceBarColor() {
      return -10755928;
   }

   @Override
   public Component resourceBarLabel() {
      return Component.translatable("awakened.scaler.bar");
   }

   @Override
   public float resourceBarFraction(ServerPlayer player, PowerData data) {
      return (float)Scaling.stage(data).ordinal() / (float)(SizeStage.count() - 1);
   }

   @Override
   public boolean resourceBarVertical() {
      return true;
   }

   @Override
   public Component resourceBarBadge(Player player, float fraction) {
      int ordinal = Mth.clamp(Math.round(fraction * (float)(SizeStage.count() - 1)), 0, SizeStage.count() - 1);
      return SizeStage.byOrdinal(ordinal).displayName();
   }

   @Override
   public ItemStack resourceBarBadgeIcon(Player player) {
      return new ItemStack(Items.SLIME_BALL);
   }
}
