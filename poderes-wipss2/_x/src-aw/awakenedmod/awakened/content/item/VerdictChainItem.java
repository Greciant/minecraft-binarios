package com.awakenedmod.awakened.content.item;

import com.awakenedmod.awakened.content.AwakenedEntities;
import com.awakenedmod.awakened.content.entity.VerdictHookEntity;
import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.progression.BalanceKeyed;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
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

public final class VerdictChainItem extends Item implements BalanceKeyed {
   public static final int MIN_WHIRL_TICKS = 8;
   private static final int USE_DURATION = 72000;
   public static final double REACH = 4.0;
   public static final double WHIRL_RADIUS = 2.5;
   public static final float WHIRL_DAMAGE = 2.0F;
   private static final String WHIRL_BALANCE_KEY = "VerdictChainWhirl";
   public static final int WHIRL_INTERVAL = 10;
   public static final int WHIRL_SPEED_PCT = 70;
   private static final double WHIRL_KNOCKBACK = 0.35;
   private static final int THROW_COOLDOWN = 100;
   private static final int FIZZLE_COOLDOWN = 20;
   private static final double HOMING_CONE_DEG = 8.0;
   private static final String ANIM_WHIRL = "verdict_whirl";
   private static final String ANIM_THROW = "verdict_throw";
   private static final Set<UUID> THREW = ConcurrentHashMap.newKeySet();
   private static final Set<UUID> WHIRLING = ConcurrentHashMap.newKeySet();

   public VerdictChainItem(Properties properties) {
      super(properties);
   }

   @Override
   public List<String> balanceKeys() {
      return List.of("VerdictChainWhirl");
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack held = player.getItemInHand(hand);
      if (player.isPassenger()) {
         return InteractionResultHolder.fail(held);
      } else {
         player.startUsingItem(hand);
         startWhirl(player);
         return InteractionResultHolder.consume(held);
      }
   }

   private static void startWhirl(Player player) {
      if (player instanceof ServerPlayer sp) {
         THREW.remove(sp.getUUID());
         WHIRLING.add(sp.getUUID());
         PowerService.playAnimation(sp, "verdict_whirl", true);
      }
   }

   public int getUseDuration(ItemStack stack, LivingEntity entity) {
      return 72000;
   }

   public UseAnim getUseAnimation(ItemStack stack) {
      return UseAnim.NONE;
   }

   public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
      if (level instanceof ServerLevel server && entity instanceof ServerPlayer player) {
         int held = 72000 - remaining;
         if (held == 8) {
            Fx.sound(server, player.position(), SoundEvents.CHAIN_STEP, SoundSource.PLAYERS, 0.9F, 1.4F);
         }

         if (held > 0 && held % 10 == 0) {
            whirlPass(server, player, stack);
         }

         return;
      }
   }

   private static void whirlPass(ServerLevel server, ServerPlayer player, ItemStack stack) {
      Vec3 centre = player.position().add(0.0, (double)player.getBbHeight() * 0.6, 0.0);
      AABB box = new AABB(centre, centre).inflate(2.5);
      boolean hit = false;

      for (LivingEntity victim : server.getEntitiesOfClass(
         LivingEntity.class, box, e -> e != player && e.isAlive() && e.attackable() && !e.isSpectator() && Harm.allowed(player, e)
      )) {
         if (!(victim.distanceToSqr(centre) > 6.25)) {
            float damage = Dmg.weaponBonus(
               server, stack, victim, server.damageSources().playerAttack(player), MasteryScaling.configScaledDamage(2.0F, "VerdictChainWhirl")
            );
            if (Dmg.hurt(victim, server, server.damageSources().playerAttack(player), damage)) {
               Vec3 out = victim.position().subtract(player.position());
               Harm.knockback(player, victim, 0.35, -out.x, -out.z);
               Fx.burst(server, ParticleTypes.CRIT, victim.position().add(0.0, (double)victim.getBbHeight() * 0.5, 0.0), 6, 0.3, 0.05);
               hit = true;
            }
         }
      }

      Fx.sound(server, player.position(), hit ? SoundEvents.CHAIN_HIT : SoundEvents.CHAIN_STEP, SoundSource.PLAYERS, hit ? 0.9F : 0.5F, hit ? 0.8F : 1.0F);
   }

   public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
      release(stack, level, entity, timeLeft);
   }

   private static void release(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
      if (level instanceof ServerLevel server && entity instanceof ServerPlayer player) {
         int held = 72000 - timeLeft;
         if (held >= 8 && !player.isPassenger()) {
            throwHook(server, player, stack);
            return;
         }

         Fx.sound(server, player.position(), SoundEvents.CHAIN_STEP, SoundSource.PLAYERS, 0.6F, 0.7F);
         cooldown(player, stack, 20);
         return;
      }
   }

   private static void throwHook(ServerLevel server, ServerPlayer player, ItemStack stack) {
      THREW.add(player.getUUID());
      WHIRLING.remove(player.getUUID());
      PowerService.stopAnimation(player);
      PowerService.playAnimation(player, "verdict_throw");
      Vec3 origin = AimUtil.shoulderHand(player, false, 0.31, 0.25, 0.7);
      Vec3 dir = AimUtil.throwDirection(player, origin, 16.0);
      LivingEntity homing = AimUtil.aimedLivingCone(player, 16.0, 8.0, e -> e.isAlive() && e.attackable() && !e.isSpectator() && Harm.allowed(player, e));
      VerdictHookEntity hook = new VerdictHookEntity(AwakenedEntities.VERDICT_HOOK.get(), server);
      hook.launch(player, origin, dir, homing, stack);
      server.addFreshEntity(hook);
      cooldown(player, stack, 100);
      Fx.sound(server, player.position(), (SoundEvent)SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 1.1F, 0.6F);
      Fx.sound(server, player.position(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1.0F, 0.7F);
   }

   private static void cooldown(ServerPlayer player, ItemStack stack, int ticks) {
      player.getCooldowns().addCooldown(stack.getItem(), ticks);
   }

   public void onStopUsing(ItemStack stack, LivingEntity entity, int count) {
      if (entity instanceof ServerPlayer player) {
         boolean threw = THREW.remove(player.getUUID());
         WHIRLING.remove(player.getUUID());
         if (!threw) {
            PowerService.stopAnimation(player);
         }
      }
   }

   private static void whirlSafetyNet(Entity entity) {
      if (entity instanceof ServerPlayer player
         && WHIRLING.contains(player.getUUID())
         && (!player.isUsingItem() || !(player.getUseItem().getItem() instanceof VerdictChainItem))) {
         WHIRLING.remove(player.getUUID());
         PowerService.stopAnimation(player);
      }
   }

   public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
      if (!level.isClientSide()) {
         whirlSafetyNet(entity);
      }
   }

   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(
         Component.translatable("item.awakened.verdict_chain.tooltip").withStyle(new ChatFormatting[]{ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC})
      );
      tooltip.add(Component.translatable("item.awakened.verdict_chain.tooltip.hint").withStyle(ChatFormatting.GRAY));
   }
}
