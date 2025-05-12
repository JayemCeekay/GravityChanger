package gravity_changer.command;

import gravity_changer.GravityComponent;
import gravity_changer.api.GravityChangerAPI;
import gravity_changer.util.GCUtil;
import gravity_changer.util.RotationUtil;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import java.util.Collection;
import java.util.List;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Validate;

public class GravityCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands
            .literal("gravity")
            .requires(source -> source.hasPermission(2));

        builder.then(Commands.literal("set_base_direction")
            .then(Commands.argument("direction", DirectionArgumentType.instance)
                .executes(context -> {
                    Entity entity = context.getSource().getEntity();
                    Validate.isTrue(entity != null);
                    Direction direction = DirectionArgumentType.getDirection(context, "direction");
                    GravityChangerAPI.setBaseGravityDirection(entity, direction);
                    return 1;
                })
                .then(Commands.argument("entities", EntityArgument.entities())
                    .executes(context -> {
                        Collection<? extends Entity> entities = EntityArgument.getEntities(context, "entities");
                        Direction direction = DirectionArgumentType.getDirection(context, "direction");
                        for (Entity entity : entities) {
                            GravityChangerAPI.setBaseGravityDirection(entity, direction);
                        }
                        return entities.size();
                    })
                )
            )
        );

        // Add a new command for setting arbitrary gravity direction using Vec3
        builder.then(Commands.literal("set_base_direction_vec")
            .then(Commands.argument("direction", Vec3ArgumentType.instance)
                .executes(context -> {
                    Entity entity = context.getSource().getEntity();
                    Validate.isTrue(entity != null);
                    Vec3 direction = Vec3ArgumentType.getVec3(context, "direction");
                    GravityChangerAPI.setBaseGravityDirectionVec(entity, direction);

                    // Send feedback
                    context.getSource().sendSuccess(
                        () -> Component.translatable(
                            "gravity_changer.command.set_vec",
                            String.format("%.2f %.2f %.2f", direction.x, direction.y, direction.z)
                        ), true
                    );

                    return 1;
                })
                .then(Commands.argument("entities", EntityArgument.entities())
                    .executes(context -> {
                        Collection<? extends Entity> entities = EntityArgument.getEntities(context, "entities");
                        Vec3 direction = Vec3ArgumentType.getVec3(context, "direction");

                        for (Entity entity : entities) {
                            GravityChangerAPI.setBaseGravityDirectionVec(entity, direction);
                        }

                        // Send feedback
                        context.getSource().sendSuccess(
                            () -> Component.translatable(
                                "gravity_changer.command.set_vec_multiple",
                                String.format("%.2f %.2f %.2f", direction.x, direction.y, direction.z),
                                entities.size()
                            ), true
                        );

                        return entities.size();
                    })
                )
            )
        );

        builder.then(Commands.literal("reset")
            .executes(context -> {
                Entity entity = context.getSource().getEntity();
                Validate.isTrue(entity != null);
                GravityChangerAPI.resetGravity(entity);
                return 1;
            })
            .then(Commands.argument("entities", EntityArgument.entities())
                .executes(context -> {
                    Collection<? extends Entity> entities = EntityArgument.getEntities(context, "entities");
                    for (Entity entity : entities) {
                        GravityChangerAPI.resetGravity(entity);
                    }
                    return entities.size();
                })
            )
        );

        builder.then(Commands.literal("set_base_strength")
            .then(Commands.argument("strength", DoubleArgumentType.doubleArg(-20, 20))
                .executes(context -> {
                    Entity entity = context.getSource().getEntity();
                    Validate.isTrue(entity != null);
                    double strength = DoubleArgumentType.getDouble(context, "strength");
                    return executeSetBaseStrength(List.of(entity), strength);
                })
                .then(Commands.argument("entities", EntityArgument.entities())
                    .executes(context -> {
                        Collection<? extends Entity> entities = EntityArgument.getEntities(context, "entities");
                        double strength = DoubleArgumentType.getDouble(context, "strength");
                        return executeSetBaseStrength(entities, strength);
                    })
                )
            )
        );

        builder.then(Commands.literal("view")
            .executes(context -> {
                Entity entity = context.getSource().getEntity();

                GravityComponent component = GravityChangerAPI.getGravityComponent(entity);
                Vec3 gravityVec = component.getBaseGravityDirectionVec();

                // Also show the Vec3-based gravity direction
                context.getSource().sendSuccess(
                    () -> Component.translatable(
                        "gravity_changer.command.inform_vec",
                        String.format("%.2f %.2f %.2f", gravityVec.x, gravityVec.y, gravityVec.z),
                        component.getBaseGravityStrength()
                    ), false
                );

                return 0;
            })
        );

        builder.then(Commands.literal("randomize_base_direction")
            .executes(context -> {
                CommandSourceStack source = context.getSource();
                Entity entity = source.getEntity();
                Validate.isTrue(entity != null);
                return executeRandomizeBaseDirection(source, List.of(entity));
            })
            .then(Commands.argument("entities", EntityArgument.entities())
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    Collection<? extends Entity> entities = EntityArgument.getEntities(context, "entities");
                    return executeRandomizeBaseDirection(source, entities);
                })
            )
        );

        builder.then(Commands.literal("set_dimension_gravity_strength")
            .then(Commands.argument("strength", DoubleArgumentType.doubleArg(-20, 20))
                .executes(context -> {
                    ServerLevel world = context.getSource().getLevel();
                    double strength = DoubleArgumentType.getDouble(context, "strength");
                    GravityChangerAPI.setDimensionGravityStrength(world, strength);
                    return 0;
                })
            )
        );

        builder.then(Commands.literal("view_dimension_info")
            .executes(context -> {
                ServerLevel world = context.getSource().getLevel();
                double strength = GravityChangerAPI.getDimensionGravityStrength(world);
                context.getSource().sendSuccess(
                    () -> Component.translatable("gravity_changer.command.dimension_info", strength), false
                );
                return 0;
            })
        );

        dispatcher.register(builder);
    }

    private static int executeSetBaseStrength(Collection<? extends Entity> entities, double strength) {
        for (Entity entity : entities) {
            GravityChangerAPI.setBaseGravityStrength(entity, strength);
        }
        return entities.size();
    }

    private static int executeRandomizeBaseDirection(CommandSourceStack source, Collection<? extends Entity> entities) {
        RandomSource random = source.getLevel().random;
        for (Entity entity : entities) {
            Direction gravityDirection = Direction.getRandom(random);
            GravityChangerAPI.setBaseGravityDirection(entity, gravityDirection);
        }
        return entities.size();
    }

    private static void getSendFeedback(CommandSourceStack source, Entity entity, Direction gravityDirection) {
        Component text = GCUtil.getDirectionText(gravityDirection);
        if (source.getEntity() != null && source.getEntity() == entity) {
            source.sendSuccess(() -> Component.translatable("commands.gravity.get.self", text), true);
        }
        else {
            source.sendSuccess(() -> Component.translatable("commands.gravity.get.other", entity.getDisplayName(), text), true);
        }
    }

}
