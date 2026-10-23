package com.filereader.backend.web.dto;

import jakarta.validation.constraints.NotBlank;

public record TermoNegacaoRequest(@NotBlank String texto) {
}
