package com.filereader.backend.web.dto;

import java.util.List;

public record LayoutDTO(ServicoDTO servico, boolean temArquivoBase, String nomeArquivoBase,
                         List<TokenDTO> tokens, ResumoLayoutDTO resumo, List<MarcacaoDTO> marcacoes) {

    public static LayoutDTO semArquivo(ServicoDTO servico) {
        return new LayoutDTO(servico, false, null, List.of(), ResumoLayoutDTO.vazio(), List.of());
    }
}
