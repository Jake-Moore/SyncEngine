package com.kamikazejam.syncengine.connections.storage;

import com.google.common.base.Preconditions;
import com.kamikazejam.syncengine.PluginSource;
import com.kamikazejam.syncengine.base.Cache;
import com.kamikazejam.syncengine.base.Sync;
import com.kamikazejam.syncengine.connections.config.MongoConf;
import com.kamikazejam.syncengine.connections.monitor.MongoMonitor;
import com.mongodb.*;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.bson.Document;
import org.bson.UuidRepresentation;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.mongojack.JacksonMongoCollection;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.kamikazejam.syncengine.util.JacksonUtil.ID_FIELD;
import static com.kamikazejam.syncengine.util.JacksonUtil.getMapper;

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
        // Load Mapper on start-up
        getMapper();

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
            Document query = new Document(ID_FIELD, cache.keyToString(key));
            Optional<X> o = Optional.ofNullable(getJackson(cache).findOne(query));
            // Initialize the object if it exists
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
        // Try saving to MongoDB with Jackson and catch/fix a host of possible errors we can receive
        try {
            // TODO add optimistic versioning
            getJackson(cache).save(sync);
            return true;
        } catch (MongoWriteException ex1) {
            // Handle the MongoWriteException
            return handleMongoWriteException(ex1, cache, sync);

        } catch (MongoException ex) {
            // Log the MongoException (unknown Mongo error)
            cache.getLoggerService().info(ex, "MongoDB error saving Object to MongoDB Layer: " + sync.getId());
            return false;

        } catch (Exception expected) {
            // Handle any other exception
            cache.getLoggerService().info(expected, "Error saving Object to MongoDB Layer: " + sync.getId());
            return false;
        }
    }

    @Override
    public <K, X extends Sync<K>> boolean has(Cache<K, X> cache, K key) {
        Preconditions.checkNotNull(key);
        try {
            Document query = new Document(ID_FIELD, cache.keyToString(key));
            return getJackson(cache).countDocuments(query) > 0;
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
        return getJackson(cache).countDocuments();
    }

    @Override
    public <K, X extends Sync<K>> boolean remove(Cache<K, X> cache, K key) {
        Preconditions.checkNotNull(key);
        try {
            Document query = new Document(ID_FIELD, cache.keyToString(key));
            return getJackson(cache).deleteMany(query).getDeletedCount() > 0;
        } catch (MongoException ex) {
            cache.getLoggerService().info(ex, "MongoDB error removing Sync from MongoDB Layer: " + key);
        } catch (Exception expected) {
            cache.getLoggerService().info(expected, "Error removing Sync from MongoDB Layer: " + key);
        }
        return false;
    }

    @Override
    public <K, X extends Sync<K>> Iterable<X> getAll(Cache<K, X> cache) {
        return getJackson(cache).find();
    }

    @Override
    public <K, X extends Sync<K>> Set<K> getKeys(Cache<K, X> cache) {
        // TODO find a better way to get keys (without having to load the entire object)
        Spliterator<X> spl = getJackson(cache).find().spliterator();
        return StreamSupport.stream(spl, false)
                .map(Sync::getId)
                .collect(Collectors.toSet());
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
    //                 Jackson Collection                //
    // ------------------------------------------------- //
    // Make each Datastore a singleton using a Map, so that we don't encounter errors like:
    //    "Two entities have been mapped using the same discriminator value"
    private final Map<String, JacksonMongoCollection<?>> collMap = new HashMap<>();

    @SuppressWarnings("unchecked")
    public <K, X extends Sync<K>> @NotNull JacksonMongoCollection<X> getJackson(Cache<K, X> cache) {
        String databaseName = cache.getDatabaseName();
        if (collMap.containsKey(databaseName)) {
            return (JacksonMongoCollection<X>) collMap.get(databaseName);
        }

        // Create a new JacksonMongoCollection
        JacksonMongoCollection<X> coll = JacksonMongoCollection.builder()
                .withObjectMapper(getMapper())
                .build(mongoClient, databaseName, cache.getName(), cache.getSyncClass(), UuidRepresentation.STANDARD);

        collMap.put(databaseName, coll);
        return coll;
    }


    // ------------------------------------------------- //
    //                   LoggerService                   //
    // ------------------------------------------------- //
    @Override
    public boolean isDebug() {
        return PluginSource.isDebug();
    }

    @Override
    public Plugin getPlugin() {
        return PluginSource.get();
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
            cache.getLoggerService().debug("Duplicate key error saving Object to MongoDB Layer: " + sync.getId());

            Optional<X> remote = cache.get(sync.getId());
            if (remote.isPresent()) {
                // If present, update local copy with remote data
                cache.getLoggerService().debug("  Updating local Object with remote Object: " + sync.getId());
                cache.updateSyncFromNewer(sync, remote.get());
            } else {
                // Otherwise remove from the cache since for some reason we couldn't fet remote
                cache.getLoggerService().debug("  ERROR - key not found in remote, uncaching local Object: " + sync.getId());
                cache.uncache(sync);
            }
            return true;
        }

        // If not a duplicate key error, log it
        cache.getLoggerService().info(ex, "MongoDB error0 saving Profile to MongoDB Layer: " + sync.getId());
        return false;
    }

    // Kept for reference when versioning is added back
//    private <K, X extends Sync<K>> boolean handleVersionMismatchException(VersionMismatchException ex, Cache<K, X> cache, X sync) {
//        String localJson = ProfileHandshakeService.toJson(cache, sync);
//        String localErr = "Local Object: " + localJson;
//
//        AtomicReference<String> remoteErr = new AtomicReference<>("Remote Object: null");
//        cache.get(sync.getId()).ifPresent(remote -> {
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
//        String err = "Versioning error saving Object to Mongo Layer: " + sync.getIdentifier() + " (Version Mismatch)"
//                        + "\n" + localErr
//                        + "\n" + remoteErr.get();
//
//        // Log debug if we couldn't save the object due to a versioning error (shouldn't happen, but log it in case)
//        cache.getLoggerService().info(ex,  err);
//        return false;
//    }
}
