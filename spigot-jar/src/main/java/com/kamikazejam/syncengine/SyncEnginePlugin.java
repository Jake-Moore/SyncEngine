package com.kamikazejam.syncengine;

import com.kamikazejam.kamicommon.KamiPlugin;
import lombok.Getter;

@Getter
@SuppressWarnings("unused")
public class SyncEnginePlugin extends KamiPlugin {

    /**
     * This class is nothing more than a loader for all SyncEngine logic
     * It supplies PluginSource with this plugin object so that SyncEngine can be initialized
     * SyncEngine can be shaded into your own project, where you'll just have to mirror these method
     *  calls in your own plugin, to initialize SyncEngine
     */

    @Override
    public void onEnableInner() {
        EngineSource.onEnable(this);
    }

    @Override
    public void onDisableInner() {
        EngineSource.onDisable();
    }

    // Disables auto-loading of default config.yml (SyncEngine uses syncengine.yml)
    @Override
    public boolean loadKamiConfig() {
        return false;
    }
}
