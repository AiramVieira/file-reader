package com.filereader.backend.web.dto;

import java.util.List;

public record DiagnosticoServicoDTO(Long servicoId, String nomeServico, List<String> termosBaixaFaltantes,
                                     List<String> termosNegacaoEncontrados, boolean elegivel) {
}
