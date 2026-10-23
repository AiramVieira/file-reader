package com.filereader.backend.parser;

import com.filereader.backend.domain.TipoArquivo;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Locale;

/** Resolve o parser adequado detectando o tipo do arquivo automaticamente (extensao, com fallback por assinatura de bytes). */
@Component
public class FileParserResolver {

    private final List<FileParser> parsers;

    public FileParserResolver(List<FileParser> parsers) {
        this.parsers = parsers;
    }

    public FileParser resolver(MultipartFile arquivo) {
        TipoArquivo tipo = detectar(arquivo);
        return parsers.stream()
                .filter(p -> p.suporta(tipo))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Nenhum parser disponivel para o arquivo: " + arquivo.getOriginalFilename()));
    }

    private TipoArquivo detectar(MultipartFile arquivo) {
        TipoArquivo porExtensao = porExtensao(arquivo.getOriginalFilename());
        if (porExtensao != null) {
            return porExtensao;
        }
        return porConteudo(arquivo);
    }

    private TipoArquivo porExtensao(String nomeOriginal) {
        if (nomeOriginal == null) {
            return null;
        }
        String nome = nomeOriginal.toLowerCase(Locale.ROOT);
        if (nome.endsWith(".pdf")) {
            return TipoArquivo.PDF;
        }
        if (nome.endsWith(".xlsx") || nome.endsWith(".xls")) {
            return TipoArquivo.XLSX;
        }
        if (nome.endsWith(".csv")) {
            return TipoArquivo.CSV;
        }
        if (nome.endsWith(".xml")) {
            return TipoArquivo.XML;
        }
        if (nome.endsWith(".txt")) {
            return TipoArquivo.TXT;
        }
        return null;
    }

    /** Fallback quando o nome do arquivo nao tem extensao reconhecida: inspeciona os primeiros bytes. */
    private TipoArquivo porConteudo(MultipartFile arquivo) {
        try {
            byte[] cabecalho = arquivo.getInputStream().readNBytes(4);
            if (cabecalho.length == 4 && cabecalho[0] == '%' && cabecalho[1] == 'P'
                    && cabecalho[2] == 'D' && cabecalho[3] == 'F') {
                return TipoArquivo.PDF;
            }
            if (cabecalho.length == 4 && cabecalho[0] == 'P' && cabecalho[1] == 'K'
                    && cabecalho[2] == 0x03 && cabecalho[3] == 0x04) {
                return TipoArquivo.XLSX;
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao inspecionar o arquivo enviado", e);
        }
        return TipoArquivo.TXT;
    }
}
