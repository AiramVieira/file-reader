CREATE TABLE servico (
    id              BIGSERIAL PRIMARY KEY,
    nome            VARCHAR(255) NOT NULL,
    descricao       VARCHAR(2000),
    tipo_arquivo    VARCHAR(20) NOT NULL CHECK (tipo_arquivo IN ('PDF', 'TXT', 'CSV', 'XLSX', 'XML')),
    ativo           BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em       TIMESTAMP NOT NULL DEFAULT now(),
    atualizado_em   TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE documento_base (
    id                      BIGSERIAL PRIMARY KEY,
    servico_id              BIGINT NOT NULL UNIQUE REFERENCES servico(id) ON DELETE CASCADE,
    nome_arquivo_original   VARCHAR(500) NOT NULL,
    caminho_armazenamento   VARCHAR(1000) NOT NULL,
    hash_sha256             VARCHAR(64) NOT NULL,
    tamanho_bytes           BIGINT NOT NULL,
    criado_em               TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE token (
    id                  BIGSERIAL PRIMARY KEY,
    documento_base_id   BIGINT NOT NULL REFERENCES documento_base(id) ON DELETE CASCADE,
    indice              INT NOT NULL,
    texto               VARCHAR(1000) NOT NULL,
    espaco_apos         VARCHAR(50) NOT NULL DEFAULT ' ',
    pagina              INT,
    linha               INT,
    coluna              INT,
    UNIQUE (documento_base_id, indice)
);

CREATE INDEX ix_token_documento_base ON token(documento_base_id);

CREATE TABLE campo_layout (
    id              BIGSERIAL PRIMARY KEY,
    servico_id      BIGINT NOT NULL REFERENCES servico(id) ON DELETE CASCADE,
    tipo            VARCHAR(20) NOT NULL CHECK (tipo IN ('COMPETENCIA', 'CNPJ', 'TERMO_BAIXA', 'TERMO_NEGACAO')),
    token_indice    INT NOT NULL,
    token_texto     VARCHAR(1000) NOT NULL,
    criado_em       TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX ix_campo_layout_servico ON campo_layout(servico_id);

-- Garante, a nivel de banco, que so pode existir 1 Competencia e 1 CNPJ marcados por Servico.
-- Termos de baixa/negacao ficam de fora do indice (podem se repetir livremente).
CREATE UNIQUE INDEX ux_campo_layout_unico_por_tipo
    ON campo_layout (servico_id, tipo)
    WHERE tipo IN ('COMPETENCIA', 'CNPJ');

CREATE TABLE ancora_token (
    id                  BIGSERIAL PRIMARY KEY,
    campo_layout_id     BIGINT NOT NULL REFERENCES campo_layout(id) ON DELETE CASCADE,
    posicao_relativa    INT NOT NULL,
    texto               VARCHAR(1000) NOT NULL,
    UNIQUE (campo_layout_id, posicao_relativa)
);

CREATE TABLE execucao_processamento (
    id                              BIGSERIAL PRIMARY KEY,
    servico_id                      BIGINT REFERENCES servico(id) ON DELETE SET NULL,
    nome_arquivo_original           VARCHAR(500) NOT NULL,
    caminho_armazenamento           VARCHAR(1000) NOT NULL,
    status                          VARCHAR(20) NOT NULL CHECK (status IN ('MATCH', 'NO_MATCH', 'AMBIGUO')),
    competencia_extraida            VARCHAR(255),
    cnpj_extraido                   VARCHAR(255),
    score_extracao_competencia      NUMERIC(4,3),
    score_extracao_cnpj             NUMERIC(4,3),
    diagnostico                     TEXT,
    processado_em                   TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX ix_execucao_processamento_servico ON execucao_processamento(servico_id);
