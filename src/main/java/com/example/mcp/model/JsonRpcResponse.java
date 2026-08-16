package com.example.mcp.model;

import com.fasterxml.jackson.databind.JsonNode;

public record JsonRpcResponse(
        String jsonrpc,
        JsonNode id,
        JsonNode result,
        JsonNode error
) {
    public static JsonRpcResponse success(JsonNode id, JsonNode result) {
        return new JsonRpcResponse("2.0", id, result, null);
    }

    public static JsonRpcResponse error(JsonNode id, int code, String message, JsonNode data) {
        var node = new com.fasterxml.jackson.databind.node.ObjectNode(
                com.fasterxml.jackson.databind.node.JsonNodeFactory.instance);
        node.put("code", code);
        node.put("message", message);
        if (data != null) {
            node.set("data", data);
        }
        return new JsonRpcResponse("2.0", id, null, node);
    }
}
