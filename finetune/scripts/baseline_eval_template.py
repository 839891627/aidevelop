#!/usr/bin/env python3
"""Template for running baseline eval prompts before and after fine-tuning.

The script can run in dry mode without any API key. When you are ready to
connect a model provider, set OPENAI_API_KEY and optionally OPENAI_BASE_URL /
OPENAI_CHAT_MODEL, then run with --call-api.
"""

from __future__ import annotations

import argparse
import json
import os
import urllib.error
import urllib.request
from pathlib import Path
from typing import Any


DEFAULT_BASE_URL = "https://api.openai.com/v1"
DEFAULT_MODEL = "gpt-4o-mini"


def repo_root() -> Path:
    return Path(__file__).resolve().parents[2]


def read_jsonl(path: Path) -> list[dict[str, Any]]:
    prompts: list[dict[str, Any]] = []
    with path.open("r", encoding="utf-8") as file:
        for line_number, line in enumerate(file, start=1):
            text = line.strip()
            if not text:
                continue
            try:
                prompt = json.loads(text)
            except json.JSONDecodeError as exc:
                raise ValueError(f"{path}:{line_number} is not valid JSON: {exc}") from exc
            prompts.append(prompt)
    return prompts


def build_messages(prompt: dict[str, Any]) -> list[dict[str, str]]:
    system = require_text(prompt, "system")
    user = require_text(prompt, "user")
    return [
        {"role": "system", "content": system},
        {"role": "user", "content": user},
    ]


def require_text(record: dict[str, Any], key: str) -> str:
    value = record.get(key)
    if not isinstance(value, str) or not value.strip():
        raise ValueError(f"{record.get('id', '<unknown>')}: field '{key}' must be a non-empty string")
    return value.strip()


def call_chat_completion(base_url: str, api_key: str, model: str, messages: list[dict[str, str]]) -> str:
    url = base_url.rstrip("/") + "/chat/completions"
    payload = {
        "model": model,
        "messages": messages,
        "temperature": 0.2,
    }
    request = urllib.request.Request(
        url=url,
        data=json.dumps(payload).encode("utf-8"),
        headers={
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json",
        },
        method="POST",
    )

    try:
        with urllib.request.urlopen(request, timeout=60) as response:
            body = json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as exc:
        detail = exc.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"Provider returned HTTP {exc.code}: {detail}") from exc

    return body["choices"][0]["message"]["content"]


def run_eval(args: argparse.Namespace) -> None:
    prompts = read_jsonl(args.prompts)
    output_records: list[dict[str, Any]] = []

    api_key = os.getenv("OPENAI_API_KEY", "")
    base_url = os.getenv("OPENAI_BASE_URL", DEFAULT_BASE_URL)
    model = os.getenv("OPENAI_CHAT_MODEL", DEFAULT_MODEL)

    if args.call_api and not api_key:
        raise SystemExit("OPENAI_API_KEY is required when --call-api is used")

    for prompt in prompts:
        messages = build_messages(prompt)
        if args.call_api:
            answer = call_chat_completion(base_url, api_key, model, messages)
        else:
            answer = "[DRY RUN] Set OPENAI_API_KEY and pass --call-api to collect model output."

        output_records.append(
            {
                "id": prompt.get("id"),
                "category": prompt.get("category"),
                "model": model if args.call_api else "dry-run",
                "messages": messages,
                "answer": answer,
                "expected_behavior": prompt.get("expected_behavior", []),
            }
        )

    args.output.parent.mkdir(parents=True, exist_ok=True)
    with args.output.open("w", encoding="utf-8") as file:
        for record in output_records:
            file.write(json.dumps(record, ensure_ascii=False, separators=(",", ":")) + "\n")

    print(f"Eval results written to {args.output}")
    print(f"Prompts: {len(output_records)}")
    if not args.call_api:
        print("Dry run only. No model provider was called.")


def parse_args() -> argparse.Namespace:
    root = repo_root()
    parser = argparse.ArgumentParser(description="Run fixed eval prompts against a chat model.")
    parser.add_argument(
        "--prompts",
        type=Path,
        default=root / "finetune" / "data" / "eval" / "eval_prompts.jsonl",
        help="Eval prompts JSONL file.",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=root / "finetune" / "data" / "eval" / "baseline_results.jsonl",
        help="Output JSONL file for model answers.",
    )
    parser.add_argument(
        "--call-api",
        action="store_true",
        help="Actually call an OpenAI-compatible chat completions API.",
    )
    return parser.parse_args()


def main() -> None:
    run_eval(parse_args())


if __name__ == "__main__":
    main()
