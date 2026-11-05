package fr.robie.yamllibrary.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Receives problems the library recovers from instead of throwing.
 * <p>
 * There are places where failing hard would be worse than carrying on: a single
 * unreadable value in an otherwise good file, or a config that will not parse at
 * all when the caller asked for a best-effort load. The library keeps going in
 * those cases, and reports here so the problem is at least visible.
 *
 * @see ConfigurationErrors
 */
@FunctionalInterface
public interface ConfigurationErrorHandler {

    /**
     * Called when the library recovers from a problem.
     * <p>
     * Implementations must not throw; anything thrown here is discarded.
     *
     * @param message what went wrong, and what the library did instead
     * @param error   the cause, or null if there was no exception
     */
    void onError(@NotNull String message, @Nullable Throwable error);
}
