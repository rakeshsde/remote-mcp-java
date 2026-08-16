package com.example.mcp.model;

import com.fasterxml.jackson.databind.JsonNode;

public record JsonRpcRequest(
        String jsonrpc,
        JsonNode id,
        String method,
        JsonNode params
) {}
