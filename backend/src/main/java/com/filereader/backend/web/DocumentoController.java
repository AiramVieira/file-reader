package com.filereader.backend.web;

import com.filereader.backend.service.DocumentoProcessamentoService;
import com.filereader.backend.web.dto.ResultadoProcessamentoDTO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documentos")
public class DocumentoController {

    private final DocumentoProcessamentoService documentoProcessamentoService;

    public DocumentoController(DocumentoProcessamentoService documentoProcessamentoService) {
        this.documentoProcessamentoService = documentoProcessamentoService;
    }

    @PostMapping(value = "/processar", consumes = "multipart/form-data")
    public ResultadoProcessamentoDTO processar(@RequestParam("arquivo") MultipartFile arquivo) {
        return documentoProcessamentoService.processar(arquivo);
    }
}
