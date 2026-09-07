"""Gera arquivos BSON mock a partir dos documentos aprovados.

Rode: python3 tests/generate_fixtures.py
Saida: tests/fixtures/*.bson
"""
import bson
import datetime
from pathlib import Path

FIXTURES_DIR = Path(__file__).parent / "fixtures"
FIXTURES_DIR.mkdir(parents=True, exist_ok=True)


def dt(s: str) -> datetime.datetime:
    """Parse YYYY-MM-DD HH:MM:SS para datetime (formato legado)."""
    return datetime.datetime.strptime(s, "%Y-%m-%d %H:%M:%S")


def write_bson(name: str, docs: list[dict]) -> None:
    path = FIXTURES_DIR / f"{name}.bson"
    with open(path, "wb") as f:
        for doc in docs:
            f.write(bson.encode(doc))
    print(f"  {name}.bson: {len(docs)} doc(s)")


def gen_contrato() -> list[dict]:
    return [{
        "_id": 5001,
        "tipo": 1,
        "cliente": 7001,
        "retirada": dt("2010-12-02 02:00:00"),
        "usa": dt("2010-12-04 02:00:00"),
        "devolucao": dt("2010-12-06 02:00:00"),
        "hoje": dt("2010-10-27 02:00:00"),
        "devolveu": dt("2010-12-06 02:00:00"),
        "criado_por": 8001,
        "baixa_por": 8001,
        "baixa": True,
        "situacao": 0,
        "comunicado": "",
        "itens": [{
            "codigo": "9001",
            "entregue": False,
            "descricao": "VESTIDO MOCK 42, ",
            "valor": 350.0,
            "atendente": 8001,
            "sub": ["M44", "SEM SAPATO", "MANGA DIR 49CM"],
        }],
        "pagamentos": [
            {"data": dt("2010-10-27 02:00:00"), "forma": 0, "valor": 250.0, "vezes": 0, "funcionario": 8001},
            {"data": dt("2010-12-02 02:00:00"), "forma": 0, "valor": 100.0, "vezes": 0, "funcionario": 8001},
        ],
    }]


def gen_cliente() -> list[dict]:
    return [{
        "_id": 7001,
        "nome": "Joao Mock da Silva",
        "cpf": "111,222,333-44",
        "rg": "",
        "nascimento": dt("1111-11-11 02:00:00"),
        "sexo": False,
        "autenticacao": False,
        "fones": ["1600000000", "900000000"],
        "obs": "",
        "email": "",
        "por": 8001,
        "endereco": {
            "cep": "",
            "tipoLog": "",
            "logradouro": "Rua Mock, 123",
            "numero": "",
            "bairro": "Centro",
            "cidade": "MOCK CITY",
            "uf": "SP",
        },
    }]


def gen_funcionario() -> list[dict]:
    return [{
        "_id": 8001,
        "sigla": "MCK",
        "nome": "Funcionario Mock",
        "senha": "mocksenha123=",
        "privilegio": 1,
        "ativo": True,
    }]


def gen_roupa() -> list[dict]:
    return [{
        "_id": 9001,
        "nome": "VESTIDO MOCK 42",
        "locado": False,
        "obs": "",
        "valor": 390.0,
        "tamanho": "42",
        "nloc": 5,
        "no_estoque": True,
        "cor": "Branco",
        "base": 380.0,
        "ajuste": 2.63,
        "data": dt("2010-10-27 02:00:00"),
        "preco_id": 0,
        "status": 1,
        "tipo": 4,
    }]


def gen_roupa_tipo() -> list[dict]:
    return [{
        "_id": 4,
        "codigo": "T",
        "nome": "Terno",
        "descricao": "Terno Masculino",
    }]


def gen_preco() -> list[dict]:
    return [{
        "_id": 0,
        "nome": "N/A",
        "cor": "white",
        "tipo": 0,
        "valor": 0.0,
        "enabled": True,
    }]


def gen_bandeira() -> list[dict]:
    return [{
        "_id": 0,
        "sigla": "NA",
        "nome": "Pendente",
        "isActive": True,
    }]


def main() -> None:
    print("Gerando fixtures BSON mock...")
    write_bson("contrato", gen_contrato())
    write_bson("cliente", gen_cliente())
    write_bson("funcionario", gen_funcionario())
    write_bson("roupa", gen_roupa())
    write_bson("roupa_tipo", gen_roupa_tipo())
    write_bson("preco", gen_preco())
    write_bson("bandeira", gen_bandeira())
    print(f"Fixtures gerados em {FIXTURES_DIR}")


if __name__ == "__main__":
    main()
