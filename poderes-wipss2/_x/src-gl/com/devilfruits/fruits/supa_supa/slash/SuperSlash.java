package com.devilfruits.fruits.supa_supa.slash;

import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.combat.AbilityProjectiles;
import com.devilfruits.core.config.CoreConfig;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.util.ServerScheduler;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.core.world.CastBudget;
import com.devilfruits.core.world.TerrainDestruction;
import com.devilfruits.fruits.supa_supa.SupaFx;
import com.devilfruits.fruits.supa_supa.SupaSounds;
import com.devilfruits.registry.DFTags;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SuperSlash extends AbilityProjectiles.Flight {
   public static final double STEP = 0.5;
   public static final double LEVEL_DEGREES = 6.0;
   private final ServerPlayer owner;
   private final AbilityContext ctx;
   private final ServerLevel level;
   private Vec3 dir;
   private Vec3 up;
   private final double speed;
   private final int height;
   private final int lifetime;
   private final float damage;
   private final long seed;
   private final IntSet struck = new IntOpenHashSet();
   private final LongSet cut = new LongOpenHashSet();
   private final CastBudget budget;
   private Vec3 base;
   private final SlashStructures structures;

   private SuperSlash(AbilityContext ctx, Vec3 base, Vec3 dir, double speed, int height, double range, float damage) {
      super(ctx.player(), base.add(bladeUp(dir, ctx.horizontalLook()).scale((double)height * 0.5)), (double)height * 0.5);
      this.owner = ctx.player();
      this.ctx = ctx;
      this.level = ctx.level();
      this.base = base;
      this.dir = dir;
      this.up = bladeUp(dir, ctx.horizontalLook());
      this.speed = speed;
      this.height = height;
      this.lifetime = Mth.ceil(range / speed);
      this.damage = damage;
      this.seed = ctx.seed();
      this.budget = new CastBudget((int)ctx.extra("maxBlocks"), (Integer)CoreConfig.MAX_DEBRIS_ENTITIES_PER_CAST.get());
      this.structures = new SlashStructures(this);
   }

   public static SuperSlash launch(AbilityContext ctx, float charge) {
      ServerPlayer player = ctx.player();
      Vec3 flat = ctx.horizontalLook();
      Vec3 dir = aim(ctx.look(), flat);
      double half = (double)((int)ctx.extra("height")) * 0.5;
      Vec3 middle = new Vec3(player.getX(), Math.floor(player.getY() + 0.01) + half, player.getZ()).add(flat.scale(1.2));
      return launch(ctx, charge, middle.subtract(bladeUp(dir, flat).scale(half)), dir);
   }

   public static Vec3 aim(Vec3 look, Vec3 flat) {
      Vec3 l = look.normalize();
      return Math.abs(l.y) < Math.sin(Math.toRadians(6.0)) ? flat : l;
   }

   public static Vec3 bladeUp(Vec3 dir, Vec3 flat) {
      Vec3 u = new Vec3(0.0, 1.0, 0.0).subtract(dir.scale(dir.y));
      return u.lengthSqr() < 1.0E-6 ? flat.scale(dir.y > 0.0 ? -1.0 : 1.0) : u.normalize();
   }

   public static SuperSlash launch(AbilityContext ctx, float charge, Vec3 base, Vec3 dir) {
      ServerPlayer player = ctx.player();
      int height = (int)ctx.extra("height");
      double speed = ctx.extra("speed");
      float power = 0.45F + 0.55F * charge;
      double range = (double)(ctx.range() * power);
      SuperSlash slash = AbilityProjectiles.track(ctx.level(), new SuperSlash(ctx, base, dir, speed, height, range, ctx.damage() * power));
      ServerFx.sendNear(ctx.level(), SupaFx.SLASH, player.getId(), base, dir.scale(speed), (float)height, slash.seed, slash.lifetime, 160.0);
      ctx.level().playSound(null, player.getX(), player.getY() + 1.0, player.getZ(), SupaSounds.SLASH_RELEASE.get(), SoundSource.PLAYERS, 2.0F, 1.0F);
      ServerScheduler.schedule(ctx.level(), 1, slash::tick);
      return slash;
   }

   public ServerPlayer owner() {
      return this.owner;
   }

   public AbilityContext context() {
      return this.ctx;
   }

   public ServerLevel level() {
      return this.level;
   }

   public Vec3 base() {
      return this.base;
   }

   public Vec3 dir() {
      return this.dir;
   }

   public Vec3 up() {
      return this.up;
   }

   public int height() {
      return this.height;
   }

   public long seed() {
      return this.seed;
   }

   public CastBudget budget() {
      return this.budget;
   }

   public double speed() {
      return this.speed;
   }

   void place(Vec3 at) {
      this.base = at;
      this.moveTo(at.add(this.up.scale((double)this.height * 0.5)));
   }

   void redirect(Vec3 at, Vec3 newDir) {
      this.dir = newDir;
      this.up = bladeUp(newDir, this.ctx.horizontalLook());
      this.place(at);
   }

   boolean tick(int age) {
      if (!this.owner.isRemoved() && this.owner.level() == this.level && !this.broken()) {
         if (this.structures.gliding()) {
            this.structures.glideTick();
            return false;
         } else {
            double v = this.structures.speed(this.speed);
            int steps = Math.max(1, Mth.ceil(v / 0.5 - 1.0E-4));
            double step = v / (double)steps;

            for (int i = 0; i < steps; i++) {
               Vec3 from = this.base;
               Vec3 to = this.base.add(this.dir.scale(step));
               if (this.structures.enter(to)) {
                  this.place(to);
                  return false;
               }

               if (!this.cutBlade(to)) {
                  this.base = to;
                  this.finish(true);
                  return true;
               }

               this.hit(from, to);
               this.base = to;
               if (this.structures.exiting() && this.structures.exitStep(step)) {
                  this.finish(true);
                  return true;
               }
            }

            if (this.structures.exiting()) {
               this.structures.exitTicked();
            }

            this.moveTo(this.base.add(this.up.scale((double)this.height * 0.5)));
            if (age >= this.lifetime && !this.structures.exiting()) {
               this.finish(true);
               return true;
            } else {
               return false;
            }
         }
      } else {
         this.finish(false);
         return true;
      }
   }

   private boolean cutBlade(Vec3 p) {
      if (!this.ctx.blockDamageAllowed()) {
         return true;
      } else {
         int samples = Math.max(1, Mth.ceil((double)this.height / 0.35));
         MutableBlockPos pos = new MutableBlockPos();

         for (int k = 0; k < samples; k++) {
            double along = ((double)k + 0.5) * (double)this.height / (double)samples;
            int dy = (int)along;
            Vec3 q = p.add(this.up.scale(along));
            pos.set(Mth.floor(q.x), Mth.floor(q.y), Mth.floor(q.z));
            if (this.cut.add(pos.asLong())) {
               BlockState state = this.level.getBlockState(pos);
               if (!state.isAir()) {
                  if (state.is(DFTags.RESISTS_TECHNIQUES)) {
                     return false;
                  }

                  if (state.getCollisionShape(this.level, pos).isEmpty() && state.getFluidState().isEmpty()) {
                     TerrainDestruction.tearOut(this.level, this.owner, pos.immutable(), null, this.budget, 6 + dy, true, false);
                  } else {
                     Vec3 side = this.dir.cross(this.up).scale(this.level.random.nextBoolean() ? 0.1 : -0.1);
                     TerrainDestruction.tearOut(this.level, this.owner, pos.immutable(), side.add(0.0, 0.12, 0.0), this.budget, 8 + dy * 2, true, false);
                  }
               }
            }
         }

         return true;
      }
   }

   private void hit(Vec3 from, Vec3 to) {
      Vec3 tip = this.up.scale((double)this.height);
      AABB box = new AABB(from, to).minmax(new AABB(from.add(tip), to.add(tip)));
      this.hit(this.up.y > 0.999 ? box.inflate(0.55, 0.05, 0.55) : box.inflate(0.55), to, this.dir);
   }

   void hitBlade(Vec3 bottom, Vec3 top, Vec3 travel) {
      this.hit(new AABB(bottom, top).inflate(0.55), bottom, travel);
   }

   private void hit(AABB box, Vec3 to, Vec3 dir) {
      Vec3 side = new Vec3(-dir.z, 0.0, dir.x);
      side = side.lengthSqr() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : side.normalize();

      for (Entity e : Targeting.inBox(this.level, this.owner, box, x -> Targeting.canHurt(this.owner, x))) {
         if (this.struck.add(e.getId()) && this.ctx.hurt(e, this.damage)) {
            double s = e.position().subtract(to).dot(side) >= 0.0 ? 1.0 : -1.0;
            Targeting.launch(e, side.scale(s * (double)this.ctx.knockback()).add(dir.scale(0.3)).add(0.0, 0.3, 0.0));
            ServerFx.sendNear(this.level, SupaFx.CUT_HIT, e.getId(), e.getBoundingBox().getCenter(), dir, this.damage, (long)e.getId(), 0, 96.0);
         }
      }

      for (Projectile p : this.level.getEntitiesOfClass(Projectile.class, box, x -> x.isAlive() && x.getOwner() != this.owner)) {
         ServerFx.sendNear(this.level, SupaFx.CUT_HIT, -1, p.position(), dir, 2.0F, (long)p.getId(), 1, 64.0);
         p.discard();
      }

      AbilityProjectiles.shatter(this.level, this.owner, v -> box.contains(v), dir);
   }

   private void finish(boolean burst) {
      this.end();
      this.structures.flown();
      if (burst) {
         Vec3 mid = this.base.add(this.up.scale((double)this.height * 0.5));
         ServerFx.sendNear(this.level, SupaFx.SLASH_END, this.owner.getId(), this.base, this.dir, (float)this.height, this.seed, 0, 160.0);
         this.level.playSound(null, mid.x, mid.y, mid.z, SupaSounds.SLASH_BURST.get(), SoundSource.PLAYERS, 1.6F, 1.0F);
      }
   }

   @Override
   protected void shatter(ServerLevel level, Entity by, Vec3 push) {
      ServerFx.sendNear(level, SupaFx.SLASH_END, this.owner.getId(), this.base, this.dir, (float)this.height, this.seed, 1, 160.0);
   }
}
