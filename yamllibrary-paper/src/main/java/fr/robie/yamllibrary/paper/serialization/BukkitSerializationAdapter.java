package fr.robie.yamllibrary.paper.serialization;

import fr.robie.yamllibrary.serialization.SerializationAdapter;
import fr.robie.yamllibrary.serialization.SerializationAdapters;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bridges Bukkit's serialization system onto this library.
 * <p>
 * Bukkit types such as {@code ItemStack}, {@code Location}, {@code Vector} and
 * {@code PotionEffect} implement
 * {@link org.bukkit.configuration.serialization.ConfigurationSerializable}, which is
 * a different interface from this library's own. Without this adapter the library
 * has no idea how to write them, and SnakeYAML would fall back to reflecting over
 * their internals.
 * <p>
 * With the adapter installed, those types are written using Bukkit's own
 * {@code serialize()} output under Bukkit's own alias, so the resulting YAML is
 * byte-for-byte compatible with what a vanilla Bukkit {@code YamlConfiguration}
 * would produce, and existing config files keep loading.
 * <p>
 * {@link fr.robie.yamllibrary.paper.PaperYamlConfiguration} installs this
 * automatically; call {@link #install()} directly only when using core's
 * {@code YamlConfiguration} on a server.
 */
public final class BukkitSerializationAdapter implements SerializationAdapter {

    private static final BukkitSerializationAdapter INSTANCE = new BukkitSerializationAdapter();

    private BukkitSerializationAdapter() {
    }

    /**
     * The shared instance.
     *
     * @return the singleton adapter
     */
    @NotNull
    public static BukkitSerializationAdapter get() {
        return INSTANCE;
    }

    /**
     * Registers the bridge, if it is not registered already.
     * <p>
     * Safe to call repeatedly.
     */
    public static void install() {
        SerializationAdapters.register(INSTANCE);
    }

    /**
     * Removes the bridge.
     *
     * @return true if it had been installed
     */
    public static boolean uninstall() {
        return SerializationAdapters.unregister(INSTANCE);
    }

    @NotNull
    @Override
    public Class<?> baseType() {
        return ConfigurationSerializable.class;
    }

    @NotNull
    @Override
    public String aliasFor(@NotNull Class<?> type) {
        return ConfigurationSerialization.getAlias(type.asSubclass(ConfigurationSerializable.class));
    }

    @NotNull
    @Override
    public Map<String, Object> serialize(@NotNull Object value) {
        Map<String, Object> serialized = ((ConfigurationSerializable) value).serialize();

        // Defensive copy: Bukkit implementations are not required to return a
        // mutable or independent map, and callers write the type key alongside it.
        return new LinkedHashMap<>(serialized);
    }

    @Override
    public boolean canDeserialize(@NotNull String alias) {
        return ConfigurationSerialization.getClassByAlias(alias) != null;
    }

    @Nullable
    @Override
    public Object deserialize(@NotNull String alias, @NotNull Map<String, ?> args) {
        Class<? extends ConfigurationSerializable> clazz = ConfigurationSerialization.getClassByAlias(alias);
        if (clazz == null) {
            return null;
        }

        // Bukkit reads the type key out of the map itself, so pass it through as-is
        // but under Bukkit's own key constant in case the two ever diverge.
        Map<String, Object> bukkitArgs = new LinkedHashMap<>(args);
        bukkitArgs.put(ConfigurationSerialization.SERIALIZED_TYPE_KEY, alias);

        return ConfigurationSerialization.deserializeObject(bukkitArgs, clazz);
    }
}
