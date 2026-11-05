package fr.robie.yamllibrary.file;

import fr.robie.yamllibrary.ConfigurationSection;
import fr.robie.yamllibrary.serialization.ConfigurationSerializable;
import fr.robie.yamllibrary.serialization.ConfigurationSerialization;
import fr.robie.yamllibrary.serialization.SerializationAdapter;
import fr.robie.yamllibrary.serialization.SerializationAdapters;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.representer.Representer;

import java.util.LinkedHashMap;
import java.util.Map;

public class YamlRepresenter extends Representer {

    public YamlRepresenter(@NotNull DumperOptions options) {
        super(options);
        this.multiRepresenters.put(ConfigurationSection.class, new RepresentConfigurationSection());
        this.multiRepresenters.put(ConfigurationSerializable.class, new RepresentConfigurationSerializable());
        this.multiRepresenters.remove(Enum.class);
    }

    /**
     * Picks up any {@link SerializationAdapter} registered since the last call
     * before representing, so adapters may be registered at any time.
     */
    @NotNull
    @Override
    public Node represent(Object data) {
        this.syncAdapters();
        return super.represent(data);
    }

    private void syncAdapters() {
        for (SerializationAdapter adapter : SerializationAdapters.adapters()) {
            this.multiRepresenters.putIfAbsent(adapter.baseType(), new RepresentAdapted(adapter));
        }
    }

    private class RepresentConfigurationSection extends RepresentMap {
        @NotNull
        @Override
        public Node representData(@NotNull Object data) {
            return super.representData(((ConfigurationSection) data).getValues(false));
        }
    }

    private class RepresentAdapted extends RepresentMap {

        private final SerializationAdapter adapter;

        private RepresentAdapted(@NotNull SerializationAdapter adapter) {
            this.adapter = adapter;
        }

        @NotNull
        @Override
        public Node representData(@NotNull Object data) {
            Map<String, Object> values = new LinkedHashMap<>();
            values.put(ConfigurationSerialization.SERIALIZED_TYPE_KEY, this.adapter.aliasFor(data.getClass()));
            values.putAll(this.adapter.serialize(data));

            return super.representData(values);
        }
    }

    private class RepresentConfigurationSerializable extends RepresentMap {

        @NotNull
        @Override
        public Node representData(@NotNull Object data) {
            ConfigurationSerializable serializable = (ConfigurationSerializable) data;
            Map<String, Object> values = new LinkedHashMap<>();
            values.put(ConfigurationSerialization.SERIALIZED_TYPE_KEY, ConfigurationSerialization.getAlias(serializable.getClass()));
            values.putAll(serializable.serialize());

            return super.representData(values);
        }
    }
}
