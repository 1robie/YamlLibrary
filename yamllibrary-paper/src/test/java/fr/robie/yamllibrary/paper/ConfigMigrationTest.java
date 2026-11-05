package fr.robie.yamllibrary.paper;

import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Config Migration Tests")
class ConfigMigrationTest {

    private static final String NAME = "versioned.yml";

    private Plugin plugin;
    private ConfigManager manager;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        this.plugin = MockBukkit.createMockPlugin("MigrationTestPlugin");
        this.manager = new ConfigManager(this.plugin);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private void writeFile(String contents) throws IOException {
        File file = this.manager.fileFor(NAME);
        assertTrue(file.getParentFile().mkdirs() || file.getParentFile().exists());
        Files.writeString(file.toPath(), contents, StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("Should treat an absent version as zero")
    void testDefaultVersion() throws IOException {
        this.writeFile("greeting: hello\n");

        assertEquals(0, this.manager.getVersion(NAME));
    }

    @Test
    @DisplayName("Should read an explicit version")
    void testReadVersion() throws IOException {
        this.writeFile("config-version: 3\n");

        assertEquals(3, this.manager.getVersion(NAME));
    }

    @Test
    @DisplayName("Should apply a single migration and stamp the version")
    void testSingleMigration() throws IOException {
        this.writeFile("cooldown: 200\n");

        int applied = this.manager.migrate(NAME, 1, List.of(
                ConfigMigration.of(0, config -> {
                    config.set("cooldown-seconds", config.getInt("cooldown") / 20);
                    config.set("cooldown", null);
                })));

        assertEquals(1, applied);
        assertEquals(10, this.manager.get(NAME).getInt("cooldown-seconds"));
        assertFalse(this.manager.get(NAME).contains("cooldown"));
        assertEquals(1, this.manager.getVersion(NAME));
    }

    @Test
    @DisplayName("Should chain migrations across several versions")
    void testChainedMigrations() throws IOException {
        this.writeFile("value: 1\n");

        int applied = this.manager.migrate(NAME, 3, List.of(
                ConfigMigration.of(0, c -> c.set("value", c.getInt("value") + 10)),
                ConfigMigration.of(1, c -> c.set("value", c.getInt("value") + 100)),
                ConfigMigration.of(2, c -> c.set("value", c.getInt("value") + 1000))));

        assertEquals(3, applied);
        assertEquals(1111, this.manager.get(NAME).getInt("value"));
        assertEquals(3, this.manager.getVersion(NAME));
    }

    @Test
    @DisplayName("Should apply migrations in version order regardless of list order")
    void testOrderIndependent() throws IOException {
        this.writeFile("value: ''\n");

        this.manager.migrate(NAME, 3, List.of(
                ConfigMigration.of(2, c -> c.set("value", c.getString("value") + "c")),
                ConfigMigration.of(0, c -> c.set("value", c.getString("value") + "a")),
                ConfigMigration.of(1, c -> c.set("value", c.getString("value") + "b"))));

        assertEquals("abc", this.manager.get(NAME).getString("value"));
    }

    @Test
    @DisplayName("Should start from the file's current version")
    void testResumesFromCurrentVersion() throws IOException {
        this.writeFile("config-version: 1\nvalue: 1\n");

        int applied = this.manager.migrate(NAME, 2, List.of(
                ConfigMigration.of(0, c -> fail("should not run: file is already at version 1")),
                ConfigMigration.of(1, c -> c.set("value", 99))));

        assertEquals(1, applied);
        assertEquals(99, this.manager.get(NAME).getInt("value"));
    }

    @Test
    @DisplayName("Should do nothing when already at the target version")
    void testAlreadyCurrent() throws IOException {
        this.writeFile("config-version: 2\nvalue: 1\n");

        assertEquals(0, this.manager.migrate(NAME, 2, List.of(
                ConfigMigration.of(1, c -> fail("should not run")))));
    }

    @Test
    @DisplayName("Should do nothing when ahead of the target version")
    void testAheadOfTarget() throws IOException {
        this.writeFile("config-version: 5\n");

        assertEquals(0, this.manager.migrate(NAME, 2, List.of()));
    }

    @Test
    @DisplayName("Should refuse to skip a version with no migration")
    void testMissingMigration() throws IOException {
        this.writeFile("value: 1\n");

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> this.manager.migrate(NAME, 2, List.of(
                        ConfigMigration.of(0, c -> c.set("value", 2)))));

        assertTrue(e.getMessage().contains("No migration from version 1"), e.getMessage());
    }

    @Test
    @DisplayName("Should leave the file untouched when a migration throws")
    void testFileUntouchedOnFailure() throws IOException {
        this.writeFile("value: 1\n");
        this.manager.get(NAME);

        assertThrows(IllegalStateException.class, () -> this.manager.migrate(NAME, 2, List.of(
                ConfigMigration.of(0, c -> c.set("value", 2)),
                ConfigMigration.of(1, c -> {
                    throw new IllegalStateException("migration is broken");
                }))));

        String onDisk = Files.readString(this.manager.fileFor(NAME).toPath(), StandardCharsets.UTF_8);

        assertTrue(onDisk.contains("value: 1"), onDisk);
        assertFalse(onDisk.contains("config-version"), onDisk);
    }

    @Test
    @DisplayName("Should roll the in-memory copy back when a migration throws")
    void testInMemoryRollbackOnFailure() throws IOException {
        this.writeFile("value: 1\n");
        PaperYamlConfiguration config = this.manager.get(NAME);

        assertThrows(IllegalStateException.class, () -> this.manager.migrate(NAME, 2, List.of(
                ConfigMigration.of(0, c -> c.set("value", 2)),
                ConfigMigration.of(1, c -> {
                    throw new IllegalStateException("migration is broken");
                }))));

        // The first step had already run in memory; the rollback must undo it.
        assertEquals(1, config.getInt("value"));
        assertEquals(0, this.manager.getVersion(NAME));
    }

    @Test
    @DisplayName("Should honour a custom version path")
    void testCustomVersionPath() throws IOException {
        this.writeFile("meta:\n  schema: 0\nvalue: 1\n");

        this.manager.migrate(NAME, "meta.schema", 1, List.of(
                ConfigMigration.of(0, c -> c.set("value", 2))));

        assertEquals(1, this.manager.getVersion(NAME, "meta.schema"));
        assertEquals(2, this.manager.get(NAME).getInt("value"));
    }

    @Test
    @DisplayName("Should reject a negative target version")
    void testNegativeTarget() {
        assertThrows(IllegalArgumentException.class, () -> this.manager.migrate(NAME, -1, List.of()));
    }
}
