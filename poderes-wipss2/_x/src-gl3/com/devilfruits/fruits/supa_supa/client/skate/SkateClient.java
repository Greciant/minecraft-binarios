package com.devilfruits.fruits.supa_supa.client.skate;

import com.devilfruits.client.animation.AnimationController;
import com.devilfruits.client.vfx.Cinematic;
import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxClock;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.VfxSound;
import com.devilfruits.client.vfx.fx.VfxWorld;
import com.devilfruits.client.vfx.particle.Ribbon;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.render.MeshTex;
import com.devilfruits.core.config.CoreClientConfig;
import com.devilfruits.fruits.supa_supa.BladeLimbs;
import com.devilfruits.fruits.supa_supa.SkateMotion;
import com.devilfruits.fruits.supa_supa.SupaAbilities;
import com.devilfruits.fruits.supa_supa.SupaNetwork;
import com.devilfruits.fruits.supa_supa.SupaSounds;
import com.devilfruits.fruits.supa_supa.SupaSupa;
import com.devilfruits.fruits.supa_supa.client.blade.BladeLimbsClient;
import com.devilfruits.fruits.supa_supa.client.vfx.SupaLook;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.Iterator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.client.event.InputEvent.InteractionKeyMappingTriggered;
import net.neoforged.neoforge.client.event.ViewportEvent.ComputeFov;
import net.neoforged.neoforge.network.PacketDistributor;

public final class SkateClient {
   private static final ResourceLocation POSE_CLIP = SupaSupa.id("skate_pose");
   private static final ResourceLocation LIFT_CLIP = SupaSupa.id("skate_lift");
   private static final RandomSource RANDOM = RandomSource.create();
   private static Vec3 skateV = Vec3.ZERO;
   private static boolean controlling;
   private static int lungeWait;
   private static float fov;
   private static final Int2ObjectMap<SkateClient.Watch> WATCH = new Int2ObjectOpenHashMap();
   public static final AnimationController.PoseLayer POSE = new AnimationController.PoseLayer() {
      @Override
      public void poses(Player player, float partialTick, AnimationController.PoseSink out) {
         BladeLimbsClient.Limbs limbs = BladeLimbsClient.of(player);
         float lift = limbs == null ? 0.0F : limbs.legs.growth(partialTick);
         if (lift > 0.01F) {
            out.add(SkateClient.LIFT_CLIP, 0.0F, Math.min(1.0F, lift));
         }

         if (SkateClient.skating(player)) {
            float weight = (float)Mth.clamp(SkateClient.speed(player) / 0.35, 0.0, 1.0);
            if (weight > 0.01F) {
               out.add(SkateClient.POSE_CLIP, (float)player.tickCount + partialTick, weight);
            }
         }
      }

      @Override
      public void lean(Player player, float partialTick, float[] pitchRoll) {
         SkateClient.Watch w = (SkateClient.Watch)SkateClient.WATCH.get(player.getId());
         if (w != null && SkateClient.skating(player)) {
            float weight = (float)Mth.clamp(w.speed / 0.35, 0.0, 1.0);
            pitchRoll[0] += 0.12F * weight;
            pitchRoll[1] += Mth.clamp(-w.turn * 3.2F, -0.42F, 0.42F) * weight;
         }
      }
   };

   public static boolean skating(Player p) {
      return BladeLimbs.of(p).legs();
   }

   public static double speed(Player p) {
      SkateClient.Watch w = (SkateClient.Watch)WATCH.get(p.getId());
      return w == null ? 0.0 : w.speed;
   }

   public static void onClientTick(Post event) {
      Minecraft mc = Minecraft.getInstance();
      ClientLevel level = mc.level;
      if (level != null && !mc.isPaused()) {
         if (mc.player != null) {
            control(mc.player);
         }

         if (lungeWait > 0) {
            lungeWait--;
         }

         Iterator<SkateClient.Watch> it = WATCH.values().iterator();

         while (it.hasNext()) {
            SkateClient.Watch w = it.next();
            w.seen = false;
         }

         for (Player p : level.players()) {
            if (skating(p)) {
               SkateClient.Watch w = (SkateClient.Watch)WATCH.computeIfAbsent(p.getId(), k -> new SkateClient.Watch());
               w.seen = true;
               watch(level, p, w);
            }
         }

         it = WATCH.values().iterator();

         while (it.hasNext()) {
            SkateClient.Watch w = it.next();
            if (!w.seen) {
               if (w.scrape != null) {
                  w.scrape.end();
               }

               if (w.trail != null) {
                  w.trail.ended = true;
               }

               it.remove();
            }
         }
      }
   }

