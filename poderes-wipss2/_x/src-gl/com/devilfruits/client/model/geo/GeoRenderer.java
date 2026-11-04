package com.devilfruits.client.model.geo;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class GeoRenderer {
   private final GeoModel model;
   private final Matrix4f[] matrices;
   private final Matrix3f[] normals;
   private final Vector3f p = new Vector3f();
   private final Vector3f n = new Vector3f();

   public GeoRenderer(GeoModel model) {
      this.model = model;
      this.matrices = new Matrix4f[model.boneCount()];
      this.normals = new Matrix3f[model.boneCount()];

      for (int i = 0; i < this.matrices.length; i++) {
         this.matrices[i] = new Matrix4f();
         this.normals[i] = new Matrix3f();
      }
   }

   public GeoModel model() {
      return this.model;
   }

   public void pose(Pose base, GeoPose pose) {
      for (GeoModel.Bone bone : this.model.bones) {
         int i = bone.index;
         int o = i * 3;
         Matrix4f m = this.matrices[i];
         Matrix3f nm = this.normals[i];
         if (bone.parent >= 0) {
            m.set(this.matrices[bone.parent]);
            nm.set(this.normals[bone.parent]);
         } else {
            m.set(base.pose());
            nm.set(base.normal());
         }

         float rx = bone.rotX + pose.rot[o];
         float ry = bone.rotY + pose.rot[o + 1];
         float rz = bone.rotZ + pose.rot[o + 2];
         m.translate(bone.pivotX + pose.pos[o], bone.pivotY + pose.pos[o + 1], bone.pivotZ + pose.pos[o + 2]);
         if (rz != 0.0F) {
            m.rotateZ(rz);
            nm.rotateZ(rz);
         }

         if (ry != 0.0F) {
            m.rotateY(ry);
            nm.rotateY(ry);
         }

         if (rx != 0.0F) {
            m.rotateX(rx);
            nm.rotateX(rx);
         }

         float sx = pose.scale[o];
         float sy = pose.scale[o + 1];
         float sz = pose.scale[o + 2];
         if (sx != 1.0F || sy != 1.0F || sz != 1.0F) {
            m.scale(sx, sy, sz);
         }

         m.translate(-bone.pivotX, -bone.pivotY, -bone.pivotZ);
      }
   }

   public Matrix4f boneMatrix(int bone) {
      return this.matrices[bone];
   }

   public Matrix3f boneNormal(int bone) {
      return this.normals[bone];
   }

   public void render(VertexConsumer vc, GeoPose pose, int light, int overlay, int color) {
      boolean[] hidden = pose.hidden;

      for (GeoModel.Bone bone : this.model.bones) {
         if (!hidden[bone.index] && (bone.parent < 0 || !hidden[bone.parent])) {
            this.renderBone(vc, bone, light, overlay, color);
         } else {
            hidden[bone.index] = true;
         }
      }
   }

   public void renderBone(VertexConsumer vc, GeoModel.Bone bone, int light, int overlay, int color) {
      float[] mesh = bone.mesh;
      if (mesh.length != 0) {
         Matrix4f m = this.matrices[bone.index];
         Matrix3f nm = this.normals[bone.index];

         for (int v = 0; v < mesh.length; v += 8) {
            m.transformPosition(mesh[v], mesh[v + 1], mesh[v + 2], this.p);
            nm.transform(mesh[v + 5], mesh[v + 6], mesh[v + 7], this.n);
            float len = this.n.length();
            if (len > 1.0E-6F) {
               this.n.div(len);
            }

            vc.addVertex(this.p.x, this.p.y, this.p.z, color, mesh[v + 3], mesh[v + 4], overlay, light, this.n.x, this.n.y, this.n.z);
         }
      }
   }
}
