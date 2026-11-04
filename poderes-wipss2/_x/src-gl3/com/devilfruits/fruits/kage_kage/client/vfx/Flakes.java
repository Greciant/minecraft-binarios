package com.devilfruits.fruits.kage_kage.client.vfx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class Flakes {
   private final List<Flakes.Flake> flakes = new ArrayList<>();

   public void burst(Vec3 at, Vec3 vel, float size, int life) {
      this.flakes.add(new Flakes.Flake(at, vel, size, life, 0.03F, 0.4F));
   }

   public void mote(Vec3 at, Vec3 drift, float size, int life) {
      this.flakes.add(new Flakes.Flake(at, drift, size, life, -0.002F, 0.15F));
   }

   public boolean isEmpty() {
      return this.flakes.isEmpty();
   }

   public void tick() {
      for (Flakes.Flake f : this.flakes) {
         f.prev = f.pos;
         f.pos = f.pos.add(f.vel);
         f.vel = f.vel.scale(0.9).add(0.0, (double)(-f.gravity), 0.0);
         f.age++;
         float k = 1.0F - (float)f.age / (float)f.life;
         f.size = f.start * Math.max(0.0F, k * (2.0F - k));
      }

      this.flakes.removeIf(fx -> fx.age >= fx.life);
   }

   public void draw(boolean glow, PoseStack ps, MultiBufferSource buffers, Vec3 cam, float partial, int light, RimConsumer rim) {
      if (!this.flakes.isEmpty()) {
         VertexConsumer vc = glow ? null : buffers.getBuffer(RenderType.entityCutout(KageLook.white()));
         if (glow) {
            rim.to(buffers.getBuffer(RenderType.eyes(KageLook.white())), 0.0F, 8007103, 1.0F);
         }

         for (Flakes.Flake f : this.flakes) {
            if (!(f.size < 0.01F) && (!glow || !(f.start < 0.06F))) {
               Vec3 p = f.prev.lerp(f.pos, (double)partial);
               ps.pushPose();
               ps.translate(p.x - cam.x, p.y - cam.y, p.z - cam.z);
               float a = ((float)f.age + partial) * f.spin;
               ps.mulPose(Axis.YP.rotation(a));
               ps.mulPose(Axis.XP.rotation(a * 0.7F));
               float h = f.size / 2.0F;
               if (glow) {
                  Voxels.rim(rim, ps.last(), -h, -h, -h, h, h, h, 63, 0.012F);
               } else {
                  int rgb = KageLook.mix(459788, 2495552, Mth.frac(f.start * 37.0F));
                  Voxels.box(vc, ps.last(), -h, -h, -h, h, h, h, 63, 0xFF000000 | rgb, light);
               }

               ps.popPose();
            }
         }
      }
   }

   private static final class Flake {
      Vec3 pos;
      Vec3 prev;
      Vec3 vel;
      float size;
      final float start;
      int age;
      final int life;
      final float gravity;
      final float spin;

      Flake(Vec3 pos, Vec3 vel, float size, int life, float gravity, float spin) {
         this.pos = pos;
         this.prev = pos;
         this.vel = vel;
         this.size = size;
         this.start = size;
         this.life = life;
         this.gravity = gravity;
         this.spin = spin;
      }
   }
}
