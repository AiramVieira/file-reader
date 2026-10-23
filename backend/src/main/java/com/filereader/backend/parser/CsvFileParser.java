package com.filereader.backend.parser;

import com.filereader.backend.domain.TipoArquivo;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** CSV: mesma tokenizacao do texto puro, mas forcando separacao em virgulas/ponto-e-virgula, que normalmente nao tem espaco ao redor. */
@Component
public class CsvFileParser implements FileParser {

    @Override
    public boolean suporta(TipoArquivo tipoArquivo) {
        return tipoArquivo == TipoArquivo.CSV;
    }

    @Override
    public DocumentoParseado parse(InputStream inputStream) throws IOException {
        String texto = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        String textoComSeparadoresExpandidos = texto
                .replace(",", " , ")
                .replace(";", " ; ");
        return new DocumentoParseado(Tokenizer.tokenizar(textoComSeparadoresExpandidos));
    }
}
