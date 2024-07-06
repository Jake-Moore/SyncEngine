package com.kamikazejam.syncengine.mode.profile.network;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RedisNetworkServiceState {

    private volatile boolean redisConnected = false;

    private volatile boolean redisInitConnect = false; // has it connected at least once

    private volatile long lastRedisConnectionAttempt = 0;

    /**
     * Check the connectivity of both databases
     *
     * @return true if both are connected
     */
    public boolean isDatabaseConnected() {
        return this.redisConnected;
    }

}
