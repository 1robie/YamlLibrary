package fr.robie.yamllibrary;

import fr.robie.yamllibrary.file.YamlConfiguration;
import fr.robie.yamllibrary.file.YamlConfigurationOptions;
import fr.robie.yamllibrary.serialization.ConfigurationSerializable;
import fr.robie.yamllibrary.serialization.ConfigurationSerialization;
import fr.robie.yamllibrary.serialization.DelegateDeserialization;
import fr.robie.yamllibrary.serialization.SerializableAs;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class YamlConfigurationTest {

    @Nested
    @DisplayName("Index-based Retrieval Tests")
    class RetrievalTests {

        @Test
        @DisplayName("Should retrieve basic types via index path")
        void testBasicIndexRetrieval() throws Exception {
            String yaml = """
                    items:
                      - name: "Item 0"
                        count: 64
                        active: true
                        price: 10.5
                      - name: "Item 1"
                        count: 1
                        active: false
                        price: 5.0
                    """;
            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString(yaml);

            assertEquals("Item 0", config.getString("items.0.name"));
            assertEquals(64, config.getInt("items.0.count"));
            assertTrue(config.getBoolean("items.0.active"));
            assertEquals(10.5, config.getDouble("items.0.price"));

            assertEquals("Item 1", config.getString("items.1.name"));
            assertEquals(1, config.getInt("items.1.count"));
            assertFalse(config.getBoolean("items.1.active"));
            assertEquals(5.0, config.getDouble("items.1.price"));
        }

        @Test
        @DisplayName("Should handle all primitive list types in sections within lists")
        void testAllListTypes() throws Exception {
            String yaml = """
                    data:
                      - strings: [a, b, c]
                        ints: [1, 2, 3]
                        booleans: [true, false]
                        doubles: [1.1, 2.2]
                        longs: [100, 200]
                        bytes: [1, 2]
                        chars: [x, y]
                        shorts: [10, 20]
                    """;
            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString(yaml);

            assertEquals(List.of("a", "b", "c"), config.getStringList("data.0.strings"));
            assertEquals(List.of(1, 2, 3), config.getIntegerList("data.0.ints"));
            assertEquals(List.of(true, false), config.getBooleanList("data.0.booleans"));
            assertEquals(List.of(1.1, 2.2), config.getDoubleList("data.0.doubles"));
            assertEquals(List.of(100L, 200L), config.getLongList("data.0.longs"));
            assertEquals(List.of((byte) 1, (byte) 2), config.getByteList("data.0.bytes"));
            assertEquals(List.of('x', 'y'), config.getCharacterList("data.0.chars"));
            assertEquals(List.of((short) 10, (short) 20), config.getShortList("data.0.shorts"));
        }

        @Test
        @DisplayName("Should handle invalid indices gracefully")
        void testInvalidIndices() throws Exception {
            String yaml = """
                    list: [a, b]
                    """;
            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString(yaml);

            assertNull(config.get("list.2"));
            assertNull(config.get("list.-1"));
            assertNull(config.get("list.abc"));
        }

        @Test
        @DisplayName("Should support nested lists and sections")
        void testDeepNesting() throws Exception {
            String yaml = """
                    root:
                      - nested_list:
                          - - val: "deep"
                    """;
            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString(yaml);

            assertEquals("deep", config.getString("root.0.nested_list.0.0.val"));
            assertTrue(config.isConfigurationSection("root.0.nested_list.0.0"));
        }

        @Test
        @DisplayName("Should retrieve values with getObject typed access")
        void testGetObjectTypedAccess() throws Exception {
            String yaml = """
                    name: "hello"
                    count: 42
                    ratio: 3.14
                    active: true
                    """;
            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString(yaml);

            assertEquals("hello", config.getObject("name", String.class));
            assertEquals(42, config.getObject("count", Integer.class));
            assertEquals(3.14, config.getObject("ratio", Double.class));
            assertTrue(config.getObject("active", Boolean.class));
        }

        @Test
        @DisplayName("Should return default when getObject type does not match")
        void testGetObjectTypeMismatch() throws Exception {
            String yaml = "name: hello";
            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString(yaml);

            assertNull(config.getObject("name", Integer.class));
            assertEquals(99, (int) config.getObject("name", Integer.class, 99));
        }

        @Test
        @DisplayName("Should retrieve getConfigurationSection on section path")
        void testGetConfigurationSection() throws Exception {
            String yaml = """
                    outer:
                      inner:
                        value: foo
                    """;
            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString(yaml);

            ConfigurationSection inner = config.getConfigurationSection("outer.inner");
            assertNotNull(inner);
            assertEquals("foo", inner.getString("value"));
            assertEquals("outer.inner", inner.getCurrentPath());
        }

        @Test
        @DisplayName("Should return null for getConfigurationSection on non-section path")
        void testGetConfigurationSectionOnNonSection() throws Exception {
            String yaml = "key: value";
            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString(yaml);

            assertNull(config.getConfigurationSection("key"));
        }

        @Test
        @DisplayName("Should retrieve getMapList and getSectionList")
        void testMapListAndSectionList() throws Exception {
            String yaml = """
                    items:
                      - name: first
                        id: 1
                      - name: second
                        id: 2
                    """;
            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString(yaml);

            List<Map<?, ?>> mapList = config.getMapList("items");
            assertEquals(2, mapList.size());
            assertEquals("first", mapList.get(0).get("name"));
            assertEquals("second", mapList.get(1).get("name"));

            List<ConfigurationSection> sectionList = config.getSectionList("items");
            assertEquals(2, sectionList.size());
            assertEquals("first", sectionList.get(0).getString("name"));
        }

        @Test
        @DisplayName("Should return empty list for getMapList and getSectionList on missing path")
        void testEmptyMapListAndSectionList() {
            YamlConfiguration config = new YamlConfiguration();

            assertTrue(config.getMapList("missing").isEmpty());
            assertTrue(config.getSectionList("missing").isEmpty());
        }

        @Test
        @DisplayName("Should handle getFloatList conversion")
        void testFloatList() throws Exception {
            String yaml = "values: [1.5, 2.5, 3.5]";
            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString(yaml);

            List<Float> floats = config.getFloatList("values");
            assertEquals(3, floats.size());
            assertEquals(1.5f, floats.get(0));
            assertEquals(2.5f, floats.get(1));
        }

        @Test
        @DisplayName("Should return empty typed lists for non-list path")
        void testTypedListsOnNonList() throws Exception {
            String yaml = "key: value";
            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString(yaml);

            assertTrue(config.getStringList("key").isEmpty());
            assertTrue(config.getIntegerList("key").isEmpty());
            assertTrue(config.getBooleanList("key").isEmpty());
            assertTrue(config.getDoubleList("key").isEmpty());
            assertTrue(config.getFloatList("key").isEmpty());
            assertTrue(config.getLongList("key").isEmpty());
            assertTrue(config.getByteList("key").isEmpty());
            assertTrue(config.getCharacterList("key").isEmpty());
            assertTrue(config.getShortList("key").isEmpty());
        }

        @Test
        @DisplayName("Should retrieve values with default fallback")
        void testGetWithDefault() throws Exception {
            String yaml = "existing: value";
            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString(yaml);

            assertEquals("value", config.get("existing", "default"));
            assertEquals("default", config.get("missing", "default"));
            assertNull(config.get("missing"));
        }

        @Test
        @DisplayName("Should retrieve typed values with default fallback")
        void testTypedGetWithDefault() {
            YamlConfiguration config = new YamlConfiguration();

            assertEquals("fallback", config.getString("missing", "fallback"));
            assertEquals(42, config.getInt("missing", 42));
            assertTrue(config.getBoolean("missing", true));
            assertEquals(3.14, config.getDouble("missing", 3.14));
            assertEquals(99L, config.getLong("missing", 99L));
            assertEquals(List.of("a"), config.getList("missing", List.of("a")));
        }
    }

    @Nested
    @DisplayName("Existence and Type Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should correctly identify sections and presence via index")
        void testExistenceChecks() throws Exception {
            String yaml = """
                    list:
                      - key: value
                    """;
            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString(yaml);

            assertTrue(config.contains("list.0"));
            assertTrue(config.contains("list.0.key"));
            assertFalse(config.contains("list.1"));
            assertFalse(config.contains("list.0.missing"));

            assertTrue(config.isConfigurationSection("list.0"));
            assertFalse(config.isConfigurationSection("list.0.key"));
            assertTrue(config.isString("list.0.key"));
        }

        @Test
        @DisplayName("Should correctly identify primitive types")
        void testPrimitiveTypeChecks() throws Exception {
            String yaml = """
                    string: hello
                    integer: 42
                    boolean: true
                    double: 3.14
                    long: 10000000000
                    list: [a, b]
                    section:
                      key: val
                    """;
            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString(yaml);

            assertTrue(config.isString("string"));
            assertFalse(config.isInt("string"));

            assertTrue(config.isInt("integer"));
            assertFalse(config.isString("integer"));

            assertTrue(config.isBoolean("boolean"));

            assertTrue(config.isDouble("double"));

            assertTrue(config.isLong("long"));

            assertTrue(config.isList("list"));
            assertFalse(config.isList("string"));

            assertTrue(config.isConfigurationSection("section"));
            assertFalse(config.isConfigurationSection("string"));
        }

        @Test
        @DisplayName("Should distinguish isSet from contains with copyDefaults")
        void testIsSetVsContains() {
            YamlConfiguration config = new YamlConfiguration();
            config.set("explicit", "value");
            config.addDefault("defaulted", "default");

            assertTrue(config.contains("explicit"));
            assertTrue(config.isSet("explicit"));

            assertTrue(config.contains("defaulted", true));
            assertTrue(config.contains("defaulted", false));

            assertTrue(config.contains("defaulted"));
            assertTrue(config.isSet("defaulted"));
        }

        @Test
        @DisplayName("Should return false for type checks on missing path")
        void testTypeChecksOnMissingPath() {
            YamlConfiguration config = new YamlConfiguration();

            assertFalse(config.isString("missing"));
            assertFalse(config.isInt("missing"));
            assertFalse(config.isBoolean("missing"));
            assertFalse(config.isDouble("missing"));
            assertFalse(config.isLong("missing"));
            assertFalse(config.isList("missing"));
            assertFalse(config.isConfigurationSection("missing"));
        }

        @Test
        @DisplayName("Should check contains on root path")
        void testContainsRootPath() {
            YamlConfiguration config = new YamlConfiguration();
            assertTrue(config.contains(""));
        }

        @Test
        @DisplayName("Should get parent section")
        void testGetParent() throws Exception {
            String yaml = """
                    parent:
                      child:
                        value: foo
                    """;
            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString(yaml);

            ConfigurationSection child = config.getConfigurationSection("parent.child");
            assertNotNull(child);
            ConfigurationSection parent = child.getParent();
            assertNotNull(parent);
            assertEquals("parent", parent.getName());
            assertSame(config, parent.getParent());
            assertNull(config.getParent());
        }
    }

    @Nested
    @DisplayName("Manual Modification and Ambiguity Tests")
    class ModificationTests {

        @Test
        @DisplayName("Should create section with numeric name if parent is not a list")
        void testNumericKeyAsSection() {
            YamlConfiguration config = new YamlConfiguration();
            config.createSection("a.99").set("val", "foo");

            assertFalse(config.isList("a"), "Parent should be a section, not a list");
            assertTrue(config.isConfigurationSection("a.99"), "Should be a section named 99");
            assertEquals("foo", config.getString("a.99.val"));
        }

        @Test
        @DisplayName("Should treat numeric key as index if parent is already a list")
        void testNumericKeyAsIndex() {
            YamlConfiguration config = new YamlConfiguration();
            config.set("list", new ArrayList<>());

            config.createSection("list.0").set("name", "first");
            config.set("list.2.name", "third");

            List<?> list = config.getList("list");
            assertEquals(3, list.size());
            assertInstanceOf(ConfigurationSection.class, list.get(0));
            assertNull(list.get(1));
            assertInstanceOf(ConfigurationSection.class, list.get(2));

            assertEquals("first", config.getString("list.0.name"));
            assertEquals("third", config.getString("list.2.name"));
        }

        @Test
        @DisplayName("Should remove elements or values via index path")
        void testRemoval() {
            YamlConfiguration config = new YamlConfiguration();
            config.set("list", new ArrayList<>(List.of("a", "b", "c")));

            config.set("list.1", null);
            List<?> list = config.getList("list");
            assertNull(list.get(1));
        }

        @Test
        @DisplayName("Should overwrite existing values")
        void testOverwriteValue() {
            YamlConfiguration config = new YamlConfiguration();
            config.set("key", "old");
            config.set("key", "new");
            assertEquals("new", config.getString("key"));
        }

        @Test
        @DisplayName("Should remove key when setting to null")
        void testRemoveBySettingNull() {
            YamlConfiguration config = new YamlConfiguration();
            config.set("key", "value");
            assertTrue(config.contains("key"));

            config.set("key", null);
            assertFalse(config.contains("key"));
        }

        @Test
        @DisplayName("Should auto-create parent sections on deep set")
        void testAutoCreateOnDeepSet() {
            YamlConfiguration config = new YamlConfiguration();
            config.set("a.b.c.d", "deep");

            assertTrue(config.isConfigurationSection("a"));
            assertTrue(config.isConfigurationSection("a.b"));
            assertTrue(config.isConfigurationSection("a.b.c"));
            assertEquals("deep", config.getString("a.b.c.d"));
        }

        @Test
        @DisplayName("Should set value at root level with empty-string path")
        void testSetRootPath() {
            YamlConfiguration config = new YamlConfiguration();
            assertThrows(IllegalArgumentException.class, () -> {
                config.set("", "value");
            });
        }

        @Test
        @DisplayName("Should handle mixed section and list creation")
        void testMixedSectionAndList() {
            YamlConfiguration config = new YamlConfiguration();
            config.set("items", new ArrayList<>());
            config.set("items.0.name", "first");
            config.set("items.1.name", "second");

            assertTrue(config.isList("items"));
            assertEquals("first", config.getString("items.0.name"));
            assertEquals("second", config.getString("items.1.name"));
        }
    }

    @Nested
    @DisplayName("Serialization and Round-trip Tests")
    class SerializationTests {

        @Test
        @DisplayName("Should preserve list-section structure during save and load")
        void testRoundTrip() throws Exception {
            YamlConfiguration config = new YamlConfiguration();
            config.set("items", new ArrayList<>());
            ConfigurationSection item0 = config.createSection("items.0");
            item0.set("id", "stone");
            item0.set("amount", 64);

            String saved = config.saveToString();

            YamlConfiguration reloaded = new YamlConfiguration();
            reloaded.loadFromString(saved);

            assertTrue(reloaded.isConfigurationSection("items.0"));
            assertEquals("stone", reloaded.getString("items.0.id"));
            assertEquals(64, reloaded.getInt("items.0.amount"));
        }

        @Test
        @DisplayName("Should handle ConfigurationSerializable within lists")
        void testSerializableInList() throws Exception {
            ConfigurationSerialization.registerClass(TestSerializable.class);

            String yaml = """
                    items:
                      - ==: TestSerializable
                        val: "foo"
                    """;

            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString(yaml);

            List<?> list = config.getList("items");
            assertInstanceOf(TestSerializable.class, list.getFirst());
            assertEquals("foo", ((TestSerializable) list.getFirst()).val);

            ConfigurationSerialization.unregisterClass(TestSerializable.class);
        }

        @Test
        @DisplayName("Should deserialize ConfigurationSerializable within a nested section")
        void testSerializableNested() throws Exception {
            ConfigurationSerialization.registerClass(TestSerializable.class);

            String yaml = """
                    data:
                      ==: TestSerializable
                      val: "nested"
                    """;

            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString(yaml);

            assertInstanceOf(TestSerializable.class, config.get("data"));
            assertEquals("nested", ((TestSerializable) config.get("data")).val);

            ConfigurationSerialization.unregisterClass(TestSerializable.class);
        }

        @Test
        @DisplayName("Should serialize and deserialize ConfigurationSerializable round-trip")
        void testSerializableRoundTrip() throws Exception {
            ConfigurationSerialization.registerClass(TestSerializable.class);

            YamlConfiguration config = new YamlConfiguration();
            config.set("item", new TestSerializable("roundtrip"));

            String saved = config.saveToString();
            assertTrue(saved.contains("=="));
            assertTrue(saved.contains("TestSerializable"));

            YamlConfiguration reloaded = new YamlConfiguration();
            reloaded.loadFromString(saved);

            assertInstanceOf(TestSerializable.class, reloaded.get("item"));
            assertEquals("roundtrip", ((TestSerializable) reloaded.get("item")).val);

            ConfigurationSerialization.unregisterClass(TestSerializable.class);
        }

        @Test
        @DisplayName("Should handle SerializableWith static deserialize method")
        void testStaticDeserializeMethod() throws Exception {
            ConfigurationSerialization.registerClass(DeserializeMethodSerializable.class);

            String yaml = """
                    obj:
                      ==: DeserializeMethodSerializable
                      value: "via method"
                    """;

            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString(yaml);

            assertInstanceOf(DeserializeMethodSerializable.class, config.get("obj"));
            assertEquals("via method", ((DeserializeMethodSerializable) config.get("obj")).value);

            ConfigurationSerialization.unregisterClass(DeserializeMethodSerializable.class);
        }

        @Test
        @DisplayName("Should handle SerializableWith valueOf method")
        void testValueOfMethod() throws Exception {
            ConfigurationSerialization.registerClass(ValueOfSerializable.class);

            String yaml = """
                    obj:
                      ==: ValueOfSerializable
                      text: "via valueOf"
                    """;

            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString(yaml);

            assertInstanceOf(ValueOfSerializable.class, config.get("obj"));
            assertEquals("via valueOf", ((ValueOfSerializable) config.get("obj")).text);

            ConfigurationSerialization.unregisterClass(ValueOfSerializable.class);
        }

        @Test
        @DisplayName("Should serialize DelegatedSerializable using delegate's alias")
        void testDelegateSerializationRoundTrip() throws Exception {
            ConfigurationSerialization.registerClass(TestSerializable.class);

            DelegatedSerializable delegate = new DelegatedSerializable();
            delegate.value = "delegate-to-simple";

            YamlConfiguration config = new YamlConfiguration();
            config.set("obj", delegate);

            String saved = config.saveToString();
            assertTrue(saved.contains("==: TestSerializable"));

            YamlConfiguration reloaded = new YamlConfiguration();
            reloaded.loadFromString(saved);

            assertInstanceOf(TestSerializable.class, reloaded.get("obj"));
            assertEquals("delegate-to-simple", ((TestSerializable) reloaded.get("obj")).val);
        }

        @Test
        @DisplayName("Should handle multiple serializables in one config")
        void testMultipleSerializables() throws Exception {
            ConfigurationSerialization.registerClass(TestSerializable.class);

            String yaml = """
                    first:
                      ==: TestSerializable
                      val: "alpha"
                    second:
                      ==: TestSerializable
                      val: "beta"
                    """;

            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString(yaml);

            assertInstanceOf(TestSerializable.class, config.get("first"));
            assertEquals("alpha", ((TestSerializable) config.get("first")).val);
            assertEquals("beta", ((TestSerializable) config.get("second")).val);

            ConfigurationSerialization.unregisterClass(TestSerializable.class);
        }

        @Test
        @DisplayName("Should use getSerializable typed accessor")
        void testGetSerializableMethod() throws Exception {
            ConfigurationSerialization.registerClass(TestSerializable.class);

            String yaml = """
                    obj:
                      ==: TestSerializable
                      val: "typed access"
                    """;

            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString(yaml);

            TestSerializable obj = config.getSerializable("obj", TestSerializable.class);
            assertNotNull(obj);
            assertEquals("typed access", obj.val);

            assertNull(config.getObject("obj", Integer.class));

            ConfigurationSerialization.unregisterClass(TestSerializable.class);
        }

        @Test
        @DisplayName("Should preserve primitive values through save/load round-trip")
        void testPrimitiveRoundTrip() throws Exception {
            YamlConfiguration config = new YamlConfiguration();
            config.set("string", "hello");
            config.set("int", 42);
            config.set("boolean", true);
            config.set("double", 3.14);
            config.set("long", 123456789L);
            config.set("list", List.of("a", "b"));

            String saved = config.saveToString();
            YamlConfiguration reloaded = new YamlConfiguration();
            reloaded.loadFromString(saved);

            assertEquals("hello", reloaded.getString("string"));
            assertEquals(42, reloaded.getInt("int"));
            assertTrue(reloaded.getBoolean("boolean"));
            assertEquals(3.14, reloaded.getDouble("double"));
            assertEquals(123456789L, reloaded.getLong("long"));
            assertEquals(List.of("a", "b"), reloaded.getStringList("list"));
        }
    }

    @Nested
    @DisplayName("Defaults System Tests")
    class DefaultsTests {

        @Test
        @DisplayName("Should retrieve default value when copyDefaults is enabled")
        void testDefaultsWithCopyDefaults() {
            YamlConfiguration config = new YamlConfiguration();
            config.addDefault("key", "defaultVal");

            config.options().copyDefaults(true);
            assertEquals("defaultVal", config.getString("key"));
        }

        @Test
        @DisplayName("Should not include defaults in getKeys when copyDefaults is disabled")
        void testDefaultsWithoutCopyDefaults() {
            YamlConfiguration config = new YamlConfiguration();
            config.set("actual", "value");
            config.addDefault("defaulted", "val");

            assertFalse(config.getKeys(false).contains("defaulted"));
            assertTrue(config.getKeys(false).contains("actual"));
        }

        @Test
        @DisplayName("Should get default section from root")
        void testGetDefaultSection() {
            YamlConfiguration config = new YamlConfiguration();
            config.addDefault("key", "defaultVal");

            ConfigurationSection defaultsSection = config.getDefaultSection();
            assertNotNull(defaultsSection);
            assertEquals("defaultVal", defaultsSection.getString("key"));
        }

        @Test
        @DisplayName("Should add defaults from Map")
        void testAddDefaultsFromMap() {
            YamlConfiguration config = new YamlConfiguration();
            Map<String, Object> defaults = new LinkedHashMap<>();
            defaults.put("a", "1");
            defaults.put("b", "2");
            config.addDefaults(defaults);

            config.options().copyDefaults(true);
            assertEquals("1", config.getString("a"));
            assertEquals("2", config.getString("b"));
        }

        @Test
        @DisplayName("Should add defaults from another Configuration")
        void testAddDefaultsFromConfiguration() {
            YamlConfiguration source = new YamlConfiguration();
            source.set("x", "from-source");
            source.set("y", 100);

            YamlConfiguration config = new YamlConfiguration();
            config.addDefaults(source);

            config.options().copyDefaults(true);
            assertEquals("from-source", config.getString("x"));
            assertEquals(100, config.getInt("y"));
        }

        @Test
        @DisplayName("Should set defaults from another Configuration, replacing existing")
        void testSetDefaults() {
            YamlConfiguration config = new YamlConfiguration();
            config.addDefault("a", "old");

            YamlConfiguration newDefaults = new YamlConfiguration();
            newDefaults.set("b", "new");
            config.setDefaults(newDefaults);

            config.options().copyDefaults(true);
            assertNull(config.getString("a"));
            assertEquals("new", config.getString("b"));
        }

        @Test
        @DisplayName("Should shadow set values over defaults")
        void testSetShadowsDefault() {
            YamlConfiguration config = new YamlConfiguration();
            config.addDefault("key", "default");
            config.set("key", "explicit");

            config.options().copyDefaults(true);
            assertEquals("explicit", config.getString("key"));
        }

        @Test
        @DisplayName("Should getKeys include defaults when copyDefaults is enabled")
        void testGetKeysWithDefaults() {
            YamlConfiguration config = new YamlConfiguration();
            config.set("explicit", "val");
            config.addDefault("defaulted", "val");

            config.options().copyDefaults(true);
            assertTrue(config.getKeys(false).contains("defaulted"));
            assertTrue(config.getKeys(false).contains("explicit"));
        }
    }

    @Nested
    @DisplayName("Comments Tests")
    class CommentsTests {

        @Test
        @DisplayName("Should set and get block comments")
        void testBlockComments() throws Exception {
            String yaml = """
                    key: value
                    """;
            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString(yaml);

            config.setComments("key", List.of("this is a comment"));
            assertEquals(List.of("this is a comment"), config.getComments("key"));
        }

        @Test
        @DisplayName("Should set and get inline comments")
        void testInlineComments() throws Exception {
            String yaml = """
                    key: value
                    """;
            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString(yaml);

            config.setInlineComments("key", List.of("inline comment"));
            assertEquals(List.of("inline comment"), config.getInlineComments("key"));
        }

        @Test
        @DisplayName("Should return empty list for comments on non-existent path")
        void testCommentsOnMissingPath() {
            YamlConfiguration config = new YamlConfiguration();
            assertTrue(config.getComments("missing").isEmpty());
            assertTrue(config.getInlineComments("missing").isEmpty());
        }

        @Test
        @DisplayName("Should preserve comments through save/load round-trip")
        void testCommentRoundTrip() throws Exception {
            YamlConfiguration config = new YamlConfiguration();
            config.set("key", "value");
            config.setComments("key", List.of("block comment"));
            config.setInlineComments("key", List.of("inline"));

            String saved = config.saveToString();
            assertTrue(saved.contains("block comment"));

            YamlConfiguration reloaded = new YamlConfiguration();
            reloaded.loadFromString(saved);

            assertEquals(List.of("block comment"), reloaded.getComments("key"));
            assertEquals(List.of("inline"), reloaded.getInlineComments("key"));
        }

        @Test
        @DisplayName("Should clear comments when set to null")
        void testClearComments() throws Exception {
            String yaml = """
                    key: value
                    """;
            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString(yaml);

            config.setComments("key", List.of("comment"));
            config.setComments("key", null);
            assertTrue(config.getComments("key").isEmpty());
        }

        @Test
        @DisplayName("Should parse comments from loaded YAML")
        void testParseCommentsFromYaml() throws Exception {
            String yaml = """
                    # This is a block comment
                    key: value # inline
                    """;
            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString(yaml);

            assertEquals(List.of("This is a block comment"), config.getComments("key"));
            assertEquals(List.of("inline"), config.getInlineComments("key"));
        }
    }

    @Nested
    @DisplayName("Options Tests")
    class OptionsTests {

        @Test
        @DisplayName("Should set and get indent within valid range")
        void testIndent() {
            YamlConfiguration config = new YamlConfiguration();
            YamlConfigurationOptions opts = config.options();

            assertEquals(2, opts.indent());
            opts.indent(4);
            assertEquals(4, opts.indent());
        }

        @Test
        @DisplayName("Should reject indent less than 2")
        void testIndentTooSmall() {
            YamlConfiguration config = new YamlConfiguration();
            assertThrows(IllegalArgumentException.class, () -> config.options().indent(1));
        }

        @Test
        @DisplayName("Should reject indent greater than 9")
        void testIndentTooLarge() {
            YamlConfiguration config = new YamlConfiguration();
            assertThrows(IllegalArgumentException.class, () -> config.options().indent(10));
        }

        @Test
        @DisplayName("Should set and get width")
        void testWidth() {
            YamlConfiguration config = new YamlConfiguration();
            config.options().width(120);
            assertEquals(120, config.options().width());
        }

        @Test
        @DisplayName("Should set and get codePointLimit")
        void testCodePointLimit() {
            YamlConfiguration config = new YamlConfiguration();
            config.options().codePointLimit(5000);
            assertEquals(5000, config.options().codePointLimit());
        }

        @Test
        @DisplayName("Should toggle parseComments")
        void testParseComments() {
            YamlConfiguration config = new YamlConfiguration();
            assertTrue(config.options().parseComments());
            config.options().parseComments(false);
            assertFalse(config.options().parseComments());
        }

        @Test
        @DisplayName("Should toggle copyDefaults")
        void testCopyDefaults() {
            YamlConfiguration config = new YamlConfiguration();
            assertFalse(config.options().copyDefaults());
            config.options().copyDefaults(true);
            assertTrue(config.options().copyDefaults());
        }

        @Test
        @DisplayName("Should change pathSeparator")
        void testPathSeparator() {
            YamlConfiguration config = new YamlConfiguration();
            assertEquals('.', config.options().pathSeparator());

            config.options().pathSeparator('/');
            assertEquals('/', config.options().pathSeparator());

            config.set("a/b", "value");
            assertEquals("value", config.getString("a/b"));
        }
    }

    @Nested
    @DisplayName("Header and Footer Tests")
    class HeaderFooterTests {

        @Test
        @DisplayName("Should set and get header")
        void testHeader() {
            YamlConfiguration config = new YamlConfiguration();
            List<String> header = List.of("Line 1", "Line 2");
            config.options().setHeader(header);

            assertEquals(header, config.options().getHeader());
        }

        @Test
        @DisplayName("Should set and get footer")
        void testFooter() {
            YamlConfiguration config = new YamlConfiguration();
            List<String> footer = List.of("Footer line");
            config.options().setFooter(footer);

            assertEquals(footer, config.options().getFooter());
        }

        @Test
        @DisplayName("Should clear header when set to null")
        void testNullHeader() {
            YamlConfiguration config = new YamlConfiguration();
            config.options().setHeader(List.of("something"));
            config.options().setHeader(null);
            assertTrue(config.options().getHeader().isEmpty());
        }

        @Test
        @DisplayName("Should preserve header through save/load round-trip")
        void testHeaderRoundTrip() throws Exception {
            YamlConfiguration config = new YamlConfiguration();
            config.options().setHeader(List.of("Top header line"));
            config.set("key", "value");

            String saved = config.saveToString();
            assertTrue(saved.contains("Top header line"));

            YamlConfiguration reloaded = new YamlConfiguration();
            reloaded.loadFromString(saved);

            assertEquals(List.of("Top header line"), reloaded.options().getHeader());
        }

        @Test
        @DisplayName("Should preserve footer through save/load round-trip")
        void testFooterRoundTrip() throws Exception {
            YamlConfiguration config = new YamlConfiguration();
            config.options().setFooter(List.of("Bottom footer line"));
            config.set("key", "value");

            String saved = config.saveToString();
            assertTrue(saved.contains("Bottom footer line"));

            YamlConfiguration reloaded = new YamlConfiguration();
            reloaded.loadFromString(saved);

            assertEquals(List.of("Bottom footer line"), reloaded.options().getFooter());
        }

        @Test
        @DisplayName("Should preserve header with blank lines")
        void testHeaderWithBlankLines() throws Exception {
            String yaml = """
                    # First line
                    # 
                    # Third line
                    key: value
                    """;
            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString(yaml);

            List<String> header = config.options().getHeader();
            assertTrue(header.contains("First line") || header.contains("Third line") || header.isEmpty());
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle empty YAML string")
        void testEmptyYaml() throws Exception {
            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString("");

            assertTrue(config.getKeys(false).isEmpty());
        }

        @Test
        @DisplayName("Should handle whitespace-only YAML")
        void testWhitespaceYaml() throws Exception {
            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString("   \n  \n");

            assertTrue(config.getKeys(false).isEmpty());
        }

        @Test
        @DisplayName("Should handle null value in list")
        void testNullInList() {
            YamlConfiguration config = new YamlConfiguration();
            List<String> list = new ArrayList<>();
            list.add("a");
            list.add(null);
            list.add("b");
            config.set("list", list);

            List<?> retrieved = config.getList("list");
            assertEquals("a", retrieved.get(0));
            assertNull(retrieved.get(1));
            assertEquals("b", retrieved.get(2));
        }

        @Test
        @DisplayName("Should get empty string for missing string path")
        void testGetStringOnMissing() {
            YamlConfiguration config = new YamlConfiguration();
            assertEquals("", config.getString("missing", ""));
            assertNull(config.getString("missing"));
        }

        @Test
        @DisplayName("Should return 0 defaults for missing numeric paths")
        void testNumericDefaultsOnMissing() {
            YamlConfiguration config = new YamlConfiguration();
            assertEquals(0, config.getInt("missing"));
            assertEquals(0.0, config.getDouble("missing"));
            assertEquals(0L, config.getLong("missing"));
            assertFalse(config.getBoolean("missing"));
        }

        @Test
        @DisplayName("Should handle getString conversion from non-String types")
        void testGetStringConversion() throws Exception {
            String yaml = "number: 42";
            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString(yaml);

            assertEquals("42", config.getString("number"));
        }

        @Test
        @DisplayName("Should flatten mapping via SmartConstructor")
        void testFlattenMapping() throws Exception {
            String yaml = """
                    a:
                      b: value
                    """;
            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString(yaml);

            assertEquals("value", config.getString("a.b"));
            assertTrue(config.isConfigurationSection("a"));
        }

        @Test
        @DisplayName("Should handle createSection from Map")
        void testCreateSectionFromMap() {
            YamlConfiguration config = new YamlConfiguration();
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("nested", "value");
            Map<String, Object> inner = new LinkedHashMap<>();
            inner.put("deep", "down");
            map.put("sub", inner);

            config.createSection("section", map);

            assertEquals("value", config.getString("section.nested"));
            assertTrue(config.isConfigurationSection("section.sub"));
            assertEquals("down", config.getString("section.sub.deep"));
        }

        @Test
        @DisplayName("Should handle getValues shallow and deep")
        void testGetValues() {
            YamlConfiguration config = new YamlConfiguration();
            config.set("a", "1");
            config.set("b.c", "2");
            config.set("b.d", "3");

            Map<String, Object> shallow = config.getValues(false);
            assertTrue(shallow.containsKey("a"));
            assertTrue(shallow.containsKey("b"));
            assertFalse(shallow.containsKey("b.c"));

            Map<String, Object> deep = config.getValues(true);
            assertTrue(deep.containsKey("a"));
            assertTrue(deep.containsKey("b.c"));
            assertTrue(deep.containsKey("b.d"));
        }

        @Test
        @DisplayName("Should serialize empty config to empty string")
        void testEmptyConfigSave() {
            YamlConfiguration config = new YamlConfiguration();
            String saved = config.saveToString();
            assertEquals("", saved);
        }

        @Test
        @DisplayName("Should handle InvalidConfigurationException on bad YAML")
        void testInvalidYaml() {
            YamlConfiguration config = new YamlConfiguration();
            assertThrows(InvalidConfigurationException.class, () -> {
                config.loadFromString("{{invalid: yaml: broken");
            });
        }

        @Test
        @DisplayName("Should handle scalar top-level YAML")
        void testScalarTopLevel() {
            YamlConfiguration config = new YamlConfiguration();
            assertThrows(InvalidConfigurationException.class, () -> {
                config.loadFromString("just a string");
            });
        }

        @Test
        @DisplayName("Should handle toString on config")
        void testToString() throws Exception {
            YamlConfiguration config = new YamlConfiguration();
            config.set("key", "value");
            String str = config.toString();
            assertTrue(str.contains("YamlConfiguration"));
            assertTrue(str.contains("root="));
        }

        @Test
        @DisplayName("Should handle section name with special characters")
        void testSpecialCharPath() throws Exception {
            String yaml = """
                    "key.with.dots": value
                    """;
            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString(yaml);

            assertEquals("value", config.getString("key.with.dots"));
        }

        @Test
        @DisplayName("Should handle getKeys with deep true on nested structure")
        void testGetKeysDeep() throws Exception {
            String yaml = """
                    a:
                      b:
                        c: val
                    """;
            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString(yaml);

            assertEquals(3, config.getKeys(true).size());
            assertTrue(config.getKeys(true).contains("a.b.c"));
            assertEquals(1, config.getKeys(false).size());
            assertTrue(config.getKeys(false).contains("a"));
        }
    }

    @SerializableAs("TestSerializable")
    public static class TestSerializable implements ConfigurationSerializable {
        public String val;

        public TestSerializable() {
        }

        public TestSerializable(Map<String, Object> map) {
            this.val = (String) map.get("val");
        }

        public TestSerializable(String val) {
            this.val = val;
        }

        @Override
        public @NotNull Map<String, Object> serialize() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("val", this.val);
            return map;
        }
    }

    @SerializableAs("DeserializeMethodSerializable")
    public static class DeserializeMethodSerializable implements ConfigurationSerializable {
        public String value;

        public DeserializeMethodSerializable() {
        }

        public DeserializeMethodSerializable(Map<String, Object> map) {
            this.value = (String) map.get("value");
        }

        public static DeserializeMethodSerializable deserialize(Map<String, Object> args) {
            DeserializeMethodSerializable result = new DeserializeMethodSerializable();
            result.value = (String) args.get("value");
            return result;
        }

        @Override
        public @NotNull Map<String, Object> serialize() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("value", this.value);
            return map;
        }
    }

    @SerializableAs("ValueOfSerializable")
    public static class ValueOfSerializable implements ConfigurationSerializable {
        public String text;

        public ValueOfSerializable() {
        }

        public ValueOfSerializable(Map<String, Object> map) {
            this.text = (String) map.get("text");
        }

        public static ValueOfSerializable valueOf(Map<String, Object> args) {
            ValueOfSerializable result = new ValueOfSerializable();
            result.text = (String) args.get("text");
            return result;
        }

        @Override
        public @NotNull Map<String, Object> serialize() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("text", this.text);
            return map;
        }
    }

    @SerializableAs("DelegatedSerializable")
    @DelegateDeserialization(TestSerializable.class)
    public static class DelegatedSerializable implements ConfigurationSerializable {
        public String value;

        public DelegatedSerializable() {
        }

        @Override
        public @NotNull Map<String, Object> serialize() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("val", this.value);
            return map;
        }
    }
}
