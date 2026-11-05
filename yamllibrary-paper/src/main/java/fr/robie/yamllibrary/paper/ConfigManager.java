package fr.robie.yamllibrary.paper;

import fr.robie.yamllibrary.InvalidConfigurationException;
import fr.robie.yamllibrary.util.Preconditions;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages a plugin's YAML files: writing defaults out of the jar on first run,
 * keeping them loaded, reloading them, and merging in keys added by a later
 * version of the plugin.
 * <p>
 * A manager handles any number of files, each identified by its path relative to
 * the plugin's data folder:
 * <pre>{@code
 * ConfigManager configs = new ConfigManager(plugin);
 * PaperYamlConfiguration config = configs.get("config.yml");
 * PaperYamlConfiguration messages = configs.get("lang/en.yml");
 * }</pre>
 * Each file is written from the matching jar resource the first time it is
 * requested, if it exists and the file is not on disk yet.
 * <p>
 * <strong>Threading.</strong> This manager's own operations are serialised
 * against each other, so a reload cannot interleave with a save or a migration.
 * The {@link PaperYamlConfiguration} objects it hands out are <em>not</em>
 * thread-safe: reading one while another thread reloads it is a data race. Read
 * config on the main thread, or copy out the values you need before going async.
 */
public class ConfigManager {

    /**
     * The default path a config's version is stored at.
     */
    public static final String VERSION_PATH = "config-version";

    private final Plugin plugin;
    private final Map<String, PaperYamlConfiguration> loaded = new ConcurrentHashMap<>();

    /**
     * Serialises this manager's own file operations against each other, so a
     * reload cannot interleave with a save or a migration.
     * <p>
     * This does not make the {@link PaperYamlConfiguration} objects themselves
     * thread-safe -- see the class documentation.
     */
    private final Object lock = new Object();

    /**
     * Creates a manager for the given plugin.
     *
     * @param plugin the owning plugin
     */
    public ConfigManager(@NotNull Plugin plugin) {
        this.plugin = Preconditions.checkNotNull(plugin, "Plugin cannot be null");
    }

    /**
     * The plugin this manager belongs to.
     *
     * @return the owning plugin
     */
    @NotNull
    public Plugin getPlugin() {
        return this.plugin;
    }

    /**
     * Gets a configuration, loading it on first request.
     * <p>
     * If the file does not exist on disk and the plugin jar contains a resource
     * with the same name, the resource is written out first.
     *
     * @param name the path relative to the plugin's data folder, e.g. {@code config.yml}
     * @return the loaded configuration, never null
     */
    @NotNull
    public PaperYamlConfiguration get(@NotNull String name) {
        Preconditions.checkNotNull(name, "Name cannot be null");

        return this.loaded.computeIfAbsent(name, this::load);
    }

    /**
     * The names of every configuration currently loaded.
     *
     * @return an unmodifiable view
     */
    @NotNull
    public Set<String> names() {
        return Set.copyOf(this.loaded.keySet());
    }

    /**
     * Writes a jar resource into the plugin's data folder, unless the file is
     * already there.
     *
     * @param name the resource path, also the destination path
     * @return true if the file was written by this call
     */
    public boolean saveDefault(@NotNull String name) {
        Preconditions.checkNotNull(name, "Name cannot be null");

        File target = this.fileFor(name);
        if (target.exists()) {
            return false;
        }
        if (this.plugin.getResource(name) == null) {
            return false;
        }

        this.plugin.saveResource(name, false);
        return true;
    }

    /**
     * Re-reads one configuration from disk.
     * <p>
     * The returned instance is the same object callers already hold, so cached
     * references stay valid across a reload.
     *
     * @param name the configuration to reload
     * @return the reloaded configuration
     */
    @NotNull
    public PaperYamlConfiguration reload(@NotNull String name) {
        Preconditions.checkNotNull(name, "Name cannot be null");

        synchronized (this.lock) {
            PaperYamlConfiguration existing = this.loaded.get(name);
            if (existing == null) {
                return this.get(name);
            }

            this.saveDefault(name);
            File file = this.fileFor(name);
            if (file.exists()) {
                try {
                    existing.load(file);
                } catch (IOException | InvalidConfigurationException e) {
                    this.plugin.getLogger().log(Level.SEVERE, "Could not reload " + name, e);
                }
            }
            return existing;
        }
    }

    /**
     * Re-reads every loaded configuration from disk.
     */
    public void reloadAll() {
        for (String name : this.loaded.keySet()) {
            this.reload(name);
        }
    }

