"""Geração de report.json com contagem e erros."""

import json
import time
from pathlib import Path


def build_report(tables: list[dict], errors: list[str], warnings: list[str], started_at: float, finished_at: float) -> dict:
    return {
        "status": "failed" if errors else "success",
        "started_at": int(started_at),
        "finished_at": int(finished_at),
        "duration_seconds": int(finished_at - started_at),
        "tables": tables,
        "errors": errors,
        "warnings": warnings,
    }


def write_report(output_dir: Path, report: dict) -> Path:
    path = output_dir / "report.json"
    with open(path, "w", encoding="utf-8") as f:
        json.dump(report, f, indent=2, ensure_ascii=False)
    return path
