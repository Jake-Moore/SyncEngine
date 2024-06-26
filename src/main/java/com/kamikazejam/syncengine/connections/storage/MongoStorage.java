package com.kamikazejam.syncengine.connections.storage;

import com.google.common.base.Preconditions;
import com.kamikazejam.syncengine.SyncEnginePlugin;
import com.kamikazejam.syncengine.base.Cache;
import com.kamikazejam.syncengine.base.Sync;
import com.kamikazejam.syncengine.connections.config.MongoConf;
import com.kamikazejam.syncengine.connections.monitor.MongoMonitor;
import com.kamikazejam.syncengine.util.MorphiaUtil;
import com.mongodb.*;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import dev.morphia.Datastore;
import dev.morphia.Morphia;
import dev.morphia.VersionMismatchException;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.query.Query;
import dev.morphia.query.filters.Filters;
import dev.morphia.query.filters.RegexFilter;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.bson.UuidRepresentation;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Getter
@SuppressWarnings("unused")
public class MongoStorage extends StorageService {
    private boolean running = false;
    @Setter
    private boolean mongoInitConnect = false;
    @Setter
    private boolean mongoConnected = false;

    // MongoDB
    @Getter(AccessLevel.NONE)
    private MongoClient mongoClient = null;

    public MongoStorage() {
    }

    // ------------------------------------------------- //
    //                     Service                       //
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
    //                StorageService                     //
    // ------------------------------------------------- //

    @Override
    public @NotNull <K, X extends Sync<K>> Optional<X> get(Cache<K, X> cache, K key) {
        Preconditions.checkNotNull(key);
        try {
            Query<X> q = getQuery(cache, key);
            Optional<X> o = Optional.ofNullable(q.first());
            o.ifPresent(x -> {
                x.setCache(cache);
                x.initialized();
            });
            return o;
        } catch (MongoException ex) {
            cache.getLoggerService().info(ex, "MongoDB error getting Object from MongoDB Layer: " + key);
            return Optional.empty();
        } catch (Exception expected) {
            cache.getLoggerService().info(expected, "Error getting Object from MongoDB Layer: " + key);
            return Optional.empty();
        }
    }

    @Override
    public <K, X extends Sync<K>> boolean save(Cache<K, X> cache, X sync) {
        // Try saving with Morphia/MongoDB and catch/fix a host of possible errors we can receive
        try {
            getDatastore(cache).save(sync);
            return true;
        } catch (MongoWriteException ex1) {
            // Handle the MongoWriteException
            return handleMongoWriteException(ex1, cache, sync);

        } catch (MongoException ex) {
            // Log the MongoException (unknown Mongo error)
            cache.getLoggerService().info(ex, "MongoDB error saving Object to MongoDB Layer: " + sync.getIdentifier());
            return false;

        } catch (VersionMismatchException ex2) {
            // Handle the VersionMismatchException
            return handleVersionMismatchException(ex2, cache, sync);

        } catch (Exception expected) {
            // Handle any other exception
            cache.getLoggerService().info(expected, "Error saving Object to MongoDB Layer: " + sync.getIdentifier());
            return false;
        }
    }

    @Override
    public <K, X extends Sync<K>> boolean has(Cache<K, X> cache, K key) {
        Preconditions.checkNotNull(key);
        try {
            return getQuery(cache, key).count() > 0;
        } catch (MongoException ex) {
            cache.getLoggerService().info(ex, "MongoDB error check if Sync exists in MongoDB Layer: " + key);
            return false;
        } catch (Exception expected) {
            cache.getLoggerService().info(expected, "Error checking if Sync exists in MongoDB Layer: " + key);
            return false;
        }
    }

    @Override
    public <K, X extends Sync<K>> long size(Cache<K, X> cache) {
        return this.createQuery(cache).count();
    }

    @Override
    public <K, X extends Sync<K>> boolean remove(Cache<K, X> cache, K key) {
        Preconditions.checkNotNull(key);
        try {
            return getQuery(cache, key).findAndDelete() != null;
        } catch (MongoException ex) {
            cache.getLoggerService().info(ex, "MongoDB error removing Sync from MongoDB Layer: " + key);
        } catch (Exception expected) {
            cache.getLoggerService().info(expected, "Error removing Sync from MongoDB Layer: " + key);
        }
        return false;
    }

