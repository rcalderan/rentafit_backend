"""Leitura de dumps do noivabd.

A partir do formato mais atual, apenas um arquivo .bson por sessao e aceito.
Esse arquivo e um dump customizado que contem todas as collections em um
unico arquivo, com a seguinte estrutura:

- 4 bytes de prefixo (magic)
- documento BSON de header
- documentos BSON de metadados (um por collection)
- separador 0xFFFFFFFF
- blocos de dados por collection:
    - documento de controle {db, collection, EOF: false, CRC: 0}
    - documentos BSON da collection
    - documento de controle {db, collection, EOF: true, CRC: <hash>}
    - separador 0xFFFFFFFF entre blocos
"""

from pathlib import Path

import bson

MAGIC_PREFIX = b"\x6d\xe2\x99\x81"
SEPARATOR = b"\xff\xff\xff\xff"


def list_bson_files(session_dir: str) -> list[Path]:
    path = Path(session_dir)
    if not path.exists():
        return []
    return sorted([p for p in path.iterdir() if p.is_file() and p.suffix.lower() == ".bson"])


def read_bson_file(file_path: Path) -> list[dict]:
    """Le arquivo .bson simples (multiplos documentos BSON concatenados).

    Mantido para compatibilidade com fixtures pequenos e testes.
    """
    with open(file_path, "rb") as f:
        data = f.read()
    return list(bson.decode_all(data))


def read_all_bson_files(session_dir: str) -> dict[str, list[dict]]:
    """Le o arquivo de dump unico da sessao e retorna as collections.

    Levanta ValueError se o formato antigo (multiplos arquivos .bson) for
    detectado, pois apenas o dump unico e suportado.
    """
    files = list_bson_files(session_dir)
    if not files:
        return {}
    if len(files) > 1:
        names = [f.name for f in files]
        raise ValueError(
            f"Formato antigo detectado: {len(files)} arquivos .bson ({names}). "
            "Apenas o dump unico e aceito."
        )
    return read_single_dump(files[0])


def read_single_dump(file_path: Path) -> dict[str, list[dict]]:
    """Parseia o arquivo de dump unico customizado.

    Retorna {collection_name: [documentos]} sem o prefixo do banco.
    """
    data = file_path.read_bytes()
    offset = _consume_magic_prefix(data, file_path)
    offset = _skip_header_and_metadata(data, offset)
    offset = _consume_separator(data, offset, file_path)
    return _read_data_blocks(data, offset)


def _decode_document_at(data: bytes, offset: int) -> tuple[int, dict]:
    length = int.from_bytes(data[offset:offset + 4], "little", signed=True)
    if length <= 0:
        raise ValueError(f"Tamanho de documento BSON invalido em offset {offset}: {length}")
    doc_bytes = data[offset:offset + length]
    return length, bson.BSON.decode(doc_bytes)


def _consume_magic_prefix(data: bytes, file_path: Path) -> int:
    if len(data) < 4:
        raise ValueError(f"Arquivo muito pequeno para ser um dump BSON: {file_path}")
    if data[:4] != MAGIC_PREFIX:
        raise ValueError(
            f"Prefixo BSON invalido em {file_path}: esperado {MAGIC_PREFIX.hex()}, "
            f"obtido {data[:4].hex()}."
        )
    return 4


def _skip_header_and_metadata(data: bytes, offset: int) -> int:
    header_length, header = _decode_document_at(data, offset)
    expected_keys = {"concurrent_collections", "version", "server_version", "tool_version"}
    if not expected_keys.issubset(header.keys()):
        raise ValueError(
            f"Header do dump BSON invalido. Chaves esperadas: {expected_keys}, "
            f"obtidas: {set(header.keys())}."
        )
    offset += header_length
    while offset < len(data):
        if data[offset:offset + 4] == SEPARATOR:
            return offset
        doc_length, doc = _decode_document_at(data, offset)
        if "collection" in doc and "metadata" in doc:
            offset += doc_length
            continue
        return offset
    return offset


def _consume_separator(data: bytes, offset: int, file_path: Path) -> int:
    if data[offset:offset + 4] != SEPARATOR:
        raise ValueError(
            f"Separador de blocos nao encontrado em {file_path} offset {offset}: "
            f"{data[offset:offset + 4].hex()}"
        )
    return offset + 4


def _read_data_blocks(data: bytes, offset: int) -> dict[str, list[dict]]:
    collections: dict[str, list[dict]] = {}
    current_collection: str | None = None

    while offset < len(data):
        if data[offset:offset + 4] == SEPARATOR:
            offset += 4
            continue

        doc_length, doc = _decode_document_at(data, offset)

        if _is_control_document(doc):
            collection = doc.get("collection")
            eof = doc.get("EOF")
            if eof is False:
                current_collection = collection
                collections.setdefault(current_collection, [])
            elif eof is True:
                current_collection = None
        else:
            if current_collection is None:
                raise ValueError(
                    f"Documento de dados encontrado sem collection ativa em offset {offset}."
                )
            collections[current_collection].append(doc)

        offset += doc_length

    return collections


def _is_control_document(doc: dict) -> bool:
    return "db" in doc and "collection" in doc and "EOF" in doc
