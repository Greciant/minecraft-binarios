package com.devilfruits.core.physics;

import com.devilfruits.core.util.ServerScheduler;
import com.devilfruits.core.world.CastBudget;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class PhysicsCommand {
   private static final double REACH = 48.0;
   @Nullable
   private static PhysicsCommand.BlastTest blast;
   private static final Map<String, PhysicsCommand.StructureTest> TESTS = new LinkedHashMap<>();

   public static void registerBlast(PhysicsCommand.BlastTest test) {
      blast = test;
   }

   public static void registerTest(String name, PhysicsCommand.StructureTest test) {
      TESTS.put(name, test);
   }

   public static LiteralArgumentBuilder<CommandSourceStack> node() {
      LiteralArgumentBuilder<CommandSourceStack> test = Commands.literal("test");

      for (String name : TESTS.keySet()) {
         test.then(Commands.literal(name).executes(ctx -> run(ctx, "fruit:" + name)));
      }

      return (LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)Commands.literal("sable")
            .then(Commands.literal("info").executes(ctx -> run(ctx, "info"))))
         .then(
            ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)test.then(
                              Commands.literal("impulse").executes(ctx -> run(ctx, "impulse"))
                           ))
                           .then(Commands.literal("damage").executes(ctx -> run(ctx, "damage"))))
                        .then(
                           ((LiteralArgumentBuilder)Commands.literal("blast").executes(ctx -> run(ctx, "blast")))
                              .then(Commands.argument("power", DoubleArgumentType.doubleArg(0.5, 16.0)).executes(ctx -> run(ctx, "blast")))
                        ))
                     .then(
                        ((LiteralArgumentBuilder)Commands.literal("jump").executes(ctx -> run(ctx, "jump")))
                           .then(Commands.argument("kg", DoubleArgumentType.doubleArg(0.1, 100000.0)).executes(ctx -> run(ctx, "jump")))
                     ))
                  .then(
                     Commands.literal("load")
                        .then(
                           ((RequiredArgumentBuilder)Commands.argument("kg", DoubleArgumentType.doubleArg(0.1, 100000.0)).executes(ctx -> run(ctx, "load")))
                              .then(Commands.argument("seconds", DoubleArgumentType.doubleArg(0.05, 60.0)).executes(ctx -> run(ctx, "load")))
                        )
                  ))
               .then(
                  Commands.literal("slam")
                     .then(
                        Commands.argument("kg", DoubleArgumentType.doubleArg(0.1, 100000.0))
                           .then(Commands.argument("speed", DoubleArgumentType.doubleArg(0.0, 10.0)).executes(ctx -> run(ctx, "slam")))
                     )
               )
         );
   }

   private static double arg(CommandContext<CommandSourceStack> ctx, String name, double fallback) {
      try {
         return DoubleArgumentType.getDouble(ctx, name);
      } catch (IllegalArgumentException var5) {
         return fallback;
      }
   }

   private static int run(CommandContext<CommandSourceStack> ctx, String what) throws CommandSyntaxException {
      CommandSourceStack source = (CommandSourceStack)ctx.getSource();
      ServerPlayer player = source.getPlayerOrException();
      if (!PhysicsWorld.active()) {
         source.sendFailure(Component.translatable("commands.devilfruits.sable.inactive"));
         return 0;
      } else {
         StructureBlock nearest = nearest(player);
         if (nearest == null) {
            source.sendFailure(Component.translatable("commands.devilfruits.sable.none", new Object[]{48}));
            return 0;
         } else {
            PhysicsStructure s = nearest.structure();
            String name = s.id().toString().substring(0, 8);
            switch (what) {
               case "impulse": {
                  double j = Math.max(1.0, s.mass()) * 4.0;
                  Vec3 dir = player.getLookAngle().multiply(1.0, 0.3, 1.0).normalize();
                  double got = StructureForces.apply(s, nearest.worldCenter(), dir.scale(j));
                  StructureForces.impactFx(player.serverLevel(), s, nearest.localPos(), nearest.worldCenter(), dir, got);
                  source.sendSuccess(
                     () -> Component.translatable(
                           "commands.devilfruits.sable.impulse", new Object[]{name, String.format("%.0f", got), String.format("%.1f", s.mass())}
                        ),
                     true
                  );
                  break;
               }
               case "damage":
                  CastBudget budget = new CastBudget(12, 6);
                  int queued = 0;

                  for (StructureBlock bx : PhysicsWorld.blocksIn(player.level(), ImpactShape.sphere(nearest.worldCenter(), 1.8), x -> x.id().equals(s.id()), 12)) {
                     if (BlockImpactService.queueBreak(
                        player.serverLevel(), player, s, bx.localPos(), player.getLookAngle().scale(0.3).add(0.0, 0.25, 0.0), true, budget
                     )) {
                        queued++;
                     }
                  }

                  BlockImpactService.decal(
                     player.serverLevel(), s, nearest.localPos(), s.dirToLocal(new Vec3(0.0, 1.0, 0.0)), BlockImpactService.Decal.CRACK, 2.0F
                  );
                  int n = queued;
                  source.sendSuccess(() -> Component.translatable("commands.devilfruits.sable.damage", new Object[]{n, name}), true);
                  break;
               case "jump": {
                  PhysicsStructure under = PhysicsWorld.structureUnder(player);
                  Vec3 at = under != null ? player.position() : nearest.worldCenter();
                  PhysicsStructure target = under != null ? under : s;
                  Vec3 dv = player.getLookAngle().multiply(0.35, 0.0, 0.35).add(0.0, 1.4, 0.0);
                  double kg = arg(ctx, "kg", Double.NaN);
                  double mass = Double.isNaN(kg) ? StructureForces.bodyMass(player) : StructureForces.massUnits(kg) * (Double)PhysicsConfig.MASS_SCALE.get();
                  Vec3 j = dv.scale(-20.0 * mass * (Double)PhysicsConfig.REACTION_MULTIPLIER.get());
                  double got = StructureForces.apply(target, at, j);
                  StructureForces.impactFx(player.serverLevel(), target, BlockPos.containing(target.toLocal(at.add(0.0, -0.3, 0.0))), at, j, got);
                  String where = String.format("%.1f %.1f %.1f", at.x, at.y, at.z);
                  source.sendSuccess(
                     () -> Component.translatable("commands.devilfruits.sable.jump", new Object[]{target.id().toString().substring(0, 8), where}), true
                  );
                  break;
               }
               case "load": {
                  PhysicsStructure under = PhysicsWorld.structureUnder(player);
                  PhysicsStructure target = under != null ? under : s;
                  Vec3 at = under != null ? player.position() : nearest.worldCenter().add(0.0, 0.5, 0.0);
                  double kg = arg(ctx, "kg", 70.0);
                  int ticks = (int)Math.max(1L, Math.round(arg(ctx, "seconds", 3.0) * 20.0));
                  double mass = StructureForces.massUnits(kg) * (Double)PhysicsConfig.MASS_SCALE.get();
                  double[] total = new double[]{0.0};
                  ServerScheduler.schedule(player.serverLevel(), 0, age -> {
                     if (!target.isValid()) {
                        return true;
                     } else {
                        Vec3 point = under != null ? player.position() : at;
                        total[0] += StructureForces.load(target, point, mass);
                        return age + 1 >= ticks;
                     }
                  });
                  String where = String.format("%.1f %.1f %.1f", at.x, at.y, at.z);
                  source.sendSuccess(
                     () -> Component.translatable(
                           "commands.devilfruits.sable.load",
                           new Object[]{
                              target.id().toString().substring(0, 8),
                              String.format("%.0f", kg),
                              String.format("%.1f", mass),
                              (double)ticks / 20.0,
                              where,
                              String.format("%.1f", target.mass())
                           }
                        ),
                     true
                  );
                  break;
               }
               case "slam": {
                  double kg = arg(ctx, "kg", 70.0);
                  double speed = arg(ctx, "speed", 1.0);
                  Vec3 dir = nearest.worldCenter().subtract(player.getEyePosition());
                  dir = dir.lengthSqr() < 1.0E-6 ? new Vec3(0.0, -1.0, 0.0) : dir.normalize();
                  double energy = 0.5 * kg * speed * 20.0 * speed * 20.0;
                  double mass = StructureForces.massUnits(kg) * (Double)PhysicsConfig.MASS_SCALE.get();
                  Vec3 contact = nearest.worldCenter().subtract(dir.scale(0.5));
                  double got = StructureForces.apply(s, contact, dir.scale(mass * speed * 20.0 * (Double)PhysicsConfig.REACTION_MULTIPLIER.get()));
                  StructureForces.impactFx(player.serverLevel(), s, nearest.localPos(), contact, dir, got);
                  int blocks = (int)Math.min(12.0, energy / 120000.0 / 2.5);
                  CastBudget budget = new CastBudget(blocks, Math.min(blocks, 6));
                  int queued = 0;

                  for (StructureBlock b : PhysicsWorld.blocksIn(
                     player.level(), ImpactShape.sphere(nearest.worldCenter(), 0.6 + (double)blocks * 0.25), x -> x.id().equals(s.id()), blocks
                  )) {
                     if (BlockImpactService.queueBreak(player.serverLevel(), player, s, b.localPos(), dir.scale(0.4), true, budget)) {
                        queued++;
                     }
                  }

                  int n = queued;
                  source.sendSuccess(
                     () -> Component.translatable(
                           "commands.devilfruits.sable.slam",
                           new Object[]{
                              name,
                              String.format("%.0f", kg),
                              String.format("%.2f", speed),
                              String.format("%.0f", energy / 1000.0),
                              String.format("%.0f", got),
                              n
                           }
                        ),
                     true
                  );
                  break;
               }
               case "blast":
                  if (blast == null) {
                     source.sendFailure(Component.translatable("commands.devilfruits.sable.no_blast"));
                     return 0;
                  }

                  double power = arg(ctx, "power", 4.0);
                  Vec3 toPlayer = player.getEyePosition().subtract(nearest.worldCenter());
                  Vec3 at = nearest.worldCenter().add(toPlayer.lengthSqr() > 1.0E-6 ? toPlayer.normalize().scale(0.9) : new Vec3(0.0, 0.9, 0.0));
                  int touched = blast.detonate(player, at, power);
                  source.sendSuccess(
                     () -> Component.translatable("commands.devilfruits.sable.blast", new Object[]{name, String.format("%.1f", power), touched}), true
                  );
                  break;
               default:
                  PhysicsCommand.StructureTest fruitTest = what.startsWith("fruit:") ? TESTS.get(what.substring(6)) : null;
                  if (fruitTest != null) {
                     Component report = fruitTest.run(player, s, nearest);
                     source.sendSuccess(() -> report, true);
                     return 1;
                  }

                  Vec3 v = s.velocityAt(nearest.worldCenter());
                  double dist = player.position().distanceTo(nearest.worldCenter());
                  source.sendSuccess(
                     () -> Component.translatable(
                           "commands.devilfruits.sable.info",
                           new Object[]{name, String.format("%.1f", s.mass()), String.format("%.2f", v.length()), String.format("%.1f", dist)}
                        ),
                     false
                  );
            }

            return 1;
         }
      }
   }

   @Nullable
   private static StructureBlock nearest(ServerPlayer player) {
      Vec3 eye = player.getEyePosition();
      StructureBlock best = null;
      double bestD = Double.MAX_VALUE;

      for (PhysicsStructure s : PhysicsWorld.bridge().structuresIn(player.level(), new AABB(eye, eye).inflate(48.0))) {
         for (StructureBlock b : PhysicsWorld.blocksIn(player.level(), ImpactShape.sphere(eye, 48.0), x -> x.id().equals(s.id()), 4096)) {
            double d = b.worldCenter().distanceToSqr(eye);
            if (d < bestD) {
               bestD = d;
               best = b;
            }
         }
      }

      return best;
   }

   private PhysicsCommand() {
   }

   public interface BlastTest {
      int detonate(ServerPlayer var1, Vec3 var2, double var3);
   }

   public interface StructureTest {
      Component run(ServerPlayer var1, PhysicsStructure var2, StructureBlock var3);
   }
}
