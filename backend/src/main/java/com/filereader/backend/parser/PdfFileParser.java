package com.filereader.backend.parser;

import com.filereader.backend.domain.TipoArquivo;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class PdfFileParser implements FileParser {

    @Override
    public boolean suporta(TipoArquivo tipoArquivo) {
        return tipoArquivo == TipoArquivo.PDF;
    }

    @Override
    public DocumentoParseado parse(InputStream inputStream) throws IOException {
        List<TokenBruto> tokens = new ArrayList<>();
        byte[] bytes = inputStream.readAllBytes();
        try (PDDocument document = Loader.loadPDF(bytes)) {
            int totalPaginas = document.getNumberOfPages();
            int indiceGlobal = 0;
            for (int pagina = 1; pagina <= totalPaginas; pagina++) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setStartPage(pagina);
                stripper.setEndPage(pagina);
                String textoPagina = stripper.getText(document);
                for (TokenBruto bruto : Tokenizer.tokenizar(textoPagina)) {
                    TokenBruto reindexado = new TokenBruto(indiceGlobal++, bruto.getTexto(), bruto.getEspacoApos());
                    reindexado.setPagina(pagina);
                    tokens.add(reindexado);
                }
            }
        }
        return new DocumentoParseado(tokens);
    }
}
