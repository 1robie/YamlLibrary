package fr.robie.yamllibrary.serialization;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Teaches the library how to read and write a family of types that do not
 * implement {@link ConfigurationSerializable}.
 * <p>
 * This is the extension point downstream platform modules use to bridge their
 * own serialization system onto this library. A Paper module, for example,
 * registers an adapter that delegates to Bukkit's serialization registry, so
 * that {@code ItemStack}, {@code Location} and friends round-trip without
 * either library knowing about the other.
 * <p>
 * Adapters are consulted only <em>after</em> the built-in
 * {@link ConfigurationSerialization} registry declines a type or alias, so an
 * adapter can never shadow a natively registered class.
 *
 * @see SerializationAdapters
 */
public interface SerializationAdapter {

    /**
     * The type whose instances this adapter writes, usually an interface.
     * <p>
     * Every value assignable to this type is handed to this adapter, so it must
     * be a type the adapter fully owns. The Paper bridge, for instance, returns
     * {@code org.bukkit.configuration.serialization.ConfigurationSerializable}.
     *
     * @return the base type this adapter claims
     */
    @NotNull
    Class<?> baseType();

    /**
     * The value written under the {@code ==} key for the given type.
     *
     * @param type a subtype of {@link #baseType()}
     * @return the alias identifying this type in the YAML document
     */
    @NotNull
    String aliasFor(@NotNull Class<?> type);

    /**
     * Breaks a value down into a map of YAML-representable values.
     * <p>
     * The returned map must not contain the {@code ==} key; the library adds it.
     *
     * @param value an instance of {@link #baseType()}
     * @return the serialized form
     */
    @NotNull
    Map<String, Object> serialize(@NotNull Object value);

    /**
     * Whether this adapter can read the given alias back.
     *
     * @param alias the value found under the {@code ==} key
     * @return true if {@link #deserialize(String, Map)} accepts this alias
     */
    boolean canDeserialize(@NotNull String alias);

    /**
     * Rebuilds a value from its serialized form.
     *
     * @param alias the value found under the {@code ==} key
     * @param args  the serialized form, including the {@code ==} key
     * @return the reconstructed value, or null if it could not be rebuilt
     */
    @Nullable
    Object deserialize(@NotNull String alias, @NotNull Map<String, ?> args);
}
