package com.kamikazejam.syncengine.base.sync;


import com.kamikazejam.syncengine.base.Sync;
import dev.morphia.query.Query;

@SuppressWarnings("rawtypes")
public interface SyncQueryModifier<X extends Sync> {

    void apply(Query<X> query);

}
