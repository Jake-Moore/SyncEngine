package com.kamikazejam.syncengine.network.player;

import com.kamikazejam.kamicommon.util.JacksonUtil;
import com.kamikazejam.kamicommon.util.PlayerUtil;
import com.kamikazejam.syncengine.EngineSource;
import com.kamikazejam.syncengine.network.player.actions.PlayerAction;
import com.kamikazejam.syncengine.network.profile.NetworkProfile;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Utility wrapper for accessing {@link Player} information, and some modifications.<br>
 * {@link Player} information and modifications are fetch
 */
@Getter
@SuppressWarnings("unused")
public class NetworkPlayer {
    private final @NotNull UUID uuid;
    public NetworkPlayer(@NotNull UUID uuid) {
        this.uuid = uuid;
    }

    public @NotNull NetworkProfile getProfile() {
        // Always fetch from NetworkService to ensure validity
        return EngineSource.getNetworkService().getOrCreate(uuid);
    }

    // ---------------------------------------------------------------------------- //
    //                         NetworkProfile Mirror Methods                        //
    // ---------------------------------------------------------------------------- //

    public boolean isOnline() {
        return getProfile().isOnline();
    }

    public boolean isOnlineOtherServer() {
        return getProfile().isOnlineOtherServer();
    }

    public @NotNull Optional<String> getUsername() {
        return getProfile().getUsernameOptional();
    }

    public @NotNull String getUsernameOrUUID() {
        return getUsername().orElse(uuid.toString());
    }

    /**
     * @return The raw hostname of the ip this player joined on. Return empty if not connected currently.
     */
    public Optional<String> getCurrentIP() {
        return getPlayer().map(player -> player.getAddress().getHostName());
    }

    /**
     * @return The raw hostname of the ip this player joined on, or last joined with. Return empty if never joined / no data found.
     */
    public Optional<String> getCurrentIPOrLastKnown() {
        Optional<Player> o = getPlayer();
        return o.map(player -> player.getAddress().getHostName())
                .or(() -> Optional.ofNullable(getProfile().getLastSeenIP()));
    }




    // ---------------------------------------------------------------------------- //
    //                              Player Access Methods                           //
    // ---------------------------------------------------------------------------- //

    public @NotNull Optional<Player> getPlayer() {
        @Nullable Player player = Bukkit.getPlayer(uuid);
        if (PlayerUtil.isFullyValidPlayer(player)) {
            return Optional.of(player);
        }
        return Optional.empty();
    }

    /**
     * @return A future that completes with the result of the action (player found & performed OR not found & not executed)
     */
    @NotNull
    public CompletableFuture<Boolean> completeAction(@NotNull PlayerAction action) {
        // If we have the player on this server -> we can directly fulfill the action using the player
        @Nullable Player player = getPlayer().orElse(null);
        if (PlayerUtil.isFullyValidPlayer(player)) {
            // Keep this a sync operation since we have the player
            // Just return an already completed future (no delay)
            action.perform(player);
            return CompletableFuture.completedFuture(true);
        }

        // Otherwise, we need to serialize the action and send it to the player's server
        String json = JacksonUtil.serialize(action);

        // TODO figure out if redis or rabbitMQ is the best way of doing this
        //  IDEALLY we have an RPC that responds with the result of the action (if the player could be found)

        throw new UnsupportedOperationException("Not implemented yet");
    }




}
