package com.filereader.backend.parser;

import com.filereader.backend.domain.TipoArquivo;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PlainTextFileParserTest {

    private final PlainTextFileParser parser = new PlainTextFileParser();

    @Test
    void deveSuportarTxtEXmlMasNaoOutros() {
        assertThat(parser.suporta(TipoArquivo.TXT)).isTrue();
        assertThat(parser.suporta(TipoArquivo.XML)).isTrue();
        assertThat(parser.suporta(TipoArquivo.CSV)).isFalse();
        assertThat(parser.suporta(TipoArquivo.PDF)).isFalse();
        assertThat(parser.suporta(TipoArquivo.XLSX)).isFalse();
    }

    @Test
    void deveParsearTextoPlanoEmTokens() throws IOException {
        String conteudo = "Servico de baixa CNPJ 12.345.678/0001-90 Competencia Janeiro/2026";
        DocumentoParseado resultado = parser.parse(new ByteArrayInputStream(conteudo.getBytes(StandardCharsets.UTF_8)));

        List<TokenBruto> tokens = resultado.getTokens();
        assertThat(tokens).hasSize(7);
        assertThat(tokens.get(4).getTexto()).isEqualTo("12.345.678/0001-90");
    }
}