    /**
     * Writes one configuration back to disk.
     *
     * @param name the configuration to write
     * @throws IOException if the file could not be written
     */
    public void save(@NotNull String name) throws IOException {
        Preconditions.checkNotNull(name, "Name cannot be null");

        synchronized (this.lock) {
            PaperYamlConfiguration config = this.loaded.get(name);
            if (config != null) {
                config.save(this.fileFor(name));
            }
        }
    }

    /**
     * Writes every loaded configuration back to disk, logging any that fail.
     */
    public void saveAll() {
        for (String name : this.loaded.keySet()) {
            try {
                this.save(name);
            } catch (IOException e) {
                this.plugin.getLogger().log(Level.SEVERE, "Could not save " + name, e);
            }
        }
    }

    /**
     * Copies keys present in the jar's copy of a file but missing from the one on
     * disk, then writes the result back.
     * <p>
     * This is how a plugin picks up options added in a later version without
     * discarding what the server owner has already configured. Existing values and
     * comments on disk are left alone.
     * <p>
     * Added keys land where the bundled file puts them rather than at the end, and
     * keys the user added that the bundled file does not know about are kept, after
     * the bundled ones. Sections count, so an empty section in the bundled file is
     * created in the user's copy. Nothing is written when nothing is added.
     *
     * @param name the configuration to top up
     * @return the number of keys added, sections included
     * @throws IOException if the updated file could not be written
     */
    public int mergeDefaults(@NotNull String name) throws IOException {
        Preconditions.checkNotNull(name, "Name cannot be null");

        PaperYamlConfiguration defaults = this.resource(name);
        if (defaults == null) {
            return 0;
        }

        synchronized (this.lock) {
            PaperYamlConfiguration config = this.get(name);

            List<String> defaultKeys = new ArrayList<>(defaults.getKeys(true));

            int added = 0;
            for (String key : defaultKeys) {
                if (!config.contains(key)) {
                    added++;
                }
            }
            if (added == 0) {
                return 0;
            }

            // Snapshot what the user has before rewriting, so their values and
            // comments survive. Sections are recorded by absence from userValues.
            Map<String, Object> userValues = new LinkedHashMap<>();
            Map<String, List<String>> userComments = new LinkedHashMap<>();
            List<String> userKeys = new ArrayList<>(config.getKeys(true));
            for (String key : userKeys) {
                if (!config.isConfigurationSection(key)) {
                    userValues.put(key, config.get(key));
                }
                userComments.put(key, config.getComments(key));
            }

            // Merged order: the bundled file's order first, then anything the user
            // added that the bundled file does not know about. Writing new keys in
            // the order the plugin author laid them out reads far better than
            // appending them all at the end.
            LinkedHashSet<String> order = new LinkedHashSet<>(defaultKeys);
            order.addAll(userKeys);

            for (String key : new ArrayList<>(config.getKeys(false))) {
                config.set(key, null);
            }

            for (String key : order) {
                if (userValues.containsKey(key)) {
                    config.set(key, userValues.get(key));
                } else if (defaults.contains(key) && !defaults.isConfigurationSection(key)) {
                    config.set(key, defaults.get(key));
                } else {
                    // A section, in either file. Creating it explicitly is what makes
                    // an empty section in the bundled file appear in the user's copy.
                    config.createSection(key);
                }

                List<String> comments = userComments.get(key);
                if (comments == null || comments.isEmpty()) {
                    comments = defaults.getComments(key);
                }
                if (comments != null && !comments.isEmpty()) {
                    config.setComments(key, comments);
                }
            }

            this.save(name);
            return added;
        }
    }

    /**
     * Reads a configuration's version, treating an absent version as 0.
     *
     * @param name        the configuration to read
     * @param versionPath the path the version is stored at
     * @return the version
     */
    public int getVersion(@NotNull String name, @NotNull String versionPath) {
        Preconditions.checkNotNull(versionPath, "Version path cannot be null");

        return this.get(name).getInt(versionPath, 0);
    }

    /**
     * Reads a configuration's version from {@link #VERSION_PATH}.
     *
     * @param name the configuration to read
     * @return the version
     */
    public int getVersion(@NotNull String name) {
        return this.getVersion(name, VERSION_PATH);
    }

