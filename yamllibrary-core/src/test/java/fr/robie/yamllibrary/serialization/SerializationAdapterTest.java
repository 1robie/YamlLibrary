package fr.robie.yamllibrary.serialization;

import fr.robie.yamllibrary.InvalidConfigurationException;
import fr.robie.yamllibrary.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SerializationAdapter Tests")
class SerializationAdapterTest {

    /**
     * A type from a hypothetical downstream platform. It deliberately does not
     * implement {@link ConfigurationSerializable} -- that is the point of the SPI.
     */
    interface Foreign {
    }

    static final class ForeignPoint implements Foreign {
        final int x;
        final int y;

        ForeignPoint(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof ForeignPoint p && p.x == this.x && p.y == this.y;
        }

        @Override
        public int hashCode() {
            return this.x * 31 + this.y;
        }
    }

    static final class ForeignAdapter implements SerializationAdapter {
        static final String ALIAS = "foreign:point";

        @NotNull
        @Override
        public Class<?> baseType() {
            return Foreign.class;
        }

        @NotNull
        @Override
        public String aliasFor(@NotNull Class<?> type) {
            return ALIAS;
        }

        @NotNull
        @Override
        public Map<String, Object> serialize(@NotNull Object value) {
            ForeignPoint point = (ForeignPoint) value;
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("x", point.x);
            map.put("y", point.y);
            return map;
        }

        @Override
        public boolean canDeserialize(@NotNull String alias) {
            return ALIAS.equals(alias);
        }

        @Nullable
        @Override
        public Object deserialize(@NotNull String alias, @NotNull Map<String, ?> args) {
            return new ForeignPoint((Integer) args.get("x"), (Integer) args.get("y"));
        }
    }

    @AfterEach
    void tearDown() {
        SerializationAdapters.clear();
    }

    @Nested
    @DisplayName("Registry Tests")
    class RegistryTests {

        @Test
        @DisplayName("Should find serializer by assignable base type")
        void testFindSerializer() {
            ForeignAdapter adapter = new ForeignAdapter();
            SerializationAdapters.register(adapter);

            assertSame(adapter, SerializationAdapters.findSerializer(ForeignPoint.class));
        }

        @Test
        @DisplayName("Should return null for an unclaimed type")
        void testFindSerializerUnclaimed() {
            SerializationAdapters.register(new ForeignAdapter());

            assertNull(SerializationAdapters.findSerializer(String.class));
        }

        @Test
        @DisplayName("Should find deserializer by alias")
        void testFindDeserializer() {
            ForeignAdapter adapter = new ForeignAdapter();
            SerializationAdapters.register(adapter);

            assertSame(adapter, SerializationAdapters.findDeserializer(ForeignAdapter.ALIAS));
            assertNull(SerializationAdapters.findDeserializer("nope"));
        }

        @Test
        @DisplayName("Should not register the same adapter twice")
        void testRegisterIdempotent() {
            ForeignAdapter adapter = new ForeignAdapter();
            SerializationAdapters.register(adapter);
            SerializationAdapters.register(adapter);

            assertEquals(1, SerializationAdapters.adapters().size());
        }

        @Test
        @DisplayName("Should unregister an adapter")
        void testUnregister() {
            ForeignAdapter adapter = new ForeignAdapter();
            SerializationAdapters.register(adapter);

            assertTrue(SerializationAdapters.unregister(adapter));
            assertTrue(SerializationAdapters.adapters().isEmpty());
            assertFalse(SerializationAdapters.unregister(adapter));
        }

        @Test
        @DisplayName("Should expose an unmodifiable view")
        void testAdaptersUnmodifiable() {
            SerializationAdapters.register(new ForeignAdapter());

            assertThrows(UnsupportedOperationException.class,
                    () -> SerializationAdapters.adapters().clear());
        }

        @Test
        @DisplayName("Should reject a null adapter")
        void testRegisterNull() {
            assertThrows(NullPointerException.class, () -> SerializationAdapters.register(null));
        }
    }

