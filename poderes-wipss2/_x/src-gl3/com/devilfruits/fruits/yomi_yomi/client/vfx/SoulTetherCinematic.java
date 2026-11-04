package com.devilfruits.fruits.yomi_yomi.client.vfx;

import com.devilfruits.client.vfx.Cinematic;
import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.fx.Tether;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.render.MeshTex;
import com.devilfruits.fruits.yomi_yomi.YomiYomi;
import com.devilfruits.fruits.yomi_yomi.entity.SoulShell;
import com.devilfruits.fruits.yomi_yomi.soul.SoulProjection;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class SoulTetherCinematic extends Cinematic {
   public static final int ACID = 8257280;
   private static final int CORE = 12910410;
   private static final double SKULL = 1.15;
   private final Player soul;
   private final RandomSource random;
   private int shellId;
   private Vec3 bodyPos;
   private float fade;
   private float prevFade;
   private int recoilAge = -1;

   public SoulTetherCinematic(Player soul, int shellId, Vec3 bodyPos, long seed) {
      super(VfxAnchor.of(soul, soul.position()), VfxParams.NONE);
      this.soul = soul;
      this.shellId = shellId;
      this.bodyPos = bodyPos;
      this.random = RandomSource.create(seed);
   }

   public Player soul() {
      return this.soul;
   }

   @Nullable
   private SoulShell shell(ClientLevel level) {
      Entity e = level.getEntity(this.shellId);
      if (e instanceof SoulShell) {
         return (SoulShell)e;
      } else {
         for (Entity ex : level.entitiesForRendering()) {
            if (ex instanceof SoulShell s && this.soul.getUUID().equals(s.ownerId())) {
               this.shellId = s.getId();
               return s;
            }
         }

         return null;
      }
   }

   private boolean localFirstPerson() {
      Minecraft mc = Minecraft.getInstance();
      return this.soul == mc.player && mc.options.getCameraType().isFirstPerson();
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      this.prevFade = this.fade;
      SoulShell shell = this.shell(level);
      if (shell != null) {
         this.bodyPos = shell.position();
      }

      boolean out = !this.soul.isRemoved() && SoulProjection.active(this.soul);
      if (out && this.recoilAge < 0 && SoulProjection.recoiling(this.soul)) {
         this.recoilAge = this.age;
      }

      this.fade = out ? Math.min(1.0F, this.fade + 0.15F) : Math.max(0.0F, this.fade - 0.25F);
      if (this.fade <= 0.0F && this.prevFade <= 0.0F && this.age > 3) {
         this.finish();
      } else {
         if (this.fade > 0.3F && !VfxSettings.legacy() && this.age % 3 == 0) {
            Vec3 p = this.bodyPos.add(0.0, 0.75, 0.0).lerp(this.soul.position().add(0.0, 1.15, 0.0), this.random.nextDouble());
            VfxParticles.emit(YomiLook.EMBER, p.x, p.y, p.z, (this.random.nextDouble() - 0.5) * 0.02, 0.01, (this.random.nextDouble() - 0.5) * 0.02);
         }
      }
   }

   @Override
   public void render(VfxFrame f) {
      float k = Mth.lerp(f.partial, this.prevFade, this.fade);
      if (!(k <= 0.01F) && this.soul.level() == Minecraft.getInstance().level) {
         Vec3 from = this.bodyPos.add(0.0, 0.75, 0.0);
         Vec3 to = this.soul.getPosition(f.entityPartial).add(0.0, this.localFirstPerson() ? 0.5 : 1.15, 0.0);
         if (this.recoilAge >= 0) {
            float t = (float)this.age + f.partial - (float)this.recoilAge;
            float r = Mth.clamp((t - 6.0F) / 3.0F, 0.0F, 1.0F);
            to = to.lerp(from, (double)(r * r));
         }

         double leash = leash();
         float strain = Tether.strain(to.distanceTo(from), leash);
         int skip = this.localFirstPerson() ? 3 : 0;
         Tether.draw(f, from, to, leash, MeshTex.HORO_MIST, 8257280, 2.4F, 0.11F, 0.85F * k, skip, 0.0F);
         Tether.draw(f, from, to, leash, MeshTex.HORO_MIST, Tether.lerpRgb(12910410, 15794096, strain), 3.0F, 0.036F, 1.0F * k, skip, 0.0F);
      }
   }

   public static double leash() {
      try {
         return YomiYomi.setting("soulLeash");
      } catch (IllegalStateException var1) {
         return 64.0;
      }
   }

   @Override
   public double cullDistance() {
      return 160.0;
   }

   @Override
   public double x() {
      return this.soul.getX();
   }

   @Override
   public double y() {
      return this.soul.getY() + 1.0;
   }

   @Override
   public double z() {
      return this.soul.getZ();
   }
}
