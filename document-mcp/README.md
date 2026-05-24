# document-mcp

MCP (Model Context Protocol) server that wraps the document-ingestion-api, exposing document search and retrieval as tools for AI assistants.

Built with Quarkus and [quarkus-mcp-server-http](https://docs.quarkiverse.io/quarkus-mcp-server/dev/).

## Prerequisites

- Java 25 (Amazon Corretto via SDKman)
- `document-ingestion-api` running on `http://localhost:8080`

## Running

```bash
./gradlew quarkusDev
```

The MCP server starts on **port 8081**. The Streamable HTTP endpoint is at:

```
http://localhost:8081/mcp
```

## Available Tools

| Tool | Description |
|------|-------------|
| `listDocuments` | List all ingested documents with title, author, and summary |
| `getDocument` | Get a specific document by ID |
| `listChaptersByDocument` | List all chapters for a document |
| `getChapter` | Get a chapter by ID with optional full text |
| `listSectionsByChapter` | List sections within a chapter |
| `listSectionsByDocument` | Paginated sections across all chapters in a document |
| `getSectionBySequence` | Get a section by its sequence number within a chapter |
| `getSection` | Get a section by ID with optional full text |
| `getBookMetadataByDocument` | Get book metadata (author, publisher, year, characters) for a document |
| `getBookMetadata` | Get book metadata by its own ID |
| `semanticSearch` | Search documents using semantic similarity, with optional document/chapter filters |

## Configuring Cursor

Add the following to your Cursor MCP settings (`.cursor/mcp.json` in your project root or `~/.cursor/mcp.json` globally):

```json
{
  "mcpServers": {
    "document-mcp": {
      "url": "http://localhost:8081/mcp"
    }
  }
}
```

After saving, restart the Cursor agent or open a new session. The tools will appear in the agent's available tool list.

## Testing

### MCP Inspector

The quickest way to verify tools work:

```bash
npx @modelcontextprotocol/inspector
```

Opens a browser UI where you can point at `http://localhost:8081/mcp`, list tools, and invoke them interactively.

### curl

```bash
# Initialize a session
curl -s -D - -X POST http://localhost:8081/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-03-26","capabilities":{},"clientInfo":{"name":"test","version":"1.0"}}}'

# Use the Mcp-Session-Id from the response headers in subsequent requests:
curl -s -X POST http://localhost:8081/mcp \
  -H "Content-Type: application/json" \
  -H "Mcp-Session-Id: <session-id-from-above>" \
  -d '{"jsonrpc":"2.0","id":2,"method":"tools/list"}'
```

## Configuration

Key properties in `src/main/resources/application.properties`:

| Property | Default | Description |
|----------|---------|-------------|
| `quarkus.http.port` | `8081` | Port for the MCP server |
| `quarkus.rest-client.document-api.url` | `http://localhost:8080` | Base URL of the document-ingestion-api |
