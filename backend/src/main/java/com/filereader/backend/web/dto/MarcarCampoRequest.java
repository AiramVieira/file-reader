package com.filereader.backend.web.dto;

import com.filereader.backend.domain.TipoCampo;
import jakarta.validation.constraints.NotNull;

/** {@code tokenIndiceFim} opcional: quando informado, marca o intervalo de tokens como um unico termo (baixa/negacao). */
public record MarcarCampoRequest(@NotNull Integer tokenIndice, Integer tokenIndiceFim, @NotNull TipoCampo tipo) {
}
