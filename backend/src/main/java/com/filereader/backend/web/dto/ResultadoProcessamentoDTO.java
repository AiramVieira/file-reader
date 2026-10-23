package com.filereader.backend.web.dto;

import com.filereader.backend.domain.StatusProcessamento;

import java.math.BigDecimal;
import java.util.List;

public record ResultadoProcessamentoDTO(
        StatusProcessamento status,
        ServicoDTO servicoIdentificado,
        String competenciaExtraida,
        BigDecimal scoreCompetencia,
        String cnpjExtraido,
        BigDecimal scoreCnpj,
        List<DiagnosticoServicoDTO> diagnostico
) {
}
