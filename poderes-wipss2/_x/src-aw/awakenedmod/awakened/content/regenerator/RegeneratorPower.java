package com.awakenedmod.awakened.content.regenerator;

import com.awakenedmod.awakened.content.AwakenedItems;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.CraftRecipe;
import com.awakenedmod.awakened.core.power.Power;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

public final class RegeneratorPower extends Power {
   private static final int REGEN_INTERVAL = 40;
   private static final int BLEED_PER_HIT = 1;

   public RegeneratorPower() {
      super("regenerator", List.of(new ClawStrikeAbility(), new PredatorPounceAbility(), new RendAbility(), new PinAbility(), new FrenzyAbility()));
   }

   @Override
   public void passiveTick(ServerPlayer player, PowerData data) {
      MinecraftServer server = player.serverLevel().getServer();
      if (server != null) {
         Bleed.tickAll(server);
      }

      super.passiveTick(player, data);
      if (player.tickCount % 40 == 0 && player.getHealth() < player.getMaxHealth()) {
         player.heal(1.0F);
      }

      AdamantiumBonding.ensureAttributes(player, data);
   }

   @Override
   public void onSuppressed(ServerPlayer player, PowerData data) {
      AdamantiumBonding.clearAttributes(player);
      FrenzyAbility.endTrance(player);
   }

   @Override
   public void onReplaced(ServerPlayer player) {
      AdamantiumBonding.clearAttributes(player);
   }

   @Override
   public void onDealDamage(ServerPlayer attacker, LivingEntity victim, DamageSource source) {
      if (!Bleed.isApplyingTick() && (Dmg.inAbilityHit() || isClawSwing(attacker, source))) {
         Bleed.apply(attacker, victim, 1);
      }
   }

   private static boolean isClawSwing(ServerPlayer attacker, DamageSource source) {
      return source.is(DamageTypes.PLAYER_ATTACK) && source.getDirectEntity() == attacker && attacker.getMainHandItem().isEmpty();
   }

   @Override
   public int themeColor() {
      return -1750693;
   }

   @Override
   public List<String> extraBalanceKeys() {
      return List.of("RegeneratorBleed");
   }

   @Override
   public int signatureSlot() {
      return 0;
   }

   @Override
   public List<CraftRecipe> recipes() {
      return List.of(
         CraftRecipe.smelting((ItemLike)AwakenedItems.ADAMANTIUM_INGOT.get(), (ItemLike)AwakenedItems.RAW_ADAMANTIUM.get()),
         CraftRecipe.shaped(
            (ItemLike)AwakenedItems.INFUSION_TANK.get(),
            List.of("III", "GAG", "III"),
            Map.of('I', Items.IRON_INGOT, 'G', Items.GLASS, 'A', (ItemLike)AwakenedItems.ADAMANTIUM_INGOT.get())
         )
      );
   }

   @Override
   public void onAwaken(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      Fx.burst(level, ParticleTypes.CRIT, player.position().add(0.0, 1.0, 0.0), 50, 0.8, 0.3);
      Fx.sound(level, player.position(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.0F, 1.1F);
      player.displayClientMessage(Component.translatable("awakened.regenerator.hint").withStyle(ChatFormatting.RED), false);
   }
}
