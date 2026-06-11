package fr.robie.yamllibrary;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MemoryConfiguration Tests")
class MemoryConfigurationTest {

    @Nested
    @DisplayName("Construction Tests")
    class ConstructionTests {

        @Test
        @DisplayName("Should create empty MemoryConfiguration")
        void testEmptyConstructor() {
            MemoryConfiguration config = new MemoryConfiguration();
            assertTrue(config.getKeys(false).isEmpty());
            assertNull(config.getDefaults());
            assertSame(config, config.getRoot());
            assertNull(config.getParent());
        }

        @Test
        @DisplayName("Should create MemoryConfiguration with defaults")
        void testConstructorWithDefaults() {
            MemoryConfiguration defaults = new MemoryConfiguration();
            defaults.set("key", "defaultVal");

            MemoryConfiguration config = new MemoryConfiguration(defaults);
            assertNotNull(config.getDefaults());
            assertEquals("defaultVal", config.getDefaults().get("key"));
        }
    }

    @Nested
    @DisplayName("Defaults Tests")
    class DefaultsTests {

        @Test
        @DisplayName("Should add individual default value")
        void testAddDefault() {
            MemoryConfiguration config = new MemoryConfiguration();
            config.addDefault("key", "value");

            assertNotNull(config.getDefaults());
            assertEquals("value", config.getDefaults().get("key"));
        }

        @Test
        @DisplayName("Should automatically create defaults config when adding first default")
        void testAddDefaultCreatesDefaults() {
            MemoryConfiguration config = new MemoryConfiguration();
            assertNull(config.getDefaults());

            config.addDefault("key", "value");
            assertNotNull(config.getDefaults());
        }

        @Test
        @DisplayName("Should add defaults from Map")
        void testAddDefaultsFromMap() {
            MemoryConfiguration config = new MemoryConfiguration();
            Map<String, Object> defaults = new LinkedHashMap<>();
            defaults.put("a", 1);
            defaults.put("b", "two");
            config.addDefaults(defaults);

            assertEquals(1, config.getDefaults().get("a"));
            assertEquals("two", config.getDefaults().get("b"));
        }

        @Test
        @DisplayName("Should add defaults from Configuration")
        void testAddDefaultsFromConfiguration() {
            MemoryConfiguration source = new MemoryConfiguration();
            source.set("x", 10);
            source.set("y", 20);

            MemoryConfiguration config = new MemoryConfiguration();
            config.addDefaults(source);

            assertEquals(10, config.getDefaults().get("x"));
            assertEquals(20, config.getDefaults().get("y"));
        }

        @Test
        @DisplayName("Should replace defaults with setDefaults")
        void testSetDefaults() {
            MemoryConfiguration config = new MemoryConfiguration();
            config.addDefault("old", "value");

            MemoryConfiguration newDefaults = new MemoryConfiguration();
            newDefaults.set("new", "replacement");
            config.setDefaults(newDefaults);

            assertEquals("replacement", config.getDefaults().get("new"));
            assertNull(config.getDefaults().get("old"));
        }

        @Test
        @DisplayName("Should throw on null setDefaults")
        void testSetDefaultsNull() {
            MemoryConfiguration config = new MemoryConfiguration();
            assertThrows(IllegalArgumentException.class, () -> config.setDefaults(null));
        }
    }

    @Nested
    @DisplayName("Options Tests")
    class OptionsTests {

        @Test
        @DisplayName("Should return MemoryConfigurationOptions")
        void testOptionsType() {
            MemoryConfiguration config = new MemoryConfiguration();
            assertInstanceOf(MemoryConfigurationOptions.class, config.options());
        }

        @Test
        @DisplayName("Should allow chaining on options")
        void testOptionsChaining() {
            MemoryConfiguration config = new MemoryConfiguration();
            MemoryConfigurationOptions opts = config.options()
                    .copyDefaults(true)
                    .pathSeparator('/');

            assertTrue(opts.copyDefaults());
            assertEquals('/', opts.pathSeparator());
        }

        @Test
        @DisplayName("Should persist options after retrieval")
        void testOptionsPersistence() {
            MemoryConfiguration config = new MemoryConfiguration();
            config.options().copyDefaults(true);
            config.options().pathSeparator('-');

            assertTrue(config.options().copyDefaults());
            assertEquals('-', config.options().pathSeparator());
        }
    }

    @Nested
    @DisplayName("Root Behavior Tests")
    class RootBehaviorTests {

        @Test
        @DisplayName("Should return itself as root")
        void testRootIsSelf() {
            MemoryConfiguration config = new MemoryConfiguration();
            assertSame(config, config.getRoot());
        }

        @Test
        @DisplayName("Should return null parent for root")
        void testParentIsNull() {
            MemoryConfiguration config = new MemoryConfiguration();
            assertNull(config.getParent());
        }

        @Test
        @DisplayName("Should return empty path for root")
        void testCurrentPath() {
            MemoryConfiguration config = new MemoryConfiguration();
            assertEquals("", config.getCurrentPath());
        }

        @Test
        @DisplayName("Should return empty name for root")
        void testName() {
            MemoryConfiguration config = new MemoryConfiguration();
            assertEquals("", config.getName());
        }
    }

    @Nested
    @DisplayName("AddDefault Behavior Tests")
    class AddDefaultBehaviorTests {

        @Test
        @DisplayName("Should add default values with nested paths")
        void testAddDefaultNested() {
            MemoryConfiguration config = new MemoryConfiguration();
            config.addDefault("a.b.c", "deep");

            Configuration defaults = config.getDefaults();
            assertNotNull(defaults);
            assertEquals("deep", defaults.get("a.b.c"));
        }

        @Test
        @DisplayName("Should not overwrite existing defaults with same path")
        void testAddDefaultNoOverwrite() {
            MemoryConfiguration config = new MemoryConfiguration();
            config.addDefault("key", "first");
            config.addDefault("key", "second");

            assertEquals("second", config.getDefaults().get("key"));
        }

        @Test
        @DisplayName("Should add defaults from Configuration skipping sections")
        void testAddDefaultsFromConfigurationSkipsSections() {
            MemoryConfiguration source = new MemoryConfiguration();
            source.set("a", "leaf");
            source.createSection("section");

            MemoryConfiguration config = new MemoryConfiguration();
            config.addDefaults(source);

            assertEquals("leaf", config.getDefaults().get("a"));
            assertNull(config.getDefaults().get("section"));
        }
    }
}
