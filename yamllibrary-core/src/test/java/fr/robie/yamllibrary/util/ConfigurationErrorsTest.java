package fr.robie.yamllibrary.util;

import fr.robie.yamllibrary.file.YamlConfiguration;
import fr.robie.yamllibrary.serialization.ConfigurationSerializable;
import fr.robie.yamllibrary.serialization.ConfigurationSerialization;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ConfigurationErrors Tests")
class ConfigurationErrorsTest {

    private final List<String> reported = new ArrayList<>();

    private void capture() {
        ConfigurationErrors.setHandler((message, error) -> this.reported.add(message));
    }

    @AfterEach
    void tearDown() {
        ConfigurationErrors.setHandler(null);
    }

    @Nested
    @DisplayName("Handler Tests")
    class HandlerTests {

        @Test
        @DisplayName("Should default to the logging handler")
        void testDefault() {
            assertSame(ConfigurationErrors.LOGGING, ConfigurationErrors.handler());
        }

        @Test
        @DisplayName("Should restore the default when set to null")
        void testResetToNull() {
            ConfigurationErrors.setHandler(ConfigurationErrors.SILENT);
            ConfigurationErrors.setHandler(null);

            assertSame(ConfigurationErrors.LOGGING, ConfigurationErrors.handler());
        }

        @Test
        @DisplayName("Should install the silent handler")
        void testSilence() {
            ConfigurationErrors.silence();

            assertSame(ConfigurationErrors.SILENT, ConfigurationErrors.handler());
        }

        @Test
        @DisplayName("Should pass the message and cause through")
        void testReport() {
            List<Throwable> causes = new ArrayList<>();
            ConfigurationErrors.setHandler((message, error) -> {
                ConfigurationErrorsTest.this.reported.add(message);
                causes.add(error);
            });

            RuntimeException cause = new RuntimeException("boom");
            ConfigurationErrors.report("something broke", cause);

            assertEquals(List.of("something broke"), ConfigurationErrorsTest.this.reported);
            assertEquals(List.of(cause), causes);
        }

        @Test
        @DisplayName("Should swallow a handler that throws")
        void testHandlerThatThrows() {
            ConfigurationErrors.setHandler((message, error) -> {
                throw new IllegalStateException("handler is broken");
            });

            assertDoesNotThrow(() -> ConfigurationErrors.report("anything", null));
        }
    }

    @Nested
    @DisplayName("Reporting Site Tests")
    class ReportingSiteTests {

        @SuppressWarnings("unused")
        public static final class Exploding implements ConfigurationSerializable {
            @NotNull
            @Override
            public Map<String, Object> serialize() {
                return Map.of();
            }

            public static Exploding deserialize(@NotNull Map<String, ?> args) {
                throw new IllegalStateException("cannot rebuild this");
            }
        }

        @Test
        @DisplayName("Should report a value that fails to deserialize")
        void testDeserializationFailureReported() throws Exception {
            ConfigurationErrorsTest.this.capture();
            ConfigurationSerialization.registerClass(Exploding.class, "exploding");
            try {
                YamlConfiguration config = new YamlConfiguration();
                config.loadFromString("bad:\n  ==: exploding\n");

                assertFalse(ConfigurationErrorsTest.this.reported.isEmpty(),
                        "the failure should have been reported");
                assertTrue(ConfigurationErrorsTest.this.reported.getFirst().contains("Exploding"),
                        ConfigurationErrorsTest.this.reported.getFirst());
            } finally {
                ConfigurationSerialization.unregisterClass("exploding");
            }
        }

        @Test
        @DisplayName("Should drop a value that cannot be rebuilt, having reported it")
        void testDeserializationDropsValue() throws Exception {
            ConfigurationErrors.silence();
            ConfigurationSerialization.registerClass(Exploding.class, "exploding");
            try {
                YamlConfiguration config = new YamlConfiguration();
                config.loadFromString("bad:\n  ==: exploding\n");

                // Matches Bukkit: a value that fails to deserialize becomes absent
                // rather than surfacing as a raw map.
                assertNull(config.get("bad"));
            } finally {
                ConfigurationSerialization.unregisterClass("exploding");
            }
        }

        @Test
        @DisplayName("Should report an unreadable file")
        void testUnreadableFileReported() {
            ConfigurationErrorsTest.this.capture();

            YamlConfiguration.loadConfiguration(new File("does/not/exist.yml"));

            assertEquals(1, ConfigurationErrorsTest.this.reported.size());
            assertTrue(ConfigurationErrorsTest.this.reported.getFirst().contains("Could not load"));
        }

        @Test
        @DisplayName("Should report malformed YAML from a reader")
        void testMalformedYamlReported() {
            ConfigurationErrorsTest.this.capture();

            YamlConfiguration config = YamlConfiguration.loadConfiguration(
                    new StringReader("this: [is: not, valid: yaml"));

            assertTrue(config.getKeys(false).isEmpty());
            assertEquals(1, ConfigurationErrorsTest.this.reported.size());
        }

        @Test
        @DisplayName("Should say nothing when a load succeeds")
        void testQuietOnSuccess() {
            ConfigurationErrorsTest.this.capture();

            YamlConfiguration.loadConfiguration(new StringReader("greeting: hello\n"));

            assertTrue(ConfigurationErrorsTest.this.reported.isEmpty(),
                    ConfigurationErrorsTest.this.reported.toString());
        }
    }
}
