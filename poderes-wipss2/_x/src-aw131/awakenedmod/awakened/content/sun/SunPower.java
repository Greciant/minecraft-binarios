package com.awakenedmod.awakened.content.sun;

import com.awakenedmod.awakened.content.AwakenedItems;
import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.CraftRecipe;
import com.awakenedmod.awakened.core.power.Power;
import java.util.List;
import java.util.Map;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public final class SunPower extends Power {
   public static final String ID = "sun";
   private static final float SUN_MAX_STAMINA = 50.0F;
   private static final float SUN_STAMINA_REGEN = 8.0F;
   private static final float AXE_STAMINA_MULT = 1.5F;

   public SunPower() {
      super("sun", List.of(new SolarAscensionAbility(), new SunOrbAbility(), new SunPillarAbility(), new SolarOnslaughtAbility(), new PersonalSunAbility()));
   }

   @Override
   public int themeColor() {
      return -15826;
   }

   @Override
   public int signatureSlot() {
      return 0;
   }

   @Override
   public void passiveTick(ServerPlayer player, PowerData data) {
      super.passiveTick(player, data);
      Solar.update(player, data);
      if (player.tickCount % 4 == 0 && (Solar.level(data) > 0.01F || Solar.steps(data) > 0 || Solar.personalSun(player, data))) {
         PowerService.sync(player);
      }
   }

   @Override
   public void onAwaken(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      Vec3 center = player.position().add(0.0, 1.0, 0.0);
      Vfx.sphere(level, center, 2.2, 16761390, 18);
      Vfx.ring(level, player.position().add(0.0, 0.1, 0.0), 3.0, 16770688, 16);
      Fx.burst(level, ParticleTypes.END_ROD, center, 80, 1.0, 0.3);
      Fx.burst(level, ParticleTypes.FLAME, center, 30, 0.8, 0.2);
      Fx.sound(level, player.position(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2F, 1.2F);
      Fx.sound(level, player.position(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 0.8F);
   }

   @Override
   public boolean hasResourceBar() {
      return true;
   }

   @Override
   public int resourceBarColor() {
      return -14022;
   }

   @Override
   public Component resourceBarLabel() {
      return Component.translatable("awakened.sun.bar");
   }

   @Override
   public float resourceBarFraction(ServerPlayer player, PowerData data) {
      return Solar.normalized(data);
   }

   @Override
   public float resourceBarMark(ServerPlayer player, PowerData data) {
      return Solar.ascensionMark(player, data);
   }

   @Override
   public boolean resourceBarVertical() {
      return true;
   }

   @Override
   public Component resourceBarBadge(Player player, float fraction) {
      long bonus = Math.round(Solar.attackBonus(fraction, Solar.hasAxe(player)));
      return Component.literal((bonus >= 0L ? "+" : "") + bonus);
   }

   @Override
   public ItemStack resourceBarBadgeIcon(Player player) {
      return new ItemStack(Items.GOLDEN_SWORD);
   }

   private static float staminaScale(ServerPlayer sp) {
      return Solar.exposure(sp, PowerService.data(sp)) * (Solar.hasAxe(sp) ? 1.5F : 1.0F);
   }

   @Override
   public float bonusMaxStamina(Player player) {
      return player instanceof ServerPlayer sp ? staminaScale(sp) * 50.0F : 0.0F;
   }

   @Override
   public float bonusStaminaRegen(Player player) {
      return player instanceof ServerPlayer sp ? staminaScale(sp) * 8.0F : 0.0F;
   }

   @Override
   public List<CraftRecipe> recipes() {
      return List.of(
         CraftRecipe.shaped((ItemLike)AwakenedItems.DORMANT_CORE.get(), List.of("GGG", "GNG", "GGG"), Map.of('G', Blocks.GOLD_BLOCK, 'N', Items.NETHER_STAR)),
         CraftRecipe.shaped((ItemLike)AwakenedItems.SUN_AXE_HEAD.get(), List.of("GG", "GG"), Map.of('G', Blocks.GOLD_BLOCK)),
         CraftRecipe.shaped((ItemLike)AwakenedItems.SUN_AXE_HANDLE.get(), List.of("I", "S", "S"), Map.of('I', Items.NETHERITE_INGOT, 'S', Items.STICK)),
         CraftRecipe.shaped(
            (ItemLike)AwakenedItems.BLANK_SUN_AXE.get(),
            List.of("H  ", " C ", "  L"),
            Map.of(
               'H',
               (ItemLike)AwakenedItems.SUN_AXE_HEAD.get(),
               'C',
               (ItemLike)AwakenedItems.DORMANT_CORE.get(),
               'L',
               (ItemLike)AwakenedItems.SUN_AXE_HANDLE.get()
            )
         )
      );
   }
}
