package com.devilfruits.fruits.kage_kage.client.vfx;

import com.devilfruits.client.vfx.VfxAnchor;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class InjectCinematic extends KageCinematic {
   private final Player user;
   private final Vec3 corpse;
   private final ShadowPool pool;

   public InjectCinematic(Player user, Vec3 corpse, long seed) {
      super(VfxAnchor.of(user, corpse), seed);
      this.user = user;
      this.corpse = corpse;
      this.pool = new ShadowPool(corpse, 1.1F, 1.3F, seed);
      this.pools.add(this.pool);
      KageLook.sound((ClientLevel)user.level(), corpse, (SoundEvent)SoundEvents.SOUL_ESCAPE.value(), 1.0F, 0.6F);
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (this.age == 12) {
         this.pool.pulse(0.8F);
         this.motes(level, this.corpse.add(0.0, 0.3, 0.0), 8, 0.05);
         KageLook.sound(level, this.corpse, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.7F, 0.5F);
      }

      if (this.age == 24) {
         this.flash(level, this.corpse.add(0.0, 0.4, 0.0), 1.2F, 0.6F);
         this.pool.pulse(1.0F);
      }

      if (this.age == 44) {
         this.pool.sink();
      }

      if (this.age > 44 && this.pool.gone() && this.flakes.isEmpty()) {
         this.finish();
      }
   }

   @Override
   protected void drawModels(boolean glow, PoseStack ps, MultiBufferSource buffers, Vec3 cam, float partial, float entityPartial) {
      float t = (float)this.age + partial;
      if (!(t > 14.0F)) {
         Vec3 chest = this.user.getPosition(entityPartial).add(0.0, (double)this.user.getBbHeight() * 0.62, 0.0);
         Vec3 into = this.corpse.add(0.0, 0.25, 0.0);
         float reach = Mth.clamp(t / 8.0F, 0.0F, 1.0F);
         float tail = t < 8.0F ? 0.0F : Mth.clamp((t - 8.0F) / 6.0F, 0.0F, 1.0F);
         int light = light(this.corpse.add(0.0, 1.0, 0.0));

         for (int k = 0; k < 3; k++) {
            Vec3 root = chest.lerp(into, (double)tail * 0.95);
            Vec3 bend = chest.lerp(into, 0.5).add((double)(k - 1) * 0.5, 1.0 + (double)k * 0.2, 0.0);
            Tendrils.draw(glow, ps, buffers, cam, root, bend, into, reach, t + (float)(k * 4), 0.16F * (1.0F - tail * 0.6F), light, 31L * (long)k);
         }
      }
   }

   @Override
   public double x() {
      return this.corpse.x;
   }

   @Override
   public double y() {
      return this.corpse.y;
   }

   @Override
   public double z() {
      return this.corpse.z;
   }
}
