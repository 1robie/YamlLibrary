package fr.robie.yamllibrary.paper;

import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ConfigManager Tests")
class ConfigManagerTest {

    private static final String NAME = "testconfig.yml";

    private Plugin plugin;
    private ConfigManager manager;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        this.plugin = MockBukkit.createMockPlugin("ConfigManagerTestPlugin");
        this.manager = new ConfigManager(this.plugin);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Nested
    @DisplayName("Defaults Tests")
    class DefaultsTests {

        @Test
        @DisplayName("Should write a bundled resource out on first request")
        void testSaveDefault() {
            assertTrue(ConfigManagerTest.this.manager.saveDefault(NAME));
            assertTrue(ConfigManagerTest.this.manager.fileFor(NAME).exists());
        }

        @Test
        @DisplayName("Should not overwrite a file that already exists")
        void testSaveDefaultDoesNotOverwrite() throws IOException {
            File file = ConfigManagerTest.this.manager.fileFor(NAME);
            assertTrue(file.getParentFile().mkdirs() || file.getParentFile().exists());
            Files.writeString(file.toPath(), "greeting: mine\n", StandardCharsets.UTF_8);

            assertFalse(ConfigManagerTest.this.manager.saveDefault(NAME));
            assertEquals("mine", ConfigManagerTest.this.manager.get(NAME).getString("greeting"));
        }

        @Test
        @DisplayName("Should report false for a name with no bundled resource")
        void testSaveDefaultMissingResource() {
            assertFalse(ConfigManagerTest.this.manager.saveDefault("does-not-exist.yml"));
        }

        @Test
        @DisplayName("Should read the bundled copy without touching disk")
        void testResource() {
            PaperYamlConfiguration bundled = ConfigManagerTest.this.manager.resource(NAME);

            assertNotNull(bundled);
            assertEquals("hello", bundled.getString("greeting"));
            assertFalse(ConfigManagerTest.this.manager.fileFor(NAME).exists());
        }

        @Test
        @DisplayName("Should return null reading a resource that is not bundled")
        void testResourceMissing() {
            assertNull(ConfigManagerTest.this.manager.resource("does-not-exist.yml"));
        }
    }

    @Nested
    @DisplayName("Loading Tests")
    class LoadingTests {

        @Test
        @DisplayName("Should load a configuration from the bundled default")
        void testGet() {
            PaperYamlConfiguration config = ConfigManagerTest.this.manager.get(NAME);

            assertEquals("hello", config.getString("greeting"));
            assertEquals(1, config.getInt("nested.a"));
        }

        @Test
        @DisplayName("Should return the same instance on repeated requests")
        void testGetCached() {
            assertSame(ConfigManagerTest.this.manager.get(NAME), ConfigManagerTest.this.manager.get(NAME));
        }

        @Test
        @DisplayName("Should return an empty configuration for an unknown name")
        void testGetUnknown() {
            PaperYamlConfiguration config = ConfigManagerTest.this.manager.get("does-not-exist.yml");

            assertTrue(config.getKeys(false).isEmpty());
        }

        @Test
        @DisplayName("Should track the names it has loaded")
        void testNames() {
            ConfigManagerTest.this.manager.get(NAME);

            assertEquals(java.util.Set.of(NAME), ConfigManagerTest.this.manager.names());
        }

        @Test
        @DisplayName("Should forget a configuration on unload")
        void testUnload() {
            PaperYamlConfiguration first = ConfigManagerTest.this.manager.get(NAME);

            assertTrue(ConfigManagerTest.this.manager.unload(NAME));
            assertFalse(ConfigManagerTest.this.manager.unload(NAME));
            assertNotSame(first, ConfigManagerTest.this.manager.get(NAME));
        }
    }

    @Nested
    @DisplayName("Save and Reload Tests")
    class SaveReloadTests {

        @Test
        @DisplayName("Should write changes back to disk")
        void testSave() throws IOException {
            PaperYamlConfiguration config = ConfigManagerTest.this.manager.get(NAME);
            config.set("greeting", "changed");
            ConfigManagerTest.this.manager.save(NAME);

            String onDisk = Files.readString(ConfigManagerTest.this.manager.fileFor(NAME).toPath(),
                    StandardCharsets.UTF_8);

            assertTrue(onDisk.contains("greeting: changed"), onDisk);
        }

        @Test
        @DisplayName("Should keep the caller's instance valid across a reload")
        void testReloadKeepsInstance() throws IOException {
            PaperYamlConfiguration config = ConfigManagerTest.this.manager.get(NAME);
            config.set("greeting", "changed");
            ConfigManagerTest.this.manager.save(NAME);

            Files.writeString(ConfigManagerTest.this.manager.fileFor(NAME).toPath(),
                    "greeting: from-disk\n", StandardCharsets.UTF_8);

            PaperYamlConfiguration reloaded = ConfigManagerTest.this.manager.reload(NAME);

            assertSame(config, reloaded);
            assertEquals("from-disk", config.getString("greeting"));
        }

        @Test
        @DisplayName("Should load on reload of something never loaded")
        void testReloadUnloaded() {
            assertEquals("hello", ConfigManagerTest.this.manager.reload(NAME).getString("greeting"));
        }

        @Test
        @DisplayName("Should reload every loaded configuration")
        void testReloadAll() throws IOException {
            PaperYamlConfiguration config = ConfigManagerTest.this.manager.get(NAME);
            Files.writeString(ConfigManagerTest.this.manager.fileFor(NAME).toPath(),
                    "greeting: bulk\n", StandardCharsets.UTF_8);

            ConfigManagerTest.this.manager.reloadAll();

            assertEquals("bulk", config.getString("greeting"));
        }

