package com.awakenedmod.awakened.client.render.limitless;

import com.awakenedmod.awakened.client.render.HeadAnchor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class FrozenGaze {
   private static final float OUT = 0.3F;
   public final Vector3f left = new Vector3f();
   public final Vector3f right = new Vector3f();
   public final Vector3f look = new Vector3f();
   public final Vector3f up = new Vector3f();
   public float head;

   public boolean of(Entity e, float partialTick, Vec3 cam) {
      if (e instanceof LivingEntity living) {
         HeadAnchor.Head h = HeadAnchor.of(living, partialTick);
         this.look.set(h.forward());
         this.up.set(h.up());
         this.head = 4.0F * h.px();
         rel(h.eye(-1, 4.3F), cam, this.left);
         rel(h.eye(1, 4.3F), cam, this.right);
         return true;
      } else {
         return false;
      }
   }

   private static void rel(Vec3 p, Vec3 cam, Vector3f out) {
      out.set((float)(p.x - cam.x), (float)(p.y - cam.y), (float)(p.z - cam.z));
   }
}
