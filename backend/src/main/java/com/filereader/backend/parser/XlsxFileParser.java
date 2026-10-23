package com.filereader.backend.parser;

import com.filereader.backend.domain.TipoArquivo;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/** Achata celula a celula em uma sequencia de tokens, guardando linha/coluna como metadado de posicao. */
@Component
public class XlsxFileParser implements FileParser {

    @Override
    public boolean suporta(TipoArquivo tipoArquivo) {
        return tipoArquivo == TipoArquivo.XLSX;
    }

    @Override
    public DocumentoParseado parse(InputStream inputStream) throws IOException {
        List<TokenBruto> tokens = new ArrayList<>();
        int indiceGlobal = 0;
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            for (Row row : sheet) {
                int numCells = row.getLastCellNum();
                if (numCells < 0) {
                    continue;
                }
                for (int c = 0; c < numCells; c++) {
                    Cell cell = row.getCell(c);
                    if (cell == null) {
                        continue;
                    }
                    String valorCelula = formatter.formatCellValue(cell).trim();
                    if (valorCelula.isEmpty()) {
                        continue;
                    }
                    List<TokenBruto> subTokens = Tokenizer.tokenizar(valorCelula);
                    for (int i = 0; i < subTokens.size(); i++) {
                        TokenBruto sub = subTokens.get(i);
                        boolean ultimoDaCelula = i == subTokens.size() - 1;
                        boolean ultimaCelulaDaLinha = c == numCells - 1;
                        String espaco = ultimoDaCelula ? (ultimaCelulaDaLinha ? "\n" : "\t") : " ";
                        TokenBruto reindexado = new TokenBruto(indiceGlobal++, sub.getTexto(), espaco);
                        reindexado.setLinha(row.getRowNum());
                        reindexado.setColuna(c);
                        tokens.add(reindexado);
                    }
                }
            }
        }
        return new DocumentoParseado(tokens);
    }
}
