package com.devilfruits.fruits.suna_suna.client;

import com.devilfruits.client.fx.ClientFxRegistry;
import com.devilfruits.client.logia.LogiaMovementClient;
import com.devilfruits.client.particle.Fx;
import com.devilfruits.client.render.CameraEffects;
import com.devilfruits.client.render.vfx.VfxManager;
import com.devilfruits.client.sound.SoundDesign;
import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.core.network.FxPayload;
import com.devilfruits.fruits.suna_suna.SandTravel;
import com.devilfruits.fruits.suna_suna.SunaFx;
import com.devilfruits.fruits.suna_suna.SunaParticles;
import com.devilfruits.fruits.suna_suna.SunaSounds;
import com.devilfruits.fruits.suna_suna.client.vfx.CloudCinematic;
import com.devilfruits.fruits.suna_suna.client.vfx.SandBlastCinematic;
import com.devilfruits.fruits.suna_suna.client.vfx.SandCinematic;
import com.devilfruits.fruits.suna_suna.client.vfx.SandCloudEffect;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class SandTravelClient {
   public static void register() {
      LogiaMovementClient.register(SandTravel.CLOUD.id(), new SandTravelClient.CloudController());
      ClientFxRegistry.register(SunaFx.CLOUD, SandTravelClient::cloud);
      ClientFxRegistry.register(SunaFx.CLOUD_END, SandTravelClient::cloudEnd);
   }

   static void tick(Player player) {
      ResourceLocation mode = LogiaMovementClient.modeOf(player);
      if (!SandTravel.CLOUD.id().equals(mode) || VfxSettings.legacy()) {
         if (SandTravel.CLOUD.id().equals(mode) && VfxManager.find(SandCloudEffect.class, e -> e.playerId == player.getId()) == null) {
            VfxManager.add(new SandCloudEffect(player));
         }
      } else if (CloudCinematic.of(player) == null) {
         VfxDirector.spawn(
            new CloudCinematic(
               (ClientLevel)player.level(), VfxAnchor.of(player, player.position()), new VfxParams(player.position(), Vec3.ZERO, 1.0F, (long)player.getId(), 0)
            )
         );
      }
   }

   private static void cloud(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      if (entity == Minecraft.getInstance().player) {
         SandTravelClient.CloudController.speed = p.power();
         CameraEffects.fovPunch(0.03F);
      }

      if (VfxSettings.legacy()) {
         burst(p.pos(), 18, 0.18);
      } else {
         RandomSource r = Fx.random();
         int tint = SandBlastCinematic.tint(level, p.pos());
         Vec3 c = p.pos().add(0.0, 0.9, 0.0);
         SandCinematic.burst(r, c, Vec3.ZERO, 1.0F, 0.3F, 22.0F, 10.0F, tint);
         SandCinematic.clouds(r, c.x, c.y, c.z, 0.0, 0.02, 0.0, 1.6F, 6.0F, tint);
      }

      SoundDesign.play(p.pos(), SunaSounds.DISSOLVE, 0.9F, 0.9F, SoundDesign.layer(SunaSounds.LAYER_GUST, 0.7F, 0.8F));
   }

   private static void cloudEnd(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      if (VfxSettings.legacy()) {
         burst(p.pos().add(0.0, 0.9, 0.0), 10, 0.1);
      } else {
         RandomSource r = Fx.random();
         int tint = SandBlastCinematic.tint(level, p.pos());
         Vec3 c = p.pos().add(0.0, 0.9, 0.0);

         for (int i = 0; i < VfxSettings.count(16.0F); i++) {
            Vec3 d = SandCinematic.jitter(r, Vec3.ZERO, 1.0F).scale(1.6);
            SandCinematic.grains(r, c.x + d.x, c.y + d.y, c.z + d.z, -d.x * 0.2, -d.y * 0.2, -d.z * 0.2, tint).life(0.35F);
         }

         SandCinematic.puff(r, c.x, c.y, c.z, 0.0, 0.01, 0.0, tint).size(1.4F);
      }

      SoundDesign.play(p.pos(), SunaSounds.REFORM, 0.8F, 1.0F);
   }

   private static void burst(Vec3 at, int count, double speed) {
      RandomSource r = Fx.random();
      int n = Fx.count(count);

      for (int i = 0; i < n; i++) {
         Fx.spawn(
            r.nextInt(4) == 0
               ? (ParticleOptions)SunaParticles.DUST.get()
               : (r.nextInt(3) == 0 ? (ParticleOptions)SunaParticles.CLUMP.get() : (ParticleOptions)SunaParticles.GRAIN.get()),
            at.x + Fx.spread(0.4),
            at.y + Fx.spread(0.6),
            at.z + Fx.spread(0.4),
            Fx.spread(speed),
            Fx.spread(speed) + 0.03,
            Fx.spread(speed)
         );
      }
   }

   private SandTravelClient() {
   }

   private static final class CloudController implements LogiaMovementClient.Controller {
      static float speed = 0.6F;
      private Vec3 velocity = Vec3.ZERO;
      private float forward;
      private float strafe;
      private float vertical;

      @Override
      public void input(LocalPlayer player, Input input) {
         this.forward = (float)((input.up ? 1 : 0) - (input.down ? 1 : 0));
         this.strafe = (float)((input.left ? 1 : 0) - (input.right ? 1 : 0));
         this.vertical = (float)((input.jumping ? 1 : 0) - (input.shiftKeyDown ? 1 : 0));
         input.forwardImpulse = 0.0F;
         input.leftImpulse = 0.0F;
         input.up = input.down = input.left = input.right = false;
         input.jumping = false;
         input.shiftKeyDown = false;
      }

      @Override
      public void tick(LocalPlayer player) {
         player.fallDistance = 0.0F;
         player.setSprinting(false);
         Vec3 look = player.getLookAngle();
         Vec3 flat = new Vec3(look.x, 0.0, look.z);
         flat = flat.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
         Vec3 left = new Vec3(flat.z, 0.0, -flat.x);
         Vec3 want = look.scale((double)this.forward).add(left.scale((double)this.strafe)).add(0.0, (double)this.vertical, 0.0);
         if (want.lengthSqr() > 1.0) {
            want = want.normalize();
         }

         boolean hurry = Minecraft.getInstance().options.keySprint.isDown();
         want = want.scale((double)speed * (hurry ? 1.5 : 1.0));
         this.velocity = this.velocity.lerp(want, 0.2);
         double bob = want.lengthSqr() < 1.0E-4 ? Math.sin((double)player.tickCount * 0.12) * 0.012 : 0.0;
         player.setDeltaMovement(this.velocity.add(0.0, bob, 0.0));
      }

      @Override
      public void leave(LocalPlayer player) {
         this.velocity = Vec3.ZERO;
         player.fallDistance = 0.0F;
      }
   }
}
