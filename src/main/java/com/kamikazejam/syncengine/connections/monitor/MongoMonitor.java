package com.kamikazejam.syncengine.connections.monitor;

import com.kamikazejam.syncengine.connections.storage.MongoService;
import com.mongodb.event.ServerHeartbeatFailedEvent;
import com.mongodb.event.ServerHeartbeatStartedEvent;
import com.mongodb.event.ServerHeartbeatSucceededEvent;
import com.mongodb.event.ServerMonitorListener;
import org.jetbrains.annotations.NotNull;

public class MongoMonitor implements ServerMonitorListener {

    private final MongoService service;

    public MongoMonitor(MongoService service) {
        this.service = service;
    }

    @Override
    public void serverHearbeatStarted(@NotNull ServerHeartbeatStartedEvent event) {
        if (!this.service.isMongoInitConnect()) {
            this.service.debug("Attempting MongoDB initial connection");
        }
    }

    @Override
    public void serverHeartbeatSucceeded(@NotNull ServerHeartbeatSucceededEvent event) {
        if (!this.service.isMongoInitConnect()) {
            this.service.setMongoInitConnect(true);
            this.service.debug("MongoDB initial connection succeeded");
        } else {
            if (!service.isMongoConnected()) {
                this.service.debug("MongoDB connection restored");
            }
        }
        this.service.setMongoConnected(true);
    }

    @Override
    public void serverHeartbeatFailed(@NotNull ServerHeartbeatFailedEvent event) {
        // Lost connection or failed to connect
        if (this.service.isMongoConnected()) {
            this.service.info("MongoDB connection lost");
        }
        this.service.setMongoConnected(false);
    }
}