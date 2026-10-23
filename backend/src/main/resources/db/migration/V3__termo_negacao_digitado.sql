-- Termos de negacao passam a ser digitados (palavra ou expressao) em vez de marcados no arquivo-modelo:
-- uma palavra marcada no proprio modelo faria o modelo deixar de ser reconhecido pelo seu servico.
ALTER TABLE campo_layout ALTER COLUMN token_indice DROP NOT NULL;

ALTER TABLE campo_layout
    ADD CONSTRAINT ck_campo_layout_token_indice
    CHECK (token_indice IS NOT NULL OR tipo = 'TERMO_NEGACAO');
