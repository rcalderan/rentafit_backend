"""Testes para readers.py, especialmente o parser do dump unico BSON."""

import sys
from pathlib import Path

import bson
import pytest

sys.path.insert(0, str(Path(__file__).parent.parent))

from readers import read_all_bson_files, read_single_dump

FIXTURES_DIR = Path(__file__).parent / "fixtures"
SINGLE_DUMP = FIXTURES_DIR / "noivabd_backup.bson"


def test_read_single_dump_parses_all_collections():
    result = read_single_dump(SINGLE_DUMP)

    assert set(result.keys()) == {"cliente", "funcionario", "roupa_tipo", "roupa", "contrato", "conf"}
    assert len(result["cliente"]) == 2
    assert len(result["funcionario"]) == 1
    assert len(result["contrato"]) == 1


def test_read_all_bson_files_accepts_single_dump(tmp_path):
    dump_path = tmp_path / "noivabd_backup.bson"
    dump_path.write_bytes(SINGLE_DUMP.read_bytes())

    result = read_all_bson_files(str(tmp_path))

    assert "cliente" in result
    assert len(result["cliente"]) == 2
    assert result["cliente"][0]["_id"] == "1"


def test_read_all_bson_files_rejects_multiple_bson_files(tmp_path):
    (tmp_path / "cliente.bson").write_bytes(bytes(bson.BSON.encode({"_id": 1})))
    (tmp_path / "contrato.bson").write_bytes(bytes(bson.BSON.encode({"_id": 2})))

    with pytest.raises(ValueError, match="Formato antigo detectado"):
        read_all_bson_files(str(tmp_path))


def test_read_single_dump_rejects_plain_bson_file(tmp_path):
    plain = tmp_path / "plain.bson"
    plain.write_bytes(bytes(bson.BSON.encode({"_id": 1})))

    with pytest.raises(ValueError, match="Prefixo BSON invalido"):
        read_single_dump(plain)