   private static void control(LocalPlayer p) {
      boolean on = skating(p) && !p.isInWater() && !p.isInLava() && !p.getAbilities().flying && !p.isPassenger() && !p.isFallFlying();
      if (!on) {
         controlling = false;
         skateV = Vec3.ZERO;
         fov *= 0.85F;
      } else {
         Vec3 dm = p.getDeltaMovement();
         if (!controlling) {
            skateV = new Vec3(dm.x, 0.0, dm.z);
            controlling = true;
         }

         Vec3 moved = new Vec3(p.getX() - p.xo, 0.0, p.getZ() - p.zo);
         if (skateV.length() > 0.08 && moved.length() < skateV.length() * 0.45) {
            skateV = moved;
         }

         SkateMotion.Params params = SupaAbilities.BLADE_SKATES.params();
         skateV = SkateMotion.step(skateV, (double)p.input.forwardImpulse, (double)p.input.leftImpulse, p.getYRot(), p.onGround(), p.input.shiftKeyDown, params);
         p.setDeltaMovement(skateV.x, dm.y, skateV.z);
         fov = fov + ((float)(skateV.length() / params.maxSpeed()) - fov) * 0.2F;
      }
   }

   public static void onInteraction(InteractionKeyMappingTriggered event) {
      Minecraft mc = Minecraft.getInstance();
      if (event.isAttack() && mc.player != null && controlling && skating(mc.player)) {
         event.setCanceled(true);
         event.setSwingHand(false);
         if (lungeWait <= 0) {
            lungeWait = (int)SupaAbilities.BLADE_SKATES.config().extra("lungeCooldownTicks");
            PacketDistributor.sendToServer(new SupaNetwork.LungePayload(), new CustomPacketPayload[0]);
            Vec3 look = mc.player.getLookAngle();
            skateV = skateV.add(new Vec3(look.x, 0.0, look.z).normalize().scale(0.22));
         }
      }
   }

   public static void onFov(ComputeFov event) {
      if (fov > 0.01F && event.usedConfiguredFov()) {
         event.setFOV(event.getFOV() * (1.0 + 0.09 * (double)Math.min(1.0F, fov)));
      }
   }

   private static void watch(ClientLevel level, Player p, SkateClient.Watch w) {
      double vx = p.getX() - p.xo;
      double vz = p.getZ() - p.zo;
      double speed = Math.sqrt(vx * vx + vz * vz);
      float heading = speed > 0.02 ? (float)Math.atan2(vz, vx) : w.heading;
      w.turn = w.init ? Mth.wrapDegrees((heading - w.heading) * (180.0F / (float)Math.PI)) * (float) (Math.PI / 180.0) : 0.0F;
      w.heading = heading;
      w.init = true;
      double before = w.speed;
      w.speed = speed;
      boolean ground = p.onGround();
      if (w.brakeWait > 0) {
         w.brakeWait--;
      }

      if (ground && p.isCrouching() && before > 0.3 && before - speed > 0.02 && w.brakeWait == 0) {
         w.brakeWait = 16;
         level.playLocalSound(p.getX(), p.getY(), p.getZ(), SupaSounds.SKATES_BRAKE.get(), SoundSource.PLAYERS, 0.9F, 0.9F + (float)before * 0.4F, false);
      }

      if (ground && speed > 0.05 && (w.scrape == null || w.scrape.isStopped())) {
         w.scrape = VfxSound.play(
            SupaSounds.SKATES_SCRAPE.get(),
            p,
            p.position(),
            true,
            age -> Math.min(0.75F, (float)w.speed * 1.1F),
            age -> 0.7F + (float)w.speed * 0.9F,
            72000,
            () -> skating(p) && w.speed > 0.03 && p.onGround() && !p.isRemoved()
         );
      }

      if (VfxSettings.tier() != CoreClientConfig.CinematicQuality.LOW && ground && !(speed < 0.12)) {
         Vec3 dir = new Vec3(vx / speed, 0.0, vz / speed);
         Vec3 side = new Vec3(-dir.z, 0.0, dir.x);
         BlockPos under = BlockPos.containing(p.getX(), p.getY() - 0.05, p.getZ());
         BlockState floor = level.getBlockState(under);
         boolean ice = floor.is(BlockTags.ICE);
         boolean snow = floor.is(Blocks.SNOW)
            || floor.is(Blocks.SNOW_BLOCK)
            || floor.is(Blocks.POWDER_SNOW)
            || level.getBlockState(p.blockPosition()).is(Blocks.SNOW);
         boolean hard = !snow && !ice && floor.getDestroySpeed(level, under) >= 1.2F;
         double y = p.getY() + 0.03;

         for (int foot = -1; foot <= 1; foot += 2) {
            Vec3 at = new Vec3(p.getX(), y, p.getZ()).add(side.scale(0.13 * (double)foot)).add(dir.scale(-0.2));
            if (snow) {
               if (RANDOM.nextFloat() < 0.8F) {
                  VfxParticles.emit(
                        SupaLook.SNOW,
                        at.x,
                        at.y + 0.05,
                        at.z,
                        -dir.x * speed * 0.6 + side.x * (double)foot * 0.04,
                        0.06 + (double)RANDOM.nextFloat() * 0.06,
                        -dir.z * speed * 0.6 + side.z * (double)foot * 0.04
                     )
                     .tint(16054527);
               }
            } else if (hard) {
               int sparks = VfxSettings.count((float)(speed * 3.2));

               for (int i = 0; i < sparks; i++) {
                  VfxParticles.emit(
                        SupaLook.SPARK,
                        at.x,
                        at.y + 0.04,
                        at.z,
                        -dir.x * (0.15 + (double)RANDOM.nextFloat() * 0.25) + side.x * (double)foot * (double)RANDOM.nextFloat() * 0.08,
                        0.05 + (double)RANDOM.nextFloat() * 0.12,
                        -dir.z * (0.15 + (double)RANDOM.nextFloat() * 0.25) + side.z * (double)foot * (double)RANDOM.nextFloat() * 0.08
                     )
                     .size(0.6F);
               }
            } else if (!ice && RANDOM.nextFloat() < 0.5F) {
               int dust = VfxWorld.shade(VfxWorld.groundColour(level, at.x, at.y - 0.5, at.z), VfxWorld.light(level, at.x, at.y + 0.5, at.z));
               VfxParticles.emit(SupaLook.DUST, at.x, at.y + 0.08, at.z, -dir.x * 0.05, 0.015, -dir.z * 0.05).tint(dust).size(0.5F).alpha(0.4F);
            }
         }

         w.scratch += speed;
         if (w.scratch >= 0.45 && (hard || ice)) {
            w.scratch = 0.0;
            float rot = heading;

            for (int footx = -1; footx <= 1; footx += 2) {
               Vec3 at = new Vec3(p.getX(), Math.floor(p.getY() + 0.01) + 0.012, p.getZ()).add(side.scale(0.13 * (double)footx));
               VfxParticles.emit(ice ? SupaLook.SCRATCH_ICE : SupaLook.SCRATCH, at.x, at.y, at.z, 0.0, 0.0, 0.0)
                  .rotation(rot)
                  .tint(ice ? 15267071 : 4211790)
                  .alpha(ice ? 0.7F : 0.5F);
            }
         }

         if (w.trail == null || w.trail.isDone()) {
            w.trail = VfxDirector.spawn(new SkateClient.Trail(p, w));
         }
      }
   }

