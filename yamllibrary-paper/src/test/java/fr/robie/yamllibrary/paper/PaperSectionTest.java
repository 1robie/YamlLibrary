package fr.robie.yamllibrary.paper;

import fr.robie.yamllibrary.ConfigurationSection;
import fr.robie.yamllibrary.InvalidConfigurationException;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PaperSection Tests")
class PaperSectionTest {

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Nested
    @DisplayName("Section Type Tests")
    class TypeTests {

        @Test
        @DisplayName("Should create Paper sections, not plain ones")
        void testCreateSection() {
            PaperYamlConfiguration config = new PaperYamlConfiguration();

            assertInstanceOf(PaperSection.class, config.createSection("kits"));
        }

        @Test
        @DisplayName("Should return Paper sections from getConfigurationSection")
        void testGetConfigurationSection() {
            PaperYamlConfiguration config = new PaperYamlConfiguration();
            config.createSection("kits");

            assertInstanceOf(PaperSection.class, config.getConfigurationSection("kits"));
        }

        @Test
        @DisplayName("Should keep the type at every depth")
        void testDeepNesting() {
            PaperYamlConfiguration config = new PaperYamlConfiguration();
            config.set("a.b.c.d", "leaf");

            ConfigurationSection deep = config.getConfigurationSection("a.b.c");

            assertInstanceOf(PaperSection.class, deep);
            assertInstanceOf(PaperSection.class, config.getConfigurationSection("a"));
            assertInstanceOf(PaperSection.class, config.getConfigurationSection("a.b"));
        }

        @Test
        @DisplayName("Should keep the type for sections built while loading a file")
        void testSectionsFromLoad() throws InvalidConfigurationException {
            PaperYamlConfiguration config = new PaperYamlConfiguration();
            config.loadFromString("kits:\n  knight:\n    level: 3\n");

            assertInstanceOf(PaperSection.class, config.getConfigurationSection("kits"));
            assertInstanceOf(PaperSection.class, config.getConfigurationSection("kits.knight"));
        }

        @Test
        @DisplayName("Should return null from getPaperSection for a missing path")
        void testGetPaperSectionMissing() {
            PaperYamlConfiguration config = new PaperYamlConfiguration();

            assertNull(config.getPaperSection("absent"));
        }

        @Test
        @DisplayName("Should return null from getPaperSection for a scalar")
        void testGetPaperSectionScalar() {
            PaperYamlConfiguration config = new PaperYamlConfiguration();
            config.set("name", "not a section");

            assertNull(config.getPaperSection("name"));
        }

        @Test
        @DisplayName("Should create on demand via paperSection")
        void testPaperSectionCreates() {
            PaperYamlConfiguration config = new PaperYamlConfiguration();

            PaperSection created = config.paperSection("kits.knight");

            assertNotNull(created);
            assertSame(created, config.getPaperSection("kits.knight"));
        }

        @Test
        @DisplayName("Should return the existing section from paperSection")
        void testPaperSectionReturnsExisting() {
            PaperYamlConfiguration config = new PaperYamlConfiguration();
            PaperSection first = config.paperSection("kits");

            assertSame(first, config.paperSection("kits"));
        }
    }

    @Nested
    @DisplayName("Nested Getter Tests")
    class NestedGetterTests {

        @Test
        @DisplayName("Should read an ItemStack from a nested section")
        void testNestedItemStack() throws InvalidConfigurationException {
            ItemStack sword = new ItemStack(Material.DIAMOND_SWORD, 1);

            PaperYamlConfiguration written = new PaperYamlConfiguration();
            written.set("kits.knight.weapon", sword);

            PaperYamlConfiguration read = new PaperYamlConfiguration();
            read.loadFromString(written.saveToString());

            PaperSection knight = read.getPaperSection("kits.knight");

            assertNotNull(knight);
            assertEquals(sword, knight.getItemStack("weapon"));
        }

