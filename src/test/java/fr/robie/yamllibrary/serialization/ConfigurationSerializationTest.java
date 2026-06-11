package fr.robie.yamllibrary.serialization;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ConfigurationSerialization Tests")
class ConfigurationSerializationTest {

    @Nested
    @DisplayName("Class Registration Tests")
    class RegistrationTests {

        @BeforeEach
        void setUp() {
            ConfigurationSerialization.registerClass(SimpleSerializable.class);
        }

        @AfterEach
        void tearDown() {
            ConfigurationSerialization.unregisterClass(SimpleSerializable.class);
            ConfigurationSerialization.unregisterClass(CustomAliasSerializable.class);
        }

        @Test
        @DisplayName("Should register class by alias from annotation")
        void testRegisterByAnnotation() {
            Class<? extends ConfigurationSerializable> clazz =
                    ConfigurationSerialization.getClassByAlias("SimpleSerializable");
            assertEquals(SimpleSerializable.class, clazz);
        }

        @Test
        @DisplayName("Should register class by fully qualified name")
        void testRegisterByClassName() {
            Class<? extends ConfigurationSerializable> clazz =
                    ConfigurationSerialization.getClassByAlias(SimpleSerializable.class.getName());
            assertEquals(SimpleSerializable.class, clazz);
        }

        @Test
        @DisplayName("Should return null for unregistered alias")
        void testUnregisteredAlias() {
            assertNull(ConfigurationSerialization.getClassByAlias("NonExistent"));
        }

        @Test
        @DisplayName("Should unregister class by alias")
        void testUnregisterByAlias() {
            ConfigurationSerialization.unregisterClass("SimpleSerializable");
            assertNull(ConfigurationSerialization.getClassByAlias("SimpleSerializable"));
        }

        @Test
        @DisplayName("Should unregister class by class type")
        void testUnregisterByClass() {
            ConfigurationSerialization.unregisterClass(SimpleSerializable.class);
            assertNull(ConfigurationSerialization.getClassByAlias("SimpleSerializable"));
            assertNull(ConfigurationSerialization.getClassByAlias(SimpleSerializable.class.getName()));
        }

        @Test
        @DisplayName("Should register class with custom alias")
        void testRegisterWithCustomAlias() {
            ConfigurationSerialization.registerClass(CustomAliasSerializable.class, "MyCustomName");
            assertEquals(CustomAliasSerializable.class,
                    ConfigurationSerialization.getClassByAlias("MyCustomName"));
        }
    }

    @Nested
    @DisplayName("Deserialization Tests")
    class DeserializationTests {

        @BeforeEach
        void setUp() {
            ConfigurationSerialization.registerClass(SimpleSerializable.class);
            ConfigurationSerialization.registerClass(WithDeserializeMethod.class);
            ConfigurationSerialization.registerClass(WithValueOfMethod.class);
        }

        @AfterEach
        void tearDown() {
            ConfigurationSerialization.unregisterClass(SimpleSerializable.class);
            ConfigurationSerialization.unregisterClass(WithDeserializeMethod.class);
            ConfigurationSerialization.unregisterClass(WithValueOfMethod.class);
        }

        @Test
        @DisplayName("Should deserialize via Map constructor")
        void testDeserializeViaConstructor() {
            Map<String, Object> args = new LinkedHashMap<>();
            args.put(ConfigurationSerialization.SERIALIZED_TYPE_KEY, "SimpleSerializable");
            args.put("value", "hello");

            ConfigurationSerializable result = ConfigurationSerialization.deserializeObject(args);
            assertInstanceOf(SimpleSerializable.class, result);
            assertEquals("hello", ((SimpleSerializable) result).value);
        }

        @Test
        @DisplayName("Should deserialize via static deserialize method")
        void testDeserializeViaStaticMethod() {
            Map<String, Object> args = new LinkedHashMap<>();
            args.put(ConfigurationSerialization.SERIALIZED_TYPE_KEY, "WithDeserializeMethod");
            args.put("data", "from method");

            ConfigurationSerializable result = ConfigurationSerialization.deserializeObject(args);
            assertInstanceOf(WithDeserializeMethod.class, result);
            assertEquals("from method", ((WithDeserializeMethod) result).data);
        }

        @Test
        @DisplayName("Should deserialize via static valueOf method")
        void testDeserializeViaValueOf() {
            Map<String, Object> args = new LinkedHashMap<>();
            args.put(ConfigurationSerialization.SERIALIZED_TYPE_KEY, "WithValueOfMethod");
            args.put("info", "via valueOf");

            ConfigurationSerializable result = ConfigurationSerialization.deserializeObject(args);
            assertInstanceOf(WithValueOfMethod.class, result);
            assertEquals("via valueOf", ((WithValueOfMethod) result).info);
        }



