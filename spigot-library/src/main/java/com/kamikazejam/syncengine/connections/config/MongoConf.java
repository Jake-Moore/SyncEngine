package com.kamikazejam.syncengine.connections.config;

import com.kamikazejam.kamicommon.configuration.config.KamiConfig;
import com.kamikazejam.syncengine.PluginSource;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

@Getter
public class MongoConf {
    private final String uri;

    // Private - fetch the MongoConf Singleton via static methods
    private MongoConf(String uri) {
        this.uri = uri;
    }

    private static @Nullable MongoConf conf = null;

    public static MongoConf get() {
        if (conf != null) {
            return conf;
        }

        // Load Config Values
        KamiConfig config = PluginSource.getConfig();
        String uri = config.getString("connections.MONGODB.uri", null);
        return conf = new MongoConf(uri);
    }
}
