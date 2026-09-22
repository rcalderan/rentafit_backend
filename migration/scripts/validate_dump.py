"""Valida o formato do dump unico BSON do noivabd.

Retorna JSON com status, collections encontradas e erros.
Nao carrega todos os documentos em memoria; apenas itera sobre os blocos.
"""

import argparse
import json
import sys
from pathlib import Path

import bson

from readers import MAGIC_PREFIX, SEPARATOR


def validate(file_path: Path) -> dict:
    result = {"valid": True, "collections": {}, "errors": [], "warnings": []}
    data = file_path.read_bytes()

    if len(data) < 4:
        return _invalid(result, "Arquivo muito pequeno para ser um dump BSON.")
    if data[:4] != MAGIC_PREFIX:
        return _invalid(
            result,
            f"Prefixo BSON invalido: esperado {MAGIC_PREFIX.hex()}, "
            f"obtido {data[:4].hex()}."
        )

    offset = 4
    try:
        offset = _skip_header(data, offset, result)
        offset = _skip_metadata(data, offset, result)
        offset = _consume_separator(data, offset, result)
        _validate_data_blocks(data, offset, result)
    except Exception as e:
        return _invalid(result, str(e))

    return result


def _invalid(result: dict, error: str) -> dict:
    result["valid"] = False
    result["errors"].append(error)
    return result


def _decode_document(data: bytes, offset: int) -> tuple[int, dict]:
    length = int.from_bytes(data[offset:offset + 4], "little", signed=True)
    if length <= 0:
        raise ValueError(f"Tamanho BSON invalido em offset {offset}: {length}")
    return length, bson.BSON.decode(data[offset:offset + length])


def _skip_header(data: bytes, offset: int, result: dict) -> int:
    length, header = _decode_document(data, offset)
    expected = {"concurrent_collections", "version", "server_version", "tool_version"}
    if not expected.issubset(header.keys()):
        raise ValueError(f"Header invalido: chaves {set(header.keys())}")
    result["version"] = header.get("version")
    return offset + length


def _skip_metadata(data: bytes, offset: int, result: dict) -> int:
    while offset < len(data):
        if data[offset:offset + 4] == SEPARATOR:
            return offset
        length, doc = _decode_document(data, offset)
        if "collection" in doc and "metadata" in doc:
            offset += length
            continue
        return offset
    return offset


def _consume_separator(data: bytes, offset: int, result: dict) -> int:
    if data[offset:offset + 4] != SEPARATOR:
        raise ValueError(f"Separador nao encontrado em offset {offset}")
    return offset + 4


def _validate_data_blocks(data: bytes, offset: int, result: dict) -> None:
    current_collection: str | None = None
    while offset < len(data):
        if data[offset:offset + 4] == SEPARATOR:
            offset += 4
            continue

        length, doc = _decode_document(data, offset)

        if "db" in doc and "collection" in doc and "EOF" in doc:
            collection = doc.get("collection")
            eof = doc.get("EOF")
            if eof is False:
                current_collection = collection
                result["collections"].setdefault(current_collection, 0)
            elif eof is True:
                current_collection = None
        elif current_collection is not None:
            result["collections"][current_collection] += 1
        else:
            raise ValueError(f"Documento fora de bloco em offset {offset}")

        offset += length


def main() -> None:
    parser = argparse.ArgumentParser(description="Valida dump BSON do noivabd")
    parser.add_argument("file", help="Caminho do arquivo .bson")
    parser.add_argument("--expected-collections", nargs="+", default=[],
                        help="Collections obrigatorias")
    args = parser.parse_args()

    result = validate(Path(args.file))
    for expected in args.expected_collections:
        if expected not in result["collections"]:
            result = _invalid(result, f"Collection obrigatoria ausente: {expected}")

    print(json.dumps(result, indent=2, ensure_ascii=False))
    sys.exit(0 if result["valid"] else 1)


if __name__ == "__main__":
    main()
