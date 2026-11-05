package fr.robie.yamllibrary.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

/**
 * Where the library sends problems it recovers from rather than throwing.
 * <p>
 * By default these go to a {@link System.Logger} at {@link Level#WARNING}, which
 * on a server means they land in the console. That is deliberate: a config error
 * that produces no output at all is very hard to diagnose from a bug report.
 * <p>
 * The handler is a single global, so a plugin should only replace it when it owns
 * its copy of this library -- that is, when the library is shaded and relocated
 * into that plugin. On a server where several plugins share one unrelocated copy,
 * whichever sets it last wins. Prefer leaving the default in place there.
 */
public final class ConfigurationErrors {

    private static final Logger LOGGER = System.getLogger("fr.robie.yamllibrary");

    /**
     * Logs to {@code System.Logger} at WARNING. This is the default.
     */
    public static final ConfigurationErrorHandler LOGGING =
            (message, error) -> LOGGER.log(Level.WARNING, message, error);

    /**
     * Discards everything. Restores the behaviour the library had before this
     * class existed.
     */
    public static final ConfigurationErrorHandler SILENT = (message, error) -> {
    };

    private static volatile ConfigurationErrorHandler handler = LOGGING;

    private ConfigurationErrors() {
        throw new AssertionError("No instances");
    }

    /**
     * The current handler.
     *
     * @return the handler, never null
     */
    @NotNull
    public static ConfigurationErrorHandler handler() {
        return handler;
    }

    /**
     * Replaces the handler.
     *
     * @param newHandler the handler to use, or null to restore {@link #LOGGING}
     */
    public static void setHandler(@Nullable ConfigurationErrorHandler newHandler) {
        handler = newHandler != null ? newHandler : LOGGING;
    }

    /**
     * Turns reporting off entirely.
     */
    public static void silence() {
        handler = SILENT;
    }

    /**
     * Reports a problem, swallowing anything the handler throws so that reporting
     * a failure can never itself become one.
     *
     * @param message what went wrong
     * @param error   the cause, or null
     */
    public static void report(@NotNull String message, @Nullable Throwable error) {
        try {
            handler.onError(message, error);
        } catch (Throwable ignored) {
        }
    }
}
