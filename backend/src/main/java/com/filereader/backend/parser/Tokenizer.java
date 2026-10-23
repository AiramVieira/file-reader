package com.filereader.backend.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Tokenizacao compartilhada: separa por espacos/quebras de linha, preservando o fragmento de espaco entre tokens. */
public final class Tokenizer {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("(\\S+)(\\s*)");

    private Tokenizer() {
    }

    public static List<TokenBruto> tokenizar(String texto) {
        List<TokenBruto> tokens = new ArrayList<>();
        if (texto == null || texto.isEmpty()) {
            return tokens;
        }
        Matcher matcher = TOKEN_PATTERN.matcher(texto);
        int indice = 0;
        while (matcher.find()) {
            String tokenTexto = matcher.group(1);
            String espaco = matcher.group(2);
            tokens.add(new TokenBruto(indice++, tokenTexto, espaco.isEmpty() ? " " : espaco));
        }
        return tokens;
    }
}