        @Test
        @DisplayName("Should throw on missing type key")
        void testMissingTypeKey() {
            Map<String, Object> args = new LinkedHashMap<>();
            args.put("key", "value");

            assertThrows(IllegalArgumentException.class,
                    () -> ConfigurationSerialization.deserializeObject(args));
        }

        @Test
        @DisplayName("Should throw on null alias")
        void testNullAlias() {
            Map<String, Object> args = new LinkedHashMap<>();
            args.put(ConfigurationSerialization.SERIALIZED_TYPE_KEY, null);

            assertThrows(IllegalArgumentException.class,
                    () -> ConfigurationSerialization.deserializeObject(args));
        }

        @Test
        @DisplayName("Should throw on non-existent alias")
        void testNonExistentAlias() {
            Map<String, Object> args = new LinkedHashMap<>();
            args.put(ConfigurationSerialization.SERIALIZED_TYPE_KEY, "NoSuchClass");

            assertThrows(IllegalArgumentException.class,
                    () -> ConfigurationSerialization.deserializeObject(args));
        }

        @Test
        @DisplayName("Should deserialize via explicit class")
        void testDeserializeObjectByClass() {
            Map<String, Object> args = new LinkedHashMap<>();
            args.put("value", "explicit class");

            ConfigurationSerializable result =
                    ConfigurationSerialization.deserializeObject(args, SimpleSerializable.class);
            assertInstanceOf(SimpleSerializable.class, result);
            assertEquals("explicit class", ((SimpleSerializable) result).value);
        }
    }

    @Nested
    @DisplayName("Alias Resolution Tests")
    class AliasTests {

        @Test
        @DisplayName("Should get alias from @SerializableAs annotation")
        void testAliasFromAnnotation() {
            assertEquals("SimpleSerializable", ConfigurationSerialization.getAlias(SimpleSerializable.class));
        }

        @Test
        @DisplayName("Should get class name when no annotation present")
        void testAliasFromClassName() {
            assertEquals(NoAnnotationSerializable.class.getName(),
                    ConfigurationSerialization.getAlias(NoAnnotationSerializable.class));
        }

        @Test
        @DisplayName("Should follow DelegateDeserialization for alias")
        void testAliasWithDelegate() {
            assertEquals("SimpleSerializable",
                    ConfigurationSerialization.getAlias(DelegatedSerializable.class));
        }
    }

    @SerializableAs("SimpleSerializable")
    public static class SimpleSerializable implements ConfigurationSerializable {
        public String value;

        public SimpleSerializable() {
        }

        public SimpleSerializable(Map<String, Object> map) {
            this.value = (String) map.get("value");
        }

        @Override
        public @NotNull Map<String, Object> serialize() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("value", this.value);
            return map;
        }
    }

    @SerializableAs("WithDeserializeMethod")
    public static class WithDeserializeMethod implements ConfigurationSerializable {
        public String data;

        public WithDeserializeMethod() {
        }

        public static WithDeserializeMethod deserialize(Map<String, Object> args) {
            WithDeserializeMethod obj = new WithDeserializeMethod();
            obj.data = (String) args.get("data");
            return obj;
        }

        @Override
        public @NotNull Map<String, Object> serialize() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("data", this.data);
            return map;
        }
    }

    @SerializableAs("WithValueOfMethod")
    public static class WithValueOfMethod implements ConfigurationSerializable {
        public String info;

        public WithValueOfMethod() {
        }

        public static WithValueOfMethod valueOf(Map<String, Object> args) {
            WithValueOfMethod obj = new WithValueOfMethod();
            obj.info = (String) args.get("info");
            return obj;
        }

        @Override
        public @NotNull Map<String, Object> serialize() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("info", this.info);
            return map;
        }
    }

    @DelegateDeserialization(SimpleSerializable.class)
    @SerializableAs("DelegatedSerializable")
    public static class DelegatedSerializable implements ConfigurationSerializable {
        @Override
        public @NotNull Map<String, Object> serialize() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("value", "delegate");
            return map;
        }
    }

    public static class NoAnnotationSerializable implements ConfigurationSerializable {
        @Override
        public @NotNull Map<String, Object> serialize() {
            return new LinkedHashMap<>();
        }
    }

    public static class CustomAliasSerializable implements ConfigurationSerializable {
        @Override
        public @NotNull Map<String, Object> serialize() {
            return new LinkedHashMap<>();
        }
    }
}