    /**
     * Brings a configuration up to date, running each migration in turn.
     * <p>
     * Migrations run against an in-memory copy. Only once every step has
     * succeeded is the version stamped and the file written, so a migration that
     * throws leaves the file on disk exactly as it was.
     * <p>
     * A file already at or beyond {@code targetVersion} is left alone. A gap with
     * no migration to cover it is an error rather than a silent skip -- quietly
     * jumping a version would stamp a file as upgraded when it was not.
     *
     * @param name          the configuration to upgrade
     * @param versionPath   the path the version is stored at
     * @param targetVersion the version to reach
     * @param migrations    the available steps, in any order
     * @return the number of migrations applied
     * @throws IOException              if the upgraded file could not be written
     * @throws IllegalStateException    if a version in the range has no migration
     * @throws IllegalArgumentException if {@code targetVersion} is negative
     */
    public int migrate(@NotNull String name,
                       @NotNull String versionPath,
                       int targetVersion,
                       @NotNull List<ConfigMigration> migrations) throws IOException {
        Preconditions.checkNotNull(name, "Name cannot be null");
        Preconditions.checkNotNull(versionPath, "Version path cannot be null");
        Preconditions.checkNotNull(migrations, "Migrations cannot be null");
        Preconditions.checkArgument(targetVersion >= 0, "Target version cannot be negative");

        synchronized (this.lock) {
        PaperYamlConfiguration config = this.get(name);
        int current = config.getInt(versionPath, 0);
        if (current >= targetVersion) {
            return 0;
        }

        List<ConfigMigration> ordered = new ArrayList<>(migrations);
        ordered.sort(Comparator.comparingInt(ConfigMigration::fromVersion));

        int applied = 0;
        try {
            while (current < targetVersion) {
                ConfigMigration step = null;
                for (ConfigMigration candidate : ordered) {
                    if (candidate.fromVersion() == current) {
                        step = candidate;
                        break;
                    }
                }

                if (step == null) {
                    throw new IllegalStateException("No migration from version " + current
                            + " for " + name + "; cannot reach version " + targetVersion);
                }

                step.apply(config);
                current++;
                applied++;
            }
        } catch (RuntimeException e) {
            // The file was never written, but the cached instance is now half
            // migrated. Put it back to what is on disk so callers holding a
            // reference do not read a partially upgraded config.
            this.reload(name);
            throw e;
        }

        config.set(versionPath, targetVersion);
        this.save(name);

        this.plugin.getLogger().info(
                "Migrated " + name + " to version " + targetVersion + " (" + applied + " step(s))");
        return applied;
        }
    }

    /**
     * Brings a configuration up to date, using {@link #VERSION_PATH}.
     *
     * @param name          the configuration to upgrade
     * @param targetVersion the version to reach
     * @param migrations    the available steps, in any order
     * @return the number of migrations applied
     * @throws IOException if the upgraded file could not be written
     */
    public int migrate(@NotNull String name, int targetVersion, @NotNull List<ConfigMigration> migrations)
            throws IOException {
        return this.migrate(name, VERSION_PATH, targetVersion, migrations);
    }

    /**
     * Reads the jar's copy of a file, without touching what is on disk.
     *
     * @param name the resource path
     * @return the resource's contents, or null if the jar has no such resource
     */
    @Nullable
    public PaperYamlConfiguration resource(@NotNull String name) {
        Preconditions.checkNotNull(name, "Name cannot be null");

        InputStream stream = this.plugin.getResource(name);
        if (stream == null) {
            return null;
        }

        try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return PaperYamlConfiguration.loadConfiguration(reader);
        } catch (IOException e) {
            this.plugin.getLogger().log(Level.SEVERE, "Could not read bundled " + name, e);
            return null;
        }
    }

    /**
     * Drops a configuration from the cache, so the next {@link #get(String)}
     * re-reads it. Unsaved changes are lost.
     *
     * @param name the configuration to forget
     * @return true if it had been loaded
     */
    public boolean unload(@NotNull String name) {
        Preconditions.checkNotNull(name, "Name cannot be null");

        return this.loaded.remove(name) != null;
    }

    /**
     * The file a given name resolves to inside the plugin's data folder.
     *
     * @param name the path relative to the data folder
     * @return the resolved file
     */
    @NotNull
    public File fileFor(@NotNull String name) {
        Preconditions.checkNotNull(name, "Name cannot be null");

        return new File(this.plugin.getDataFolder(), name);
    }

    @NotNull
    private PaperYamlConfiguration load(@NotNull String name) {
        this.saveDefault(name);

        File file = this.fileFor(name);
        if (!file.exists()) {
            return new PaperYamlConfiguration();
        }
        return PaperYamlConfiguration.loadConfiguration(file);
    }
}
