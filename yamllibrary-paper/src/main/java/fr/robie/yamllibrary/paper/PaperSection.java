package fr.robie.yamllibrary.paper;

import fr.robie.yamllibrary.ConfigurationSection;
import fr.robie.yamllibrary.util.Preconditions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.banner.Pattern;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * The Bukkit and Adventure getters, on any section rather than only on the
 * configuration object.
 * <p>
 * Every getter here is written in terms of the plain
 * {@link ConfigurationSection} accessors, so the same code serves the root
 * configuration ({@link PaperYamlConfiguration}) and every section nested inside
 * it ({@link PaperMemorySection}). That is what makes this work:
 * <pre>{@code
 * PaperSection rewards = config.getPaperSection("rewards");
 * ItemStack sword = rewards.getItemStack("sword");
 * }</pre>
 * Sections are created by {@link PaperYamlConfiguration} throughout the tree, so
 * a section at any depth is a {@code PaperSection}.
 */
public interface PaperSection extends ConfigurationSection {

    // ------------------------------------------------------------------
    // Navigation
    // ------------------------------------------------------------------

    /**
     * Gets a nested section, typed so its Bukkit getters are reachable.
     *
     * @param path the path to read
     * @return the section, or null if absent or not a section
     */
    @Nullable
    default PaperSection getPaperSection(@NotNull String path) {
        ConfigurationSection section = this.getConfigurationSection(path);
        return section instanceof PaperSection paper ? paper : null;
    }

    /**
     * Gets a nested section, creating it if it is not there yet.
     *
     * @param path the path to read or create
     * @return the section, never null
     */
    @NotNull
    default PaperSection paperSection(@NotNull String path) {
        PaperSection existing = this.getPaperSection(path);
        if (existing != null) {
            return existing;
        }

        ConfigurationSection created = this.createSection(path);
        if (created instanceof PaperSection paper) {
            return paper;
        }
        throw new IllegalStateException(
                "Section at '" + path + "' is not a PaperSection; was it created by a PaperYamlConfiguration?");
    }

    // ------------------------------------------------------------------
    // Bukkit serializable types
    // ------------------------------------------------------------------

    /**
     * Gets an item stack.
     *
     * @param path the path to read
     * @return the item stack, or null if absent or of another type
     */
    @Nullable
    default ItemStack getItemStack(@NotNull String path) {
        return this.getObject(path, ItemStack.class);
    }

    /**
     * Gets an item stack, falling back to a default.
     *
     * @param path the path to read
     * @param def  the value to return when absent
     * @return the item stack, or {@code def}
     */
    @Contract("_, !null -> !null")
    @Nullable
    default ItemStack getItemStack(@NotNull String path, @Nullable ItemStack def) {
        return this.getObject(path, ItemStack.class, def);
    }

    /**
     * Gets a location.
     *
     * @param path the path to read
     * @return the location, or null if absent or of another type
     */
    @Nullable
    default Location getLocation(@NotNull String path) {
        return this.getObject(path, Location.class);
    }

    /**
     * Gets a location, falling back to a default.
     *
     * @param path the path to read
     * @param def  the value to return when absent
     * @return the location, or {@code def}
     */
    @Contract("_, !null -> !null")
    @Nullable
    default Location getLocation(@NotNull String path, @Nullable Location def) {
        return this.getObject(path, Location.class, def);
    }

    /**
     * Gets a vector.
     *
     * @param path the path to read
     * @return the vector, or null if absent or of another type
     */
    @Nullable
    default Vector getVector(@NotNull String path) {
        return this.getObject(path, Vector.class);
    }

    /**
     * Gets a vector, falling back to a default.
     *
     * @param path the path to read
     * @param def  the value to return when absent
     * @return the vector, or {@code def}
     */
    @Contract("_, !null -> !null")
    @Nullable
    default Vector getVector(@NotNull String path, @Nullable Vector def) {
        return this.getObject(path, Vector.class, def);
    }

