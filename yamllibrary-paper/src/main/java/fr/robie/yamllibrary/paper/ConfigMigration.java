package fr.robie.yamllibrary.paper;

import org.jetbrains.annotations.NotNull;

/**
 * One step in upgrading a config file from an older layout to a newer one.
 * <p>
 * {@link ConfigManager#mergeDefaults(String)} handles keys that were simply
 * added. A migration is for the changes it cannot infer: a key that moved, a
 * value whose units changed, an option that split into two.
 * <p>
 * Each migration declares the version it upgrades <em>from</em>, and is
 * understood to produce {@code fromVersion() + 1}. {@link ConfigManager} chains
 * them, so a file three versions behind runs three migrations in order.
 * <pre>{@code
 * ConfigMigration renameCooldown = ConfigMigration.of(1, config -> {
 *     config.set("cooldown-seconds", config.getInt("cooldown") / 20);
 *     config.set("cooldown", null);
 * });
 * }</pre>
 */
@FunctionalInterface
public interface ConfigMigration {

    /**
     * Builds a migration from a version number and an action.
     *
     * @param fromVersion the version this upgrades from
     * @param action      what to change
     * @return the migration
     */
    @NotNull
    static ConfigMigration of(int fromVersion, @NotNull Action action) {
        return new ConfigMigration() {
            @Override
            public int fromVersion() {
                return fromVersion;
            }

            @Override
            public void apply(@NotNull PaperYamlConfiguration config) {
                action.apply(config);
            }
        };
    }

    /**
     * The version this migration upgrades from. Applying it yields
     * {@code fromVersion() + 1}.
     *
     * @return the source version
     */
    default int fromVersion() {
        return 0;
    }

    /**
     * Rewrites the configuration in place.
     * <p>
     * Anything thrown here aborts the migration run, leaving the file on disk
     * untouched so a broken migration cannot destroy a server's config.
     *
     * @param config the configuration to change
     */
    void apply(@NotNull PaperYamlConfiguration config);

    /**
     * The body of a migration, for use with {@link #of(int, Action)}.
     */
    @FunctionalInterface
    interface Action {

        /**
         * Rewrites the configuration in place.
         *
         * @param config the configuration to change
         */
        void apply(@NotNull PaperYamlConfiguration config);
    }
}
