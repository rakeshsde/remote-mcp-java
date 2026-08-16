# Remote Java MCP Server

Minimal MCP server built from scratch in Java/Spring Boot for learning remote MCP over HTTP.

## Features

- `POST /mcp`
- Streamable HTTP-style MCP headers: `MCP-Protocol-Version`, `Mcp-Method`, `Mcp-Name`
- API-key authentication using `Authorization: Bearer <key>`
- `server/discover`
- legacy-compatible `initialize`
- `tools/list`
- `tools/call`
- `ping`
- `add(a, b)` tool
- Docker/Render deployment

## Local run

Set the API key:

```bash
export MCP_API_KEY='demo-secret'
./mvnw spring-boot:run
```

or:

```bash
export MCP_API_KEY='demo-secret'
java -jar target/remote-mcp-java-0.0.1-SNAPSHOT.jar
```

The server listens on `http://localhost:8080/mcp`.

## Test authentication

No token:

```bash
curl -i http://localhost:8080/mcp \
  -H 'Content-Type: application/json' \
  -H 'MCP-Protocol-Version: 2026-07-28' \
  -H 'Mcp-Method: tools/list' \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}'
```

Expected: `401 Unauthorized`.

Correct token:

```bash
curl -i http://localhost:8080/mcp \
  -H 'Authorization: Bearer demo-secret' \
  -H 'Content-Type: application/json' \
  -H 'MCP-Protocol-Version: 2026-07-28' \
  -H 'Mcp-Method: tools/list' \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}'
```

Call the tool:

```bash
curl -i http://localhost:8080/mcp \
  -H 'Authorization: Bearer demo-secret' \
  -H 'Content-Type: application/json' \
  -H 'MCP-Protocol-Version: 2026-07-28' \
  -H 'Mcp-Method: tools/call' \
  -H 'Mcp-Name: add' \
  -d '{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"add","arguments":{"a":2,"b":3}}}'
```

Expected tool result is `5.0`.

## Render

1. Push this repo to GitHub.
2. Render Dashboard -> New -> Web Service -> connect the repo.
3. Choose Docker as the runtime.
4. Choose the Free instance for testing.
5. Add environment variable `MCP_API_KEY` with a strong random value.
6. Deploy.

Render provides a public `onrender.com` URL and managed TLS for web services. Free services spin down after 15 minutes without inbound traffic, so the first request after idle can be slow.

## Important

The API key is deliberately a transport-level concern. It is not part of `tools/call.arguments`.

For example:

```text
HTTP header:
Authorization: Bearer demo-secret

JSON-RPC body:
{
  "method": "tools/call",
  "params": {
    "name": "add",
    "arguments": {"a": 2, "b": 3}
  }
}
```
