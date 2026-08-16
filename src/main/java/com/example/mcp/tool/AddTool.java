package com.example.mcp.tool;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

@Component
public class AddTool {

    public JsonNode execute(
            JsonNode arguments,
            com.fasterxml.jackson.databind.ObjectMapper mapper) {

        if (arguments == null
                || !arguments.has("a")
                || !arguments.has("b")
                || !arguments.get("a").isNumber()
                || !arguments.get("b").isNumber()) {

            throw new IllegalArgumentException(
                    "arguments must contain numeric fields 'a' and 'b'");
        }

        double result =
                arguments.get("a").asDouble()
                        + arguments.get("b").asDouble();

        return mapper.getNodeFactory().numberNode(result);
    }
}
