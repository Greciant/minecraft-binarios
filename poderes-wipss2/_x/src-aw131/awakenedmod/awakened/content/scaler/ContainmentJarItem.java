package com.awakenedmod.awakened.content.scaler;

import com.awakenedmod.awakened.core.Bosses;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.power.AbilityFeedback;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class ContainmentJarItem extends Item {
   public static final float MAX_CAPTURE_SCALE = 0.27F;
   public static final int RESUME_CAP_TICKS = 100;
   private static final double AIR_RELEASE_DISTANCE = 1.5;
   private static final String K_ENTITY = "jar_entity";
   private static final String K_PLAYER = "jar_player";
   private static final String K_NAME = "jar_name";

   public ContainmentJarItem(Properties properties) {
      super(properties);
   }

   public static boolean isFilled(ItemStack stack) {
      return stack.has(DataComponents.CUSTOM_DATA);
   }

   public static UUID jarredPlayerId(ItemStack stack) {
      CustomData data = (CustomData)stack.get(DataComponents.CUSTOM_DATA);
      if (data == null) {
         return null;
      } else {
         String id = data.copyTag().getString("jar_player");

         try {
            return id.isEmpty() ? null : UUID.fromString(id);
         } catch (IllegalArgumentException var4) {
            return null;
         }
      }
   }

   public static CompoundTag capturedEntityTag(ItemStack stack) {
      CustomData data = (CustomData)stack.get(DataComponents.CUSTOM_DATA);
      if (data == null) {
         return null;
      } else {
         CompoundTag root = data.copyTag();
         return root.contains("jar_entity") ? root.getCompound("jar_entity") : null;
      }
   }

   private static String captiveName(ItemStack stack) {
      CustomData data = (CustomData)stack.get(DataComponents.CUSTOM_DATA);
      return data == null ? "" : data.copyTag().getString("jar_name");
   }

   private static void fill(ItemStack stack, CompoundTag root) {
      stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
      stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(1));
   }

   public static void empty(ItemStack stack) {
      stack.remove(DataComponents.CUSTOM_DATA);
      stack.remove(DataComponents.CUSTOM_MODEL_DATA);
   }

   public Component getName(ItemStack stack) {
      String name = captiveName(stack);
      return (Component)(name.isEmpty() ? super.getName(stack) : Component.translatable("item.awakened.containment_jar.filled", new Object[]{name}));
   }

   public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity target, InteractionHand usedHand) {
      if (isFilled(stack)) {
         return InteractionResult.PASS;
      } else {
         boolean fits = target.isAlive() && target.getScale() <= 0.27F && !Bosses.is(target);
         if (user instanceof ServerPlayer carrier) {
            if (!fits) {
               AbilityFeedback.fail(carrier, "awakened.scaler.jar_too_big");
               return InteractionResult.FAIL;
            } else if (SeizeControl.isSeizedVictim(target) || JarControl.isJarred(target)) {
               AbilityFeedback.fail(carrier, "awakened.scaler.jar_taken");
               return InteractionResult.FAIL;
            } else if (!Harm.allowed(carrier, target)) {
               AbilityFeedback.fail(carrier, "awakened.feedback.teammate");
               return InteractionResult.FAIL;
            } else {
               return target instanceof ServerPlayer victim ? capturePlayer(carrier, stack, victim) : captureMob(carrier, stack, target);
            }
         } else {
            return fits ? InteractionResult.SUCCESS : InteractionResult.FAIL;
         }
      }
   }

   private static InteractionResult captureMob(ServerPlayer carrier, ItemStack stack, LivingEntity target) {
      CompoundTag entityTag = new CompoundTag();
      target.saveWithoutId(entityTag);
      entityTag.putString("id", EntityType.getKey(target.getType()).toString());
      CompoundTag root = new CompoundTag();
      root.put("jar_entity", entityTag);
      root.putString("jar_name", target.getDisplayName().getString());
      fill(stack, root);
      RayEffects.pause(target.getUUID());
      captureFx(carrier, target.position().add(0.0, (double)target.getBbHeight() * 0.5, 0.0));
      target.discard();
      return InteractionResult.SUCCESS;
   }

   private static InteractionResult capturePlayer(ServerPlayer carrier, ItemStack stack, ServerPlayer victim) {
      if (victim != carrier && !victim.isSpectator()) {
         CompoundTag root = new CompoundTag();
         root.putString("jar_player", victim.getUUID().toString());
         root.putString("jar_name", victim.getName().getString());
         fill(stack, root);
         JarControl.capture(carrier, victim);
         captureFx(carrier, victim.position().add(0.0, (double)victim.getBbHeight() * 0.5, 0.0));
         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.FAIL;
      }
   }

   private static void captureFx(ServerPlayer carrier, Vec3 pos) {
      ServerLevel level = Dmg.serverLevel(carrier);
      Fx.burst(level, ParticleTypes.CLOUD, pos, 8, 0.25, 0.05);
      Fx.burst(level, Fx.dust(6021288, 1.0F), pos, 6, 0.2, 0.03);
      Fx.sound(level, pos, SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 0.9F, 1.3F);
   }

   public InteractionResult useOn(UseOnContext context) {
      ItemStack stack = context.getItemInHand();
      if (!isFilled(stack)) {
         return InteractionResult.PASS;
      } else if (context.getPlayer() instanceof ServerPlayer carrier) {
         Vec3 var5 = Vec3.atBottomCenterOf(context.getClickedPos().relative(context.getClickedFace()));
         release(carrier, stack, var5);
         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.SUCCESS;
      }
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
      ItemStack stack = user.getItemInHand(hand);
      if (!isFilled(stack)) {
         return InteractionResultHolder.pass(stack);
      } else {
         if (user instanceof ServerPlayer carrier) {
            release(carrier, stack, airSpot(carrier));
         }

         return InteractionResultHolder.success(stack);
      }
   }

   private static Vec3 airSpot(ServerPlayer carrier) {
      Vec3 look = carrier.getLookAngle();
      return carrier.position().add(look.x * 1.5, 0.1, look.z * 1.5);
   }

   private static void release(ServerPlayer carrier, ItemStack stack, Vec3 spot) {
      CustomData data = (CustomData)stack.get(DataComponents.CUSTOM_DATA);
      if (data != null) {
         CompoundTag root = data.copyTag();
         UUID playerId = jarredPlayerId(stack);
         empty(stack);
         ServerLevel level = Dmg.serverLevel(carrier);
         if (playerId != null) {
            releasePlayer(carrier, level, playerId, spot);
         } else {
            CompoundTag entityTag = root.contains("jar_entity") ? root.getCompound("jar_entity") : null;
            if (entityTag != null) {
               Entity released = EntityType.loadEntityRecursive(entityTag, level, e -> {
                  e.moveTo(spot.x, spot.y, spot.z, e.getYRot(), e.getXRot());
                  return e;
               });
               if (released == null) {
                  carrier.displayClientMessage(Component.translatable("awakened.scaler.jar_gone"), true);
               } else {
                  level.addFreshEntityWithPassengers(released);
                  if (released instanceof LivingEntity living) {
                     RayEffects.resume(living, 100);
                  }

                  releaseFx(level, spot);
               }
            }
         }
      }
   }

   private static void releasePlayer(ServerPlayer carrier, ServerLevel level, UUID playerId, Vec3 spot) {
      ServerPlayer victim = level.getServer().getPlayerList().getPlayer(playerId);
      if (victim != null && JarControl.isJarred(victim)) {
         SeizeControl.pinTo(victim, spot);
         JarControl.free(victim, null);
         releaseFx(level, spot);
      } else {
         carrier.displayClientMessage(Component.translatable("awakened.scaler.jar_gone"), true);
      }
   }

   private static void releaseFx(ServerLevel level, Vec3 spot) {
      Fx.burst(level, ParticleTypes.CLOUD, spot.add(0.0, 0.2, 0.0), 8, 0.25, 0.05);
      Fx.sound(level, spot, SoundEvents.BOTTLE_EMPTY, SoundSource.PLAYERS, 0.9F, 1.1F);
   }
}