    @Nested
    @DisplayName("Round-trip Tests")
    class RoundTripTests {

        @Test
        @DisplayName("Should write an adapted value with a type key")
        void testWrite() {
            SerializationAdapters.register(new ForeignAdapter());

            YamlConfiguration config = new YamlConfiguration();
            config.set("spawn", new ForeignPoint(3, 4));

            String yaml = config.saveToString();

            assertTrue(yaml.contains("==: " + ForeignAdapter.ALIAS), yaml);
            assertTrue(yaml.contains("x: 3"), yaml);
            assertTrue(yaml.contains("y: 4"), yaml);
        }

        @Test
        @DisplayName("Should read an adapted value back")
        void testRead() throws InvalidConfigurationException {
            SerializationAdapters.register(new ForeignAdapter());

            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString("spawn:\n  ==: foreign:point\n  x: 3\n  y: 4\n");

            assertEquals(new ForeignPoint(3, 4), config.get("spawn"));
        }

        @Test
        @DisplayName("Should round-trip an adapted value")
        void testRoundTrip() throws InvalidConfigurationException {
            SerializationAdapters.register(new ForeignAdapter());

            YamlConfiguration written = new YamlConfiguration();
            written.set("spawn", new ForeignPoint(7, -2));

            YamlConfiguration read = new YamlConfiguration();
            read.loadFromString(written.saveToString());

            assertEquals(new ForeignPoint(7, -2), read.get("spawn"));
        }

        @Test
        @DisplayName("Should round-trip adapted values nested in a list")
        void testRoundTripInList() throws InvalidConfigurationException {
            SerializationAdapters.register(new ForeignAdapter());

            YamlConfiguration written = new YamlConfiguration();
            written.set("points", List.of(new ForeignPoint(1, 1), new ForeignPoint(2, 2)));

            YamlConfiguration read = new YamlConfiguration();
            read.loadFromString(written.saveToString());

            assertEquals(List.of(new ForeignPoint(1, 1), new ForeignPoint(2, 2)), read.get("points"));
        }

        @Test
        @DisplayName("Should pick up an adapter registered after the config was created")
        void testLateRegistration() {
            YamlConfiguration config = new YamlConfiguration();
            config.set("spawn", new ForeignPoint(1, 2));

            SerializationAdapters.register(new ForeignAdapter());

            assertTrue(config.saveToString().contains("==: " + ForeignAdapter.ALIAS));
        }
    }

    @Nested
    @DisplayName("Precedence Tests")
    class PrecedenceTests {

        @SerializableAs("foreign:point")
        static final class Native implements ConfigurationSerializable, Foreign {
            @NotNull
            @Override
            public Map<String, Object> serialize() {
                return Map.of("native", true);
            }

            @SuppressWarnings("unused")
            public static Native deserialize(@NotNull Map<String, ?> args) {
                return new Native();
            }
        }

        @Test
        @DisplayName("Should prefer a registered class over an adapter for the same alias")
        void testNativeWins() {
            SerializationAdapters.register(new ForeignAdapter());
            ConfigurationSerialization.registerClass(Native.class, "foreign:point");
            try {
                Object result = ConfigurationSerialization.deserializeValue(
                        Map.of(ConfigurationSerialization.SERIALIZED_TYPE_KEY, "foreign:point"));

                assertInstanceOf(Native.class, result);
            } finally {
                ConfigurationSerialization.unregisterClass("foreign:point");
            }
        }

        @Test
        @DisplayName("Should throw when neither a class nor an adapter knows the alias")
        void testUnknownAlias() {
            assertThrows(IllegalArgumentException.class, () -> ConfigurationSerialization.deserializeValue(
                    Map.of(ConfigurationSerialization.SERIALIZED_TYPE_KEY, "totally:unknown")));
        }

        @Test
        @DisplayName("Should throw when the type key is missing")
        void testMissingTypeKey() {
            assertThrows(IllegalArgumentException.class,
                    () -> ConfigurationSerialization.deserializeValue(Map.of("x", 1)));
        }
    }
}
