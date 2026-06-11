package fr.robie.yamllibrary.serialization;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public interface ConfigurationSerializable {
    @NotNull
    @ApiStatus.OverrideOnly
    Map<String, Object> serialize();


}
