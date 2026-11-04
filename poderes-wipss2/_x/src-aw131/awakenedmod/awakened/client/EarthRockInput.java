package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.content.earth.HoverRockEntity;
import com.awakenedmod.awakened.core.net.AbilityActionPayload;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class EarthRockInput {
   private static final int ROCK_THROW_SLOT = 0;
   private static final int FAULT_LINE_SLOT = 2;
   private static final double PUNCH_DIST = 3.5;
   private static final double PUNCH_FLAT_DOT = 0.6;

   private EarthRockInput() {
   }

   public static void register() {
      ClickClaims.add(EarthRockInput::hasSteeredFissure, () -> send(2, 6));
      ClickClaims.add(EarthRockInput::isPunchingRock, () -> send(0, 0));
   }

   private static void send(int slot, int action) {
      PacketDistributor.sendToServer(new AbilityActionPayload(slot, action), new CustomPacketPayload[0]);
   }

   private static boolean hasSteeredFissure() {
      ClientPowerState state = ClientPowerState.get();
      return state.awakened() && "geokinetic".equals(state.livePowerId()) && FaultSteerState.live();
   }

   private static boolean isPunchingRock() {
      ClientPowerState state = ClientPowerState.get();
      if (state.awakened() && "geokinetic".equals(state.livePowerId())) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null && mc.level != null) {
            UUID uuid = mc.player.getUUID();
            Vec3 eye = mc.player.getEyePosition();
            Vec3 look = mc.player.getViewVector(1.0F);
            Vec3 flatLook = new Vec3(look.x, 0.0, look.z);
            if (flatLook.lengthSqr() < 1.0E-4) {
               return false;
            } else {
               flatLook = flatLook.normalize();
               AABB box = mc.player.getBoundingBox().inflate(4.5);

               for (HoverRockEntity rock : mc.level.getEntitiesOfClass(HoverRockEntity.class, box, r -> !r.launched() && uuid.equals(r.ownerId()))) {
                  Vec3 to = rock.position().subtract(eye);
                  if (!(to.length() > 3.5)) {
                     Vec3 flatTo = new Vec3(to.x, 0.0, to.z);
                     if (flatTo.lengthSqr() < 1.0E-4 || flatTo.normalize().dot(flatLook) > 0.6) {
                        return true;
                     }
                  }
               }

               return false;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }
}