        @Test
        @DisplayName("Should save every loaded configuration")
        void testSaveAll() throws IOException {
            ConfigManagerTest.this.manager.get(NAME).set("greeting", "bulk");

            ConfigManagerTest.this.manager.saveAll();

            assertTrue(Files.readString(ConfigManagerTest.this.manager.fileFor(NAME).toPath(),
                    StandardCharsets.UTF_8).contains("greeting: bulk"));
        }
    }

    @Nested
    @DisplayName("Defaults Merge Tests")
    class MergeTests {

        @Test
        @DisplayName("Should add keys the bundled copy has and the file lacks")
        void testMergeAddsMissing() throws IOException {
            File file = ConfigManagerTest.this.manager.fileFor(NAME);
            assertTrue(file.getParentFile().mkdirs() || file.getParentFile().exists());
            Files.writeString(file.toPath(), "greeting: mine\n", StandardCharsets.UTF_8);

            int added = ConfigManagerTest.this.manager.mergeDefaults(NAME);
            PaperYamlConfiguration config = ConfigManagerTest.this.manager.get(NAME);

            // Sections count too: nested, nested.a, nested.b, added-later, empty-section
            assertEquals(5, added);
            assertEquals(1, config.getInt("nested.a"));
            assertTrue(config.getBoolean("added-later"));
        }

        @Test
        @DisplayName("Should create a section the bundled file has but the user's lacks")
        void testMergeCreatesEmptySection() throws IOException {
            File file = ConfigManagerTest.this.manager.fileFor(NAME);
            assertTrue(file.getParentFile().mkdirs() || file.getParentFile().exists());
            Files.writeString(file.toPath(), "greeting: mine\n", StandardCharsets.UTF_8);

            ConfigManagerTest.this.manager.mergeDefaults(NAME);

            assertTrue(ConfigManagerTest.this.manager.get(NAME).isConfigurationSection("empty-section"));
        }

        @Test
        @DisplayName("Should write new keys in the bundled file's order")
        void testMergePreservesDefaultsOrder() throws IOException {
            File file = ConfigManagerTest.this.manager.fileFor(NAME);
            assertTrue(file.getParentFile().mkdirs() || file.getParentFile().exists());
            // The user has only the last of the bundled keys; the ones added must
            // land before it, where the bundled file puts them.
            Files.writeString(file.toPath(), "added-later: false\n", StandardCharsets.UTF_8);

            ConfigManagerTest.this.manager.mergeDefaults(NAME);

            List<String> keys = List.copyOf(ConfigManagerTest.this.manager.get(NAME).getKeys(false));

            assertEquals(List.of("greeting", "nested", "added-later", "empty-section"), keys);
        }

        @Test
        @DisplayName("Should keep user-only keys, after the bundled ones")
        void testMergeKeepsUserOnlyKeys() throws IOException {
            File file = ConfigManagerTest.this.manager.fileFor(NAME);
            assertTrue(file.getParentFile().mkdirs() || file.getParentFile().exists());
            Files.writeString(file.toPath(), "my-own-key: kept\n", StandardCharsets.UTF_8);

            ConfigManagerTest.this.manager.mergeDefaults(NAME);

            PaperYamlConfiguration config = ConfigManagerTest.this.manager.get(NAME);
            List<String> keys = List.copyOf(config.getKeys(false));

            assertEquals("kept", config.getString("my-own-key"));
            assertEquals("my-own-key", keys.getLast(), keys.toString());
        }

        @Test
        @DisplayName("Should leave existing values alone")
        void testMergeKeepsExisting() throws IOException {
            File file = ConfigManagerTest.this.manager.fileFor(NAME);
            assertTrue(file.getParentFile().mkdirs() || file.getParentFile().exists());
            Files.writeString(file.toPath(), "greeting: mine\n", StandardCharsets.UTF_8);

            ConfigManagerTest.this.manager.mergeDefaults(NAME);

            assertEquals("mine", ConfigManagerTest.this.manager.get(NAME).getString("greeting"));
        }

        @Test
        @DisplayName("Should add nothing when the file is already complete")
        void testMergeNoop() throws IOException {
            ConfigManagerTest.this.manager.get(NAME);

            assertEquals(0, ConfigManagerTest.this.manager.mergeDefaults(NAME));
        }

        @Test
        @DisplayName("Should add nothing when there is no bundled copy")
        void testMergeNoResource() throws IOException {
            assertEquals(0, ConfigManagerTest.this.manager.mergeDefaults("does-not-exist.yml"));
        }

        @Test
        @DisplayName("Should persist merged keys to disk")
        void testMergePersists() throws IOException {
            File file = ConfigManagerTest.this.manager.fileFor(NAME);
            assertTrue(file.getParentFile().mkdirs() || file.getParentFile().exists());
            Files.writeString(file.toPath(), "greeting: mine\n", StandardCharsets.UTF_8);

            ConfigManagerTest.this.manager.mergeDefaults(NAME);

            assertTrue(Files.readString(file.toPath(), StandardCharsets.UTF_8).contains("added-later"));
        }
    }

    @Test
    @DisplayName("Should reject a null plugin")
    void testNullPlugin() {
        assertThrows(NullPointerException.class, () -> new ConfigManager(null));
    }
}
