package fr.robie.yamllibrary.paper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adventure text parsing, isolated in its own class so that the rest of the
 * module still loads on a server without Adventure on the classpath.
 * <p>
 * Nothing here is touched until a component getter is actually called.
 */
final class ComponentSupport {

    /**
     * Whether Adventure is on the classpath. Checked without loading any
     * Adventure class, so this is safe to evaluate anywhere.
     */
    static final boolean AVAILABLE = isPresent("net.kyori.adventure.text.minimessage.MiniMessage");

    private ComponentSupport() {
        throw new AssertionError("No instances");
    }

    private static boolean isPresent(@NotNull String className) {
        try {
            Class.forName(className, false, ComponentSupport.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    /**
     * Throws if Adventure is missing, with a message that names the problem.
     */
    static void requireAvailable() {
        if (!AVAILABLE) {
            throw new IllegalStateException(
                    "Adventure is not on the classpath; component getters require a Paper server "
                            + "or an explicit net.kyori:adventure-text-minimessage dependency");
        }
    }

    /**
     * Legacy serializers are cached per prefix character. Both these types are
     * immutable and thread-safe, and building one per call shows up when parsing
     * a lore list for every viewer.
     */
    private static final Map<Character, LegacyComponentSerializer> LEGACY = new ConcurrentHashMap<>();

    @NotNull
    static Component miniMessage(@NotNull String input) {
        return MiniMessageHolder.INSTANCE.deserialize(input);
    }

    @NotNull
    static Component miniMessage(@NotNull String input, @NotNull TagResolver @NotNull [] resolvers) {
        if (resolvers.length == 0) {
            return MiniMessageHolder.INSTANCE.deserialize(input);
        }
        return MiniMessageHolder.INSTANCE.deserialize(input, resolvers);
    }

    @NotNull
    static Component legacy(@NotNull String input, char character) {
        return LEGACY.computeIfAbsent(character, LegacyComponentSerializer::legacy).deserialize(input);
    }

    /**
     * Defers touching MiniMessage until the first parse, so that
     * {@link #AVAILABLE} stays answerable on a server without Adventure.
     */
    private static final class MiniMessageHolder {
        private static final MiniMessage INSTANCE = MiniMessage.miniMessage();
    }
}