    @Override
    public <K, X extends Sync<K>> Collection<X> getAll(Cache<K, X> cache) {
        return createQuery(cache).stream().toList();
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
    private final Map<String, Datastore> datastoreMap = new HashMap<>();

    public @NotNull Datastore getDatastore(Cache<?, ?> cache) {
        String databaseName = cache.getDatabaseName();
        if (!datastoreMap.containsKey(databaseName)) {
            MorphiaConfig config = MorphiaUtil.getMorphiaConfig()
                    .database(databaseName); // creates a new instance with this database name

            try {
                SyncEnginePlugin.get().getLogger().info("Creating Datastore for database: " + databaseName);
                Datastore ds = Morphia.createDatastore(mongoClient, config);
                datastoreMap.put(databaseName, ds);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        return datastoreMap.get(databaseName);
    }


    // ------------------------------------------------- //
    //                   LoggerService                   //
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
        return "MongoStorage";
    }


    // ------------------------------------------------- //
    //                Helper Methods                     //
    // ------------------------------------------------- //

    private <K, X extends Sync<K>> boolean handleMongoWriteException(MongoWriteException ex, Cache<K, X> cache, X sync) {
        // There's a chance on start we may create and try to save an object that is being created by another instance
        //  at this very moment, in that case we should fetch the remote, and update our local copy with the remote
        if (ex.getError().getCategory() == ErrorCategory.DUPLICATE_KEY) {
            cache.getLoggerService().debug("Duplicate key error saving Object to MongoDB Layer: " + sync.getIdentifier());

            Optional<X> remote = cache.get(sync.getIdentifier());
            if (remote.isPresent()) {
                // If present, update local copy with remote data
                cache.getLoggerService().debug("  Updating local Object with remote Object: " + sync.getIdentifier());
                cache.updateSyncFromNewer(sync, remote.get());
            } else {
                // Otherwise remove from the cache since for some reason we couldn't fet remote
                cache.getLoggerService().debug("  ERROR - key not found in remote, uncaching local Object: " + sync.getIdentifier());
                cache.uncache(sync);
            }
            return true;
        }

        // If not a duplicate key error, log it
        cache.getLoggerService().info(ex, "MongoDB error0 saving Profile to MongoDB Layer: " + sync.getIdentifier());
        return false;
    }

    private <K, X extends Sync<K>> boolean handleVersionMismatchException(VersionMismatchException ex, Cache<K, X> cache, X sync) {
        // TODO uncomment
//        String localJson = ProfileHandshakeService.toJson(cache, sync);
//        String localErr = "Local Object: " + localJson;
//
//        AtomicReference<String> remoteErr = new AtomicReference<>("Remote Object: null");
//        cache.get(sync.getIdentifier()).ifPresent(remote -> {
//            String remoteJson = ProfileHandshakeService.toJson(cache, remote);
//            remoteErr.set("Remote Object: " + remoteJson);
//
//            // Update our local copy, since this error means it's out of date, and our logging already fetched remote
//            cache.updateSyncFromNewer(sync, remote);
//        });
//
//        // Ignore VersionMismatchException from IdProfile, since we don't have a good way of handling
//        //   Swapping version switching, and saving on quit while swapping causes this exception
//        // if (sync instanceof IdProfile) { return true; }
//
//        String err = "Morphia Versioning error saving Object to Mongo Layer: " + sync.getIdentifier() + " (Version Mismatch)"
//                        + "\n" + localErr
//                        + "\n" + remoteErr.get();
//
//        // Log debug if we couldn't save the object due to a versioning error (shouldn't happen, but log it in case)
//        cache.getLoggerService().info(ex,  err);
        return false;
    }

    public <K, X extends Sync<K>> Query<X> getQuery(Cache<K, X> cache, K key) {
        Query<X> q = createQuery(cache);
        Preconditions.checkNotNull(q, "Query is null");
        Preconditions.checkNotNull(cache.getEmptySync(), "emptySync is null");
        Preconditions.checkNotNull(cache.getEmptySync().identifierFieldName(), "identifierFieldName() is null for Sync Object: " + cache.getEmptySync().getClass().getSimpleName());
        return createQueryQuoteExactInsensitive(cache, q, cache.getEmptySync().identifierFieldName(), key);
    }

    public <K, X extends Sync<K>> Query<X> createQuery(Cache<K, X> cache) {
        return this.getDatastore(cache).find(cache.getSyncClass());
    }

    public <K, X extends Sync<K>> Query<X> createQueryQuoteExactInsensitive(Cache<K, X> cache, String fieldName, K value) {
        return createQueryQuoteExactInsensitive(cache, createQuery(cache), fieldName, value);
    }

    public <K, X extends Sync<K>> Query<X> createQueryQuoteExactInsensitive(Cache<K, X> cache, Query<X> q, String fieldName, K key) {
        String str = "^" + Pattern.quote(cache.keyToString(key)) + "$";
        RegexFilter regexFilter = Filters.regex(fieldName, Pattern.compile(str));
        return q.filter(regexFilter.caseInsensitive());
    }
}
