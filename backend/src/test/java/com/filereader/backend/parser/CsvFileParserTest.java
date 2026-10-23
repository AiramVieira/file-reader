package com.filereader.backend.parser;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CsvFileParserTest {

    private final CsvFileParser parser = new CsvFileParser();

    @Test
    void deveSepararCamposDelimitadosPorVirgulaEmTokensDistintos() throws IOException {
        String conteudo = "12.345.678/0001-90,Janeiro/2026,Aprovado";
        DocumentoParseado resultado = parser.parse(new ByteArrayInputStream(conteudo.getBytes(StandardCharsets.UTF_8)));

        List<TokenBruto> tokens = resultado.getTokens();
        assertThat(tokens).extracting(TokenBruto::getTexto)
                .containsExactly("12.345.678/0001-90", ",", "Janeiro/2026", ",", "Aprovado");
    }
}
