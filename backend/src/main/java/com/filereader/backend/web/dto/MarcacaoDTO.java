package com.filereader.backend.web.dto;

import com.filereader.backend.domain.TipoCampo;

/**
 * Uma marcacao do layout. {@code tokenIndice} e nulo para termos digitados; {@code tokenIndiceFim} so existe em
 * termos marcados como intervalo. {@code presenteNoModelo} indica termo de negacao que aparece no proprio
 * arquivo-modelo (documentos iguais ao modelo nao serao reconhecidos pelo servico).
 */
public record MarcacaoDTO(Long id, TipoCampo tipo, Integer tokenIndice, Integer tokenIndiceFim, String texto,
                          boolean presenteNoModelo) {
}
