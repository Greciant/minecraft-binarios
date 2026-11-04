package com.devilfruits.client.render.vfx;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;

public class StunLabelEffect extends VfxEffect {
   private static final int RED = -50384;
   private static final int OUTLINE = -14022648;
   private final int entityId;
   private long until;
   private double x;
   private double y;
   private double z;
   private float lift;
   private int shownTenths = -1;
   private FormattedCharSequence text = FormattedCharSequence.EMPTY;
   private int width;
   private final Matrix4f m = new Matrix4f();

   public StunLabelEffect(Entity entity, long until) {
      this.entityId = entity.getId();
      this.until = until;
      this.follow(entity);
   }

   public int entityId() {
      return this.entityId;
   }

   public void extend(long until) {
      this.until = until;
   }

   public void end() {
      this.until = 0L;
   }

   private void follow(Entity e) {
      this.x = e.getX();
      this.y = e.getY();
      this.z = e.getZ();
      this.lift = e.getBbHeight() + 0.55F;
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      Entity e = level.getEntity(this.entityId);
      long left = this.until - level.getGameTime();
      if (e != null && e.isAlive() && left > 0L) {
         this.follow(e);
         int tenths = (int)Math.ceil((double)left / 2.0);
         if (tenths != this.shownTenths) {
            this.shownTenths = tenths;
            String label = Component.translatable("hud.devilfruits.stunned").getString()
               + " ("
               + String.format(Locale.ROOT, "%.1f", (float)tenths / 10.0F)
               + ")";
            this.text = Component.literal(label).getVisualOrderText();
            this.width = Minecraft.getInstance().font.width(this.text);
         }
      } else {
         this.finish();
      }
   }

   @Override
   public void renderTextured(VfxContext ctx) {
      Entity e = Minecraft.getInstance().level == null ? null : Minecraft.getInstance().level.getEntity(this.entityId);
      double ex = e != null ? Mth.lerp((double)ctx.partialTick, e.xOld, e.getX()) : this.x;
      double ey = e != null ? Mth.lerp((double)ctx.partialTick, e.yOld, e.getY()) : this.y;
      double ez = e != null ? Mth.lerp((double)ctx.partialTick, e.zOld, e.getZ()) : this.z;
      float pop = Math.min(1.0F, ((float)this.age + ctx.partialTick) / 3.0F);
      float scale = 0.03F * (0.7F + 0.3F * pop);
      this.m.set(ctx.matrix).translate(ctx.rx(ex), ctx.ry(ey + (double)this.lift), ctx.rz(ez)).rotate(ctx.camera.rotation()).scale(scale, -scale, scale);
      Font font = Minecraft.getInstance().font;
      font.drawInBatch8xOutline(this.text, (float)(-this.width) / 2.0F, 0.0F, -50384, -14022648, this.m, ctx.buffers, 15728880);
   }

   @Override
   public double x() {
      return this.x;
   }

   @Override
   public double y() {
      return this.y + (double)this.lift;
   }

   @Override
   public double z() {
      return this.z;
   }

   @Override
   public double cullDistance() {
      return 48.0;
   }
}
