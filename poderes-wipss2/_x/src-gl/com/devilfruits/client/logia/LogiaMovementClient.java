package com.devilfruits.client.logia;

import com.devilfruits.core.logia.LogiaDash;
import com.devilfruits.registry.DFAttachments;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre;

@EventBusSubscriber(
   modid = "devilfruits",
   value = {Dist.CLIENT}
)
public final class LogiaMovementClient {
   private static final Map<ResourceLocation, LogiaMovementClient.Controller> CONTROLLERS = new HashMap<>();
   @Nullable
   private static ResourceLocation current;

   public static void register(ResourceLocation modeId, LogiaMovementClient.Controller controller) {
      if (CONTROLLERS.putIfAbsent(modeId, controller) != null) {
         throw new IllegalStateException("Duplicate Logia movement controller " + modeId);
      }
   }

   @Nullable
   public static ResourceLocation modeOf(Player player) {
      return player.hasData(DFAttachments.PLAYER_DATA) ? DFAttachments.data(player).logia().movementMode() : null;
   }

   @SubscribeEvent
   public static void onInput(MovementInputUpdateEvent event) {
      if (event.getEntity() instanceof LocalPlayer player) {
         ResourceLocation mode = modeOf(player);
         LogiaMovementClient.Controller controller = mode == null ? null : CONTROLLERS.get(mode);
         if (controller != null) {
            controller.input(player, event.getInput());
         }
      }
   }

   @SubscribeEvent
   public static void onPlayerTick(Pre event) {
      if (event.getEntity() instanceof LocalPlayer player) {
         ResourceLocation mode = modeOf(player);
         if (current != null && !current.equals(mode)) {
            LogiaMovementClient.Controller old = CONTROLLERS.get(current);
            if (old != null) {
               old.leave(player);
            }
         }

         current = mode;
         LogiaMovementClient.Controller controller = mode == null ? null : CONTROLLERS.get(mode);
         if (controller != null) {
            controller.tick(player);
         }
      }
   }

   @SubscribeEvent
   public static void onLogout(LoggingOut event) {
      current = null;
      LogiaMovementClient.DashController.path = null;
   }

   static void dashStarted(LocalPlayer player, Vec3 to, int ticks) {
      LogiaMovementClient.DashController.path = new LogiaMovementClient.DashController.Path(player.position(), to, Math.max(1, ticks));
   }

   private LogiaMovementClient() {
   }

   static {
      register(LogiaDash.MODE.id(), new LogiaMovementClient.DashController());
   }

   public interface Controller {
      default void input(LocalPlayer player, Input input) {
      }

      default void tick(LocalPlayer player) {
      }

      default void leave(LocalPlayer player) {
      }
   }

   private static final class DashController implements LogiaMovementClient.Controller {
      @Nullable
      static LogiaMovementClient.DashController.Path path;
      private static int age;
      @Nullable
      private static LogiaMovementClient.DashController.Path running;

      @Override
      public void input(LocalPlayer player, Input input) {
         input.forwardImpulse = 0.0F;
         input.leftImpulse = 0.0F;
         input.up = input.down = input.left = input.right = false;
         input.jumping = false;
         input.shiftKeyDown = false;
      }

      @Override
      public void tick(LocalPlayer player) {
         if (path != running) {
            running = path;
            age = 0;
         }

         player.fallDistance = 0.0F;
         player.setSprinting(false);
         if (running == null) {
            player.setDeltaMovement(Vec3.ZERO);
         } else {
            age++;
            Vec3 at = running.from.lerp(running.to, LogiaDash.progress((double)age / (double)running.ticks));
            player.setDeltaMovement(age > running.ticks ? Vec3.ZERO : at.subtract(player.position()));
         }
      }

      @Override
      public void leave(LocalPlayer player) {
         path = null;
         running = null;
         player.fallDistance = 0.0F;
      }

      static record Path(Vec3 from, Vec3 to, int ticks) {
      }
   }
}
