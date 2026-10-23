package com.filereader.backend.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CriarServicoRequest(@NotBlank String nome, String descricao) {
}
