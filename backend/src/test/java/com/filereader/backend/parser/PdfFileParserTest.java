package com.filereader.backend.parser;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PdfFileParserTest {

    private final PdfFileParser parser = new PdfFileParser();

    @Test
    void deveExtrairTokensDeUmPdfComMetadadoDePagina() throws IOException {
        byte[] pdf = gerarPdfDeTeste("CNPJ: 12.345.678/0001-90 Competencia: Janeiro/2026");

        DocumentoParseado resultado = parser.parse(new ByteArrayInputStream(pdf));
        List<TokenBruto> tokens = resultado.getTokens();

        assertThat(tokens).isNotEmpty();
        assertThat(tokens).extracting(TokenBruto::getTexto).contains("CNPJ:", "12.345.678/0001-90", "Competencia:");
        assertThat(tokens.get(0).getPagina()).isEqualTo(1);
    }

    private byte[] gerarPdfDeTeste(String texto) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(50, 700);
                stream.showText(texto);
                stream.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }
}
