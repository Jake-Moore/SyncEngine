package com.kamikazejam.syncengine.connections.storage;

import com.google.common.base.Preconditions;
import com.kamikazejam.syncengine.EngineSource;
import com.kamikazejam.syncengine.base.Cache;
import com.kamikazejam.syncengine.base.Sync;
import com.kamikazejam.syncengine.base.exception.VersionMismatchException;
import com.kamikazejam.syncengine.connections.config.MongoConf;
import com.kamikazejam.syncengine.connections.monitor.MongoMonitor;
import com.kamikazejam.syncengine.connections.redis.RedisService;
import com.kamikazejam.syncengine.connections.storage.iterable.TransformingIterator;
import com.kamikazejam.syncengine.util.JacksonUtil;
import com.mongodb.*;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.bson.UuidRepresentation;
import org.bson.conversions.Bson;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mongojack.JacksonMongoCollection;

import java.util.*;
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
            this.error("Failed to start MongoStorage, connection failed.");
            return false;
        }
        // Client was created, MongoMonitor should log more as the connection succeeds or fails
        return true;
    }

    @Override
    public boolean shutdown() {
        // If not running, warn and return true (we are already shutdown)
        if (!running) {
            this.warn("MongoStorage.shutdown() called while service is not running!");
            return true;
        }

        // Disconnect from MongoDB
        boolean mongo = this.disconnectMongo();
        this.running = false;

        if (!mongo) {
            this.error("Failed to shutdown MongoStorage, disconnect failed.");
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
            Bson query = Filters.eq(ID_FIELD, cache.keyToString(key));
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
    public <K, X extends Sync<K>> boolean save(Cache<K, X> cache, X sync) throws VersionMismatchException {

        // Try saving to MongoDB with Jackson and catch/fix a host of possible errors we can receive
        try {
            // **Optimistic Versioning**
            //  1. Fetch the database object (to compare version)
            //     - Use Projections to retrieve only the necessary fields
            Bson query = Filters.eq(ID_FIELD, cache.keyToString(sync.getId()));
            @Nullable X database = getJackson(cache).find(query).projection(Projections.include(ID_FIELD, "version")).first();
            @Nullable Long dbVer = (database == null) ? null : database.getVersion();

            // 2. Apply Optimistic Versioning (only fails with a valid, non-equal database version)
            if (dbVer != null && dbVer != sync.getVersion()) {
                throw new VersionMismatchException(cache, sync.getVersion(), dbVer);
            }

            // 3. Continue (Version passed, save the object)
            sync.setVersion(sync.getVersion() + 1);
            getJackson(cache).save(sync);
            return true;
        } catch (VersionMismatchException v) {
            // pass through
            throw v;
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
            Bson query = Filters.eq(ID_FIELD, cache.keyToString(key));
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
            Bson query = Filters.eq(ID_FIELD, cache.keyToString(key));
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
    public <K, X extends Sync<K>> Iterable<K> getKeys(Cache<K, X> cache) {
        // Fetch all documents, but use Projection to only retrieve the ID field
        Iterator<X> iterator = getJackson(cache).find().projection(Projections.include(ID_FIELD)).iterator();
        return () -> new TransformingIterator<>(iterator, Sync::getId);
    }

    @Override
    public boolean canCache() {
        @Nullable RedisService redisService = EngineSource.getRedisService();
        // If we have Redis on this instance, check both MongoDB and Redis
        if (redisService != null) {
            return mongoConnected && redisService.isRedisConnected();
        }
        // Otherwise just check that MongoDB is connected
        return mongoConnected;
    }

    // ------------------------------------------------- //
    //                MongoDB Connection                 //
    // ------------------------------------------------- //
    public boolean connectMongo() {
        // Can only have one MongoClient instance
        Preconditions.checkState(this.mongoClient == null, "[MongoStorage] MongoClient instance already exists!");

        try {
            MongoClientSettings.Builder settingsBuilder = MongoClientSettings.builder()
                    .uuidRepresentation(UuidRepresentation.STANDARD)
                    .applyToServerSettings(builder -> builder.addServerMonitorListener(new MongoMonitor(this)));

            // Using connection URI
            ConnectionString connectionString = new ConnectionString(MongoConf.get().getUri());
            settingsBuilder.applyConnectionString(connectionString);
            this.mongoClient = MongoClients.create(settingsBuilder.build());

            // Verify this client connection is valid
            try {
                EngineSource.get().getLogger().info("CONNECTING TO MONGODB (30 second timeout)...");
                List<String> ignored = StreamSupport.stream(this.mongoClient.listDatabaseNames().spliterator(), false).toList();
            }catch (MongoTimeoutException ignored) {
                // Connection failed (invalid?)
                return false;
            }catch (Throwable t) {
                t.printStackTrace();
                return false;
            }

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
        String key = cache.getDatabaseName() + "." + cache.getName();
        if (collMap.containsKey(key)) {
            return (JacksonMongoCollection<X>) collMap.get(key);
        }

        // Create a new JacksonMongoCollection
        JacksonMongoCollection<X> coll = JacksonMongoCollection.builder()
                .withObjectMapper(getMapper())
                .build(mongoClient, cache.getDatabaseName(), cache.getName(), cache.getSyncClass(), UuidRepresentation.STANDARD);

        collMap.put(key, coll);
        return coll;
    }


    // ------------------------------------------------- //
    //                   LoggerService                   //
    // ------------------------------------------------- //
    @Override
    public boolean isDebug() {
        return EngineSource.isDebug();
    }

    @Override
    public Plugin getPlugin() {
        return EngineSource.get();
    }

    @Override
    public String getLoggerName() {
        return "MongoStorage";
    }


    // ------------------------------------------------- //
    //                Helper Methods                     //
    // ------------------------------------------------- //

    @SuppressWarnings("SameReturnValue")
    private <K, X extends Sync<K>> boolean handleMongoWriteException(MongoWriteException ex, Cache<K, X> cache, X sync) {
        // There's a chance on start we may create and try to save an object that is being created by another instance
        //  at this very moment, in that case we should fetch the remote, and update our local copy with the remote
        if (ex.getError().getCategory() == ErrorCategory.DUPLICATE_KEY) {
            // There's no 'good' resolution strategy, we should probably just log the error
            //  and hope the developer solves the issue on their end.
            // Two options exist for auto-correcting this error:
            //  1. Update local copy with remote data (we lose whatever was in local)
            //  2. Update remote with local (we lose whatever was in remote)

            cache.getLoggerService().debug("Duplicate key error saving Object to MongoDB Layer: " + cache.getSyncClass().getName() + " - " + sync.getId());
            cache.getLoggerService().debug("  Local Object Json: " + JacksonUtil.toJson(sync));

            Optional<X> remote = cache.getFromDatabase(sync.getId(), false);
            remote.ifPresent(x -> {
                String json = JacksonUtil.toJson(x);
                cache.getLoggerService().debug("  Remote Object Json: " + json);
            });
            // Technically did not resolve the issue
            return false;
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
