package com.kamikazejam.syncengine.util;

import dev.morphia.config.MorphiaConfig;
import org.jetbrains.annotations.NotNull;

public class MorphiaUtil {
    private static MorphiaConfig morphiaConfig = null;

    public static @NotNull MorphiaConfig getMorphiaConfig() {
        if (morphiaConfig != null) {
            return morphiaConfig;
        }
        // Defaults for MorphiaConfig
        return morphiaConfig = MorphiaConfig.load()
                .storeNulls(true)
                .storeEmpties(true);
    }
}
