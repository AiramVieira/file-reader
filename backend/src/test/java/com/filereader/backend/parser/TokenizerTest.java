package com.filereader.backend.parser;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TokenizerTest {

    @Test
    void deveTokenizarPreservandoEspacosEQuebrasDeLinha() {
        String texto = "CNPJ: 12.345.678/0001-90\nCompetencia: Janeiro/2026";

        List<TokenBruto> tokens = Tokenizer.tokenizar(texto);

        assertThat(tokens).hasSize(4);
        assertThat(tokens.get(0).getTexto()).isEqualTo("CNPJ:");
        assertThat(tokens.get(0).getEspacoApos()).isEqualTo(" ");
        assertThat(tokens.get(1).getTexto()).isEqualTo("12.345.678/0001-90");
        assertThat(tokens.get(1).getEspacoApos()).isEqualTo("\n");
        assertThat(tokens.get(2).getTexto()).isEqualTo("Competencia:");
        assertThat(tokens.get(3).getTexto()).isEqualTo("Janeiro/2026");
    }

    @Test
    void deveIndexarTokensSequencialmenteAPartirDeZero() {
        List<TokenBruto> tokens = Tokenizer.tokenizar("um dois tres");

        assertThat(tokens).extracting(TokenBruto::getIndice).containsExactly(0, 1, 2);
    }

    @Test
    void deveRetornarListaVaziaParaTextoNuloOuVazio() {
        assertThat(Tokenizer.tokenizar(null)).isEmpty();
        assertThat(Tokenizer.tokenizar("")).isEmpty();
    }
}
