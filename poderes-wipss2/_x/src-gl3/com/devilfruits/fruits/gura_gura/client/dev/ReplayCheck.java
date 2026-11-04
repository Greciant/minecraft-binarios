package com.devilfruits.fruits.gura_gura.client.dev;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.dev.AutoTest;
import com.devilfruits.client.dev.CaptureRig;
import com.devilfruits.client.dev.CaptureStage;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.rules.Mastery;
import com.devilfruits.fruits.gura_gura.GuraGura;
import com.devilfruits.registry.DFAttachments;
import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Arrays;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent.Post;
import net.neoforged.neoforge.common.NeoForge;

public final class ReplayCheck extends CaptureStage {
   private static final String[] BLOWS = new String[]{"kaishin", "kabutowari", "gekishin"};
   private static long joined;
   private static final long[] CAST = new long[BLOWS.length];
   private static long left;
   private static double stageY;

   public ReplayCheck() {
      super("vfx_rp_");
   }

   @Override
   protected ResourceLocation fruit() {
      return GuraGura.ID;
   }

   @Override
   protected void setup(ServerPlayer p) {
      DFAttachments.data(p).setMastery(10);
      command(p, "time set 6000");
   }

   @Override
   protected void onReset(ServerPlayer p) {
      PlayerFruitData data = DFAttachments.data(p);
      data.clearCooldowns();
      data.setEnergy((float)Mastery.maxEnergy(data));
   }

   static void log(String message, Object... args) {
      DevilFruits.LOGGER.info("[devilfruits autotest] replay: " + message, args);
   }

   @Override
   protected int takes(AutoTest s, int t) {
      s.add(t, mc -> {
         joined = System.currentTimeMillis() - (long)mc.player.tickCount * 50L;
         stageY = (double)s.origin().getY();
         log("recording since {} ms ago", System.currentTimeMillis() - joined);
      });
      t += 20;

      for (int i = 0; i < BLOWS.length; i++) {
         int n = i;
         t = this.take(s, t, 20, none(), 0, p -> castAbility(p, "devilfruits:gura_gura/" + BLOWS[n], 1), rig(14.0, 135.0F, 14.0F, 0.5), 60, c -> {
            s.add(c, mc -> CAST[n] = System.currentTimeMillis() - joined);
            this.shot(s, c + 9, "live_" + BLOWS[n]);
         });
      }

      s.add(t + 10, mc -> {
         left = System.currentTimeMillis() - joined;
         log("blows at {} ms, world left at {} ms (join-relative)", Arrays.toString(CAST), left);
         CaptureRig.off();
         mc.options.hideGui = true;
         NeoForge.EVENT_BUS.register(ReplayCheck.Viewer.class);
         mc.level.disconnect();
         mc.disconnect(new TitleScreen());
      });
      return t + 400;
   }

   public static final class Viewer {
      private static final long TIMEOUT_MS = 600000L;
      private static final long STARTED = System.currentTimeMillis();
      private static final boolean[] PLAYED = new boolean[ReplayCheck.BLOWS.length];
      private static Object handler;
      private static File replay;
      private static long size = -1L;
      private static long stableSince;
      private static long openedAt;
      private static int stage;
      private static long stageAt;
      private static int frames;

      @SubscribeEvent
      public static void onFrame(Post event) {
         Minecraft mc = Minecraft.getInstance();
         long now = System.currentTimeMillis();

         try {
            if (now - STARTED > 600000L) {
               ReplayCheck.log("timed out in stage {}", stage);
               mc.stop();
            } else if (handler == null) {
               waitAndOpen(mc, now);
            } else {
               step(mc, now);
            }
         } catch (Exception var5) {
            DevilFruits.LOGGER.error("[devilfruits autotest] replay: stage {} failed", stage, var5);
            mc.stop();
         }
      }

      private static void next(long now) {
         stage++;
         stageAt = now;
         frames = 0;
      }

