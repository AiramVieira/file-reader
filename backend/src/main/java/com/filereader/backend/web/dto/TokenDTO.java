package com.filereader.backend.web.dto;

import com.filereader.backend.domain.TipoCampo;

import java.util.List;

public record TokenDTO(int indice, String texto, String espacoApos, Integer pagina, Integer linha, Integer coluna,
                        List<TipoCampo> tiposMarcados) {
}
