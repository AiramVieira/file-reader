package com.filereader.backend.parser;

import com.filereader.backend.domain.TipoArquivo;

import java.io.IOException;
import java.io.InputStream;

/** Contrato comum de parsing: qualquer formato suportado vira uma lista ordenada e uniforme de tokens. */
public interface FileParser {

    boolean suporta(TipoArquivo tipoArquivo);

    DocumentoParseado parse(InputStream inputStream) throws IOException;
}
