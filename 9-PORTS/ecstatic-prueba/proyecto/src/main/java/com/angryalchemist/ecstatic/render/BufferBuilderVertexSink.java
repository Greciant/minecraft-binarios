package com.angryalchemist.ecstatic.render;

import com.mojang.blaze3d.vertex.BufferBuilder;

// Port 1.20.1: el BufferBuilder de 1.20.1 escribe los elementos en el orden del formato
// (si no coincide, el elemento se ignora en silencio) y cada vertice necesita endVertex().
// Formatos: POSITION_COLOR (pos, color), POSITION_TEX_COLOR (pos, uv, color),
// BLOCK_SAFE (pos, color, uv0, uv2, normal).
final class BufferBuilderVertexSink implements VertexSink {
    private final BufferBuilder builder;

    BufferBuilderVertexSink(BufferBuilder builder) {
        this.builder = builder;
    }

    @Override
    public void vertex(float x, float y, float z, int colorRgb, int alpha) {
        int r = colorRgb >> 16 & 0xFF;
        int g = colorRgb >> 8 & 0xFF;
        int b = colorRgb & 0xFF;
        this.builder.vertex(x, y, z).color(r, g, b, alpha).endVertex();
    }

    @Override
    public void litVertex(float x, float y, float z, int colorRgb, int alpha, float nx, float ny, float nz) {
        int r = colorRgb >> 16 & 0xFF;
        int g = colorRgb >> 8 & 0xFF;
        int b = colorRgb & 0xFF;
        this.builder.vertex(x, y, z).color(r, g, b, alpha).uv(0.5F, 0.5F).uv2(0xF000F0).normal(nx, ny, nz).endVertex();
    }

    @Override
    public void texturedVertex(float x, float y, float z, float u, float v, int colorRgb, int alpha) {
        int r = colorRgb >> 16 & 0xFF;
        int g = colorRgb >> 8 & 0xFF;
        int b = colorRgb & 0xFF;
        this.builder.vertex(x, y, z).uv(u, v).color(r, g, b, alpha).endVertex();
    }

    @Override
    public void litTexturedVertex(float x, float y, float z, float u, float v, int colorRgb, int alpha, float nx, float ny, float nz) {
        int r = colorRgb >> 16 & 0xFF;
        int g = colorRgb >> 8 & 0xFF;
        int b = colorRgb & 0xFF;
        this.builder.vertex(x, y, z).color(r, g, b, alpha).uv(u, v).uv2(0xF000F0).normal(nx, ny, nz).endVertex();
    }
}