        @Test
        @DisplayName("Should read through a chain of sections")
        void testChainedNavigation() throws InvalidConfigurationException {
            PaperYamlConfiguration written = new PaperYamlConfiguration();
            written.set("kits.knight.weapon", new ItemStack(Material.IRON_SWORD));

            PaperYamlConfiguration read = new PaperYamlConfiguration();
            read.loadFromString(written.saveToString());

            ItemStack weapon = read.getPaperSection("kits")
                    .getPaperSection("knight")
                    .getItemStack("weapon");

            assertEquals(new ItemStack(Material.IRON_SWORD), weapon);
        }

        @Test
        @DisplayName("Should read a Vector from a nested section")
        void testNestedVector() throws InvalidConfigurationException {
            Vector offset = new Vector(0.5, 1.0, -0.5);

            PaperYamlConfiguration written = new PaperYamlConfiguration();
            written.set("arena.spawn.offset", offset);

            PaperYamlConfiguration read = new PaperYamlConfiguration();
            read.loadFromString(written.saveToString());

            assertEquals(offset, read.getPaperSection("arena.spawn").getVector("offset"));
        }

        @Test
        @DisplayName("Should read a Material from a nested section")
        void testNestedMaterial() {
            PaperYamlConfiguration config = new PaperYamlConfiguration();
            config.set("arena.floor.block", "diamond_block");

            assertEquals(Material.DIAMOND_BLOCK,
                    config.getPaperSection("arena.floor").getMaterial("block"));
        }

        @Test
        @DisplayName("Should read an item list from a nested section")
        void testNestedItemStackList() throws InvalidConfigurationException {
            List<ItemStack> loot = List.of(
                    new ItemStack(Material.STONE, 1),
                    new ItemStack(Material.DIRT, 2));

            PaperYamlConfiguration written = new PaperYamlConfiguration();
            written.set("kits.knight.loot", loot);

            PaperYamlConfiguration read = new PaperYamlConfiguration();
            read.loadFromString(written.saveToString());

            assertEquals(loot, read.getPaperSection("kits.knight").getItemStackList("loot"));
        }

        @Test
        @DisplayName("Should parse MiniMessage from a nested section")
        void testNestedComponent() {
            PaperYamlConfiguration config = new PaperYamlConfiguration();
            config.set("messages.join.title", "<green>Welcome");

            net.kyori.adventure.text.Component component =
                    config.getPaperSection("messages.join").getComponent("title");

            assertNotNull(component);
            assertEquals("Welcome", net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                    .plainText().serialize(component));
        }

        @Test
        @DisplayName("Should read an enum from a nested section")
        void testNestedEnum() {
            PaperYamlConfiguration config = new PaperYamlConfiguration();
            config.set("kits.knight.mode", "adventure");

            assertEquals(org.bukkit.GameMode.ADVENTURE,
                    config.getPaperSection("kits.knight").getEnum("mode", org.bukkit.GameMode.class));
        }

        @Test
        @DisplayName("Should iterate child sections as Paper sections")
        void testIterateChildren() throws InvalidConfigurationException {
            PaperYamlConfiguration written = new PaperYamlConfiguration();
            written.set("kits.knight.weapon", new ItemStack(Material.IRON_SWORD));
            written.set("kits.archer.weapon", new ItemStack(Material.BOW));

            PaperYamlConfiguration read = new PaperYamlConfiguration();
            read.loadFromString(written.saveToString());

            PaperSection kits = read.getPaperSection("kits");
            assertNotNull(kits);

            int found = 0;
            for (String key : kits.getKeys(false)) {
                PaperSection kit = kits.getPaperSection(key);
                assertNotNull(kit, key);
                assertNotNull(kit.getItemStack("weapon"), key);
                found++;
            }

            assertEquals(2, found);
        }
    }

    @Nested
    @DisplayName("Core Compatibility Tests")
    class CoreCompatibilityTests {

        @Test
        @DisplayName("Should leave core configurations using plain sections")
        void testCoreUnaffected() {
            fr.robie.yamllibrary.file.YamlConfiguration core =
                    new fr.robie.yamllibrary.file.YamlConfiguration();
            core.set("a.b", 1);

            assertFalse(core.getConfigurationSection("a") instanceof PaperSection);
        }
    }
}
