#!/usr/bin/env python3
"""Build a small chat SFT dataset from raw loan-assistant examples."""

from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Any


DEFAULT_SYSTEM_PROMPT = (
    "你是一个合规、谨慎、专业的金融贷款助手。回答时不要承诺审批结果，"
    "不编造客户数据，涉及制度和客户信息时提醒以系统查询和正式规则为准。"
)


def repo_root() -> Path:
    return Path(__file__).resolve().parents[2]


def read_jsonl(path: Path) -> list[dict[str, Any]]:
    records: list[dict[str, Any]] = []
    with path.open("r", encoding="utf-8") as file:
        for line_number, line in enumerate(file, start=1):
            text = line.strip()
            if not text:
                continue
            try:
                record = json.loads(text)
            except json.JSONDecodeError as exc:
                raise ValueError(f"{path}:{line_number} is not valid JSON: {exc}") from exc
            records.append(record)
    return records


def to_chat_sft(record: dict[str, Any]) -> dict[str, Any]:
    example_id = require_text(record, "id")
    user = require_text(record, "user")
    assistant = require_text(record, "assistant")
    system = record.get("system") or DEFAULT_SYSTEM_PROMPT

    if not isinstance(system, str) or not system.strip():
        raise ValueError(f"{example_id}: field 'system' must be a non-empty string when present")

    return {
        "id": example_id,
        "messages": [
            {"role": "system", "content": system.strip()},
            {"role": "user", "content": user.strip()},
            {"role": "assistant", "content": assistant.strip()},
        ],
        "metadata": {
            "source": "loan_assistant_examples",
            "tags": normalize_tags(record.get("tags", []), example_id),
        },
    }


def require_text(record: dict[str, Any], key: str) -> str:
    value = record.get(key)
    if not isinstance(value, str) or not value.strip():
        raise ValueError(f"{record.get('id', '<unknown>')}: field '{key}' must be a non-empty string")
    return value


def normalize_tags(value: Any, example_id: str) -> list[str]:
    if not isinstance(value, list):
        raise ValueError(f"{example_id}: field 'tags' must be a list")
    tags: list[str] = []
    for tag in value:
        if not isinstance(tag, str) or not tag.strip():
            raise ValueError(f"{example_id}: every tag must be a non-empty string")
        tags.append(tag.strip())
    return tags


def split_dataset(records: list[dict[str, Any]], validation_ratio: float) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    if not records:
        raise ValueError("No records found in raw dataset")
    if not 0 < validation_ratio < 0.5:
        raise ValueError("--validation-ratio must be greater than 0 and less than 0.5")

    validation_size = max(1, round(len(records) * validation_ratio))
    validation = records[-validation_size:]
    train = records[:-validation_size]
    if not train:
        raise ValueError("Training split is empty; add more raw examples or lower --validation-ratio")
    return train, validation


def write_jsonl(path: Path, records: list[dict[str, Any]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8") as file:
        for record in records:
            file.write(json.dumps(record, ensure_ascii=False, separators=(",", ":")) + "\n")


def build_dataset(raw_path: Path, output_dir: Path, validation_ratio: float) -> tuple[Path, Path, int, int]:
    raw_records = read_jsonl(raw_path)
    sft_records = [to_chat_sft(record) for record in raw_records]
    train, validation = split_dataset(sft_records, validation_ratio)

    train_path = output_dir / "train.jsonl"
    validation_path = output_dir / "validation.jsonl"
    write_jsonl(train_path, train)
    write_jsonl(validation_path, validation)
    return train_path, validation_path, len(train), len(validation)


def parse_args() -> argparse.Namespace:
    root = repo_root()
    parser = argparse.ArgumentParser(description="Build chat SFT train/validation JSONL files.")
    parser.add_argument(
        "--raw",
        type=Path,
        default=root / "finetune" / "data" / "raw" / "loan_assistant_examples.jsonl",
        help="Path to raw loan-assistant examples JSONL.",
    )
    parser.add_argument(
        "--output-dir",
        type=Path,
        default=root / "finetune" / "data" / "processed",
        help="Directory for generated train.jsonl and validation.jsonl.",
    )
    parser.add_argument(
        "--validation-ratio",
        type=float,
        default=0.2,
        help="Validation split ratio. Uses the last N examples for deterministic output.",
    )
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    train_path, validation_path, train_count, validation_count = build_dataset(
        raw_path=args.raw,
        output_dir=args.output_dir,
        validation_ratio=args.validation_ratio,
    )

    print("SFT dataset built successfully")
    print(f"  train:      {train_path} ({train_count} examples)")
    print(f"  validation: {validation_path} ({validation_count} examples)")


if __name__ == "__main__":
    main()
