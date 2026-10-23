package com.filereader.backend.web.dto;

import jakarta.validation.constraints.NotBlank;

public record AtualizarServicoRequest(@NotBlank String nome, String descricao, boolean ativo) {
}