   public static void clear() {
      ObjectIterator var0 = WATCH.values().iterator();

      while (var0.hasNext()) {
         SkateClient.Watch w = (SkateClient.Watch)var0.next();
         if (w.scrape != null) {
            w.scrape.end();
         }
      }

      WATCH.clear();
      controlling = false;
      skateV = Vec3.ZERO;
   }

   private SkateClient() {
   }

   static final class Trail extends Cinematic {
      private final Player player;
      private final SkateClient.Watch watch;
      private final Ribbon[] ribbons = new Ribbon[2];
      boolean ended;

      Trail(Player player, SkateClient.Watch watch) {
         super(VfxAnchor.of(player, player.position()), VfxParams.NONE);
         this.player = player;
         this.watch = watch;

         for (int i = 0; i < 2; i++) {
            Ribbon r = Ribbon.create(14);
            if (r != null) {
               r.look(0.045F, 9.0F, 0.75F, 0.25F, 1.2F, 5).texture(MeshTex.CUT_RIBBON, 0.3F);
            }

            this.ribbons[i] = r;
         }
      }

      private Vec3 foot(int i, float partial) {
         Vec3 pos = this.player.getPosition(partial);
         float yaw = this.watch.heading;
         Vec3 side = new Vec3((double)(-Mth.sin(yaw)), 0.0, (double)Mth.cos(yaw));
         return pos.add(side.scale(i == 0 ? 0.13 : -0.13)).add(0.0, 0.06, 0.0);
      }

      @Override
      public void tick(ClientLevel level) {
         super.tick(level);
         if (!this.ended && !this.player.isRemoved()) {
            if (this.watch.speed > 0.25) {
               for (int i = 0; i < 2; i++) {
                  if (this.ribbons[i] != null) {
                     Vec3 f = this.foot(i, 1.0F);
                     this.ribbons[i].push(f.x, f.y, f.z, VfxClock.time());
                  }
               }
            }
         } else {
            boolean faded = true;

            for (Ribbon r : this.ribbons) {
               faded &= r == null || r.faded(VfxClock.time());
            }

            if (faded) {
               this.finish();
            }
         }
      }

      @Override
      public void render(VfxFrame f) {
         float fade = (float)Mth.clamp((this.watch.speed - 0.2) * 2.5, 0.0, 1.0) * (this.ended ? 0.5F : 1.0F);

         for (int i = 0; i < 2; i++) {
            if (this.ribbons[i] != null) {
               Vec3 h = this.foot(i, f.entityPartial);
               this.ribbons[i].render(f, h.x, h.y, h.z, fade);
            }
         }
      }

      @Override
      public void removed() {
         for (Ribbon r : this.ribbons) {
            if (r != null) {
               r.release();
            }
         }
      }

      @Override
      public double x() {
         return this.player.getX();
      }

      @Override
      public double y() {
         return this.player.getY();
      }

      @Override
      public double z() {
         return this.player.getZ();
      }
   }

   static final class Watch {
      double speed;
      float heading;
      float turn;
      boolean init;
      int brakeWait;
      double scratch;
      VfxSound scrape;
      SkateClient.Trail trail;
      boolean seen;
   }
}
