package com.kamikazejam.syncengine.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kamikazejam.kamicommon.KamiPlugin;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@SuppressWarnings("SameParameterValue")
public class DependencyChecker {
    public static boolean isSatisfied(KamiPlugin plugin) {
        // Load the properties.json file
        InputStream properties = plugin.getResource("properties.json");
        if (properties == null) {
            plugin.getLogger().severe("Could not find properties.json file");
            Bukkit.getPluginManager().disablePlugin(plugin);
            return false;
        }
        // Load the data from properties.json
        JsonObject o = (JsonObject) JsonParser.parseReader(new InputStreamReader(properties, StandardCharsets.UTF_8));

        // Verify KamiCommon Version
        if (!verifyPluginVersion(plugin, o, "kamicommon.version", "KamiCommon", (name, ver) -> onVerFailure(plugin, name, ver))) {
            Bukkit.getPluginManager().disablePlugin(plugin);
            return false;
        }
        return true;
    }
    private static void onVerFailure(KamiPlugin plugin, String pluginName, String minVer) {
        plugin.getLogger().severe(pluginName + " version is too old! (" + minVer + " or higher required)");
    }

    private static boolean verifyPluginVersion(KamiPlugin plugin, JsonObject o, String key, String pluginName, @Nullable KamiPlugin.ErrorPropertiesCallback callback) {
        // Fetch properties.json version
        String minVer = o.get(key).getAsString();
        if (minVer == null || minVer.isEmpty()) {
            plugin.getLogger().severe("Could not find " + pluginName + " version in properties.json");
            return false;
        }
        return plugin.verifyPluginVersion(minVer, pluginName, callback);
    }
}
