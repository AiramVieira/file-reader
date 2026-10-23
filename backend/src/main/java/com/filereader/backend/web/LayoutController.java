package com.filereader.backend.web;

import com.filereader.backend.domain.TipoCampo;
import com.filereader.backend.service.LayoutMappingService;
import com.filereader.backend.web.dto.LayoutDTO;
import com.filereader.backend.web.dto.MarcarCampoRequest;
import com.filereader.backend.web.dto.TermoNegacaoRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/servicos/{servicoId}/layout")
public class LayoutController {

    private final LayoutMappingService layoutMappingService;

    public LayoutController(LayoutMappingService layoutMappingService) {
        this.layoutMappingService = layoutMappingService;
    }

    @GetMapping
    public LayoutDTO obter(@PathVariable Long servicoId) {
        return layoutMappingService.obterLayout(servicoId);
    }

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public LayoutDTO upload(@PathVariable Long servicoId, @RequestParam("arquivo") MultipartFile arquivo) {
        return layoutMappingService.uploadArquivoBase(servicoId, arquivo);
    }

    @PutMapping("/marcacoes")
    public LayoutDTO marcar(@PathVariable Long servicoId, @Valid @RequestBody MarcarCampoRequest request) {
        return layoutMappingService.marcarCampo(servicoId, request.tokenIndice(), request.tokenIndiceFim(), request.tipo());
    }

    @DeleteMapping("/marcacoes")
    public LayoutDTO desmarcar(@PathVariable Long servicoId,
                                @RequestParam int tokenIndice,
                                @RequestParam TipoCampo tipo) {
        return layoutMappingService.desmarcarCampo(servicoId, tokenIndice, tipo);
    }

    @DeleteMapping("/marcacoes/{campoId}")
    public LayoutDTO removerMarcacao(@PathVariable Long servicoId, @PathVariable Long campoId) {
        return layoutMappingService.removerMarcacao(servicoId, campoId);
    }

    @PostMapping("/termos-negacao")
    public LayoutDTO adicionarTermoNegacao(@PathVariable Long servicoId, @Valid @RequestBody TermoNegacaoRequest request) {
        return layoutMappingService.adicionarTermoNegacao(servicoId, request.texto());
    }

    @DeleteMapping("/termos-negacao")
    public LayoutDTO removerTermoNegacao(@PathVariable Long servicoId, @RequestParam String texto) {
        return layoutMappingService.removerTermoNegacao(servicoId, texto);
    }
}
