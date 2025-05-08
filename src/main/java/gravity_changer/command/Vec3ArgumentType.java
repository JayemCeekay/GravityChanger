package gravity_changer.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * Argument type for parsing Vec3 values for arbitrary gravity directions.
 */
public class Vec3ArgumentType implements ArgumentType<Vec3> {
    
    public static final Vec3ArgumentType instance = new Vec3ArgumentType();
    
    public static final DynamicCommandExceptionType INVALID_VEC3_EXCEPTION =
        new DynamicCommandExceptionType(object ->
            Component.literal("Invalid Vec3 format: " + object + ". Use 'x y z' format.")
        );
    
    public static final DynamicCommandExceptionType ZERO_VEC3_EXCEPTION =
        new DynamicCommandExceptionType(object ->
            Component.literal("Cannot use zero vector (0 0 0) as gravity direction.")
        );
    
    public static Vec3 getVec3(CommandContext<?> context, String name) {
        return context.getArgument(name, Vec3.class);
    }
    
    @Override
    public Vec3 parse(StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();
        double x = reader.readDouble();
        if (!reader.canRead() || reader.peek() != ' ') {
            reader.setCursor(start);
            throw INVALID_VEC3_EXCEPTION.createWithContext(reader, "Missing y and z components");
        }
        
        reader.skip(); // Skip space
        double y = reader.readDouble();
        if (!reader.canRead() || reader.peek() != ' ') {
            reader.setCursor(start);
            throw INVALID_VEC3_EXCEPTION.createWithContext(reader, "Missing z component");
        }
        
        reader.skip(); // Skip space
        double z = reader.readDouble();
        
        // Check if the vector is zero
        if (x == 0 && y == 0 && z == 0) {
            reader.setCursor(start);
            throw ZERO_VEC3_EXCEPTION.createWithContext(reader, "0 0 0");
        }
        
        // Normalize the vector
        double length = Math.sqrt(x * x + y * y + z * z);
        return new Vec3(x / length, y / length, z / length);
    }
    
    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        // Suggest some common directions
        return SharedSuggestionProvider.suggest(
            Arrays.asList(
                "0 -1 0", // DOWN
                "0 1 0",  // UP
                "0 0 1",  // NORTH
                "0 0 -1", // SOUTH
                "1 0 0",  // EAST
                "-1 0 0", // WEST
                "1 1 0",  // Diagonal example
                "0 0.5 0.5" // Another example
            ),
            builder
        );
    }
    
    @Override
    public Collection<String> getExamples() {
        return Arrays.asList(
            "0 -1 0",
            "0 1 0",
            "1 0 0",
            "1 1 0"
        );
    }
    
    public static void init() {
        ArgumentTypeRegistry.registerArgumentType(
            new ResourceLocation("gravity_changer:vec3"),
            Vec3ArgumentType.class,
            SingletonArgumentInfo.contextFree(() -> Vec3ArgumentType.instance)
        );
    }
}