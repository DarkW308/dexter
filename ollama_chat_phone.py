"""Ollama transport implementing Dexter's ChatModel protocol."""

from __future__ import annotations

import json
import urllib.request
from typing import Any


class OllamaChatModel:
    def __init__(
        self,
        model: str = "qwen3.5:0.8b",
        base_url: str = "http://127.0.0.1:11434",
        timeout: int = 120,
    ):
        self.model = model
        self.base_url = base_url.rstrip("/")
        self.timeout = timeout

    def create_chat_completion(self, **kwargs: Any) -> dict:
        messages = kwargs.get("messages", [])
        response_format = kwargs.get("response_format")

        schema = None
        if isinstance(response_format, dict):
            schema = response_format.get("schema")

        format_value = None
        if schema is not None:
            format_value = schema
        elif response_format == {"type": "json_object"}:
            format_value = "json"

        payload = {
            "model": self.model,
            "messages": messages,
            "stream": False,
            "think": False,
            "options": {
                "temperature": kwargs.get("temperature", 0.0),
                "num_ctx": 1024,
                "num_predict": min(
                    kwargs.get("max_tokens", 150),
                    150,
                ),
            },
        }

        if format_value is not None:
            payload["format"] = format_value

        body = json.dumps(payload).encode("utf-8")

        request = urllib.request.Request(
            f"{self.base_url}/api/chat",
            data=body,
            headers={"Content-Type": "application/json"},
            method="POST",
        )

        try:
            with urllib.request.urlopen(
                request,
                timeout=self.timeout,
            ) as response:
                raw = response.read().decode("utf-8")
        except Exception as exc:
            raise RuntimeError(
                f"Ollama request failed: {exc}"
            ) from exc

        result = json.loads(raw)

        message = result.get("message", {})
        content = message.get("content")

        if not isinstance(content, str) or not content.strip():
            raise RuntimeError(
                "Ollama returned empty response."
            )

        return {
            "choices": [
                {
                    "message": {
                        "content": content.strip(),
                    }
                }
            ]
        }


