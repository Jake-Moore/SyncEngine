package com.kamikazejam.syncengine.util.jackson;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.kamikazejam.syncengine.util.jackson.deserialize.BlockDeserializer;
import com.kamikazejam.syncengine.util.jackson.serialize.BlockSerializer;
import org.bukkit.block.Block;

public class JacksonSpigotModule extends SimpleModule {
    public JacksonSpigotModule() {
        super("JacksonSpigotModule", Version.unknownVersion());
        // Block
        addSerializer(Block.class, new BlockSerializer());
        addDeserializer(Block.class, new BlockDeserializer());
    }
}
