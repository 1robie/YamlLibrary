package fr.robie.yamllibrary.paper;

import fr.robie.yamllibrary.ConfigurationSection;
import fr.robie.yamllibrary.MemorySection;
import org.jetbrains.annotations.NotNull;

/**
 * A section nested inside a {@link PaperYamlConfiguration}.
 * <p>
 * It is a plain {@link MemorySection} that also carries the {@link PaperSection}
 * getters, and it creates its own children the same way, so the Bukkit and
 * Adventure getters stay available at every depth.
 */
public class PaperMemorySection extends MemorySection implements PaperSection {

    /**
     * Creates a section under the given parent.
     *
     * @param parent the section this one hangs off
     * @param path   this section's path relative to {@code parent}
     */
    public PaperMemorySection(@NotNull ConfigurationSection parent, @NotNull String path) {
        super(parent, path);
    }

    @NotNull
    @Override
    protected MemorySection createChildSection(@NotNull ConfigurationSection parent, @NotNull String path) {
        return new PaperMemorySection(parent, path);
    }
}
