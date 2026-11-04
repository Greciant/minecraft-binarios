package com.awakenedmod.awakened.client.chrono.stasis;

import com.awakenedmod.awakened.client.VanillaParticles;
import com.awakenedmod.awakened.client.chrono.ChronoFxClient;
import com.awakenedmod.awakened.content.chronokinetic.ChronoSounds;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class StasisCue {
   public static final int SECOND = 20;
   private static final Set<Object> PLAYED = new HashSet<>();
   private static final double PARTICLE_RANGE_SQR = 1024.0;
   private final ClientLevel level;
   private final ChronoFxClient.Effect effect;
   private final boolean leads;
   @Nullable
   private final Entity body;

   public StasisCue(ClientLevel level, ChronoFxClient.Effect effect, boolean leads, @Nullable Entity body) {
      this.level = level;
      this.effect = effect;
      this.leads = leads;
      this.body = body;
   }

   public static void newTick() {
      PLAYED.clear();
   }

   public ClientLevel level() {
      return this.level;
   }

   @Nullable
   public Entity body() {
      return this.body;
   }

   public int age() {
      return (int)this.effect.age(0.0F);
   }

   public int hold() {
      return Math.max(1, this.effect.duration);
   }

   public boolean released() {
      return this.effect.ended();
   }

   public int sinceRelease() {
      return this.released() ? (int)this.effect.sinceEnd(0.0F) : -1;
   }

   public int secondsLeftMark() {
      int left = this.hold() - this.age();
      return !this.released() && left > 0 && left % 20 == 0 ? left / 20 : -1;
   }

   public boolean leads() {
      return this.leads;
   }

   public Vec3 feet() {
      return this.body != null ? this.body.position() : this.effect.pos.subtract(0.0, (double)this.effect.radius * 0.65, 0.0);
   }

   public float width() {
      return this.body != null ? this.body.getBbWidth() : this.effect.radius * 0.6F;
   }

   public float height() {
      return this.body != null ? this.body.getBbHeight() : this.effect.radius;
   }

   public float bodyYaw() {
      if (this.body instanceof LivingEntity living) {
         return living.yBodyRot;
      } else {
         return this.body != null ? this.body.getYRot() : 0.0F;
      }
   }

   public Vec3 local(double x, double y, double z) {
      float yaw = this.bodyYaw() * (float) (Math.PI / 180.0);
      float cos = Mth.cos(yaw);
      float sin = Mth.sin(yaw);
      return this.feet().add(x * (double)cos - z * (double)sin, y, x * (double)sin + z * (double)cos);
   }

   public Vec3 chest() {
      return this.feet().add(0.0, (double)this.height() * 0.65, 0.0);
   }

   public void play(ChronoSounds.Cue cue) {
      if (!this.hidden() && PLAYED.add(cue)) {
         cue.playLocal(this.level, this.chest(), 1.0F, 1.0F);
      }
   }

   @Nullable
   public Particle particle(ParticleOptions options, Vec3 at, Vec3 velocity) {
      if (this.hidden()) {
         return null;
      } else {
         Minecraft mc = Minecraft.getInstance();
         if (!options.getType().getOverrideLimiter()) {
            Vec3 eye = mc.gameRenderer.getMainCamera().getPosition();
            if (eye.distanceToSqr(at) > 1024.0 || !this.particlesAllowed(mc)) {
               return null;
            }
         }

         return mc.particleEngine.createParticle(options, at.x, at.y, at.z, velocity.x, velocity.y, velocity.z);
      }
   }

   private boolean particlesAllowed(Minecraft mc) {
      return VanillaParticles.allowed(mc, this.level.random);
   }

   private boolean hidden() {
      Player viewer = Minecraft.getInstance().player;
      return this.body != null && viewer != null && this.body.isInvisibleTo(viewer);
   }
}
