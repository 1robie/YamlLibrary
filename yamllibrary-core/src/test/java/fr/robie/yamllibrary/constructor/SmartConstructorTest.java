package fr.robie.yamllibrary.constructor;

import fr.robie.yamllibrary.directive.KeyDirectiveRegistry;
import fr.robie.yamllibrary.directive.MergeKeyDirective;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SmartConstructor Tests")
class SmartConstructorTest {

    private Yaml createYaml() {
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setMaxAliasesForCollections(Integer.MAX_VALUE);
        loaderOptions.setCodePointLimit(Integer.MAX_VALUE);
        loaderOptions.setNestingDepthLimit(100);
        SmartConstructor constructor = new SmartConstructor(loaderOptions);
        return new Yaml(constructor);
    }

    @Nested
    @DisplayName("Deep Key Processing Tests")
    class DeepKeyTests {

        @Test
        @DisplayName("Should create nested maps with :: separator")
        @SuppressWarnings("unchecked")
        void testDeepKeyCreatesNestedMaps() {
            Yaml yaml = createYaml();
            String input = "a::b::c: value\n";
            Map<String, Object> result = yaml.load(input);

            assertTrue(result.containsKey("a"));
            Map<String, Object> a = (Map<String, Object>) result.get("a");
            assertTrue(a.containsKey("b"));
            Map<String, Object> b = (Map<String, Object>) a.get("b");
            assertEquals("value", b.get("c"));
        }

        @Test
        @DisplayName("Should create nested lists with numeric segments in :: keys")
        @SuppressWarnings("unchecked")
        void testDeepKeyWithListIndex() {
            Yaml yaml = createYaml();
            String input = "items::0::name: \"first\"\nitems::1::name: \"second\"\n";
            Map<String, Object> result = yaml.load(input);

            assertTrue(result.containsKey("items"));
            List<Object> items = (List<Object>) result.get("items");
            Map<String, Object> item0 = (Map<String, Object>) items.get(0);
            assertEquals("first", item0.get("name"));
            Map<String, Object> item1 = (Map<String, Object>) items.get(1);
            assertEquals("second", item1.get("name"));
        }

        @Test
        @DisplayName("Should create mixed nested structures with :: keys")
        @SuppressWarnings("unchecked")
        void testDeepKeyMixed() {
            Yaml yaml = createYaml();
            String input = "config::database::0::host: \"localhost\"\nconfig::database::0::port: 5432\nconfig::database::1::host: \"backup\"\n";
            Map<String, Object> result = yaml.load(input);

            Map<String, Object> config = (Map<String, Object>) result.get("config");
            List<Object> databases = (List<Object>) config.get("database");
            Map<String, Object> db0 = (Map<String, Object>) databases.get(0);
            assertEquals("localhost", db0.get("host"));
            assertEquals(5432, db0.get("port"));
            Map<String, Object> db1 = (Map<String, Object>) databases.get(1);
            assertEquals("backup", db1.get("host"));
        }

        @Test
        @DisplayName("Should merge values when using same :: key path")
        @SuppressWarnings("unchecked")
        void testDeepKeyMerge() {
            Yaml yaml = createYaml();
            String input = "person::name: \"Alice\"\nperson::age: 30\n";
            Map<String, Object> result = yaml.load(input);

            Map<String, Object> person = (Map<String, Object>) result.get("person");
            assertEquals("Alice", person.get("name"));
            assertEquals(30, person.get("age"));
        }
    }

    @Nested
    @DisplayName("Merge Directive Tests")
    class MergeDirectiveTests {

        @Test
        @DisplayName("Should merge a map via $$merge directive")
        @SuppressWarnings("unchecked")
        void testMergeMap() {
            Yaml yaml = createYaml();
            String input = """
                    defaults: &defaults
                      timeout: 30
                      retries: 3
                    config:
                      url: "example.com"
                      $$merge: *defaults
                    """;
            Map<String, Object> result = yaml.load(input);
            Map<String, Object> config = (Map<String, Object>) result.get("config");

            assertEquals("example.com", config.get("url"));
            assertEquals(30, config.get("timeout"));
            assertEquals(3, config.get("retries"));
        }

