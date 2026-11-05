package fr.robie.yamllibrary.serialization;

import fr.robie.yamllibrary.util.Preconditions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The registry of {@link SerializationAdapter}s consulted when a value is not
 * handled by {@link ConfigurationSerialization} itself.
 * <p>
 * Adapters are matched in registration order, first match wins.
 */
public final class SerializationAdapters {

    private static final List<SerializationAdapter> ADAPTERS = new CopyOnWriteArrayList<>();

    private SerializationAdapters() {
        throw new AssertionError("No instances");
    }

    /**
     * Registers an adapter, if it is not registered already.
     *
     * @param adapter the adapter to add
     */
    public static void register(@NotNull SerializationAdapter adapter) {
        Preconditions.checkNotNull(adapter, "Adapter cannot be null");
        if (!ADAPTERS.contains(adapter)) {
            ADAPTERS.add(adapter);
        }
    }

    /**
     * Removes a previously registered adapter.
     *
     * @param adapter the adapter to remove
     * @return true if it was registered
     */
    public static boolean unregister(@NotNull SerializationAdapter adapter) {
        Preconditions.checkNotNull(adapter, "Adapter cannot be null");
        return ADAPTERS.remove(adapter);
    }

    /**
     * Removes every registered adapter.
     */
    public static void clear() {
        ADAPTERS.clear();
    }

    /**
     * The registered adapters, in match order.
     *
     * @return an unmodifiable view
     */
    @NotNull
    public static List<SerializationAdapter> adapters() {
        return List.copyOf(ADAPTERS);
    }

    /**
     * Finds the adapter that writes the given type.
     *
     * @param type the type to write
     * @return the first matching adapter, or null
     */
    @Nullable
    public static SerializationAdapter findSerializer(@NotNull Class<?> type) {
        Preconditions.checkNotNull(type, "Type cannot be null");
        for (SerializationAdapter adapter : ADAPTERS) {
            if (adapter.baseType().isAssignableFrom(type)) {
                return adapter;
            }
        }
        return null;
    }

    /**
     * Finds the adapter that reads the given alias.
     *
     * @param alias the value found under the {@code ==} key
     * @return the first matching adapter, or null
     */
    @Nullable
    public static SerializationAdapter findDeserializer(@NotNull String alias) {
        Preconditions.checkNotNull(alias, "Alias cannot be null");
        for (SerializationAdapter adapter : ADAPTERS) {
            if (adapter.canDeserialize(alias)) {
                return adapter;
            }
        }
        return null;
    }
}
