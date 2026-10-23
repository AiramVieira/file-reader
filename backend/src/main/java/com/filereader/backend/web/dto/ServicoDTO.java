package com.filereader.backend.web.dto;

import com.filereader.backend.domain.Servico;

public record ServicoDTO(Long id, String nome, String descricao, boolean ativo) {

    public static ServicoDTO of(Servico servico) {
        return new ServicoDTO(servico.getId(), servico.getNome(), servico.getDescricao(), servico.isAtivo());
    }
}
