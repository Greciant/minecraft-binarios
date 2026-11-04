package com.devilfruits.client.logia;

import com.devilfruits.client.particle.Fx;
import com.devilfruits.client.render.vfx.VfxEffect;
import com.devilfruits.core.logia.LogiaDash;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class LogiaDashEffect extends VfxEffect {
   private final Player player;
   private final int maxAge;
   private double px;
   private double py;
   private double pz;

   public LogiaDashEffect(Player player, Vec3 from, int ticks) {
      this.player = player;
      this.maxAge = ticks + 10 + 2;
      this.px = from.x;
      this.py = from.y;
      this.pz = from.z;
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      ResourceLocation mode = LogiaMovementClient.modeOf(this.player);
      if (!this.player.isRemoved() && this.age <= this.maxAge && (this.age <= 2 || mode != null && mode.equals(LogiaDash.MODE.id()))) {
         LogiaTheme theme = LogiaThemes.current(this.player);
         Vec3 prev = new Vec3(this.px, this.py, this.pz);
         Vec3 now = this.player.position();
         this.px = now.x;
         this.py = now.y;
         this.pz = now.z;
         Vec3 step = now.subtract(prev);
         double length = step.length();
         Vec3 along = length > 1.0E-4 ? step.scale(1.0 / length) : Vec3.ZERO;
         Minecraft mc = Minecraft.getInstance();
         boolean ownFirstPerson = this.player == mc.player && mc.options.getCameraType().isFirstPerson();
         RandomSource r = Fx.random();
         float h = this.player.getBbHeight();
         if (ownFirstPerson) {
            rushPast(theme, mc, along, r);
         } else {
            int n = Fx.count((float)(2.0 + length * 9.0));
            double spin = (double)this.age * 0.9;
            Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();

            for (int i = 0; i < n; i++) {
               double t = Math.sqrt(r.nextDouble());
               Vec3 at = prev.lerp(now, t);
               double a = spin + r.nextDouble() * (float) (Math.PI * 2);
               double rad = 0.15 + r.nextDouble() * 0.3;
               double y = 0.15 + r.nextDouble() * (double)h * 0.75;
               double ox = Math.cos(a) * rad;
               double oz = Math.sin(a) * rad;
               double s = 0.05 + r.nextDouble() * 0.1;
               if (!(camera.distanceToSqr(at.x + ox, at.y + y, at.z + oz) < 2.25)) {
                  Fx.spawn(
                     r.nextInt(5) == 0 ? theme.burst(r) : theme.shed(r),
                     at.x + ox,
                     at.y + y,
                     at.z + oz,
                     along.x * s - oz * 0.12,
                     along.y * s + 0.01,
                     along.z * s + ox * 0.12
                  );
               }
            }
         }

         theme.hooks().dashing(this.player, prev, now);
      } else {
         this.finish();
      }
   }

   private static void rushPast(LogiaTheme theme, Minecraft mc, Vec3 along, RandomSource r) {
      if (along != Vec3.ZERO) {
         Vec3 eye = mc.gameRenderer.getMainCamera().getPosition();
         Vec3 side = new Vec3(-along.z, 0.0, along.x);
         side = side.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : side.normalize();
         Vec3 up = side.cross(along).normalize();
         int n = Fx.count(5.0F);

         for (int i = 0; i < n; i++) {
            double a = r.nextDouble() * (float) (Math.PI * 2);
            double rad = 1.3 + r.nextDouble() * 0.8;
            Vec3 at = eye.add(along.scale(2.5 + r.nextDouble() * 2.0)).add(side.scale(Math.cos(a) * rad)).add(up.scale(Math.sin(a) * rad * 0.7));
            double s = 0.6 + r.nextDouble() * 0.4;
            Fx.spawn(theme.shed(r), at.x, at.y, at.z, -along.x * s, -along.y * s, -along.z * s);
         }
      }
   }

   @Override
   public double x() {
      return this.px;
   }

   @Override
   public double y() {
      return this.py;
   }

   @Override
   public double z() {
      return this.pz;
   }
}
