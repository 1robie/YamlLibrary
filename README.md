# YamlLibrary

A YAML configuration library for Java 21, in two pieces:

| Module | What it is | Depends on |
| --- | --- | --- |
| `yamllibrary-core` | Platform-independent configuration API | SnakeYAML, nothing else |
| `yamllibrary-paper` | Bukkit and Adventure support for Paper servers | core, `paper-api` (provided) |

Core has a single runtime dependency — SnakeYAML. No Guava, no logging framework.

## Install

```kotlin
repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // On a Paper server:
    implementation("fr.robie:yamllibrary-paper:1.0-SNAPSHOT")

    // Or, anywhere else:
    implementation("fr.robie:yamllibrary-core:1.0-SNAPSHOT")
}
```

`yamllibrary-paper` pulls core transitively. It does **not** pull `paper-api` — the server provides that.

## Core

```java
YamlConfiguration config = YamlConfiguration.loadConfiguration(new File("config.yml"));

String name = config.getString("server.name", "unnamed");
int port = config.getInt("server.port", 25565);
List<String> motd = config.getStringList("server.motd");

config.set("server.name", "changed");
config.setComments("server.name", List.of("The name shown in the list"));
config.save(new File("config.yml"));
```

Comments, headers and footers survive a load/save round-trip.

### Beyond plain YAML

Core adds three things on top of SnakeYAML, via `SmartConstructor`:

- **Deep keys** — `a::b::c: value` expands into nested sections.
- **Key directives** — keys prefixed `$$` are handled by a `KeyDirective`. `$$merge` is built in; register your own through `KeyDirectiveRegistry`.
- **Value selectors** — pick between alternative values at parse time.

### Serialization

Implement `ConfigurationSerializable` and register the class:

```java
@SerializableAs("Region")
public class Region implements ConfigurationSerializable {
    @Override public Map<String, Object> serialize() { return Map.of("world", world, "radius", radius); }
    public static Region deserialize(Map<String, ?> args) { /* ... */ }
}

ConfigurationSerialization.registerClass(Region.class);
```

The type is written under the `==` key, the same convention Bukkit uses.

### Teaching it about foreign types

For types you cannot make implement `ConfigurationSerializable` — because they are someone else's — implement `SerializationAdapter` instead and register it:

```java
SerializationAdapters.register(myAdapter);
```

An adapter claims a `baseType()` wholesale and supplies `aliasFor` / `serialize` / `deserialize`. This is exactly how the Paper module bridges Bukkit's separate serialization system; see `BukkitSerializationAdapter` for a worked example.

Adapters are consulted **after** the built-in registry, so a registered class always wins over an adapter for the same alias.

### Error reporting

Some failures are recovered from rather than thrown: a value that will not deserialize, a file that will not parse when the caller asked for a best-effort load. These go to `ConfigurationErrors`, which logs to `System.Logger` at `WARNING` by default.

```java
ConfigurationErrors.setHandler((message, error) -> plugin.getLogger().log(Level.WARNING, message, error));
ConfigurationErrors.silence(); // opt out entirely
```