        @Test
        @DisplayName("Should merge list of maps via $$merge directive")
        @SuppressWarnings("unchecked")
        void testMergeListOfMaps() {
            Yaml yaml = createYaml();
            String input = """
                    base: &base
                      enabled: true
                    configs:
                      - $$merge: *base
                        name: "service-a"
                      - $$merge: *base
                        name: "service-b"
                    """;
            Map<String, Object> result = yaml.load(input);
            List<Object> configs = (List<Object>) result.get("configs");

            assertEquals(2, configs.size());
            Map<String, Object> c0 = (Map<String, Object>) configs.get(0);
            assertTrue((Boolean) c0.get("enabled"));
            assertEquals("service-a", c0.get("name"));
            Map<String, Object> c1 = (Map<String, Object>) configs.get(1);
            assertEquals("service-b", c1.get("name"));
        }

        @Test
        @DisplayName("Should let explicit values override merged values")
        @SuppressWarnings("unchecked")
        void testMergeOverride() {
            Yaml yaml = createYaml();
            String input = """
                    base: &base
                      timeout: 30
                      url: "default"
                    config:
                      $$merge: *base
                      timeout: 60
                    """;
            Map<String, Object> result = yaml.load(input);
            Map<String, Object> config = (Map<String, Object>) result.get("config");

            assertEquals(60, config.get("timeout"));
            assertEquals("default", config.get("url"));
        }

        @Test
        @DisplayName("Should handle merge into existing maps recursively")
        @SuppressWarnings("unchecked")
        void testMergeRecursive() {
            Yaml yaml = createYaml();
            String input = """
                    base: &base
                      nested:
                        a: 1
                        b: 2
                    config:
                      $$merge: *base
                      nested:
                        b: 3
                        c: 4
                    """;
            Map<String, Object> result = yaml.load(input);
            Map<String, Object> config = (Map<String, Object>) result.get("config");
            Map<String, Object> nested = (Map<String, Object>) config.get("nested");

            assertEquals(1, nested.get("a"));
            assertEquals(3, nested.get("b"));
            assertEquals(4, nested.get("c"));
        }
    }

    @Nested
    @DisplayName("Value Selector Tests")
    class ValueSelectorTests {

        @Test
        @DisplayName("Should select value and include fallback key in result")
        @SuppressWarnings("unchecked")
        void testValueSelectorWithFallback() {
            Yaml yaml = createYaml();
            String input = """
                    setting:
                      $$merge:
                        key: "matched"
                      $$fallback: "fallback"
                    """;
            Map<String, Object> result = yaml.load(input);
            Map<String, Object> setting = (Map<String, Object>) result.get("setting");

            assertEquals("matched", setting.get("key"));
        }
    }

    @Nested
    @DisplayName("Flatten Mapping Tests")
    class FlattenMappingTests {

        @Test
        @DisplayName("Should handle flattened mappings with anchors via YamlConfiguration")
        void testFlattenMapping() throws Exception {
            fr.robie.yamllibrary.file.YamlConfiguration config =
                    new fr.robie.yamllibrary.file.YamlConfiguration();
            String input = """
                    base: &base
                      key: value
                    extended:
                      <<: *base
                      extra: "more"
                    """;
            config.loadFromString(input);

            assertEquals("value", config.getString("extended.key"));
            assertEquals("more", config.getString("extended.extra"));
        }

        @Test
        @DisplayName("Should handle multiple anchors in flatten via YamlConfiguration")
        void testMultipleAnchors() throws Exception {
            fr.robie.yamllibrary.file.YamlConfiguration config =
                    new fr.robie.yamllibrary.file.YamlConfiguration();
            String input = """
                    a: &a
                      x: 1
                    b: &b
                      y: 2
                    combined:
                      <<: [*a, *b]
                      z: 3
                    """;
            config.loadFromString(input);

            assertEquals(1, config.getInt("combined.x"));
            assertEquals(2, config.getInt("combined.y"));
            assertEquals(3, config.getInt("combined.z"));
        }
    }

    @Nested
    @DisplayName("KeyDirectiveRegistry Tests")
    class KeyDirectiveRegistryTests {

        @Test
        @DisplayName("Should have MergeKeyDirective registered by default")
        void testDefaultDirectives() {
            assertFalse(KeyDirectiveRegistry.getAll().isEmpty());
            assertInstanceOf(MergeKeyDirective.class, KeyDirectiveRegistry.getAll().getFirst());
        }
    }
}
