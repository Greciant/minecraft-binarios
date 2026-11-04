package com.devilfruits.client.form;

import com.devilfruits.client.input.InputHandler;
import com.devilfruits.core.form.FruitForm;
import com.devilfruits.registry.DFAttachments;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CalculateDetachedCameraDistanceEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre;

@EventBusSubscriber(
   modid = "devilfruits",
   value = {Dist.CLIENT}
)
public final class FormMovements {
   private static final Map<ResourceLocation, FormMovements.FormMovement> MOVEMENTS = new HashMap<>();
   @Nullable
   private static FruitForm lastForm;

   public static void register(ResourceLocation formId, FormMovements.FormMovement movement) {
      if (MOVEMENTS.putIfAbsent(formId, movement) != null) {
         throw new IllegalStateException("Duplicate form movement " + formId);
      }
   }

   @Nullable
   private static FruitForm formOf(Player player) {
      return player.hasData(DFAttachments.PLAYER_DATA) ? DFAttachments.data(player).form() : null;
   }

   @Nullable
   private static FormMovements.FormMovement movementOf(@Nullable FruitForm form) {
      return form != null && !form.isBase() ? MOVEMENTS.get(form.id()) : null;
   }

   @SubscribeEvent
   public static void onInput(MovementInputUpdateEvent event) {
      if (event.getEntity() instanceof LocalPlayer player) {
         FruitForm form = formOf(player);
         FormMovements.FormMovement movement = movementOf(form);
         if (movement != null) {
            movement.input(player, form, event.getInput());
         }
      }
   }

   @SubscribeEvent
   public static void onPlayerTick(Pre event) {
      if (event.getEntity() instanceof LocalPlayer player) {
         FruitForm form = formOf(player);
         if (lastForm != null && lastForm != form) {
            FormMovements.FormMovement old = movementOf(lastForm);
            if (old != null) {
               old.leave(player, lastForm);
            }
         }

         lastForm = form;
         FormMovements.FormMovement movement = movementOf(form);
         if (movement != null) {
            movement.tick(player, form, InputHandler.isBoosting());
         }
      }
   }

   @SubscribeEvent
   public static void onLogout(LoggingOut event) {
      lastForm = null;
   }

   public static float cameraLift(Entity entity) {
      if (entity instanceof Player player) {
         FruitForm form = formOf(player);
         if (form != null && form.cameraDistance() > 0.0F) {
            return form.cameraDistance() / 3.0F;
         }
      }

      return 0.0F;
   }

   @SubscribeEvent
   public static void onCameraDistance(CalculateDetachedCameraDistanceEvent event) {
      if (event.getCamera().getEntity() instanceof Player player) {
         FruitForm form = formOf(player);
         if (form != null && form.cameraDistance() > 0.0F) {
            event.setDistance(form.cameraDistance());
         }
      }
   }

   private FormMovements() {
   }

   public interface FormMovement {
      default void input(LocalPlayer player, FruitForm form, Input input) {
      }

      default void tick(LocalPlayer player, FruitForm form, boolean boosting) {
      }

      default void leave(LocalPlayer player, FruitForm form) {
      }
   }
}
