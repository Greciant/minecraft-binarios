package com.devilfruits.fruits.bomu_bomu.item;

import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.fruits.bomu_bomu.BomuAbilities;
import com.devilfruits.fruits.bomu_bomu.BomuBomu;
import com.devilfruits.fruits.bomu_bomu.BomuFx;
import com.devilfruits.fruits.bomu_bomu.BomuItems;
import com.devilfruits.fruits.bomu_bomu.BomuSounds;
import com.devilfruits.fruits.bomu_bomu.entity.BombBullet;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class BombPistolItem extends Item {
   public static final float OPEN = 0.08F;
   public static final float SPIN_FROM = 0.18F;
   public static final float SPIN_TO = 0.48F;
   public static final float BLOW = 0.55F;
   public static final float BLOW_TO = 0.85F;

   public BombPistolItem(Properties properties) {
      super(properties);
   }

   public static int rounds(ItemStack stack) {
      Integer r = (Integer)stack.get((DataComponentType)BomuItems.ROUNDS.get());
      return r == null ? 0 : r;
   }

   public static void setRounds(ItemStack stack, int rounds) {
      stack.set((DataComponentType)BomuItems.ROUNDS.get(), Mth.clamp(rounds, 0, capacity()));
   }

   public static boolean bombBreath(ItemStack stack) {
      return Boolean.TRUE.equals(stack.get((DataComponentType)BomuItems.BOMB_BREATH.get()));
   }

   public static void setBombBreath(ItemStack stack, boolean bomb) {
      stack.set((DataComponentType)BomuItems.BOMB_BREATH.get(), bomb);
   }

   public static int capacity() {
      return (int)BomuBomu.setting("pistolRounds");
   }

   public static int loadTicks() {
      return Math.max(8, (int)Math.round(BomuBomu.setting("pistolReloadSeconds") * 20.0));
   }

   @Nullable
   public static InteractionHand hand(Player player) {
      if (player.getMainHandItem().getItem() instanceof BombPistolItem) {
         return InteractionHand.MAIN_HAND;
      } else {
         return player.getOffhandItem().getItem() instanceof BombPistolItem ? InteractionHand.OFF_HAND : null;
      }
   }

   public static Vec3 muzzle(Player player, InteractionHand hand) {
      Vec3 look = player.getLookAngle();
      boolean right = hand == InteractionHand.MAIN_HAND == (player.getMainArm() == HumanoidArm.RIGHT);
      Vec3 side = new Vec3(-look.z, 0.0, look.x).normalize().scale(right ? 0.24 : -0.24);
      return player.getEyePosition().add(look.scale(0.95)).add(side).add(0.0, -0.14, 0.0);
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      int rounds = rounds(stack);
      boolean load = player.isShiftKeyDown() ? rounds < capacity() : rounds <= 0;
      if (!load) {
         if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
         } else {
            if (!level.isClientSide() && player instanceof ServerPlayer sp) {
               fire(sp, hand, stack);
            }

            return InteractionResultHolder.consume(stack);
         }
      } else {
         player.startUsingItem(hand);
         return InteractionResultHolder.consume(stack);
      }
   }

   public static void fire(ServerPlayer player, InteractionHand hand, ItemStack stack) {
      if (BomuBomu.isUser(player) && bombBreath(stack)) {
         AbilityManager.press(player, BomuAbilities.SHOT);
      } else {
         if (!player.getAbilities().instabuild) {
            setRounds(stack, rounds(stack) - 1);
            stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
         }

         Vec3 muzzle = muzzle(player, hand);
         Vec3 dir = player.getLookAngle();
         BombBullet.gust(player, muzzle, dir.scale(4.0), 6);
         player.getCooldowns().addCooldown(stack.getItem(), BomuAbilities.SHOT.config().cooldownTicks());
         player.level()
            .playSound(null, muzzle.x, muzzle.y, muzzle.z, BomuSounds.SHOT_PLAIN.get(), SoundSource.PLAYERS, 0.9F, 1.1F + player.getRandom().nextFloat() * 0.1F);
         ServerFx.send(player, BomuFx.SHOT_FIRE, muzzle, dir, 0.6F, player.getRandom().nextLong(), hand == InteractionHand.MAIN_HAND ? 0 : 2);
      }
   }

   public int getUseDuration(ItemStack stack, LivingEntity entity) {
      return 72000;
   }

   public UseAnim getUseAnimation(ItemStack stack) {
      return UseAnim.NONE;
   }

   public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
      if (!level.isClientSide() && entity instanceof ServerPlayer player) {
         int total = loadTicks();
         int used = this.getUseDuration(stack, entity) - remaining;
         if (used == total) {
            fill(stack, player);
         } else {
            int open = Math.round(0.08F * (float)total);
            int spinFrom = Math.round(0.18F * (float)total);
            int spinTo = Math.round(0.48F * (float)total);
            int blow = Math.round(0.55F * (float)total);
            if (used == open) {
               sound(player, BomuSounds.SHOT_OPEN.get(), 0.7F, 1.2F);
            } else if (used >= spinFrom && used <= spinTo && (used - spinFrom) % 3 == 0) {
               sound(player, BomuSounds.SHOT_RELOAD.get(), 0.5F, 1.4F + (float)(used - spinFrom) * 0.02F);
            } else if (used == blow) {
               sound(player, BomuSounds.SHOT_BLOW.get(), 0.9F, BomuBomu.immune(player) ? 0.85F : 1.05F);
               InteractionHand hand = player.getUsedItemHand();
               ServerFx.send(
                  player,
                  BomuFx.BREATH,
                  muzzle(player, hand),
                  player.getLookAngle(),
                  BomuBomu.immune(player) ? 1.0F : 0.0F,
                  player.getRandom().nextLong(),
                  hand == InteractionHand.MAIN_HAND ? 0 : 1
               );
            }
         }
      }
   }

   public static void fill(ItemStack stack, Player player) {
      setRounds(stack, capacity());
      setBombBreath(stack, BomuBomu.immune(player));
      sound(player, BomuSounds.SHOT_CLOSE.get(), 0.9F, 1.05F);
   }

   private static void sound(Player player, SoundEvent event, float volume, float pitch) {
      player.level().playSound(null, player.getX(), player.getY(), player.getZ(), event, SoundSource.PLAYERS, volume, pitch);
   }

   public boolean isEnchantable(ItemStack stack) {
      return false;
   }

   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      int rounds = rounds(stack);
      tooltip.add(
         Component.translatable(
               rounds > 0 && bombBreath(stack) ? "item.devilfruits.bomb_pistol.rounds_bomb" : "item.devilfruits.bomb_pistol.rounds",
               new Object[]{rounds, capacity()}
            )
            .withStyle(rounds > 0 ? ChatFormatting.GOLD : ChatFormatting.GRAY)
      );
      tooltip.add(Component.translatable("item.devilfruits.bomb_pistol.hint").withStyle(ChatFormatting.DARK_GRAY));
      tooltip.add(Component.translatable("item.devilfruits.bomb_pistol.bomu").withStyle(ChatFormatting.DARK_RED));
   }
}
