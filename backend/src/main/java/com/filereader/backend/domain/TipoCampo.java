package com.filereader.backend.domain;

public enum TipoCampo {
    COMPETENCIA,
    CNPJ,
    TERMO_BAIXA,
    TERMO_NEGACAO;

    public boolean isSelecaoUnica() {
        return this == COMPETENCIA || this == CNPJ;
    }
}
