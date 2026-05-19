#!/usr/bin/env python3
"""Validate chat SFT JSONL files and print lightweight data-quality stats."""

from __future__ import annotations

import argparse
import json
import re
from collections import Counter
from pathlib import Path
from statistics import mean
from typing import Any


EXPECTED_ROLE_ORDER = ["system", "user", "assistant"]
SENSITIVE_PATTERNS = {
    "possible_phone": re.compile(r"(?<!\d)1[3-9]\d{9}(?!\d)"),
    "possible_id_card": re.compile(r"(?<!\d)\d{17}[\dXx](?!\d)"),
    "possible_api_key": re.compile(r"(?i)(api[_-]?key|secret|token|password)\s*[:=]\s*['\"]?[\w\-]{8,}"),
}


def repo_root() -> Path:
    return Path(__file__).resolve().parents[2]


def read_jsonl(path: Path) -> list[tuple[int, dict[str, Any]]]:
    if not path.exists():
        raise FileNotFoundError(f"{path} does not exist. Run build_sft_dataset.py first.")

    records: list[tuple[int, dict[str, Any]]] = []
    with path.open("r", encoding="utf-8") as file:
        for line_number, line in enumerate(file, start=1):
            text = line.strip()
            if not text:
                continue
            try:
                value = json.loads(text)
            except json.JSONDecodeError as exc:
                raise ValueError(f"{path}:{line_number} is not valid JSON: {exc}") from exc
            if not isinstance(value, dict):
                raise ValueError(f"{path}:{line_number} must be a JSON object")
            records.append((line_number, value))
    return records


def validate_file(path: Path) -> dict[str, Any]:
    records = read_jsonl(path)
    errors: list[str] = []
    warnings: list[str] = []
    role_counter: Counter[str] = Counter()
    char_lengths: list[int] = []
    user_assistant_pairs: Counter[str] = Counter()

    for line_number, record in records:
        example_id = record.get("id", f"{path.name}:{line_number}")
        messages = record.get("messages")
        if not isinstance(messages, list):
            errors.append(f"{example_id}: field 'messages' must be a list")
            continue
        if len(messages) != 3:
            errors.append(f"{example_id}: expected exactly 3 messages, got {len(messages)}")
            continue

        roles: list[str] = []
        contents: list[str] = []
        for index, message in enumerate(messages):
            if not isinstance(message, dict):
                errors.append(f"{example_id}: message[{index}] must be an object")
                continue
            role = message.get("role")
            content = message.get("content")
            if role not in {"system", "user", "assistant"}:
                errors.append(f"{example_id}: message[{index}].role is invalid: {role!r}")
            if not isinstance(content, str) or not content.strip():
                errors.append(f"{example_id}: message[{index}].content must be a non-empty string")
                content = ""
            roles.append(str(role))
            contents.append(content)
            role_counter[str(role)] += 1
            char_lengths.append(len(content))
            check_sensitive_content(content, example_id, index, warnings)

        if roles != EXPECTED_ROLE_ORDER:
            errors.append(f"{example_id}: expected role order {EXPECTED_ROLE_ORDER}, got {roles}")
        if len(contents) == 3:
            pair_key = normalize_pair(contents[1], contents[2])
            user_assistant_pairs[pair_key] += 1

    duplicates = [count for count in user_assistant_pairs.values() if count > 1]
    duplicate_count = sum(count - 1 for count in duplicates)
    if duplicate_count:
        warnings.append(f"Found {duplicate_count} duplicate user/assistant pair(s)")

    return {
        "path": path,
        "examples": len(records),
        "errors": errors,
        "warnings": warnings,
        "role_counter": role_counter,
        "char_lengths": char_lengths,
        "duplicate_count": duplicate_count,
    }


def check_sensitive_content(content: str, example_id: str, message_index: int, warnings: list[str]) -> None:
    for name, pattern in SENSITIVE_PATTERNS.items():
        if pattern.search(content):
            warnings.append(f"{example_id}: message[{message_index}] matched {name}")


def normalize_pair(user: str, assistant: str) -> str:
    return " ".join((user + "\n" + assistant).split())


def print_report(reports: list[dict[str, Any]]) -> None:
    total_errors = 0
    total_warnings = 0

    for report in reports:
        total_errors += len(report["errors"])
        total_warnings += len(report["warnings"])
        lengths = report["char_lengths"]

        print(f"\nFile: {report['path']}")
        print(f"  examples: {report['examples']}")
        print(f"  roles:    {dict(report['role_counter'])}")
        if lengths:
            print(
                "  length:   "
                f"min={min(lengths)}, avg={mean(lengths):.1f}, max={max(lengths)} chars"
            )
        print(f"  duplicate user/assistant pairs: {report['duplicate_count']}")

        for warning in report["warnings"]:
            print(f"  WARNING: {warning}")
        for error in report["errors"]:
            print(f"  ERROR: {error}")

    print("\nValidation summary")
    print(f"  files:    {len(reports)}")
    print(f"  errors:   {total_errors}")
    print(f"  warnings: {total_warnings}")

    if total_errors:
        raise SystemExit(1)


def parse_args() -> argparse.Namespace:
    root = repo_root()
    default_processed = root / "finetune" / "data" / "processed"
    parser = argparse.ArgumentParser(description="Validate chat SFT JSONL files.")
    parser.add_argument(
        "paths",
        nargs="*",
        type=Path,
        default=[default_processed / "train.jsonl", default_processed / "validation.jsonl"],
        help="JSONL files to validate. Defaults to processed train and validation files.",
    )
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    reports = [validate_file(path) for path in args.paths]
    print_report(reports)


if __name__ == "__main__":
    main()
