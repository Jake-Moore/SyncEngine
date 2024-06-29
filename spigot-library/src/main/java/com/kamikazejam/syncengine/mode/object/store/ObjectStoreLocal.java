package com.kamikazejam.syncengine.mode.object.store;

import com.kamikazejam.syncengine.base.store.StoreLocal;
import com.kamikazejam.syncengine.mode.object.SyncObject;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class ObjectStoreLocal<X extends SyncObject> extends StoreLocal<String, X> {
    public ObjectStoreLocal() {}

    @Override
    public @NotNull String getLayerName() {
        return "Object Local";
    }
}
