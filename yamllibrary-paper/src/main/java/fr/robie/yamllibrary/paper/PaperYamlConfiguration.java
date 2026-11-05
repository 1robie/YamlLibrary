package fr.robie.yamllibrary.paper;

import fr.robie.yamllibrary.ConfigurationSection;
import fr.robie.yamllibrary.MemorySection;
import fr.robie.yamllibrary.file.YamlConfiguration;
import fr.robie.yamllibrary.paper.serialization.BukkitSerializationAdapter;
import fr.robie.yamllibrary.util.ConfigurationErrors;
import fr.robie.yamllibrary.util.Preconditions;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.Reader;

/**
 * A {@link YamlConfiguration} that understands Bukkit and Adventure types.
 * <p>
 * Creating one installs {@link BukkitSerializationAdapter}, so Bukkit types
 * round-trip in the same on-disk format a vanilla Bukkit
 * {@code YamlConfiguration} uses. The typed getters themselves live on
 * {@link PaperSection}, which this class implements and which every section it
 * creates also implements -- so they work at any depth:
 * <pre>{@code
 * PaperYamlConfiguration config = PaperYamlConfiguration.loadConfiguration(file);
 * ItemStack top = config.getItemStack("reward");
 * ItemStack nested = config.getPaperSection("kits.knight").getItemStack("weapon");
 * }</pre>
 */
public class PaperYamlConfiguration extends YamlConfiguration implements PaperSection {

    static {
        BukkitSerializationAdapter.install();
    }

    /**
     * Creates an empty configuration and installs the Bukkit bridge.
     */
    public PaperYamlConfiguration() {
        super();
        BukkitSerializationAdapter.install();
    }

    /**
     * Creates {@link PaperMemorySection}s, so that every section in this
     * configuration -- at any depth -- carries the Bukkit and Adventure getters.
     */
    @NotNull
    @Override
    protected MemorySection createChildSection(@NotNull ConfigurationSection parent, @NotNull String path) {
        return new PaperMemorySection(parent, path);
    }

    /**
     * Loads a configuration from a file, returning an empty one if it cannot be read.
     *
     * @param file the file to read
     * @return the configuration, never null
     */
    @NotNull
    public static PaperYamlConfiguration loadConfiguration(@NotNull File file) {
        Preconditions.checkNotNull(file, "File cannot be null");

        PaperYamlConfiguration config = new PaperYamlConfiguration();
        try {
            config.load(file);
        } catch (Exception e) {
            ConfigurationErrors.report("Could not load " + file + "; returning an empty configuration", e);
        }
        return config;
    }

    /**
     * Loads a configuration from a reader, returning an empty one if it cannot be read.
     *
     * @param reader the reader to read
     * @return the configuration, never null
     */
    @NotNull
    public static PaperYamlConfiguration loadConfiguration(@NotNull Reader reader) {
        Preconditions.checkNotNull(reader, "Reader cannot be null");

        PaperYamlConfiguration config = new PaperYamlConfiguration();
        try {
            config.load(reader);
        } catch (Exception e) {
            ConfigurationErrors.report("Could not load from reader; returning an empty configuration", e);
        }
        return config;
    }
}
