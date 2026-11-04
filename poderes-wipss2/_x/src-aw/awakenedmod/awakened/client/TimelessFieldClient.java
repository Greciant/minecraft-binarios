package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.content.velocity.TimelessAbility;
import com.awakenedmod.awakened.content.velocity.VelocityPalette;
import com.awakenedmod.awakened.content.velocity.VelocityVisualState;
import com.awakenedmod.awakened.core.data.VelocityVisual;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent.Pre;

public final class TimelessFieldClient {
   public static final double CASTER_EXEMPT_RADIUS = 1.5;
   public static final int HEARTBEAT_TICKS = 40;
   private static volatile List<TimelessFieldClient.Field> fields = List.of();

   private TimelessFieldClient() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(TimelessFieldClient::onClientTickPre);
   }

   private static void onClientTickPre(Pre event) {
      refresh();
   }

   static void refresh() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null) {
         fields = List.of();
      } else {
         List<TimelessFieldClient.Field> next = null;

         for (Player p : mc.level.players()) {
            VelocityVisual vis = VelocityVisualState.of(p);
            double radius = vis.fieldRadius();
            if (!(radius <= 0.0)) {
               if (next == null) {
                  next = new ArrayList<>(2);
               }

               next.add(new TimelessFieldClient.Field(p, radius, divisorOf(vis), VelocityPalette.of(vis.color())));
            }
         }

         fields = next == null ? List.of() : List.copyOf(next);
      }
   }

   public static boolean active() {
      return !fields.isEmpty();
   }

   public static List<TimelessFieldClient.Field> fields() {
      return fields;
   }

   public static TimelessFieldClient.Field fieldAt(double x, double y, double z) {
      List<TimelessFieldClient.Field> snapshot = fields;
      TimelessFieldClient.Field best = null;
      double bestDist = Double.MAX_VALUE;

      for (TimelessFieldClient.Field field : snapshot) {
         if (field.nearCaster(x, y, z)) {
            return null;
         }

         if (field.contains(x, y, z)) {
            double dist = field.caster.distanceToSqr(x, y, z);
            if (dist < bestDist) {
               bestDist = dist;
               best = field;
            }
         }
      }

      return best;
   }

   public static TimelessFieldClient.Field fieldOf(Entity entity) {
      Vec3 pos = entity.position();
      return fieldAt(pos.x, pos.y, pos.z);
   }

   public static int divisorAt(double x, double y, double z) {
      TimelessFieldClient.Field field = fieldAt(x, y, z);
      return field == null ? 1 : field.divisor;
   }

   private static int divisorOf(VelocityVisual vis) {
      return TimelessAbility.divisor(vis.gear());
   }

   public static record Field(Player caster, double radius, int divisor, VelocityPalette palette) {
      boolean contains(double x, double y, double z) {
         return this.caster.distanceToSqr(x, y, z) <= this.radius * this.radius;
      }

      boolean nearCaster(double x, double y, double z) {
         return this.caster.distanceToSqr(x, y, z) <= 2.25;
      }
   }
}
