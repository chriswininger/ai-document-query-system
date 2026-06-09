#!/usr/bin/env python3
"""
MCP Chat Harness — interactive CLI chat using Ollama + MCP tools from document-mcp.
"""

import asyncio
import sys

from langchain_ollama import ChatOllama
from langchain_mcp_adapters.client import MultiServerMCPClient
from langgraph.prebuilt import create_react_agent

MCP_SERVER_URL = "http://localhost:8081/mcp"
OLLAMA_MODEL = "gemma4:e4b"
OLLAMA_BASE_URL = "http://localhost:11434"

SYSTEM_PROMPT = (
    "You are a helpful research assistant with access to a document library. "
    "Use the available tools to search and retrieve documents, chapters, and sections "
    "related to users question. Based on the users question create a research plan out of the existing tools. What tools"
    "What series of queries could you make to try to find an answer to the question? Return a step by step plan another"
    "agent can follow."
)


async def main():
    print(f"Connecting to MCP server at {MCP_SERVER_URL}...")
    print(f"Using Ollama model: {OLLAMA_MODEL}")
    print()

    llm = ChatOllama(
        model=OLLAMA_MODEL,
        base_url=OLLAMA_BASE_URL,
        num_ctx=65536,
        think=True,
    )

    mcp_client = MultiServerMCPClient(
        {
            "document-mcp": {
                "transport": "streamable_http",
                "url": MCP_SERVER_URL,
            }
        }
    )

    tools = await mcp_client.get_tools()
    print(f"Discovered {len(tools)} tools:")
    for tool in tools:
        print(f"  - {tool.name}: {tool.description[:80]}")
    print()

    agent = create_react_agent(llm, tools)

    messages = [{"role": "system", "content": SYSTEM_PROMPT}]

    print("Chat ready. Type 'quit' or 'exit' to stop.\n")
    print("-" * 60)

    while True:
        try:
            user_input = input("\nYou: ").strip()
        except (EOFError, KeyboardInterrupt):
            print("\nBye!")
            break

        if not user_input:
            continue
        if user_input.lower() in ("quit", "exit"):
            print("Bye!")
            break

        messages.append({"role": "user", "content": user_input})

        print()
        thinking_active = False
        response_active = False
        full_response = ""

        async for chunk, metadata in agent.astream(
            {"messages": messages}, stream_mode="messages"
        ):
            # Tool result messages — show a brief preview
            if getattr(chunk, "type", None) == "tool":
                if thinking_active:
                    print()
                    thinking_active = False
                raw = chunk.content if isinstance(chunk.content, str) else str(chunk.content)
                preview = raw[:200] + ("..." if len(raw) > 200 else "")
                print(f"\n  [Tool Result] {preview}", flush=True)
                response_active = False
                continue

            # Only process AI message chunks from here on
            if not hasattr(chunk, "additional_kwargs"):
                continue

            # Completed tool calls (streamed as a full chunk)
            if getattr(chunk, "tool_calls", None):
                if thinking_active:
                    print()
                    thinking_active = False
                for tc in chunk.tool_calls:
                    print(f"\n  [Tool Call] {tc['name']}({tc['args']})", flush=True)
                response_active = False
                continue

            # Thinking tokens
            thinking_delta = (chunk.additional_kwargs or {}).get("thinking", "")
            if thinking_delta:
                if not thinking_active:
                    print("  [Thinking] ", end="", flush=True)
                    thinking_active = True
                print(thinking_delta, end="", flush=True)

            # Response tokens
            if chunk.content:
                if not response_active:
                    if thinking_active:
                        print("\n")
                        thinking_active = False
                    print("Assistant: ", end="", flush=True)
                    response_active = True
                print(chunk.content, end="", flush=True)
                full_response += chunk.content

        print()
        messages.append({"role": "assistant", "content": full_response})


if __name__ == "__main__":
    asyncio.run(main())
