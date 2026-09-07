"""Leitura de arquivos .bson com a biblioteca pymongo.bson."""

import os
from pathlib import Path

import bson


def list_bson_files(session_dir: str) -> list[Path]:
    path = Path(session_dir)
    if not path.exists():
        return []
    return sorted([p for p in path.iterdir() if p.is_file() and p.suffix.lower() == ".bson"])


def read_bson_file(file_path: Path) -> list[dict]:
    with open(file_path, "rb") as f:
        data = f.read()
    return list(bson.decode_all(data))


def read_all_bson_files(session_dir: str) -> dict[str, list[dict]]:
    files = list_bson_files(session_dir)
    result = {}
    for file in files:
        docs = read_bson_file(file)
        result[file.stem] = docs
    return result
