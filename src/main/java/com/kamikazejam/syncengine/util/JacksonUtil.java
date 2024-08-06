package com.kamikazejam.syncengine.util;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.kamikazejam.syncengine.base.Sync;
import com.kamikazejam.syncengine.util.jackson.JacksonSpigotModule;
import lombok.SneakyThrows;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class JacksonUtil {
    public static final @NotNull String ID_FIELD = "_id";

    private static @Nullable ObjectMapper mapper = null;
    public static @NotNull ObjectMapper getMapper() {
        if (mapper != null) return mapper;
        mapper = new ObjectMapper();
        // Optional: enable pretty printing
        // mapper.enable(SerializationFeature.INDENT_OUTPUT);

        // Don't fail on empty POJOs
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        // to prevent exception when encountering unknown property:
        //  i.e. if the json has a property no longer in the class
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        // Configure Jackson to only use fields for serialization (ignoring transient fields)
        //   We have to disable setters and getters, otherwise a transient getter or setter will cause it to be serialized
        VisibilityChecker.Std check = new VisibilityChecker.Std(
                JsonAutoDetect.Visibility.NONE,           // don't use getters for field mapping
                JsonAutoDetect.Visibility.NONE,           // don't use getters for field mapping
                JsonAutoDetect.Visibility.NONE,           // don't use setters for field mapping
                JsonAutoDetect.Visibility.NONE,           // don't use creators
                JsonAutoDetect.Visibility.ANY             // any field
        );
        mapper.setVisibility(check);

        // Enable serialization of null and empty values
        mapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);

        // Add Basic Spigot Types Module, for handling basic types
        mapper.registerModule(new JacksonSpigotModule());

        return mapper;
    }

    @SneakyThrows
    public static <K, X extends Sync<K>> @NotNull String toJson(X sync) {
        return getMapper().writeValueAsString(sync);
    }

    @SneakyThrows @Contract("_, !null -> !null")
    public static <K, X extends Sync<K>> @Nullable X fromJson(Class<X> clazz, @Nullable String json) {
        if (json == null) { return null; }
        return getMapper().readValue(json, clazz);
    }

    @SneakyThrows
    public static @NotNull String serialize(@NotNull Object o) {
        return getMapper().writeValueAsString(o);
    }

    @SneakyThrows @Contract("_, !null -> !null")
    public static <X> @Nullable X deserialize(Class<X> clazz, @Nullable String json) {
        if (json == null) { return null; }
        return getMapper().readValue(json, clazz);
    }
}
