-- Termos de baixa/negacao podem ser marcados como intervalo de tokens (varias palavras seguidas).
-- O texto do termo fica com as palavras unidas por "_" (ex.: "Pagamento_de_boleto").
ALTER TABLE campo_layout ADD COLUMN token_indice_fim INT;

ALTER TABLE campo_layout
    ADD CONSTRAINT ck_campo_layout_intervalo
    CHECK (token_indice_fim IS NULL
        OR (token_indice IS NOT NULL AND token_indice_fim > token_indice AND tipo IN ('TERMO_BAIXA', 'TERMO_NEGACAO')));
