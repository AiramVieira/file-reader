package com.filereader.backend.parser;

import java.util.List;

/** Resultado uniforme de qualquer {@link FileParser}: uma lista ordenada de tokens. */
public class DocumentoParseado {

    private final List<TokenBruto> tokens;

    public DocumentoParseado(List<TokenBruto> tokens) {
        this.tokens = tokens;
    }

    public List<TokenBruto> getTokens() {
        return tokens;
    }
}
