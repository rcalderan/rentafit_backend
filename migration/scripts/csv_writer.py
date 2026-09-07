"""Geração de arquivos CSV a partir de linhas normalizadas."""

import csv
from pathlib import Path

from config import CSV_HEADERS, NULL_WHEN_EMPTY

NULL_MARKER = "\\N"


def write_csv(output_dir: Path, table_name: str, rows: list[dict]) -> Path:
    path = output_dir / f"{table_name}.csv"
    headers = CSV_HEADERS.get(table_name, [])
    null_fields = NULL_WHEN_EMPTY.get(table_name, set())
    with open(path, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=headers, extrasaction="ignore")
        writer.writeheader()
        for row in rows:
            out = {}
            for h in headers:
                val = row.get(h)
                if val is None:
                    out[h] = NULL_MARKER
                elif val == "" and h in null_fields:
                    out[h] = NULL_MARKER
                else:
                    out[h] = val
            writer.writerow(out)
    return path
