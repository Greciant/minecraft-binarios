package com.awakenedmod.awakened.content.item;

import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.progression.BalanceKeyed;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class IaiBladeItem extends Item implements BalanceKeyed {
   public static final int MIN_COIL_TICKS = 10;
   private static final int USE_DURATION = 72000;
   private static final double DASH_LENGTH = 8.0;
   private static final double DASH_SPEED = 1.7;
   private static final double LINE_HALF_WIDTH = 0.6;
   private static final float DASH_DAMAGE = 12.0F;
   private static final String DASH_BALANCE_KEY = "IaiBladeDash";
   private static final int MAX_TARGETS = 3;
   private static final double KNOCKBACK = 1.1;
   private static final int RECOVERY_TICKS = 15;
   private static final int COOLDOWN_TICKS = 160;
   private static final double MAX_RISE = 0.3;
   private static final String ANIM_COIL = "iai_blade_coil";
   private static final String ANIM_CUT = "iai_blade_cut";
   private static final Set<UUID> DASHED = ConcurrentHashMap.newKeySet();
   private static final Set<UUID> COILING = ConcurrentHashMap.newKeySet();

   public IaiBladeItem(Properties properties) {
      super(properties);
   }

   @Override
   public List<String> balanceKeys() {
      return List.of("IaiBladeDash");
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack held = player.getItemInHand(hand);
      if (player.isPassenger()) {
         return InteractionResultHolder.fail(held);
      } else {
         player.startUsingItem(hand);
         startCoil(player);
         return InteractionResultHolder.consume(held);
      }
   }

   private static void startCoil(Player player) {
      if (player instanceof ServerPlayer sp) {
         DASHED.remove(sp.getUUID());
         COILING.add(sp.getUUID());
         PowerService.playAnimation(sp, "iai_blade_coil");
      }
   }

   public int getUseDuration(ItemStack stack, LivingEntity entity) {
      return 72000;
   }

   public UseAnim getUseAnimation(ItemStack stack) {
      return UseAnim.NONE;
   }

   public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
      if (level instanceof ServerLevel server && entity instanceof ServerPlayer player && 72000 - remaining == 10) {
         Fx.sound(server, player.position(), (SoundEvent)SoundEvents.CROSSBOW_LOADING_END.value(), SoundSource.PLAYERS, 0.8F, 1.3F);
      }
   }

   public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
      release(stack, level, entity, timeLeft);
   }

   private static void release(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
      if (level instanceof ServerLevel server && entity instanceof ServerPlayer player) {
         int held = 72000 - timeLeft;
         if (held >= 10 && !player.isPassenger()) {
            dashStrike(server, player, stack);
            return;
         }

         Fx.sound(server, player.position(), (SoundEvent)SoundEvents.CROSSBOW_LOADING_END.value(), SoundSource.PLAYERS, 0.6F, 0.8F);
         return;
      }
   }

   public void onStopUsing(ItemStack stack, LivingEntity entity, int count) {
      if (entity instanceof ServerPlayer player) {
         boolean dashed = DASHED.remove(player.getUUID());
         COILING.remove(player.getUUID());
         if (!dashed) {
            PowerService.stopAnimation(player);
         }
      }
   }

   private static void coilSafetyNet(Entity entity) {
      if (entity instanceof ServerPlayer player
         && COILING.contains(player.getUUID())
         && (!player.isUsingItem() || !(player.getUseItem().getItem() instanceof IaiBladeItem))) {
         COILING.remove(player.getUUID());
         PowerService.stopAnimation(player);
      }
   }

   public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
      if (!level.isClientSide()) {
         coilSafetyNet(entity);
      }
   }

   private static void dashStrike(ServerLevel server, ServerPlayer player, ItemStack stack) {
      DASHED.add(player.getUUID());
      COILING.remove(player.getUUID());
      PowerService.stopAnimation(player);
      PowerService.playAnimation(player, "iai_blade_cut");
      Vec3 look = player.getLookAngle();
      Vec3 flat = new Vec3(look.x, 0.0, look.z);
      flat = flat.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0.0F, player.getYRot()) : flat.normalize();
      double rise = Math.max(0.0, Math.min(0.3, look.y * 0.5));
      Vec3 dir = new Vec3(flat.x, rise, flat.z).normalize();
      Vec3 from = player.position().add(0.0, (double)player.getBbHeight() * 0.5, 0.0);
      Vec3 to = from.add(dir.scale(8.0));
      AABB sweep = new AABB(from, to).inflate(0.6, 1.0, 0.6);
      List<LivingEntity> onLine = new ArrayList<>();

      for (LivingEntity victim : server.getEntitiesOfClass(
         LivingEntity.class, sweep, e -> e != player && e.isAlive() && e.attackable() && !e.isAlliedTo(player) && Harm.allowed(player, e)
      )) {
         if (victim.getBoundingBox().inflate(0.6).clip(from, to).isPresent()) {
            onLine.add(victim);
         }
      }

      onLine.sort(Comparator.comparingDouble(e -> e.distanceToSqr(from)));
      int cut = 0;
      float dashDamage = MasteryScaling.scaledDamage(12.0F, player, "IaiBladeDash");

      for (LivingEntity victimx : onLine) {
         if (cut >= 3) {
            break;
         }

         boolean landed = victimx.hurt(server.damageSources().playerAttack(player), dashDamage);
         if (landed) {
            victimx.knockback(1.1, -dir.x, -dir.z);
            Fx.burst(server, ParticleTypes.CRIT, victimx.position().add(0.0, (double)victimx.getBbHeight() * 0.5, 0.0), 10, 0.4, 0.1);
            cut++;
         }
      }

      player.setDeltaMovement(dir.scale(1.7).add(0.0, 0.08, 0.0));
      player.hurtMarked = true;
      player.resetFallDistance();
      player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 15, 1, false, false));
      player.getCooldowns().addCooldown(stack.getItem(), 160);
      Fx.sound(server, player.position(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.4F, 0.5F);
      Fx.sound(server, player.position(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, cut > 0 ? 0.9F : 0.6F);

      for (int i = 1; i <= 4; i++) {
         Fx.burst(server, ParticleTypes.SWEEP_ATTACK, from.add(dir.scale((double)i * 8.0 / 4.0)), 1, 0.2, 0.0);
      }
   }

   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.translatable("item.awakened.iai_blade.tooltip").withStyle(new ChatFormatting[]{ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC}));
      tooltip.add(Component.translatable("item.awakened.iai_blade.tooltip.hint").withStyle(ChatFormatting.GRAY));
   }
}
