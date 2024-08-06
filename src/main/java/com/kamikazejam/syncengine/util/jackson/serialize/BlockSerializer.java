package com.kamikazejam.syncengine.util.jackson.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.bukkit.block.Block;

import java.io.IOException;

public class BlockSerializer extends JsonSerializer<Block> {
    @Override
    public void serialize(Block block, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        // Implement your custom serialization logic here
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("world", block.getWorld().getName());
        jsonGenerator.writeNumberField("x", block.getX());
        jsonGenerator.writeNumberField("y", block.getY());
        jsonGenerator.writeNumberField("z", block.getZ());
        jsonGenerator.writeEndObject();
    }
}
