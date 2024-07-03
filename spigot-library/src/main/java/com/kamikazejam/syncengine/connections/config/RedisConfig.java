package com.kamikazejam.syncengine.connections.config;

import com.kamikazejam.kamicommon.configuration.config.KamiConfig;
import com.kamikazejam.kamicommon.redis.util.RedisConf;
import com.kamikazejam.syncengine.EngineSource;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
public class RedisConfig {

    private static @Nullable RedisConf conf = null;
    public static @NotNull RedisConf get() {
        if (conf != null) {
            return conf;
        }

        KamiConfig config = EngineSource.getConfig();

        // Load Config Values
        String address = config.getString("connections.REDIS.address");
        int port = config.getInt("connections.REDIS.port");
        boolean auth = config.getBoolean("connections.REDIS.auth.enabled", false);

        if (auth) {
            String password = config.getString("connections.REDIS.auth.password");
            return conf = RedisConf.of(address, port, password);
        }else {
            return conf = RedisConf.of(address, port);
        }
    }
}
