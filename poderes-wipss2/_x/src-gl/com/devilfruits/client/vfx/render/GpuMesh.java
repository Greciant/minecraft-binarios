package com.devilfruits.client.vfx.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexBuffer.Usage;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;

final class GpuMesh {
   private final ByteBufferBuilder bytes;
   private VertexBuffer vbo;
   private BufferBuilder builder;
   private boolean uploaded;

   GpuMesh(int capacity) {
      this.bytes = new ByteBufferBuilder(capacity);
   }

   BufferBuilder begin() {
      if (this.builder == null) {
         this.builder = new BufferBuilder(this.bytes, Mode.QUADS, VfxFormat.FORMAT);
      }

      return this.builder;
   }

   boolean upload() {
      this.uploaded = false;
      if (this.builder == null) {
         return false;
      } else {
         MeshData mesh = this.builder.build();
         this.builder = null;
         if (mesh == null) {
            return false;
         } else {
            if (this.vbo == null) {
               this.vbo = new VertexBuffer(Usage.DYNAMIC);
            }

            this.vbo.bind();
            this.vbo.upload(mesh);
            this.uploaded = true;
            return true;
         }
      }
   }

   boolean ready() {
      return this.uploaded;
   }

   void draw(ShaderInstance shader, Matrix4f modelView, Matrix4f projection) {
      if (this.uploaded) {
         this.vbo.bind();
         this.vbo.drawWithShader(modelView, projection, shader);
      }
   }

   void discard() {
      if (this.builder != null) {
         MeshData mesh = this.builder.build();
         if (mesh != null) {
            mesh.close();
         }

         this.builder = null;
      }

      this.uploaded = false;
   }
}
