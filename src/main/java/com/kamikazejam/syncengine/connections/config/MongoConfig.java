package com.kamikazejam.syncengine.connections.config;

import com.kamikazejam.kamicommon.configuration.config.KamiConfig;
import com.kamikazejam.syncengine.EngineSource;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

@Getter
public class MongoConfig {
    private final String uri;

    // Private - fetch the MongoConf Singleton via static methods
    private MongoConfig(String uri) {
        this.uri = uri;
    }

    private static @Nullable MongoConfig conf = null;

    public static MongoConfig get() {
        if (conf != null) {
            return conf;
        }

        // Load Config Values
        KamiConfig config = EngineSource.getConfig();
        String uri = config.getString("connections.MONGODB.uri", null);
        return conf = new MongoConfig(uri);
    }
}