The handler is a single global — see [Shading](#shading) before setting it from a plugin.

## Paper

```java
PaperYamlConfiguration config = PaperYamlConfiguration.loadConfiguration(file);

ItemStack reward = config.getItemStack("reward");
Location spawn = config.getLocation("arena.spawn");
Material floor = config.getMaterial("arena.floor", Material.STONE);
GameMode mode = config.getEnum("arena.mode", GameMode.class, GameMode.ADVENTURE);
Component title = config.getComponent("messages.title");
```

Creating one installs the Bukkit bridge, so `ItemStack`, `Location`, `Vector`, `Color`, `PotionEffect`, `FireworkEffect`, `BoundingBox`, `AttributeModifier`, `Pattern` and anything else implementing Bukkit's `ConfigurationSerializable` round-trip in **exactly the on-disk format vanilla Bukkit uses**. Existing config files keep working, and files this library writes are readable by a plain Bukkit `YamlConfiguration`.

### Getters work at every depth

`PaperYamlConfiguration` creates `PaperMemorySection`s throughout the tree, so a nested section carries the same getters:

```java
ItemStack weapon = config.getPaperSection("kits.knight").getItemStack("weapon");

for (String key : config.getPaperSection("kits").getKeys(false)) {
    PaperSection kit = config.getPaperSection("kits").getPaperSection(key);
    kit.getItemStack("weapon");
}
```

`getPaperSection(path)` returns null when absent; `paperSection(path)` creates it.

Every getter lives on the `PaperSection` interface as a default method, so there is one implementation shared by the root config and every section.

### Adventure

```java
Component title = config.getComponent("messages.title");                 // MiniMessage
Component legacy = config.getLegacyComponent("messages.old");            // &c codes
List<Component> lore = config.getComponentList("item.lore");

// With placeholders:
Component join = config.getComponent("messages.join",
        Placeholder.unparsed("player", player.getName()));
```

Adventure is optional at runtime. If it is missing, only the component getters fail, with a message naming the cause — the rest of the library works.

### Managing a plugin's files

```java
ConfigManager configs = new ConfigManager(this);

PaperYamlConfiguration config = configs.get("config.yml");   // written from the jar on first run
PaperYamlConfiguration lang = configs.get("lang/en.yml");

configs.reload("config.yml");   // same instance, re-read — cached references stay valid
configs.saveAll();
```

**Adding options in a new version** — copies keys the bundled file has and the user's does not, leaving their values and comments alone:

```java
configs.mergeDefaults("config.yml");
```

New keys are written in the order the bundled file lays them out, not appended at the end, and keys the user added that the bundled file does not know about are kept and moved after them. Sections count as keys, so an empty section in the bundled file appears in the user's copy. The file is only rewritten when something is actually added.

**Changing the layout** — for keys that moved or values whose meaning changed:

```java
configs.migrate("config.yml", 2, List.of(
        ConfigMigration.of(0, c -> {
            c.set("cooldown-seconds", c.getInt("cooldown") / 20);
            c.set("cooldown", null);
        }),
        ConfigMigration.of(1, c -> c.set("enabled", true))));
```

Migrations run in version order and chain, so a file two versions behind runs both. They apply to an in-memory copy: if any step throws, the file on disk is untouched and the cached instance is rolled back. A gap with no migration is an error rather than a silent skip.

## Threading

`ConfigManager` serialises its own operations — two `reload`s, a `save` racing a `migrate`, and so on cannot interleave.

It does **not** make the configuration objects thread-safe. `MemorySection` is backed by a plain `LinkedHashMap`, so a `reload` mutating a config while another thread is reading it is still a data race. If you read config off the main thread, either copy the values you need on the main thread first, or do your own locking around reload.

## Shading

`ConfigurationSerialization`, `SerializationAdapters` and `ConfigurationErrors` are static registries. If two plugins each shade this library **without relocating it**, they share or fight over that state depending on classloader isolation.

If you shade, relocate:

```kotlin
tasks.shadowJar {
    relocate("fr.robie.yamllibrary", "com.yourplugin.libs.yamllibrary")
}
```

With relocation, each plugin gets its own registries and `ConfigurationErrors.setHandler` is safe to point at your plugin logger. Without it, leave the global handler alone — whichever plugin sets it last wins.

## Building

```bash
./gradlew build
```

Java 21 toolchain, Gradle wrapper included. `./gradlew publishToMavenLocal` installs both modules.

The Paper module builds against `paper-api` **snapshots**, which move without warning. A build that passed yesterday can fail today through no change of yours; check the Paper and MockBukkit versions in `gradle/libs.versions.toml` before assuming a regression is yours. MockBukkit must match the `paper-api` version it was built against.

## Licence

[GPL-3.0-only](LICENSE).

The core module reimplements Bukkit's configuration classes (`MemorySection`, `YamlConfiguration`, `ConfigurationSerialization` and the `==` type-key format), and Bukkit's API is GPL-3.0. Code derived from it has to be GPL-3.0 too, so that is the licence used throughout. The Paper module links against `paper-api` as well.

In practice: a plugin that bundles this library is distributed under the GPL-3.0 terms. Bukkit and Paper plugins are commonly GPL already, but check that is acceptable for yours before depending on it.