    /**
     * Gets a colour.
     *
     * @param path the path to read
     * @return the colour, or null if absent or of another type
     */
    @Nullable
    default Color getColor(@NotNull String path) {
        return this.getObject(path, Color.class);
    }

    /**
     * Gets a colour, falling back to a default.
     *
     * @param path the path to read
     * @param def  the value to return when absent
     * @return the colour, or {@code def}
     */
    @Contract("_, !null -> !null")
    @Nullable
    default Color getColor(@NotNull String path, @Nullable Color def) {
        return this.getObject(path, Color.class, def);
    }

    /**
     * Gets an offline player.
     *
     * @param path the path to read
     * @return the player, or null if absent or of another type
     */
    @Nullable
    default OfflinePlayer getOfflinePlayer(@NotNull String path) {
        return this.getObject(path, OfflinePlayer.class);
    }

    /**
     * Gets a potion effect.
     *
     * @param path the path to read
     * @return the effect, or null if absent or of another type
     */
    @Nullable
    default PotionEffect getPotionEffect(@NotNull String path) {
        return this.getObject(path, PotionEffect.class);
    }

    /**
     * Gets a list of potion effects, skipping any entry that is not one.
     *
     * @param path the path to read
     * @return the effects, never null
     */
    @NotNull
    default List<PotionEffect> getPotionEffectList(@NotNull String path) {
        return this.getTypedList(path, PotionEffect.class);
    }

    /**
     * Gets a firework effect.
     *
     * @param path the path to read
     * @return the effect, or null if absent or of another type
     */
    @Nullable
    default FireworkEffect getFireworkEffect(@NotNull String path) {
        return this.getObject(path, FireworkEffect.class);
    }

    /**
     * Gets a list of firework effects, skipping any entry that is not one.
     *
     * @param path the path to read
     * @return the effects, never null
     */
    @NotNull
    default List<FireworkEffect> getFireworkEffectList(@NotNull String path) {
        return this.getTypedList(path, FireworkEffect.class);
    }

    /**
     * Gets a bounding box.
     *
     * @param path the path to read
     * @return the box, or null if absent or of another type
     */
    @Nullable
    default BoundingBox getBoundingBox(@NotNull String path) {
        return this.getObject(path, BoundingBox.class);
    }

    /**
     * Gets a bounding box, falling back to a default.
     *
     * @param path the path to read
     * @param def  the value to return when absent
     * @return the box, or {@code def}
     */
    @Contract("_, !null -> !null")
    @Nullable
    default BoundingBox getBoundingBox(@NotNull String path, @Nullable BoundingBox def) {
        return this.getObject(path, BoundingBox.class, def);
    }

    /**
     * Gets an attribute modifier.
     *
     * @param path the path to read
     * @return the modifier, or null if absent or of another type
     */
    @Nullable
    default AttributeModifier getAttributeModifier(@NotNull String path) {
        return this.getObject(path, AttributeModifier.class);
    }

    /**
     * Gets a list of attribute modifiers, skipping any entry that is not one.
     *
     * @param path the path to read
     * @return the modifiers, never null
     */
    @NotNull
    default List<AttributeModifier> getAttributeModifierList(@NotNull String path) {
        return this.getTypedList(path, AttributeModifier.class);
    }

    /**
     * Gets a banner pattern.
     *
     * @param path the path to read
     * @return the pattern, or null if absent or of another type
     */
    @Nullable
    default Pattern getPattern(@NotNull String path) {
        return this.getObject(path, Pattern.class);
    }

    /**
     * Gets a list of banner patterns, skipping any entry that is not one.
     *
     * @param path the path to read
     * @return the patterns, never null
     */
    @NotNull
    default List<Pattern> getPatternList(@NotNull String path) {
        return this.getTypedList(path, Pattern.class);
    }

