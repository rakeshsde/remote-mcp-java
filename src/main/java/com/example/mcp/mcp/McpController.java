package com.example.mcp.mcp;

import com.example.mcp.model.JsonRpcRequest;
import com.example.mcp.model.JsonRpcResponse;
import com.example.mcp.tool.AddTool;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class McpController {

    private static final String CURRENT_PROTOCOL = "2026-07-28";
    private static final String LEGACY_PROTOCOL = "2025-11-25";

    private final ObjectMapper mapper;
    private final AddTool addTool;

    public McpController(ObjectMapper mapper, AddTool addTool) {
        this.mapper = mapper;
        this.addTool = addTool;
    }

    @PostMapping(value = "/mcp", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> handle(
            @RequestBody JsonRpcRequest request,
            @RequestHeader(value = "MCP-Protocol-Version", required = false) String protocolVersion,
            @RequestHeader(value = "Mcp-Method", required = false) String mcpMethod,
            @RequestHeader(value = "Mcp-Name", required = false) String mcpName) {

        if (!"2.0".equals(request.jsonrpc())) {
            return ResponseEntity.badRequest().body(error(request.id(), -32600, "Invalid Request", null));
        }

        // Modern Streamable HTTP requests are self-describing and carry routing headers.
        // We also accept legacy initialize calls so the server is easy to experiment with.
        if (protocolVersion != null && !protocolVersion.equals(CURRENT_PROTOCOL) && !protocolVersion.equals(LEGACY_PROTOCOL)) {
            return ResponseEntity.badRequest().body(error(
                    request.id(), -32602, "Unsupported MCP protocol version: " + protocolVersion, null));
        }

        if (mcpMethod != null && !mcpMethod.equals(request.method())) {
            return ResponseEntity.badRequest().body(error(
                    request.id(), -32602, "Mcp-Method does not match JSON-RPC method", null));
        }

        if ("tools/call".equals(request.method()) && mcpName != null) {
            JsonNode toolName = request.params() == null ? null : request.params().get("name");
            if (toolName != null && toolName.isTextual() && !mcpName.equals(toolName.asText())) {
                return ResponseEntity.badRequest().body(error(
                        request.id(), -32602, "Mcp-Name does not match tool name", null));
            }
        }

        try {
            JsonNode result = switch (request.method()) {
                case "initialize" -> initialize(request.params());
                case "notifications/initialized" -> null;
                case "server/discover" -> discover();
                case "tools/list" -> toolsList();
                case "tools/call" -> toolsCall(request.params());
                case "ping" -> mapper.createObjectNode();
                default -> throw new RpcException(-32601, "Method not found: " + request.method());
            };

            if (request.id() == null) {
                return ResponseEntity.noContent().build();
            }
            return resultToResponse(request.id(), result == null ? mapper.createObjectNode() : result);
        } catch (RpcException e) {
            return ResponseEntity.ok(error(request.id(), e.code, e.getMessage(), null));
        }
    }

    private ResponseEntity<JsonNode> resultToResponse(JsonNode id, JsonNode result) {
        ObjectNode response = mapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.set("id", id);
        response.set("result", result);
        return ResponseEntity.ok(response);
    }

    private JsonNode initialize(JsonNode params) {
        ObjectNode result = mapper.createObjectNode();
        result.put("protocolVersion", protocolVersionFromParams(params));

        ObjectNode capabilities = result.putObject("capabilities");
        capabilities.putObject("tools");

        ObjectNode serverInfo = result.putObject("serverInfo");
        serverInfo.put("name", "remote-java-mcp");
        serverInfo.put("version", "0.0.1");
        return result;
    }

    private String protocolVersionFromParams(JsonNode params) {
        if (params != null && params.has("protocolVersion")) {
            String requested = params.get("protocolVersion").asText();
            if (CURRENT_PROTOCOL.equals(requested) || LEGACY_PROTOCOL.equals(requested)) {
                return requested;
            }
        }
        return LEGACY_PROTOCOL;
    }

    private JsonNode discover() {
        ObjectNode result = mapper.createObjectNode();
        result.put("protocolVersion", CURRENT_PROTOCOL);
        ObjectNode serverInfo = result.putObject("serverInfo");
        serverInfo.put("name", "remote-java-mcp");
        serverInfo.put("version", "0.0.1");
        ObjectNode capabilities = result.putObject("capabilities");
        capabilities.putObject("tools");
        return result;
    }

    private JsonNode toolsList() {
        ObjectNode result = mapper.createObjectNode();
        ArrayNode tools = result.putArray("tools");

        ObjectNode add = tools.addObject();
        add.put("name", "add");
        add.put("description", "Add two numbers");

        ObjectNode inputSchema = add.putObject("inputSchema");
        inputSchema.put("type", "object");
        ObjectNode properties = inputSchema.putObject("properties");
        properties.putObject("a").put("type", "number");
        properties.putObject("b").put("type", "number");
        ArrayNode required = inputSchema.putArray("required");
        required.add("a");
        required.add("b");
        return result;
    }

    private JsonNode toolsCall(JsonNode params) {
        if (params == null || !params.has("name")) {
            throw new RpcException(-32602, "Missing tool name");
        }

        String name = params.get("name").asText();
        if (!"add".equals(name)) {
            throw new RpcException(-32602, "Unknown tool: " + name);
        }

        try {
            JsonNode value = addTool.execute(params.get("arguments"), mapper);
            ObjectNode result = mapper.createObjectNode();
            ArrayNode content = result.putArray("content");
            ObjectNode text = content.addObject();
            text.put("type", "text");
            text.put("text", value.asText());
            result.put("isError", false);
            return result;
        } catch (IllegalArgumentException e) {
            ObjectNode result = mapper.createObjectNode();
            result.put("isError", true);
            ArrayNode content = result.putArray("content");
            ObjectNode text = content.addObject();
            text.put("type", "text");
            text.put("text", e.getMessage());
            return result;
        }
    }

    private JsonNode error(JsonNode id, int code, String message, JsonNode data) {
        ObjectNode response = mapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        if (id == null) {
            response.putNull("id");
        } else {
            response.set("id", id);
        }
        ObjectNode error = response.putObject("error");
        error.put("code", code);
        error.put("message", message);
        if (data != null) {
            error.set("data", data);
        }
        return response;
    }

    private static final class RpcException extends RuntimeException {
        private final int code;

        private RpcException(int code, String message) {
            super(message);
            this.code = code;
        }
    }
}
