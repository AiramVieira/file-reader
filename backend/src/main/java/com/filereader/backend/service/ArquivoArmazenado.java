package com.filereader.backend.service;

public record ArquivoArmazenado(String nomeOriginal, String caminho, String hash, long tamanho, byte[] conteudo) {
}
