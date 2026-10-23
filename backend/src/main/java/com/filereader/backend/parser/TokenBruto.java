package com.filereader.backend.parser;

import lombok.Getter;
import lombok.Setter;

/** Token cru resultante do parsing de um arquivo, antes de ser persistido como {@link com.filereader.backend.domain.Token}. */
@Getter
public class TokenBruto {

    private final int indice;
    private final String texto;
    private final String espacoApos;

    @Setter
    private Integer pagina;
    @Setter
    private Integer linha;
    @Setter
    private Integer coluna;

    public TokenBruto(int indice, String texto, String espacoApos) {
        this.indice = indice;
        this.texto = texto;
        this.espacoApos = espacoApos;
    }
}
