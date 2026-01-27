-- V4__create_saocarlos_nfse_tables.sql
-- Tabelas para controle de NFS-e São Carlos (GINFES v3.01)

-- Tabela de lotes de RPS
CREATE TABLE saocarlos_lote_rps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    numero_lote VARCHAR(50) UNIQUE NOT NULL,
    protocolo VARCHAR(50),
    customer_id UUID REFERENCES customers(id),
    cnpj_prestador VARCHAR(14) NOT NULL,
    inscricao_municipal_prestador VARCHAR(15),
    quantidade_rps INTEGER NOT NULL,
    situacao VARCHAR(20) NOT NULL,
    data_envio TIMESTAMP WITH TIME ZONE,
    data_recebimento TIMESTAMP WITH TIME ZONE,
    xml_enviado TEXT,
    xml_resposta TEXT,
    mensagem_erro TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Índices para lote
CREATE INDEX idx_saocarlos_lote_protocolo ON saocarlos_lote_rps(protocolo);
CREATE INDEX idx_saocarlos_lote_numero ON saocarlos_lote_rps(numero_lote);
CREATE INDEX idx_saocarlos_lote_customer ON saocarlos_lote_rps(customer_id);
CREATE INDEX idx_saocarlos_lote_situacao ON saocarlos_lote_rps(situacao);

-- Tabela de RPS
CREATE TABLE saocarlos_rps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lote_id UUID REFERENCES saocarlos_lote_rps(id),
    customer_id UUID REFERENCES customers(id),
    numero_rps BIGINT NOT NULL,
    serie_rps VARCHAR(5) NOT NULL,
    tipo_rps INTEGER NOT NULL,
    data_emissao TIMESTAMP NOT NULL,
    natureza_operacao INTEGER NOT NULL,
    regime_especial_tributacao INTEGER,
    simples_nacional INTEGER NOT NULL,
    incentivador_cultural INTEGER NOT NULL,
    status_rps INTEGER NOT NULL,
    valor_servicos DECIMAL(15,2) NOT NULL,
    valor_deducoes DECIMAL(15,2),
    valor_pis DECIMAL(15,2),
    valor_cofins DECIMAL(15,2),
    valor_inss DECIMAL(15,2),
    valor_ir DECIMAL(15,2),
    valor_csll DECIMAL(15,2),
    valor_iss DECIMAL(15,2),
    valor_iss_retido DECIMAL(15,2),
    valor_outras_retencoes DECIMAL(15,2),
    base_calculo DECIMAL(15,2),
    aliquota DECIMAL(5,4),
    valor_liquido_nfse DECIMAL(15,2),
    desconto_incondicionado DECIMAL(15,2),
    desconto_condicionado DECIMAL(15,2),
    item_lista_servico VARCHAR(5) NOT NULL,
    codigo_cnae VARCHAR(7),
    codigo_tributacao_municipio VARCHAR(20),
    discriminacao TEXT NOT NULL,
    codigo_municipio INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_saocarlos_rps_numero_serie UNIQUE (numero_rps, serie_rps)
);

-- Índices para RPS
CREATE INDEX idx_saocarlos_rps_lote ON saocarlos_rps(lote_id);
CREATE INDEX idx_saocarlos_rps_customer ON saocarlos_rps(customer_id);
CREATE INDEX idx_saocarlos_rps_numero_serie ON saocarlos_rps(numero_rps, serie_rps);

-- Tabela de NFS-e geradas
CREATE TABLE saocarlos_nfse (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lote_id UUID REFERENCES saocarlos_lote_rps(id),
    rps_id UUID REFERENCES saocarlos_rps(id),
    customer_id UUID NOT NULL REFERENCES customers(id),
    numero_nfse BIGINT NOT NULL,
    codigo_verificacao VARCHAR(9) NOT NULL,
    data_emissao TIMESTAMP NOT NULL,
    numero_rps_substituido BIGINT,
    serie_rps_substituido VARCHAR(5),
    status VARCHAR(20) NOT NULL,
    valor_servicos DECIMAL(15,2) NOT NULL,
    valor_liquido DECIMAL(15,2) NOT NULL,
    base_calculo DECIMAL(15,2),
    aliquota DECIMAL(5,4),
    valor_iss DECIMAL(15,2),
    xml_nfse TEXT,
    link_visualizacao VARCHAR(500),
    data_cancelamento TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_saocarlos_nfse_numero UNIQUE (numero_nfse),
    CONSTRAINT uk_saocarlos_nfse_codigo_verificacao UNIQUE (codigo_verificacao)
);

-- Índices para NFS-e
CREATE INDEX idx_saocarlos_nfse_lote ON saocarlos_nfse(lote_id);
CREATE INDEX idx_saocarlos_nfse_rps ON saocarlos_nfse(rps_id);
CREATE INDEX idx_saocarlos_nfse_customer ON saocarlos_nfse(customer_id);
CREATE INDEX idx_saocarlos_nfse_numero ON saocarlos_nfse(numero_nfse);
CREATE INDEX idx_saocarlos_nfse_codigo_verificacao ON saocarlos_nfse(codigo_verificacao);
CREATE INDEX idx_saocarlos_nfse_status ON saocarlos_nfse(status);
CREATE INDEX idx_saocarlos_nfse_data_emissao ON saocarlos_nfse(data_emissao);

-- Comentários nas tabelas
COMMENT ON TABLE saocarlos_lote_rps IS 'Controle de lotes de RPS enviados para São Carlos (GINFES v3.01)';
COMMENT ON TABLE saocarlos_rps IS 'RPS (Recibos Provisórios de Serviço) emitidos para São Carlos';
COMMENT ON TABLE saocarlos_nfse IS 'NFS-e autorizadas pelo sistema GINFES de São Carlos';

COMMENT ON COLUMN saocarlos_lote_rps.situacao IS 'PENDENTE, ENVIADO, PROCESSADO, PROCESSADO_ERRO, ERRO_ENVIO, CANCELADO';
COMMENT ON COLUMN saocarlos_rps.tipo_rps IS '1-RPS, 2-Nota Fiscal Conjugada, 3-Cupom';
COMMENT ON COLUMN saocarlos_rps.natureza_operacao IS '1-Trib. no município, 2-Trib. fora município, 3-Isenção, 4-Imune, 5-Exig. suspensa, 6-Sem incidência';
COMMENT ON COLUMN saocarlos_rps.simples_nacional IS '1-Sim, 2-Não';
COMMENT ON COLUMN saocarlos_rps.incentivador_cultural IS '1-Sim, 2-Não';
COMMENT ON COLUMN saocarlos_rps.status_rps IS '1-Normal, 2-Cancelado';
COMMENT ON COLUMN saocarlos_nfse.status IS 'AUTORIZADA, CANCELADA, SUBSTITUIDA';
