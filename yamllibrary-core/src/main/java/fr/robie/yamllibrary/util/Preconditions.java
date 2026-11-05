package fr.robie.yamllibrary.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Minimal argument-validation helpers, so the library carries no third-party
 * runtime dependency other than SnakeYAML.
 */
public final class Preconditions {

    private Preconditions() {
        throw new AssertionError("No instances");
    }

    /**
     * Throws a {@link NullPointerException} with the given message if {@code reference} is null.
     *
     * @param reference the value to check
     * @param message   the exception message
     * @param <T>       the value type
     * @return {@code reference}, never null
     */
    @Contract("null, _ -> fail; !null, _ -> param1")
    @NotNull
    public static <T> T checkNotNull(@Nullable T reference, @NotNull String message) {
        if (reference == null) {
            throw new NullPointerException(message);
        }
        return reference;
    }

    /**
     * Throws an {@link IllegalArgumentException} with the given message if {@code expression} is false.
     *
     * @param expression the condition that must hold
     * @param message    the exception message
     */
    @Contract("false, _ -> fail")
    public static void checkArgument(boolean expression, @NotNull String message) {
        if (!expression) {
            throw new IllegalArgumentException(message);
        }
    }
}
