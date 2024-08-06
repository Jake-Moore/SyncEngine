package com.kamikazejam.syncengine.util.jackson.deserialize;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.io.IOException;
import java.util.Objects;

public class BlockDeserializer extends JsonDeserializer<Block> {
    @Override
    public Block deserialize(JsonParser jp, DeserializationContext context) throws IOException, JacksonException {
        ObjectCodec codec = jp.getCodec();
        JsonNode node = codec.readTree(jp);

        String worldName = node.get("world").asText();
        World world = Objects.requireNonNull(Bukkit.getWorld(worldName));

        return world.getBlockAt(
                node.get("x").asInt(),
                node.get("y").asInt(),
                node.get("z").asInt()
        );
    }
}
