package com.filereader.backend.parser;

import com.filereader.backend.domain.TipoArquivo;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** TXT e XML sao tratados como texto puro tokenizado (XML nao e parseado como arvore DOM, para uniformidade com os demais formatos). */
@Component
public class PlainTextFileParser implements FileParser {

    @Override
    public boolean suporta(TipoArquivo tipoArquivo) {
        return tipoArquivo == TipoArquivo.TXT || tipoArquivo == TipoArquivo.XML;
    }

    @Override
    public DocumentoParseado parse(InputStream inputStream) throws IOException {
        String texto = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        return new DocumentoParseado(Tokenizer.tokenizar(texto));
    }
}
