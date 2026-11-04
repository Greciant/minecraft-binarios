package com.devilfruits.fruits.yomi_yomi.client.vfx;

import com.devilfruits.client.vfx.Cinematic;
import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.render.MeshWriter;
import com.devilfruits.client.vfx.sprite.VfxSprites;
import com.devilfruits.fruits.yomi_yomi.client.SoulKingGhost;
import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class SoulKingAura extends Cinematic {
   private final int id;
   private Vec3 at;

   public SoulKingAura(Entity user) {
      super(VfxAnchor.of(user, user.position()), VfxParams.NONE);
      this.id = user.getId();
      this.at = user.position();
   }

   public int id() {
      return this.id;
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (!SoulKingGhost.present(this.id)) {
         this.finish();
      } else {
         Vec3 base = SoulKingGhost.point(this.id, 8);
         if (base != null) {
            this.at = base;
         }
      }
   }

   @Override
   public void render(VfxFrame f) {
      BufferBuilder d = f.distortion();
      float fade = SoulKingGhost.fire(this.id);
      if (d != null && !(fade <= 0.01F)) {
         VfxSprites.Sprite glow = VfxSprites.GLOW;
         Vec3 head = SoulKingGhost.point(this.id, 1);
         Vec3 chest = SoulKingGhost.point(this.id, 3);
         Vec3 base = SoulKingGhost.point(this.id, 8);
         if (head != null) {
            MeshWriter.distortQuad(
               d,
               f.rx(head.x),
               f.ry(head.y + 1.0),
               f.rz(head.z),
               3.4F,
               0.55F * fade,
               1,
               false,
               f.left.x,
               f.left.y,
               f.left.z,
               f.up.x,
               f.up.y,
               f.up.z,
               glow.u0(),
               glow.v0(),
               glow.u1(),
               glow.v1(),
               f.time * 0.006F,
               0.35F
            );
         }

         if (chest != null) {
            MeshWriter.distortQuad(
               d,
               f.rx(chest.x),
               f.ry(chest.y + 0.8),
               f.rz(chest.z),
               4.2F,
               0.35F * fade,
               1,
               false,
               f.left.x,
               f.left.y,
               f.left.z,
               f.up.x,
               f.up.y,
               f.up.z,
               glow.u0(),
               glow.v0(),
               glow.u1(),
               glow.v1(),
               0.37F + f.time * 0.005F,
               0.35F
            );
         }

         if (base != null) {
            MeshWriter.distortQuad(
               d,
               f.rx(base.x),
               f.ry(base.y + 1.3),
               f.rz(base.z),
               4.0F,
               0.45F * fade,
               1,
               false,
               f.left.x,
               f.left.y,
               f.left.z,
               f.up.x,
               f.up.y,
               f.up.z,
               glow.u0(),
               glow.v0(),
               glow.u1(),
               glow.v1(),
               0.71F + f.time * 0.006F,
               0.35F
            );
         }
      }
   }

   @Override
   public double x() {
      return this.at.x;
   }

   @Override
   public double y() {
      return this.at.y + 3.0;
   }

   @Override
   public double z() {
      return this.at.z;
   }

   @Override
   public double cullDistance() {
      return 96.0;
   }
}