      private static void step(Minecraft mc, long now) {
         frames++;
         if (stage > 0) {
            place();
         }

         long ts = sender("currentTimeStamp");
         switch (stage) {
            case 0:
               if (now - openedAt > 4000L) {
                  jump(ReplayCheck.CAST[0] - 1500L);
                  place();
                  setSpeed(1.0);
                  ReplayCheck.log("playing from {} ms", sender("currentTimeStamp"));
                  next(now);
               }
               break;
            case 1:
               unpause();

               for (int i = 0; i < ReplayCheck.BLOWS.length; i++) {
                  if (!PLAYED[i] && ts >= ReplayCheck.CAST[i] + 450L) {
                     PLAYED[i] = true;
                     shot("play_" + ReplayCheck.BLOWS[i]);
                  }
               }

               if (ts >= ReplayCheck.CAST[ReplayCheck.BLOWS.length - 1] + 2000L) {
                  long t0 = System.nanoTime();
                  jump(ReplayCheck.CAST[0] + 450L);
                  place();
                  setSpeed(1.0);
                  ReplayCheck.log("jumped back to {} ms in {} ms", sender("currentTimeStamp"), (System.nanoTime() - t0) / 1000000L);
                  next(now);
               } else if (now - stageAt > 30000L) {
                  ReplayCheck.log("replay stuck at {} ms (paused = {})", ts, call(call(handler, "getReplaySender"), "paused"));
                  next(now);
               }
               break;
            case 2:
               unpause();
               if (frames == 3) {
                  shot("back_first");
               }

               if (ts >= ReplayCheck.CAST[0] + 1200L || now - stageAt > 10000L) {
                  shot("back_later");
                  jump(ReplayCheck.CAST[1] + 450L);
                  place();
                  setSpeed(0.0);
                  next(now);
               }
               break;
            case 3:
               if (frames == 10) {
                  shot("pause_a");
               }

               if (now - stageAt > 2500L) {
                  shot("pause_b");
                  ReplayCheck.log("paused at {} ms", ts);
                  next(now);
               }
               break;
            case 4:
               if (now - stageAt > 1500L) {
                  ReplayCheck.log("done");
                  call(handler, "endReplay");
                  next(now);
               }
               break;
            default:
               if (now - stageAt > 3000L) {
                  mc.stop();
               }
         }
      }

      private static void waitAndOpen(Minecraft mc, long now) throws Exception {
         if (mc.level == null && mc.screen instanceof TitleScreen) {
            if (replay == null) {
               Object core = Class.forName("com.replaymod.core.ReplayMod").getField("instance").get(null);
               Object folders = core.getClass().getField("folders").get(core);
               Path dir = (Path)folders.getClass().getMethod("getReplayFolder").invoke(folders);
               File[] files = dir.toFile().listFiles((d, name) -> name.endsWith(".mcpr"));

               for (File f : files == null ? new File[0] : files) {
                  if (f.lastModified() >= STARTED - 120000L && (replay == null || f.lastModified() > replay.lastModified())) {
                     replay = f;
                  }
               }

               if (replay == null) {
                  return;
               }
            }

            if (replay.length() != size) {
               size = replay.length();
               stableSince = now;
            } else if (now - stableSince >= 2000L) {
               ReplayCheck.log("opening {} ({} KB)", replay.getName(), size / 1024L);
               Object rr = Class.forName("com.replaymod.replay.ReplayModReplay").getField("instance").get(null);
               rr.getClass().getMethod("startReplay", File.class).invoke(rr, replay);
               handler = rr.getClass().getMethod("getReplayHandler").invoke(rr);
               openedAt = now;
               long duration = ((Number)call(handler, "getReplayDuration")).longValue();

               for (int i = 0; i < ReplayCheck.CAST.length; i++) {
                  ReplayCheck.CAST[i] = duration - (ReplayCheck.left - ReplayCheck.CAST[i]);
               }

               ReplayCheck.log("replay open, {} ms long, blows at {} ms", duration, Arrays.toString(ReplayCheck.CAST));
            }
         }
      }

      private static Object call(Object o, String name) {
         try {
            Method m = o.getClass().getMethod(name);
            return m.invoke(o);
         } catch (ReflectiveOperationException var3) {
            throw new IllegalStateException(var3);
         }
      }

      private static long sender(String name) {
         return ((Number)call(call(handler, "getReplaySender"), name)).longValue();
      }

      private static void jump(long ms) {
         try {
            handler.getClass().getMethod("doJump", int.class, boolean.class).invoke(handler, (int)Math.max(0L, ms), true);
         } catch (ReflectiveOperationException var3) {
            throw new IllegalStateException(var3);
         }
      }

      private static void unpause() {
         if ((Boolean)call(call(handler, "getReplaySender"), "paused")) {
            setSpeed(1.0);
         }
      }

      private static void setSpeed(double speed) {
         try {
            Object sender = call(handler, "getReplaySender");
            sender.getClass().getMethod("setReplaySpeed", double.class).invoke(sender, speed);
         } catch (ReflectiveOperationException var3) {
            throw new IllegalStateException(var3);
         }
      }

      private static void place() {
         Object camera = call(handler, "getCameraEntity");
         if (camera != null) {
            try {
               camera.getClass().getMethod("setCameraPosition", double.class, double.class, double.class).invoke(camera, 10.4, ReplayCheck.stageY + 4.0, 7.5);
               camera.getClass().getMethod("setCameraRotation", float.class, float.class, float.class).invoke(camera, 135.0F, 14.0F, 0.0F);
            } catch (ReflectiveOperationException var2) {
               throw new IllegalStateException(var2);
            }
         }
      }

      private static void shot(String name) {
         Minecraft mc = Minecraft.getInstance();
         long ts = sender("currentTimeStamp");
         Screenshot.grab(mc.gameDirectory, "vfx_rp_" + name + ".png", mc.getMainRenderTarget(), message -> ReplayCheck.log("screenshot {} at {} ms", name, ts));
      }

      private Viewer() {
      }
   }
}
