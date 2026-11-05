package fr.robie.yamllibrary.paper;

import fr.robie.yamllibrary.InvalidConfigurationException;
import fr.robie.yamllibrary.paper.serialization.BukkitSerializationAdapter;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PaperYamlConfiguration Tests")
class PaperYamlConfigurationTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Nested
    @DisplayName("Bukkit Serialization Bridge Tests")
    class BridgeTests {

        @Test
        @DisplayName("Should install the bridge when a configuration is created")
        void testAutoInstall() {
            BukkitSerializationAdapter.uninstall();

            new PaperYamlConfiguration();

            assertTrue(fr.robie.yamllibrary.serialization.SerializationAdapters
                    .adapters().contains(BukkitSerializationAdapter.get()));
        }

        @Test
        @DisplayName("Should round-trip an ItemStack")
        void testItemStackRoundTrip() throws InvalidConfigurationException {
            ItemStack stack = new ItemStack(Material.DIAMOND_SWORD, 3);

            PaperYamlConfiguration written = new PaperYamlConfiguration();
            written.set("reward", stack);

            PaperYamlConfiguration read = new PaperYamlConfiguration();
            read.loadFromString(written.saveToString());

            assertEquals(stack, read.getItemStack("reward"));
        }

        @Test
        @DisplayName("Should write an ItemStack in the Bukkit on-disk format")
        void testItemStackFormat() {
            PaperYamlConfiguration config = new PaperYamlConfiguration();
            config.set("reward", new ItemStack(Material.DIAMOND_SWORD, 3));

            String yaml = config.saveToString();

            assertTrue(yaml.contains("==: org.bukkit.inventory.ItemStack"), yaml);
        }

        @Test
        @DisplayName("Should round-trip a Vector")
        void testVectorRoundTrip() throws InvalidConfigurationException {
            Vector vector = new Vector(1.5, -2.0, 3.25);

            PaperYamlConfiguration written = new PaperYamlConfiguration();
            written.set("offset", vector);

            PaperYamlConfiguration read = new PaperYamlConfiguration();
            read.loadFromString(written.saveToString());

            assertEquals(vector, read.getVector("offset"));
        }

        @Test
        @DisplayName("Should round-trip a Color")
        void testColorRoundTrip() throws InvalidConfigurationException {
            Color color = Color.fromRGB(12, 34, 56);

            PaperYamlConfiguration written = new PaperYamlConfiguration();
            written.set("tint", color);

            PaperYamlConfiguration read = new PaperYamlConfiguration();
            read.loadFromString(written.saveToString());

            assertEquals(color, read.getColor("tint"));
        }

        @Test
        @DisplayName("Should round-trip a Location")
        void testLocationRoundTrip() throws InvalidConfigurationException {
            World world = PaperYamlConfigurationTest.this.server.addSimpleWorld("spawnworld");
            Location location = new Location(world, 1.0, 64.0, -3.0, 90.0f, 45.0f);

            PaperYamlConfiguration written = new PaperYamlConfiguration();
            written.set("spawn", location);

            PaperYamlConfiguration read = new PaperYamlConfiguration();
            read.loadFromString(written.saveToString());

            assertEquals(location, read.getLocation("spawn"));
        }

        @Test
        @DisplayName("Should round-trip Bukkit values nested in a list")
        void testListRoundTrip() throws InvalidConfigurationException {
            List<ItemStack> stacks = List.of(
                    new ItemStack(Material.STONE, 1),
                    new ItemStack(Material.DIRT, 2));

            PaperYamlConfiguration written = new PaperYamlConfiguration();
            written.set("rewards", stacks);

            PaperYamlConfiguration read = new PaperYamlConfiguration();
            read.loadFromString(written.saveToString());

            assertEquals(stacks, read.getItemStackList("rewards"));
        }

        @Test
        @DisplayName("Should skip non-matching entries in a typed list")
        void testTypedListSkipsOthers() {
            PaperYamlConfiguration config = new PaperYamlConfiguration();
            config.set("mixed", List.of(new ItemStack(Material.STONE), "not an item"));

            assertEquals(1, config.getItemStackList("mixed").size());
        }

        @Test
        @DisplayName("Should return an empty list for a missing path")
        void testTypedListMissing() {
            assertTrue(new PaperYamlConfiguration().getItemStackList("nope").isEmpty());
        }

        @Test
        @DisplayName("Should read a config written by vanilla Bukkit")
        void testReadsBukkitWrittenYaml() throws InvalidConfigurationException {
            org.bukkit.configuration.file.YamlConfiguration bukkit =
                    new org.bukkit.configuration.file.YamlConfiguration();
            bukkit.set("reward", new ItemStack(Material.GOLDEN_APPLE, 5));

            PaperYamlConfiguration ours = new PaperYamlConfiguration();
            ours.loadFromString(bukkit.saveToString());

            assertEquals(new ItemStack(Material.GOLDEN_APPLE, 5), ours.getItemStack("reward"));
        }

        @Test
        @DisplayName("Should write a config vanilla Bukkit can read")
        void testWritesBukkitReadableYaml() throws Exception {
            PaperYamlConfiguration ours = new PaperYamlConfiguration();
            ours.set("reward", new ItemStack(Material.GOLDEN_APPLE, 5));

            org.bukkit.configuration.file.YamlConfiguration bukkit =
                    new org.bukkit.configuration.file.YamlConfiguration();
            bukkit.loadFromString(ours.saveToString());

            assertEquals(new ItemStack(Material.GOLDEN_APPLE, 5), bukkit.getItemStack("reward"));
        }
    }

    @Nested
    @DisplayName("Typed Getter Tests")
    class GetterTests {

        @Test
        @DisplayName("Should read a Material by name")
        void testMaterial() {
            PaperYamlConfiguration config = new PaperYamlConfiguration();
            config.set("block", "diamond_block");

            assertEquals(Material.DIAMOND_BLOCK, config.getMaterial("block"));
        }

        @Test
        @DisplayName("Should fall back for an unknown Material")
        void testMaterialDefault() {
            PaperYamlConfiguration config = new PaperYamlConfiguration();
            config.set("block", "not_a_block");

            assertEquals(Material.STONE, config.getMaterial("block", Material.STONE));
            assertNull(config.getMaterial("block"));
        }

        @Test
        @DisplayName("Should read an enum case-insensitively")
        void testEnum() {
            PaperYamlConfiguration config = new PaperYamlConfiguration();
            config.set("mode", "  adventure  ");

            assertEquals(org.bukkit.GameMode.ADVENTURE, config.getEnum("mode", org.bukkit.GameMode.class));
        }

        @Test
        @DisplayName("Should fall back for an unknown enum constant")
        void testEnumDefault() {
            PaperYamlConfiguration config = new PaperYamlConfiguration();
            config.set("mode", "nonsense");

            assertEquals(org.bukkit.GameMode.SURVIVAL,
                    config.getEnum("mode", org.bukkit.GameMode.class, org.bukkit.GameMode.SURVIVAL));
        }

        @Test
        @DisplayName("Should read a NamespacedKey with and without a namespace")
        void testNamespacedKey() {
            PaperYamlConfiguration config = new PaperYamlConfiguration();
            config.set("bare", "my_key");
            config.set("full", "myplugin:my_key");

            assertEquals(NamespacedKey.minecraft("my_key"), config.getNamespacedKey("bare"));
            assertEquals("myplugin", config.getNamespacedKey("full").getNamespace());
        }

        @Test
        @DisplayName("Should read a World by name")
        void testWorldByName() {
            PaperYamlConfigurationTest.this.server.addSimpleWorld("arena");

            PaperYamlConfiguration config = new PaperYamlConfiguration();
            config.set("world", "arena");

            assertNotNull(config.getWorld("world"));
            assertEquals("arena", config.getWorld("world").getName());
        }

        @Test
        @DisplayName("Should read a World by UUID")
        void testWorldByUuid() {
            World world = PaperYamlConfigurationTest.this.server.addSimpleWorld("arena");

            PaperYamlConfiguration config = new PaperYamlConfiguration();
            config.set("world", world.getUID().toString());

            assertEquals(world, config.getWorld("world"));
        }

        @Test
        @DisplayName("Should return null for an unknown World")
        void testWorldMissing() {
            PaperYamlConfiguration config = new PaperYamlConfiguration();
            config.set("world", "nowhere");

            assertNull(config.getWorld("world"));
            assertNull(config.getWorld("absent"));
        }
    }

    @Nested
    @DisplayName("Adventure Tests")
    class AdventureTests {

        @Test
        @DisplayName("Should parse MiniMessage")
        void testComponent() {
            PaperYamlConfiguration config = new PaperYamlConfiguration();
            config.set("title", "<red>Hello</red>");

            net.kyori.adventure.text.Component component = config.getComponent("title");

            assertNotNull(component);
            assertEquals("Hello", net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                    .plainText().serialize(component));
        }

        @Test
        @DisplayName("Should return null for a missing component")
        void testComponentMissing() {
            assertNull(new PaperYamlConfiguration().getComponent("absent"));
        }

        @Test
        @DisplayName("Should parse a MiniMessage list")
        void testComponentList() {
            PaperYamlConfiguration config = new PaperYamlConfiguration();
            config.set("lore", List.of("<gray>one", "<gray>two"));

            assertEquals(2, config.getComponentList("lore").size());
        }

        @Test
        @DisplayName("Should resolve placeholder tags")
        void testComponentWithResolver() {
            PaperYamlConfiguration config = new PaperYamlConfiguration();
            config.set("join", "<green>Welcome, <player>!");

            net.kyori.adventure.text.Component component = config.getComponent("join",
                    net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed("player", "Robie"));

            assertEquals("Welcome, Robie!", net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                    .plainText().serialize(component));
        }

        @Test
        @DisplayName("Should resolve placeholder tags across a list")
        void testComponentListWithResolver() {
            PaperYamlConfiguration config = new PaperYamlConfiguration();
            config.set("lore", List.of("<gray>Owner: <player>", "<gray>Tier: <tier>"));

            List<net.kyori.adventure.text.Component> lore = config.getComponentList("lore",
                    net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed("player", "Robie"),
                    net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed("tier", "gold"));

            assertEquals("Owner: Robie", net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                    .plainText().serialize(lore.getFirst()));
            assertEquals("Tier: gold", net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                    .plainText().serialize(lore.get(1)));
        }

        @Test
        @DisplayName("Should leave an unresolved tag alone when no resolver is given")
        void testComponentWithoutResolver() {
            PaperYamlConfiguration config = new PaperYamlConfiguration();
            config.set("join", "Welcome, <player>!");

            assertEquals("Welcome, <player>!", net.kyori.adventure.text.serializer.plain
                    .PlainTextComponentSerializer.plainText().serialize(config.getComponent("join")));
        }

        @Test
        @DisplayName("Should resolve tags from a nested section")
        void testNestedComponentWithResolver() {
            PaperYamlConfiguration config = new PaperYamlConfiguration();
            config.set("messages.join.title", "<player> joined");

            net.kyori.adventure.text.Component component = config.getPaperSection("messages.join")
                    .getComponent("title",
                            net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed("player", "Robie"));

            assertEquals("Robie joined", net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                    .plainText().serialize(component));
        }

        @Test
        @DisplayName("Should parse legacy colour codes")
        void testLegacyComponent() {
            PaperYamlConfiguration config = new PaperYamlConfiguration();
            config.set("title", "&cHello");

            net.kyori.adventure.text.Component component = config.getLegacyComponent("title");

            assertNotNull(component);
            assertEquals("Hello", net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                    .plainText().serialize(component));
        }
    }
}
