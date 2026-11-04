package com.devilfruits.fruits.yomi_yomi.client.vfx;

import com.devilfruits.client.vfx.Cinematic;
import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxParams;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public abstract class YomiCinematic extends Cinematic implements YomiRender.Drawable {
   protected final RandomSource random;
   protected final List<IceProp> ice = new ArrayList<>();
   protected final List<Frost.Creep> creeps = new ArrayList<>();
   protected final Vec3 at;

   protected YomiCinematic(VfxAnchor anchor, Vec3 at, long seed) {
      super(anchor, VfxParams.NONE);
      this.at = at;
      this.random = RandomSource.create(seed);
   }

   protected abstract int length();

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (this.age == 1) {
         YomiRender.add(this);
      }

      for (Frost.Creep c : this.creeps) {
         c.tick();
      }

      float end = (float)this.length();

      for (IceProp p : this.ice) {
         p.tick((float)this.age);
         end = Math.max(end, p.end());
      }

      if ((float)this.age > end + 1.0F) {
         this.finish();
      }
   }

   @Override
   public void removed() {
      YomiRender.remove(this);
   }

   @Override
   public void draw(PoseStack ps, MultiBufferSource buffers, Vec3 cam, float partial) {
      if (!this.isDone()) {
         float t = (float)this.age + partial;

         for (IceProp p : this.ice) {
            p.draw(ps, buffers, cam, t);
         }
      }
   }

   @Override
   public double x() {
      return this.at.x;
   }

   @Override
   public double y() {
      return this.at.y;
   }

   @Override
   public double z() {
      return this.at.z;
   }

   @Nullable
   protected Entity anchorEntity() {
      Minecraft mc = Minecraft.getInstance();
      return mc.level == null ? null : this.anchor.entity(mc.level);
   }
}
