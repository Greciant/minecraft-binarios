package com.devilfruits.fruits.doru_doru.ability;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.fruits.doru_doru.DoruAbilities;
import com.devilfruits.fruits.doru_doru.DoruDoru;
import com.devilfruits.fruits.doru_doru.DoruFx;
import com.devilfruits.fruits.doru_doru.DoruSounds;
import com.devilfruits.fruits.doru_doru.atelier.BuildEngine;
import com.devilfruits.fruits.doru_doru.atelier.SculptRequest;
import com.devilfruits.fruits.doru_doru.atelier.TechniqueGen;
import com.devilfruits.fruits.doru_doru.atelier.VoxelSet;
import com.devilfruits.fruits.doru_doru.coating.WaxCoating;
import com.devilfruits.fruits.doru_doru.wax.WaxBlockEntity;
import com.devilfruits.fruits.doru_doru.wax.WaxHeatService;
import com.devilfruits.fruits.doru_doru.wax.WaxMask;
import com.devilfruits.fruits.doru_doru.wax.WaxOwners;
import com.devilfruits.fruits.doru_doru.wax.WaxWorld;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.Long2LongMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class CandleServiceAbility extends Ability {
   private static final Map<UUID, CandleServiceAbility.Service> SERVICES = new HashMap<>();

   public CandleServiceAbility() {
      super(
         DoruDoru.id("candle_service"),
         CastType.INSTANT,
         AbilityStats.builder()
            .energy(40)
            .cooldown(120)
            .mastery(8)
            .damage(0.0)
            .range(10.0)
            .extra("coatRadius", 16.0, 2.0, 48.0, "Radius in blocks the candles' wax flakes reach.")
            .extra("candleSeconds", 30.0, 5.0, 600.0, "Seconds the candles burn before the service melts away.")
            .extra("candles", 8.0, 3.0, 16.0, "Candles in the ring on the stand's plate (plus the one in the middle).")
            .extra("standIntegrity", 0.5, 0.05, 1.0, "Share of the stand that must stay; below it the service collapses.")
            .extra("riseTicks", 30.0, 1.0, 200.0, "Ticks the stand takes to rise.")
            .build()
      );
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.builder(this.defaults()).manual().build();
   }

   @Override
   public Component checkRequirements(ServerPlayer player, PlayerFruitData data) {
      return DoruDoru.heatBlock(player);
   }

   @Override
   public void activate(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      ServerLevel level = ctx.level();
      Vec3 base = ground(level, player, ctx.look(), (double)ctx.range());
      if (base == null) {
         player.displayClientMessage(Component.translatable("message.devilfruits.doru_doru.no_ground"), true);
      } else {
         TechniqueGen.Service s = TechniqueGen.service(base, (int)ctx.extra("candles"), ctx.seed());
         VoxelSet all = s.all();
         int rise = (int)ctx.extra("riseTicks");
         long now = level.getGameTime();
         long burn = (long)(ctx.extra("candleSeconds") * 20.0);
         SculptRequest r = new SculptRequest(
            SculptRequest.Tool.SHAPE, 0, List.of(base), new float[4], 0.0F, SculptRequest.Symmetry.NONE, 2, Vec3.ZERO, DoruAbilities.color(player), 0.0F
         );
         BuildEngine.Spec spec = BuildEngine.Spec.technique(this.id().getPath(), 3, rise)
            .lifetime((long)rise + burn + 400L)
            .integrity(false)
            .hands(player.position());
         CandleServiceAbility.Service old = SERVICES.remove(player.getUUID());
         if (old != null) {
            end(player.getServer().getLevel(old.level), old);
         }

         if (BuildEngine.submit(player, r, all, spec)) {
            int ownerIndex = WaxOwners.get(level.getServer()).index(player.getUUID());
            Vec3 plate = base.add(0.0, 3.0, 0.0);
            SERVICES.put(
               player.getUUID(),
               new CandleServiceAbility.Service(
                  player.getUUID(),
                  ownerIndex,
                  level.dimension(),
                  plate,
                  s,
                  all,
                  now + (long)rise,
                  now + (long)rise + burn,
                  ctx.extra("coatRadius"),
                  ctx.extra("standIntegrity"),
                  ctx.seed()
               )
            );
            level.playSound(null, base.x, base.y, base.z, DoruSounds.CANDLE_RISE.get(), SoundSource.PLAYERS, 1.4F, 0.9F);
            ServerFx.animate(player, ctx.animation(DoruDoru.id("service_raise")));
         }
      }
   }

   @Nullable
   static Vec3 ground(ServerLevel level, ServerPlayer player, Vec3 look, double range) {
      Vec3 eye = player.getEyePosition();
      BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(look.scale(range)), Block.COLLIDER, Fluid.NONE, player));
      if (hit.getType() != Type.BLOCK || hit.getDirection() != Direction.UP) {
         Vec3 far = hit.getType() == Type.BLOCK ? hit.getLocation().subtract(look.scale(0.5)) : eye.add(look.scale(Math.min(range, 7.0)));
         hit = level.clip(new ClipContext(far, far.add(0.0, -10.0, 0.0), Block.COLLIDER, Fluid.NONE, player));
         if (hit.getType() != Type.BLOCK) {
            return null;
         }
      }

      return new Vec3(hit.getLocation().x, (double)(hit.getBlockPos().getY() + 1), hit.getLocation().z);
   }

   public static void tick(ServerLevel level) {
      if (!SERVICES.isEmpty()) {
         long now = level.getGameTime();
         Iterator<CandleServiceAbility.Service> it = SERVICES.values().iterator();

         while (it.hasNext()) {
            CandleServiceAbility.Service s = it.next();
            if (s.level.equals(level.dimension())) {
               ServerPlayer owner = level.getServer().getPlayerList().getPlayer(s.owner);
               boolean gone = owner == null || owner.isDeadOrDying() || now >= s.end || WaxHeatService.tooHot(level);
               if (!gone && now >= s.lit && (now - s.lit) % 20L == 10L) {
                  gone = (double)integrity(level, s) < s.minIntegrity;
               }

               if (gone) {
                  it.remove();
                  end(level, s);
               } else if (now >= s.lit) {
                  long t = now - s.lit;
                  if (t % 5L == 0L) {
                     burn(level, s, now);
                     coat(level, s, owner);
                  }

                  if (t % 10L == 0L) {
                     pulse(level, s, now);
                  }

                  if (t % 40L == 0L) {
                     level.playSound(null, s.plate.x, s.plate.y + 1.5, s.plate.z, DoruSounds.FLAME.get(), SoundSource.PLAYERS, 0.9F, 0.9F);
                  }

                  if (t % 60L == 30L) {
                     level.playSound(null, s.plate.x, s.plate.y + 3.0, s.plate.z, DoruSounds.FLAKES.get(), SoundSource.PLAYERS, 0.7F, 1.0F);
                  }
               }
            }
         }
      }
   }

   private static void burn(ServerLevel level, CandleServiceAbility.Service s, long now) {
      float done = 1.0F - s.left(now);
      VoxelSet layer = new VoxelSet();

      for (int i = 0; i < s.candles.size(); i++) {
         TechniqueGen.Candle c = s.candles.get(i);

         for (int target = Math.min(c.height(), (int)((float)c.height() * done)); s.burnt[i] < target; s.burnt[i]++) {
            c.layer(c.top() - s.burnt[i], layer);
         }
      }

      ObjectIterator var11 = layer.blocks().long2LongEntrySet().iterator();

      while (var11.hasNext()) {
         Entry e = (Entry)var11.next();
         BlockPos pos = BlockPos.of(e.getLongKey());
         if (level.isLoaded(pos)) {
            BlockEntity var10 = level.getBlockEntity(pos);
            if (var10 instanceof WaxBlockEntity) {
               WaxBlockEntity be = (WaxBlockEntity)var10;
               if (be.owner() == s.ownerIndex && be.kind() == 3) {
                  WaxWorld.setMask(level, pos, be, be.mask() & ~e.getLongValue());
               }
            }
         }
      }
   }

   private static void coat(ServerLevel level, CandleServiceAbility.Service s, ServerPlayer owner) {
      double r = s.radius;
      AABB box = new AABB(s.plate.x - r, s.plate.y - 10.0, s.plate.z - r, s.plate.x + r, s.plate.y + 12.0, s.plate.z + r);

      for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, ex -> ex != owner && ex.isAlive() && !ex.isSpectator())) {
         double dx = e.getX() - s.plate.x;
         double dz = e.getZ() - s.plate.z;
         if (!(dx * dx + dz * dz > r * r) && level.canSeeSky(BlockPos.containing(e.getX(), e.getEyeY(), e.getZ()))) {
            WaxCoating.expose(e, 5, owner);
         }
      }
   }

   private static float integrity(ServerLevel level, CandleServiceAbility.Service s) {
      if (s.standCells == 0) {
         return 1.0F;
      } else {
         int cells = 0;

         for (int i = 0; i < s.stand.size(); i++) {
            BlockPos pos = BlockPos.of(s.stand.getLong(i));
            if (!level.isLoaded(pos)) {
               return 1.0F;
            }

            if (level.getBlockEntity(pos) instanceof WaxBlockEntity be && be.owner() == s.ownerIndex && be.kind() == 3) {
               cells += WaxMask.count(be.mask());
            }
         }

         return Math.min(1.0F, (float)cells / (float)s.standCells);
      }
   }

   private static void pulse(ServerLevel level, CandleServiceAbility.Service s, long now) {
      ServerFx.sendNear(
         level, DoruFx.SERVICE, -1, s.plate, new Vec3(2.35, (double)s.left(now), s.radius), (float)(s.end - now) / 20.0F, s.seed, s.candles.size() - 1, 128.0
      );
   }

   private static void end(@Nullable ServerLevel level, CandleServiceAbility.Service s) {
      if (level != null && !s.over) {
         s.over = true;
         long now = level.getGameTime();

         for (int i = 0; i < s.all.size(); i++) {
            BlockPos pos = BlockPos.of(s.all.getLong(i));
            if (level.isLoaded(pos)) {
               BlockEntity var7 = level.getBlockEntity(pos);
               if (var7 instanceof WaxBlockEntity) {
                  WaxBlockEntity be = (WaxBlockEntity)var7;
                  if (be.owner() == s.ownerIndex && be.kind() == 3) {
                     be.setExpires(now);
                     WaxHeatService.poke(level, pos);
                  }
               }
            }
         }

         ServerFx.sendNear(level, DoruFx.SERVICE_END, -1, s.plate, new Vec3(2.35, (double)s.left(now), s.radius), 0.0F, s.seed, s.candles.size() - 1, 128.0);
         level.playSound(null, s.plate.x, s.plate.y, s.plate.z, DoruSounds.MELT.get(), SoundSource.PLAYERS, 1.2F, 0.8F);
      }
   }

   public static void forget(ServerPlayer player) {
      CandleServiceAbility.Service s = SERVICES.remove(player.getUUID());
      if (s != null) {
         end(player.getServer().getLevel(s.level), s);
      }
   }

   public static float running(ServerPlayer player) {
      CandleServiceAbility.Service s = SERVICES.get(player.getUUID());
      return s == null ? -1.0F : integrity(player.serverLevel(), s);
   }

   public static void clear() {
      SERVICES.clear();
   }

   private static final class Service {
      final UUID owner;
      final int ownerIndex;
      final ResourceKey<Level> level;
      final Vec3 plate;
      final List<TechniqueGen.Candle> candles;
      final int[] burnt;
      final LongArrayList stand = new LongArrayList();
      final LongArrayList all = new LongArrayList();
      final int standCells;
      final long lit;
      final long end;
      final double radius;
      final double minIntegrity;
      final long seed;
      boolean over;

      Service(
         UUID owner,
         int ownerIndex,
         ResourceKey<Level> level,
         Vec3 plate,
         TechniqueGen.Service s,
         VoxelSet all,
         long lit,
         long end,
         double radius,
         double minIntegrity,
         long seed
      ) {
         this.owner = owner;
         this.ownerIndex = ownerIndex;
         this.level = level;
         this.plate = plate;
         this.candles = s.candles();
         this.burnt = new int[this.candles.size()];
         this.stand.addAll(s.stand().blocks().keySet());
         this.all.addAll(all.blocks().keySet());
         this.standCells = s.stand().size();
         this.lit = lit;
         this.end = end;
         this.radius = radius;
         this.minIntegrity = minIntegrity;
         this.seed = seed;
      }

      float left(long now) {
         return now <= this.lit ? 1.0F : Math.max(0.0F, 1.0F - (float)(now - this.lit) / (float)Math.max(1L, this.end - this.lit));
      }
   }
}
