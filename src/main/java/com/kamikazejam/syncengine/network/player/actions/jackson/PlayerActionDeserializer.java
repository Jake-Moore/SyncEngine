package com.kamikazejam.syncengine.network.player.actions.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.kamikazejam.syncengine.network.player.actions.PlayerAction;
import com.kamikazejam.syncengine.network.player.actions.PlayerActionType;

import java.io.IOException;

public class PlayerActionDeserializer extends JsonDeserializer<PlayerAction> {

    @Override
    public PlayerAction deserialize(JsonParser jp, DeserializationContext context) throws IOException {
        ObjectCodec codec = jp.getCodec();
        JsonNode node = codec.readTree(jp);
        String typeStr = node.get("type").asText();
        PlayerActionType type = PlayerActionType.valueOf(typeStr);

        return codec.treeToValue(node, type.getClazz());
    }
}
