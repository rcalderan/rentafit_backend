#!/usr/bin/env python3
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import padding
from cryptography.hazmat.backends import default_backend
import base64

# Chave pública fornecida
public_key_pem = """-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAv8JNmhAnT9sgT8as24Hs
1mjrJ+nY2vFUJoysWMzVClQKfCW0le4dj6s9UwIMhwvsyyC1aw4pJbZa0XPelrO4
77tEsKBgh6skS1M+wTAnIN4wyYFOititik8TeiN5//SIvEn28tJ1z4MM+G1RLOz0
CEJB6VTCndnbxNe7PDw9WqmT7eoFvYj8KQ8A4UEZ611nVztIo0EioRwJD+nRbD9e
1ahZt4RnaVeWurUdZkFwNJAKankLZzac26K39D2a6n2yLoszxttugCY7n5/Vgo3n
f3oDlmSrXqzrAKvR9RCJjrvHgGi3Zl0wx1ZZZo1PYY/s9Iy+CoivV68Yn07L5Uno
EwIDAQAB
-----END PUBLIC KEY-----"""

# Carregar a chave pública
public_key = serialization.load_pem_public_key(
    public_key_pem.encode(),
    backend=default_backend()
)

# Senha a criptografar
password = "admin12345"

# Criptografar com RSA usando PKCS1Padding
encrypted = public_key.encrypt(
    password.encode(),
    padding.PKCS1v15()
)

# Codificar em Base64
encrypted_b64 = base64.b64encode(encrypted).decode()

print("=" * 60)
print("CRIPTOGRAFIA RSA COM CHAVE PÚBLICA")
print("=" * 60)
print(f"Senha original: {password}")
print(f"\nSenha criptografada (Base64):")
print(encrypted_b64)
print("=" * 60)

