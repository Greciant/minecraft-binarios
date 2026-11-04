package com.devilfruits.fruits.uo_uo_seiryu.client.dev;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.dev.AutoTest;
import com.devilfruits.client.dev.CaptureRig;
import com.devilfruits.client.dev.CaptureStage;
import com.devilfruits.core.body.SerpentBody;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.form.FormFlags;
import com.devilfruits.core.form.FormManager;
import com.devilfruits.core.rules.FruitRules;
import com.devilfruits.core.rules.Mastery;
import com.devilfruits.core.util.ServerScheduler;
import com.devilfruits.fruits.uo_uo_seiryu.DragonShells;
import com.devilfruits.fruits.uo_uo_seiryu.UoUoSeiryu;
import com.devilfruits.fruits.uo_uo_seiryu.client.render.DragonRenderer;
import com.devilfruits.fruits.uo_uo_seiryu.entity.SeiryuDragonEntity;
import com.devilfruits.registry.DFAttachments;
import com.mojang.authlib.GameProfile;
import io.netty.channel.ChannelHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

public final class DragonSyncCheck extends CaptureStage {
   private static final double SPEED = 2.7;
   private static final double RADIUS = 34.0;
   @Nullable
   private static ServerPlayer rider;
   private static double lastBodyX;
   private static double lastBodyZ;
   private static double lastHeadX;
   private static double lastHeadZ;

   public DragonSyncCheck() {
      super("vfx_dsync_");
   }

   @Override
   protected ResourceLocation fruit() {
      return UoUoSeiryu.ID;
   }

   @Override
   protected void setup(ServerPlayer p) {
      command(p, "time set 6000");
   }

   private static ServerPlayer mockPlayer(ServerLevel level) {
      CommonListenerCookie cookie = CommonListenerCookie.createInitial(
         new GameProfile(UUID.nameUUIDFromBytes("dragon_rider".getBytes()), "dragon_rider"), false
      );
      ServerPlayer player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), ClientInformation.createDefault());
      Connection connection = new Connection(PacketFlow.SERVERBOUND);
      new EmbeddedChannel(new ChannelHandler[]{connection});
      NetworkRegistry.configureMockConnection(connection);
      level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
      return player;
   }

   @Nullable
   private static SeiryuDragonEntity shellOf(Player owner) {
      for (SeiryuDragonEntity shell : DragonRenderer.shells()) {
         if (shell.owner() == owner) {
            return shell;
         }
      }

      return null;
   }

   private static void log(Minecraft mc, int tick) {
      if (mc.level != null && rider != null) {
         Player remote = mc.level.getPlayerByUUID(rider.getUUID());
         SeiryuDragonEntity shell = remote == null ? null : shellOf(remote);
         SerpentBody body = shell == null ? null : shell.body();
         if (remote != null && body != null) {
            double[] p = new double[3];
            body.point((double)body.neck, 1.0F, p);
            double gap = Math.sqrt((p[0] - remote.getX()) * (p[0] - remote.getX()) + (p[2] - remote.getZ()) * (p[2] - remote.getZ()));
            double bodyMoved = Math.sqrt((p[0] - lastBodyX) * (p[0] - lastBodyX) + (p[2] - lastBodyZ) * (p[2] - lastBodyZ));
            double headMoved = Math.sqrt((remote.getX() - lastHeadX) * (remote.getX() - lastHeadX) + (remote.getZ() - lastHeadZ) * (remote.getZ() - lastHeadZ));
            lastBodyX = p[0];
            lastBodyZ = p[2];
            lastHeadX = remote.getX();
            lastHeadZ = remote.getZ();
            double behind = Math.sqrt(
               (remote.getX() - rider.getX()) * (remote.getX() - rider.getX()) + (remote.getZ() - rider.getZ()) * (remote.getZ() - rider.getZ())
            );
            DevilFruits.LOGGER
               .info(
                  "[dsync] t{} head moved {} body moved {} gap {} head behind server {}",
                  new Object[]{
                     tick, String.format("%.2f", headMoved), String.format("%.2f", bodyMoved), String.format("%.2f", gap), String.format("%.2f", behind)
                  }
               );
         } else {
            DevilFruits.LOGGER.info("[dsync] t{} remote {} shell {} body {}", new Object[]{tick, remote != null, shell != null, body != null});
         }
      }
   }

   @Override
   protected int takes(AutoTest s, int t) {
      int start = t + 40;
      s.add(
         start,
         mc -> s.server(
               mc,
               p -> {
                  ServerLevel level = p.serverLevel();
                  double cx = 0.5;
                  double cy = (double)(s.origin().getY() + 22);
                  double cz = -40.0;
                  rider = mockPlayer(level);
                  rider.moveTo(cx + 34.0, cy, cz, 0.0F, 0.0F);
                  FruitRules.grantPower(rider, UoUoSeiryu.fruit(), false);
                  PlayerFruitData data = DFAttachments.data(rider);
                  Mastery.setLevel(rider, data, 10, false);
                  DevilFruits.LOGGER.info("[dsync] dragon form: {}", FormManager.request(rider, "dragon", FormManager.Reason.COMMAND));
                  double[] angle = new double[]{0.0};
                  ServerScheduler.schedule(
                     level,
                     1,
                     age -> {
                        if (rider != null && !rider.isRemoved()) {
                           angle[0] += 0.07941176470588236;
                           double x = cx + Math.cos(angle[0]) * 34.0;
                           double z = cz + Math.sin(angle[0]) * 34.0;
                           rider.moveTo(x, cy + Math.sin(angle[0] * 2.0) * 4.0, z, Mth.wrapDegrees((float)Math.toDegrees(angle[0]) + 180.0F), 0.0F);
                           rider.setDeltaMovement(-Math.sin(angle[0]) * 2.7, 0.0, Math.cos(angle[0]) * 2.7);
                           FormFlags.set(rider, 2, true);
                           rider.doTick();
                           if (age % 40 == 0) {
                              DevilFruits.LOGGER
                                 .info(
                                    "[dsync] server age {} form {} shell {}",
                                    new Object[]{age, DFAttachments.data(rider).formName(), DragonShells.get(rider) != null}
                                 );
                           }

                           return age > 400;
                        } else {
                           return true;
                        }
                     }
                  );
                  p.teleportTo(level, 0.5, (double)s.origin().getY(), -2.5, 180.0F, -15.0F);
               }
            )
      );
      s.add(start + 2, mc -> {
         CaptureRig.off();
         mc.options.setCameraType(CameraType.FIRST_PERSON);
      });

      for (int k = 20; k <= 300; k++) {
         int kk = k;
         s.add(start + k, mc -> log(mc, kk));
      }

      for (int k = 60; k <= 300; k += 20) {
         int kk = k;
         s.add(start + k, mc -> s.shot(mc, String.format("vfx_dsync_%03d", kk)));
      }

      s.add(start + 310, mc -> s.server(mc, p -> {
            if (rider != null) {
               FormManager.revert(rider, FormManager.Reason.COMMAND);
               p.getServer().getPlayerList().remove(rider);
               rider = null;
            }
         }));
      return start + 320;
   }
}
