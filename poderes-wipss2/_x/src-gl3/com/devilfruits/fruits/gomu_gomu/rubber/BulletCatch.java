package com.devilfruits.fruits.gomu_gomu.rubber;

import com.devilfruits.fruits.gomu_gomu.GomuGomu;
import com.devilfruits.fruits.gomu_gomu.GomuNetwork;
import com.devilfruits.fruits.gomu_gomu.GomuSounds;
import com.devilfruits.fruits.gomu_gomu.GomuTags;
import com.devilfruits.fruits.gomu_gomu.GomuTriggers;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class BulletCatch {
   public static final String RETURNED = "devilfruits_gomu_returned";
   private static final List<BulletCatch.Held> HELD = new ArrayList<>();

   public static boolean isBullet(Entity projectile) {
      if (projectile.getType().is(GomuTags.BULLETS)) {
         return true;
      } else {
         ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(projectile.getType());
         return GomuGomu.fruit().config().list("bulletEntities").contains(id.toString());
      }
   }

   public static boolean catches(ServerPlayer user, Projectile projectile) {
      return GomuGomu.canUse(user) && GomuGomu.flag("bulletImmunity") && isBullet(projectile) && projectile.getOwner() != user;
   }

   public static long absorb(ServerPlayer user, Projectile projectile, @Nullable Vec3 hit) {
      Vec3 velocity = projectile.getDeltaMovement();
      Vec3 dir = velocity.lengthSqr() > 1.0E-6 ? velocity.normalize() : user.getLookAngle().reverse();
      Vec3 at = surface(user, projectile.position(), dir, hit);
      CompoundTag saved = new CompoundTag();
      boolean kept = projectile.save(saved);
      projectile.discard();
      ServerLevel level = user.serverLevel();
      int hold = (int)GomuGomu.setting("bulletHoldTicks");
      long ejectAt = level.getGameTime() + (long)hold;
      Vec3 offset = at.subtract(user.position());
      if (kept) {
         HELD.add(new BulletCatch.Held(user.getUUID(), saved, offset, dir, velocity.length(), ejectAt));
      }

      GomuNetwork.absorbed(user, offset, dir, level.getGameTime(), hold);
      level.playSound(null, at.x, at.y, at.z, GomuSounds.BULLET_ABSORB.get(), SoundSource.PLAYERS, 0.9F, 0.9F + level.random.nextFloat() * 0.2F);
      return ejectAt;
   }

   private static Vec3 surface(ServerPlayer user, Vec3 from, Vec3 dir, @Nullable Vec3 hit) {
      AABB box = user.getBoundingBox();
      Optional<Vec3> clip = box.clip(from.subtract(dir.scale(2.0)), from.add(dir.scale(2.0)));
      if (clip.isPresent()) {
         return clip.get();
      } else {
         return hit != null && box.inflate(0.3).contains(hit)
            ? hit
            : new Vec3(
               Math.max(box.minX, Math.min(from.x, box.maxX)), Math.max(box.minY, Math.min(from.y, box.maxY)), Math.max(box.minZ, Math.min(from.z, box.maxZ))
            );
      }
   }

   public static void tick(MinecraftServer server) {
      if (!HELD.isEmpty()) {
         Iterator<BulletCatch.Held> it = HELD.iterator();

         while (it.hasNext()) {
            BulletCatch.Held held = it.next();
            ServerPlayer user = server.getPlayerList().getPlayer(held.user);
            if (user != null && user.isAlive()) {
               if (user.serverLevel().getGameTime() >= held.ejectAt) {
                  it.remove();
                  eject(user, held);
               }
            } else {
               it.remove();
            }
         }
      }
   }

   static void eject(ServerPlayer user, BulletCatch.Held held) {
      ServerLevel level = user.serverLevel();
      CompoundTag tag = held.entity.copy();
      double factor = GomuGomu.setting("returnedBulletDamageMultiplier");
      if (tag.contains("Damage", 99)) {
         tag.putFloat("Damage", (float)((double)tag.getFloat("Damage") * factor));
      }

      if (tag.contains("damage", 99)) {
         tag.putDouble("damage", tag.getDouble("damage") * factor);
      }

      Entity copy = (Entity)EntityType.create(tag, level).orElse(null);
      if (copy != null) {
         Vec3 out = held.dir.reverse();
         Vec3 at = user.position().add(held.offset).add(out.scale(0.35));
         copy.setUUID(UUID.randomUUID());
         copy.moveTo(at.x, at.y, at.z, copy.getYRot(), copy.getXRot());
         double speed = Math.max(0.2, held.speed * GomuGomu.setting("returnedBulletSpeed"));
         if (copy instanceof Projectile projectile) {
            projectile.setOwner(user);
            projectile.shoot(out.x, out.y, out.z, (float)speed, 0.0F);
         } else {
            copy.setDeltaMovement(out.scale(speed));
         }

         copy.getPersistentData().putBoolean("devilfruits_gomu_returned", true);
         level.addFreshEntity(copy);
         GomuTriggers.BULLET_RETURNED.get().trigger(user);
         level.playSound(null, at.x, at.y, at.z, GomuSounds.BULLET_RETURN.get(), SoundSource.PLAYERS, 1.0F, 0.95F + level.random.nextFloat() * 0.15F);
      }
   }

   public static void release(ServerPlayer player) {
      HELD.removeIf(h -> h.user.equals(player.getUUID()));
   }

   public static int holding(ServerPlayer player) {
      return (int)HELD.stream().filter(h -> h.user.equals(player.getUUID())).count();
   }

   public static void ejectNow(ServerPlayer player) {
      Iterator<BulletCatch.Held> it = HELD.iterator();
      List<BulletCatch.Held> due = new ArrayList<>();

      while (it.hasNext()) {
         BulletCatch.Held held = it.next();
         if (held.user.equals(player.getUUID())) {
            it.remove();
            due.add(held);
         }
      }

      due.forEach(h -> eject(player, h));
   }

   public static void clear() {
      HELD.clear();
   }

   private BulletCatch() {
   }

   static record Held(UUID user, CompoundTag entity, Vec3 offset, Vec3 dir, double speed, long ejectAt) {
   }
}
