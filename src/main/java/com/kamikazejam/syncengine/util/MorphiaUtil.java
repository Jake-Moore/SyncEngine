package com.kamikazejam.syncengine.util;

import dev.morphia.config.MorphiaConfig;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MorphiaUtil {
    private static MorphiaConfig morphiaConfig = null;

    public static @NotNull MorphiaConfig getMorphiaConfig() {
        if (morphiaConfig != null) {
            return morphiaConfig;
        }
        // Defaults for MorphiaConfig
        return morphiaConfig = MorphiaConfig.load()
                .packages(List.of("com.kamikazejam.syncengine.entity.*"))
                .storeNulls(true)
                .storeEmpties(true);
    }
}
