package com.kamikazejam.syncengine.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JacksonUtil {
    public static final @NotNull String ID_FIELD = "_id";

    private static @Nullable ObjectMapper mapper = null;
    public static @NotNull ObjectMapper getMapper() {
        if (mapper != null) return mapper;
        mapper = new ObjectMapper();
        // mapper.enable(SerializationFeature.INDENT_OUTPUT);  // Optional: enable pretty printing

        // Enable serialization of null and empty values
        mapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
        return mapper;
    }
}
