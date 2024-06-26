package com.kamikazejam.syncengine.connections.storage;

import com.google.common.base.Preconditions;
import com.kamikazejam.syncengine.SyncEnginePlugin;
import com.kamikazejam.syncengine.base.Service;
import com.kamikazejam.syncengine.base.error.LoggerService;
import com.kamikazejam.syncengine.connections.config.MongoConf;
import com.kamikazejam.syncengine.connections.monitor.MongoMonitor;
import com.kamikazejam.syncengine.util.MorphiaUtil;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.bson.UuidRepresentation;
import org.bukkit.plugin.Plugin;

@Getter
public class MongoService extends LoggerService implements Service {
    private boolean running = false;
    @Setter private boolean mongoInitConnect = false;
    @Setter private boolean mongoConnected = false;

    // MongoDB
    @Getter(AccessLevel.NONE) private MongoClient mongoClient = null;

    public MongoService() {}

    // ------------------------------------------------- //
    //                StorageService                     //
    // ------------------------------------------------- //
    @Override
    public boolean start() {
        this.debug("Connecting to MongoDB");
        // Load MorphiaConfig on start-up
        MorphiaUtil.getMorphiaConfig();

        boolean mongo = this.connectMongo();
        this.running = true;

        if (!mongo) {
            this.error("Failed to start MongoService, connection failed.");
            return false;
        }
        // Client was created, MongoMonitor should log more as the connection succeeds or fails
        return true;
    }

    @Override
    public boolean shutdown() {
        // If not running, warn and return true (we are already shutdown)
        if (!running) {
            this.warn("MongoService.shutdown() called while service is not running!");
            return true;
        }

        // Disconnect from MongoDB
        boolean mongo = this.disconnectMongo();
        this.running = false;

        if (!mongo) {
            this.error("Failed to shutdown MongoService, disconnect failed.");
            return false;
        }

        this.debug("Disconnected from MongoDB");
        return true;
    }



    // ------------------------------------------------- //
    //                MongoDB Connection                 //
    // ------------------------------------------------- //
    public boolean connectMongo() {
        // Can only have one MongoClient instance
        Preconditions.checkState(this.mongoClient == null, "[MongoService] MongoClient instance already exists!");

        try {
            MongoClientSettings.Builder settingsBuilder = MongoClientSettings.builder()
                    .uuidRepresentation(UuidRepresentation.STANDARD)
                    .applyToServerSettings(builder -> builder.addServerMonitorListener(new MongoMonitor(this)));

            // Using connection URI
            ConnectionString connectionString = new ConnectionString(MongoConf.get().getUri());
            settingsBuilder.applyConnectionString(connectionString);
            this.mongoClient = MongoClients.create(settingsBuilder.build());
            // the datastore will be setup in the service class
            // If MongoDB fails it will automatically attempt to reconnect until it connects
            return true;
        } catch (Exception ex) {
            // Generic exception catch... just in case.
            return false; // Failed to connect.
        }
    }

    private boolean disconnectMongo() {
        if (this.mongoClient != null) {
            this.mongoClient.close();
        }
        return true;
    }



    // ------------------------------------------------- //
    //                 Morphia Datastore                 //
    // ------------------------------------------------- //
    // Make each Datastore a singleton using a Map, so that we don't encounter errors like:
    //    "Two entities have been mapped using the same discriminator value"
//    private static final Map<String, Datastore> datastoreMap = new HashMap<>();



    // ------------------------------------------------- //
    //                   ErrorService                    //
    // ------------------------------------------------- //
    @Override
    public boolean isDebug() {
        return SyncEnginePlugin.get().isDebug();
    }
    @Override
    public Plugin getPlugin() {
        return SyncEnginePlugin.get();
    }
    @Override
    public String getLoggerName() {
        return "MongoService";
    }
}
