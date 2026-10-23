package com.filereader.backend.parser;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class XlsxFileParserTest {

    private final XlsxFileParser parser = new XlsxFileParser();

    @Test
    void deveAchatarCelulasEmTokensComLinhaEColuna() throws IOException {
        byte[] planilha = gerarPlanilhaDeTeste();

        DocumentoParseado resultado = parser.parse(new ByteArrayInputStream(planilha));
        List<TokenBruto> tokens = resultado.getTokens();

        assertThat(tokens).extracting(TokenBruto::getTexto)
                .containsExactly("CNPJ", "12.345.678/0001-90", "Competencia", "Janeiro/2026");
        assertThat(tokens.get(0).getLinha()).isZero();
        assertThat(tokens.get(0).getColuna()).isZero();
        assertThat(tokens.get(1).getColuna()).isEqualTo(1);
        assertThat(tokens.get(2).getLinha()).isEqualTo(1);
    }

    private byte[] gerarPlanilhaDeTeste() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("dados");
            Row linha0 = sheet.createRow(0);
            linha0.createCell(0).setCellValue("CNPJ");
            linha0.createCell(1).setCellValue("12.345.678/0001-90");
            Row linha1 = sheet.createRow(1);
            linha1.createCell(0).setCellValue("Competencia");
            linha1.createCell(1).setCellValue("Janeiro/2026");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }
}
