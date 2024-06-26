package com.kamikazejam.syncraft.base.sync;


import com.kamikazejam.syncraft.base.Sync;
import dev.morphia.query.Query;

@SuppressWarnings("rawtypes")
public interface SyncQueryModifier<X extends Sync> {

    void apply(Query<X> query);

}
