package com.filereader.backend.web.dto;

import java.util.List;

public record ResumoLayoutDTO(String competencia, String cnpj, List<String> termosBaixa, List<String> termosNegacao) {

    public static ResumoLayoutDTO vazio() {
        return new ResumoLayoutDTO(null, null, List.of(), List.of());
    }
}
