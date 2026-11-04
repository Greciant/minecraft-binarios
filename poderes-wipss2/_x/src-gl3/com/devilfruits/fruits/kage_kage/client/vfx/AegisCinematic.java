package com.devilfruits.fruits.kage_kage.client.vfx;

import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.client.vfx.fx.VfxWorld;
import com.devilfruits.core.form.FormFlags;
import com.devilfruits.fruits.kage_kage.KageKage;
import com.mojang.blaze3d.vertex.PoseStack;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class AegisCinematic extends KageCinematic {
   private static final int CHUNKS = 3;
   private final Player user;
   private final ShadowChunk[] chunks = new ShadowChunk[3];
   private final float[] offset = new float[3];
   private final ShadowPool pool;
   private int sinkingSince = -1;
   private int flagGone;

   public AegisCinematic(Player user, long seed) {
      super(VfxAnchor.of(user, user.position()), seed);
      this.user = user;

      for (int i = 0; i < 3; i++) {
         this.chunks[i] = new ShadowChunk(seed + (long)i * 31L).snap(user.position());
         this.offset[i] = (float) (Math.PI * 2) * (float)i / 3.0F + this.random.nextFloat() * 0.6F;
      }

      this.pool = new ShadowPool(this.feet(), 0.95F, 1.2F, seed);
      this.pools.add(this.pool);
      ClientLevel level = (ClientLevel)user.level();
      KageLook.sound(level, user.position(), SoundEvents.BAT_TAKEOFF, 0.6F, 0.6F);
      KageLook.sound(level, user.position(), (SoundEvent)SoundEvents.SOUL_ESCAPE.value(), 0.8F, 0.7F);
      KageLook.sound(level, user.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 0.5F, 0.5F);
   }

   @Nullable
   public static AegisCinematic of(Player user) {
      return VfxDirector.find(AegisCinematic.class, c -> c.user == user && c.sinkingSince < 0);
   }

   private Vec3 feet() {
      Vec3 p = this.user.position();
      return new Vec3(p.x, VfxWorld.ground(this.user.level(), p.x, p.y + 0.5, p.z, 3), p.z);
   }

   public Vec3[] handOver() {
      Vec3[] out = new Vec3[3];

      for (int i = 0; i < 3; i++) {
         out[i] = this.chunks[i].pos;
      }

      this.finish();
      return out;
   }

   public void sink() {
      if (this.sinkingSince < 0) {
         this.sinkingSince = this.age;
         ShadowPool last = new ShadowPool(this.feet(), 0.7F, 0.9F, this.random.nextLong());
         this.pools.add(last);
         KageLook.sound((ClientLevel)this.user.level(), this.user.position(), SoundEvents.SOUL_SAND_BREAK, 0.7F, 0.6F);
      }
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (this.user.isRemoved()) {
         this.finish();
      } else {
         if (this.sinkingSince < 0 && this.age > 4 && !FormFlags.has(this.user, KageKage.ID, KageKage.AEGIS) && ++this.flagGone > 10) {
            this.sink();
         }

         if (this.age == 14) {
            this.pool.sink();
         }

         if (this.sinkingSince < 0) {
            this.pool.centre = this.feet();
         }

         Vec3 base = this.user.position();
         float rise = Mth.clamp((float)this.age / 10.0F, 0.0F, 1.0F);
         float fall = this.sinkingSince < 0 ? 0.0F : Mth.clamp((float)(this.age - this.sinkingSince) / 10.0F, 0.0F, 1.0F);

         for (int i = 0; i < 3; i++) {
            ShadowChunk c = this.chunks[i];
            c.tick();
            float a = this.offset[i] + (float)this.age * 0.085F;
            float r = Mth.lerp(outBack(rise), 0.25F, 0.95F + 0.06F * Mth.sin((float)this.age * 0.11F + (float)i));
            double h = (double)Mth.lerp(
               rise * rise * (3.0F - 2.0F * rise), 0.05F, 0.8F + 0.3F * (float)i / 2.0F + 0.07F * Mth.sin((float)this.age * 0.2F + (float)i * 2.0F)
            );
            h = Mth.lerp((double)(fall * fall), h, 0.0);
            c.pos = new Vec3(base.x + (double)(Mth.cos(a) * r), base.y + h, base.z + (double)(Mth.sin(a) * r));
            c.radius = 0.2F * outBack(rise) * (1.0F - fall);
            c.heat = 0.95F + 0.15F * Mth.sin((float)this.age * 0.3F + (float)i * 1.9F);
            c.sy = 1.0F + 0.08F * Mth.sin((float)this.age * 0.25F + (float)i);
            if (this.random.nextFloat() < 0.08F) {
               this.motes(level, c.pos, 1, 0.01);
            }
         }

         if (this.sinkingSince >= 0 && this.age - this.sinkingSince > 24) {
            this.finish();
         }
      }
   }

   @Override
   protected void drawModels(boolean glow, PoseStack ps, MultiBufferSource buffers, Vec3 cam, float partial, float entityPartial) {
      int light = light(this.user.position().add(0.0, 1.0, 0.0));

      for (ShadowChunk c : this.chunks) {
         c.draw(glow, ps, buffers, cam, partial, light, this.rim);
      }
   }

   @Override
   public double x() {
      return this.user.getX();
   }

   @Override
   public double y() {
      return this.user.getY() + 1.0;
   }

   @Override
   public double z() {
      return this.user.getZ();
   }
}
