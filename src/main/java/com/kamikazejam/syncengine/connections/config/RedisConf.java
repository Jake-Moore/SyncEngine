package com.kamikazejam.syncengine.connections.config;

import com.kamikazejam.kamicommon.configuration.config.KamiConfig;
import com.kamikazejam.syncengine.SyncEnginePlugin;
import io.lettuce.core.RedisURI;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
public class RedisConf {
    private final String address;
    private final int port;
    private final boolean auth;
    private final String password;

    // Private - fetch the RedisConf Singleton via static methods
    private RedisConf(String address, int port, boolean auth, String password) {
        this.address = address;
        this.port = port;
        this.auth = auth;
        this.password = password;
    }

    public RedisURI getRedisURI() {
        RedisURI.Builder builder = RedisURI.builder()
                .withHost(address)
                .withPort(port)
                .withSsl(false);
        if (auth) {
            builder.withPassword((CharSequence) password);
        }
        return builder.build();
    }


    private static @Nullable RedisConf conf = null;

    public static @NotNull RedisConf get() {
        if (conf != null) {
            return conf;
        }

        KamiConfig config = SyncEnginePlugin.get().getKamiConfig();

        // Load Config Values
        String address = config.getString("connections.REDIS.address");
        int port = config.getInt("connections.REDIS.port");
        boolean auth = config.getBoolean("connections.REDIS.auth.enabled", false);
        String password = config.getString("connections.REDIS.auth.password");

        return conf = new RedisConf(address, port, auth, password);
    }
}