    /**
     * Gets a list of item stacks, skipping any entry that is not one.
     *
     * @param path the path to read
     * @return the item stacks, never null
     */
    @NotNull
    default List<ItemStack> getItemStackList(@NotNull String path) {
        return this.getTypedList(path, ItemStack.class);
    }

    /**
     * Gets a list of locations, skipping any entry that is not one.
     *
     * @param path the path to read
     * @return the locations, never null
     */
    @NotNull
    default List<Location> getLocationList(@NotNull String path) {
        return this.getTypedList(path, Location.class);
    }

    /**
     * Gets a list of values of the given type, skipping entries of any other type.
     *
     * @param path  the path to read
     * @param clazz the element type to keep
     * @param <T>   the element type
     * @return the matching elements, never null
     */
    @NotNull
    default <T> List<T> getTypedList(@NotNull String path, @NotNull Class<T> clazz) {
        Preconditions.checkNotNull(clazz, "Class cannot be null");

        List<?> raw = this.getList(path);
        List<T> result = new ArrayList<>();
        if (raw == null) {
            return result;
        }

        for (Object element : raw) {
            if (clazz.isInstance(element)) {
                result.add(clazz.cast(element));
            }
        }
        return result;
    }

    // ------------------------------------------------------------------
    // Types stored as strings
    // ------------------------------------------------------------------

