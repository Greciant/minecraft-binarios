package com.devilfruits.client.limb;

import com.devilfruits.DevilFruits;
import com.devilfruits.core.limb.LimbPath;
import com.devilfruits.core.limb.LimbUv;
import com.devilfruits.core.network.FxPayload;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.resources.PlayerSkin.Model;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3f;

public final class LimbTest {
   private static LimbTest.Skin skin = LimbTest.Skin.OWN;
   private static LimbTest.Mode mode = LimbTest.Mode.OFF;
   private static final Vector3d PIVOT = new Vector3d();
   private static final Vector3f P = new Vector3f();

   static void onFx(ClientLevel level, @Nullable Entity entity, FxPayload payload) {
      set(
         LimbTest.Skin.values()[Math.min(payload.extra() & 15, LimbTest.Skin.values().length - 1)],
         LimbTest.Mode.values()[Math.min(payload.extra() >> 4 & 15, LimbTest.Mode.values().length - 1)]
      );
   }

   public static void set(LimbTest.Skin skin, LimbTest.Mode mode) {
      AbstractClientPlayer player = Minecraft.getInstance().player;
      if (player != null) {
         Limbs.clear(player);
         LimbTest.skin = mode == LimbTest.Mode.OFF ? LimbTest.Skin.OWN : skin;
         LimbTest.mode = mode;
         switch (mode) {
            case OFF:
            default:
               break;
            case REST:
               Limbs.add(player, new LimbSpec(), (f, path) -> {
                  f.rootPosed();
                  path.add(f.posed(0.0F, 12.0F, 0.0F, P));
                  return mode == LimbTest.Mode.REST;
               });
               break;
            case B3:
               Limbs.add(player, new LimbSpec(), straight(LimbTest.Mode.B3, 3.0, 0.35));
               Limbs.add(player, new LimbSpec().arm(LimbUv.Side.LEFT), straight(LimbTest.Mode.B3, 3.0, -0.35));
               break;
            case B10:
               Limbs.add(player, new LimbSpec(), straight(LimbTest.Mode.B10, 10.0, 0.0));
               break;
            case B20:
               Limbs.add(player, new LimbSpec(), straight(LimbTest.Mode.B20, 20.0, 0.0));
               break;
            case BENT:
               Limbs.add(player, new LimbSpec(), LimbTest::bent);
               break;
            case LEG:
               Limbs.add(player, new LimbSpec().leg(LimbUv.Side.RIGHT), LimbTest::leg);
               break;
            case THIN: {
               LimbSpec spec = new LimbSpec();
               spec.tension = 1.0F;
               spec.fist = 1.35F;
               Limbs.add(player, spec, straight(LimbTest.Mode.THIN, 4.0, 0.0));
               break;
            }
            case TWIST: {
               LimbSpec spec = new LimbSpec();
               spec.twistTurns = 3.5F;
               LimbDriver line = straight(LimbTest.Mode.TWIST, 10.0, 0.0);
               Limbs.add(player, spec, (f, path) -> {
                  spec.spin = f.time() * 0.6F;
                  return line.path(f, path);
               });
            }
         }
      }
   }

   static void reset() {
      skin = LimbTest.Skin.OWN;
      mode = LimbTest.Mode.OFF;
   }

   @Nullable
   public static PlayerSkin skin(AbstractClientPlayer player) {
      return skin.look != null && player == Minecraft.getInstance().player ? skin.look : null;
   }

   private static LimbDriver straight(LimbTest.Mode owner, double blocks, double side) {
      return (f, path) -> {
         Vec3 look = f.player().getViewVector(f.partial());
         Vec3 right = look.cross(new Vec3(0.0, 1.0, 0.0)).normalize();
         f.pivotWorld(PIVOT);
         path.add(f.px(PIVOT.x + look.x * blocks + right.x * side, PIVOT.y + look.y * blocks, PIVOT.z + look.z * blocks + right.z * side, P));
         return mode == owner;
      };
   }

   private static boolean bent(LimbFrame f, LimbPath path) {
      Vec3 look = f.player().getViewVector(f.partial());
      Vec3 right = look.cross(new Vec3(0.0, 1.0, 0.0)).normalize();
      f.pivotWorld(PIVOT);
      point(f, path, look.scale(2.5).add(right.scale(1.8)).add(0.0, 0.4, 0.0));
      point(f, path, look.scale(5.0).add(right.scale(-1.0)).add(0.0, 0.9, 0.0));
      point(f, path, look.scale(8.0).add(right.scale(0.4)));
      return mode == LimbTest.Mode.BENT;
   }

   private static boolean leg(LimbFrame f, LimbPath path) {
      Vec3 look = f.player().getViewVector(f.partial()).multiply(1.0, 0.0, 1.0).normalize();
      f.pivotWorld(PIVOT);
      point(f, path, look.scale(0.6).add(0.0, 2.4, 0.0));
      point(f, path, look.scale(1.6).add(0.0, 4.6, 0.0));
      return mode == LimbTest.Mode.LEG;
   }

   private static void point(LimbFrame f, LimbPath path, Vec3 offset) {
      path.add(f.px(PIVOT.x + offset.x, PIVOT.y + offset.y, PIVOT.z + offset.z, P));
   }

   private LimbTest() {
   }

   public static enum Mode {
      OFF,
      REST,
      B3,
      B10,
      B20,
      BENT,
      LEG,
      THIN,
      TWIST;
   }

   public static enum Skin {
      OWN(null, null),
      CLASSIC("classic", Model.WIDE),
      SLIM("slim", Model.SLIM),
      HD("hd", Model.WIDE);

      @Nullable
      final PlayerSkin look;

      private Skin(@Nullable String name, @Nullable Model model) {
         ResourceLocation texture = name == null ? null : DevilFruits.id("textures/entity/gomu_gomu/test_skins/" + name + ".png");
         this.look = texture == null ? null : new PlayerSkin(texture, null, null, null, model, true);
      }
   }
}
