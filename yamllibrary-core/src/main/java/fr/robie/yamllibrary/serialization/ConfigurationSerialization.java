package fr.robie.yamllibrary.serialization;

import fr.robie.yamllibrary.util.ConfigurationErrors;
import fr.robie.yamllibrary.util.Preconditions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

public class ConfigurationSerialization {
    public static final String SERIALIZED_TYPE_KEY = "==";
    private final Class<? extends ConfigurationSerializable> clazz;
    private static final Map<String, Class<? extends ConfigurationSerializable>> aliases = new HashMap<>();

    protected ConfigurationSerialization(@NotNull Class<? extends ConfigurationSerializable> clazz) {
        this.clazz = clazz;
    }

    @Nullable
    protected Method getMethod(@NotNull String name) {
        try {
            Method method = this.clazz.getDeclaredMethod(name, Map.class);

            if (!ConfigurationSerializable.class.isAssignableFrom(method.getReturnType())) {
                return null;
            }
            if (!Modifier.isStatic(method.getModifiers())) {
                return null;
            }

            return method;
        } catch (NoSuchMethodException | SecurityException ex) {
            return null;
        }
    }

    @Nullable
    protected Constructor<? extends ConfigurationSerializable> getConstructor() {
        try {
            return this.clazz.getConstructor(Map.class);
        } catch (NoSuchMethodException | SecurityException ex) {
            return null;
        }
    }

    @Nullable
    protected ConfigurationSerializable deserializeViaMethod(@NotNull Method method, @NotNull Map<String, ?> args) {
        try {
            ConfigurationSerializable result = (ConfigurationSerializable) method.invoke(null, args);

            if (result == null) {
                ConfigurationErrors.report("Could not call method '" + method + "' of " + this.clazz
                        + " for deserialization: method returned null", null);
            } else {
                return result;
            }
        } catch (Throwable ex) {
            ConfigurationErrors.report("Could not call method '" + method + "' of " + this.clazz
                            + " for deserialization",
                    ex instanceof InvocationTargetException ? ex.getCause() : ex);
        }

        return null;
    }

    @Nullable
    protected ConfigurationSerializable deserializeViaCtor(@NotNull Constructor<? extends ConfigurationSerializable> ctor, @NotNull Map<String, ?> args) {
        try {
            return ctor.newInstance(args);
        } catch (Throwable ex) {
            ConfigurationErrors.report("Could not call constructor '" + ctor + "' of " + this.clazz
                            + " for deserialization",
                    ex instanceof InvocationTargetException ? ex.getCause() : ex);
        }

        return null;
    }

    @Nullable
    public ConfigurationSerializable deserialize(@NotNull Map<String, ?> args) {
        Preconditions.checkNotNull(args, "Args must not be null");

        ConfigurationSerializable result = null;
        Method method = this.getMethod("deserialize");

        if (method != null) {
            result = this.deserializeViaMethod(method, args);
        }

        if (result == null) {
            method = this.getMethod("valueOf");

            if (method != null) {
                result = this.deserializeViaMethod(method, args);
            }
        }

        if (result == null) {
            Constructor<? extends ConfigurationSerializable> constructor = this.getConstructor();

            if (constructor != null) {
                result = this.deserializeViaCtor(constructor, args);
            }
        }

        return result;
    }

    @Nullable
    public static ConfigurationSerializable deserializeObject(@NotNull Map<String, ?> args, @NotNull Class<? extends ConfigurationSerializable> clazz) {
        return new ConfigurationSerialization(clazz).deserialize(args);
    }

    @Nullable
    public static ConfigurationSerializable deserializeObject(@NotNull Map<String, ?> args) {
        String alias = resolveAlias(args);

        Class<? extends ConfigurationSerializable> clazz = getClassByAlias(alias);
        if (clazz == null) {
            throw new IllegalArgumentException("Specified class does not exist ('" + alias + "')");
        }

        return new ConfigurationSerialization(clazz).deserialize(args);
    }

    /**
     * Rebuilds any serialized value, whether it is backed by a class registered
     * here or by a {@link SerializationAdapter}.
     * <p>
     * The built-in registry is consulted first, so registered
     * {@link ConfigurationSerializable} classes always win over adapters.
     *
     * @param args the serialized form, which must contain the {@code ==} key
     * @return the reconstructed value, or null if nothing could rebuild it
     */
    @Nullable
    public static Object deserializeValue(@NotNull Map<String, ?> args) {
        Preconditions.checkNotNull(args, "Args must not be null");

        String alias = resolveAlias(args);

        Class<? extends ConfigurationSerializable> clazz = getClassByAlias(alias);
        if (clazz != null) {
            return new ConfigurationSerialization(clazz).deserialize(args);
        }

        SerializationAdapter adapter = SerializationAdapters.findDeserializer(alias);
        if (adapter != null) {
            return adapter.deserialize(alias, args);
        }

        throw new IllegalArgumentException("Specified class does not exist ('" + alias + "')");
    }

    /**
     * Reads the {@code ==} key out of a serialized form.
     *
     * @param args the serialized form
     * @return the alias
     * @throws IllegalArgumentException if the key is missing or its value is null
     * @throws ClassCastException       if the key's value is not a string
     */
    @NotNull
    private static String resolveAlias(@NotNull Map<String, ?> args) {
        if (!args.containsKey(SERIALIZED_TYPE_KEY)) {
            throw new IllegalArgumentException("Args doesn't contain type key ('" + SERIALIZED_TYPE_KEY + "')");
        }

        String alias;
        try {
            alias = (String) args.get(SERIALIZED_TYPE_KEY);
        } catch (ClassCastException ex) {
            ex.fillInStackTrace();
            throw ex;
        }

        if (alias == null) {
            throw new IllegalArgumentException("Cannot have null alias");
        }
        return alias;
    }

    public static void registerClass(@NotNull Class<? extends ConfigurationSerializable> clazz) {
        DelegateDeserialization delegate = clazz.getAnnotation(DelegateDeserialization.class);

        if (delegate == null) {
            registerClass(clazz, getAlias(clazz));
            registerClass(clazz, clazz.getName());
        }
    }

    public static void registerClass(@NotNull Class<? extends ConfigurationSerializable> clazz, @NotNull String alias) {
        aliases.put(alias, clazz);
    }

    public static void unregisterClass(@NotNull String alias) {
        aliases.remove(alias);
    }

    public static void unregisterClass(@NotNull Class<? extends ConfigurationSerializable> clazz) {
        while (aliases.values().remove(clazz)) {
        }
    }

    @Nullable
    public static Class<? extends ConfigurationSerializable> getClassByAlias(@NotNull String alias) {
        return aliases.get(alias);
    }

    @NotNull
    public static String getAlias(@NotNull Class<? extends ConfigurationSerializable> clazz) {
        DelegateDeserialization delegate = clazz.getAnnotation(DelegateDeserialization.class);

        if (delegate != null) {
            if (delegate.value() != clazz) {
                return getAlias(delegate.value());
            }
        }

        SerializableAs alias = clazz.getAnnotation(SerializableAs.class);

        if (alias != null) {
            return alias.value();
        }

        return clazz.getName();
    }
}