    /**
     * Gets a world by the name or UUID stored at the path.
     *
     * @param path the path to read
     * @return the world, or null if absent or not loaded
     */
    @Nullable
    default World getWorld(@NotNull String path) {
        String value = this.getString(path);
        if (value == null) {
            return null;
        }

        World byName = Bukkit.getWorld(value);
        if (byName != null) {
            return byName;
        }

        try {
            return Bukkit.getWorld(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Gets a namespaced key, accepting both {@code namespace:key} and a bare key
     * in the Minecraft namespace.
     *
     * @param path the path to read
     * @return the key, or null if absent or malformed
     */
    @Nullable
    default NamespacedKey getNamespacedKey(@NotNull String path) {
        String value = this.getString(path);
        if (value == null) {
            return null;
        }
        return NamespacedKey.fromString(value.toLowerCase(Locale.ROOT));
    }

    /**
     * Gets a material by name or namespaced key.
     *
     * @param path the path to read
     * @return the material, or null if absent or unrecognised
     */
    @Nullable
    default Material getMaterial(@NotNull String path) {
        String value = this.getString(path);
        if (value == null) {
            return null;
        }
        return Material.matchMaterial(value);
    }

    /**
     * Gets a material, falling back to a default.
     *
     * @param path the path to read
     * @param def  the value to return when absent or unrecognised
     * @return the material, or {@code def}
     */
    @Contract("_, !null -> !null")
    @Nullable
    default Material getMaterial(@NotNull String path, @Nullable Material def) {
        Material material = this.getMaterial(path);
        return material != null ? material : def;
    }

    /**
     * Gets an enum constant by name, case-insensitively.
     *
     * @param path  the path to read
     * @param clazz the enum type
     * @param <E>   the enum type
     * @return the constant, or null if absent or unrecognised
     */
    @Nullable
    default <E extends Enum<E>> E getEnum(@NotNull String path, @NotNull Class<E> clazz) {
        Preconditions.checkNotNull(clazz, "Class cannot be null");

        String value = this.getString(path);
        if (value == null) {
            return null;
        }

        try {
            return Enum.valueOf(clazz, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Gets an enum constant, falling back to a default.
     *
     * @param path  the path to read
     * @param clazz the enum type
     * @param def   the value to return when absent or unrecognised
     * @param <E>   the enum type
     * @return the constant, or {@code def}
     */
    @Contract("_, _, !null -> !null")
    @Nullable
    default <E extends Enum<E>> E getEnum(@NotNull String path, @NotNull Class<E> clazz, @Nullable E def) {
        E value = this.getEnum(path, clazz);
        return value != null ? value : def;
    }

    // ------------------------------------------------------------------
    // Adventure
    // ------------------------------------------------------------------

    /**
     * Gets a string parsed as MiniMessage.
     *
     * @param path the path to read
     * @return the component, or null if absent
     * @throws IllegalStateException if Adventure is not on the classpath
     */
    @Nullable
    default Component getComponent(@NotNull String path) {
        ComponentSupport.requireAvailable();

        String value = this.getString(path);
        return value != null ? ComponentSupport.miniMessage(value) : null;
    }

    /**
     * Gets a string parsed as MiniMessage, with placeholders resolved.
     * <p>
     * This is how a config value like {@code "<green>Welcome, <player>!"} gets its
     * {@code <player>} tag filled in:
     * <pre>{@code
     * section.getComponent("join", Placeholder.unparsed("player", name));
     * }</pre>
     *
     * @param path      the path to read
     * @param resolvers the tags to make available while parsing
     * @return the component, or null if absent
     * @throws IllegalStateException if Adventure is not on the classpath
     */
    @Nullable
    default Component getComponent(@NotNull String path, @NotNull TagResolver @NotNull ... resolvers) {
        ComponentSupport.requireAvailable();
        Preconditions.checkNotNull(resolvers, "Resolvers cannot be null");

        String value = this.getString(path);
        return value != null ? ComponentSupport.miniMessage(value, resolvers) : null;
    }

    /**
     * Gets a string list parsed as MiniMessage, with placeholders resolved.
     *
     * @param path      the path to read
     * @param resolvers the tags to make available while parsing
     * @return the components, never null
     * @throws IllegalStateException if Adventure is not on the classpath
     */
    @NotNull
    default List<Component> getComponentList(@NotNull String path, @NotNull TagResolver @NotNull ... resolvers) {
        ComponentSupport.requireAvailable();
        Preconditions.checkNotNull(resolvers, "Resolvers cannot be null");

        List<Component> result = new ArrayList<>();
        for (String line : this.getStringList(path)) {
            result.add(ComponentSupport.miniMessage(line, resolvers));
        }
        return result;
    }

    /**
     * Gets a string parsed as MiniMessage, falling back to a default.
     *
     * @param path the path to read
     * @param def  the value to return when absent
     * @return the component, or {@code def}
     * @throws IllegalStateException if Adventure is not on the classpath
     */
    @Contract("_, !null -> !null")
    @Nullable
    default Component getComponent(@NotNull String path, @Nullable Component def) {
        Component value = this.getComponent(path);
        return value != null ? value : def;
    }

    /**
     * Gets a string list parsed as MiniMessage, one component per line.
     *
     * @param path the path to read
     * @return the components, never null
     * @throws IllegalStateException if Adventure is not on the classpath
     */
    @NotNull
    default List<Component> getComponentList(@NotNull String path) {
        ComponentSupport.requireAvailable();

        List<Component> result = new ArrayList<>();
        for (String line : this.getStringList(path)) {
            result.add(ComponentSupport.miniMessage(line));
        }
        return result;
    }

    /**
     * Gets a string parsed as legacy {@code &}-prefixed colour codes.
     *
     * @param path the path to read
     * @return the component, or null if absent
     * @throws IllegalStateException if Adventure is not on the classpath
     */
    @Nullable
    default Component getLegacyComponent(@NotNull String path) {
        return this.getLegacyComponent(path, '&');
    }

    /**
     * Gets a string parsed as legacy colour codes with the given prefix character.
     *
     * @param path      the path to read
     * @param character the colour code prefix, usually {@code &}
     * @return the component, or null if absent
     * @throws IllegalStateException if Adventure is not on the classpath
     */
    @Nullable
    default Component getLegacyComponent(@NotNull String path, char character) {
        ComponentSupport.requireAvailable();

        String value = this.getString(path);
        return value != null ? ComponentSupport.legacy(value, character) : null;
    }
}
