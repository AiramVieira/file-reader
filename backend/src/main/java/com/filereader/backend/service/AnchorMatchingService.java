package com.filereader.backend.service;

import com.filereader.backend.domain.AncoraToken;
import com.filereader.backend.domain.CampoLayout;
import com.filereader.backend.parser.TokenBruto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Extrai o valor de CNPJ/Competencia de um documento novo, localizando no texto tokenizado os mesmos tokens-ancora
 * salvos ao mapear o layout.
 * <p>
 * Ancoras sao tokens de texto fixo proximos ao campo (tokens com digitos, como datas, valores e codigos, sao pulados
 * na hora da marcacao porque mudam de um documento para outro). O score combina o total de ancoras encontradas com o
 * melhor lado (antes/depois): basta um lado inteiro bater para o valor ser aceito, ja que o outro lado com frequencia
 * e outro dado variavel (ex.: razao social logo apos o CNPJ). So sao candidatos tokens com o mesmo formato do valor
 * marcado no modelo. Empate no score maximo e reportado como ambiguo, sem desempate por sorte.
 */
@Service
public class AnchorMatchingService {

    private final double limiar;

    public AnchorMatchingService(@Value("${filereader.anchor.match-threshold}") double limiar) {
        this.limiar = limiar;
    }

    public record ResultadoExtracao(String valor, BigDecimal score, boolean ambiguo) {
        public static ResultadoExtracao vazio() {
            return new ResultadoExtracao(null, BigDecimal.ZERO, false);
        }
    }

    /**
     * Escolhe as ancoras de um campo: ate {@code quantidadeAntes}/{@code quantidadeDepois} tokens sem digitos mais
     * proximos de cada lado, a no maximo {@code distanciaMaxima} posicoes. Se um lado nao tiver nenhum token fixo
     * nesse alcance, usa os vizinhos imediatos daquele lado, mesmo que variaveis.
     *
     * @return mapa posicao relativa -> texto do token
     */
    public static Map<Integer, String> selecionarAncoras(Map<Integer, String> textoPorIndice, int indiceCampo,
                                                         int quantidadeAntes, int quantidadeDepois, int distanciaMaxima) {
        Map<Integer, String> ancoras = new LinkedHashMap<>();
        ancoras.putAll(selecionarLado(textoPorIndice, indiceCampo, -1, quantidadeAntes, distanciaMaxima));
        ancoras.putAll(selecionarLado(textoPorIndice, indiceCampo, 1, quantidadeDepois, distanciaMaxima));
        return ancoras;
    }

    private static Map<Integer, String> selecionarLado(Map<Integer, String> textoPorIndice, int indiceCampo,
                                                       int direcao, int quantidade, int distanciaMaxima) {
        Map<Integer, String> fixos = new LinkedHashMap<>();
        for (int k = 1; k <= distanciaMaxima && fixos.size() < quantidade; k++) {
            String texto = textoPorIndice.get(indiceCampo + direcao * k);
            if (texto == null) {
                break;
            }
            if (!pareceDadoVariavel(texto)) {
                fixos.put(direcao * k, texto);
            }
        }
        if (!fixos.isEmpty()) {
            return fixos;
        }
        Map<Integer, String> vizinhos = new LinkedHashMap<>();
        for (int k = 1; k <= quantidade; k++) {
            String texto = textoPorIndice.get(indiceCampo + direcao * k);
            if (texto == null) {
                break;
            }
            vizinhos.put(direcao * k, texto);
        }
        return vizinhos;
    }

    /** Tokens com digitos (datas, valores, codigos, numeros de documento) tendem a mudar entre documentos do mesmo layout. */
    static boolean pareceDadoVariavel(String texto) {
        return texto.chars().anyMatch(Character::isDigit);
    }

    /**
     * Formato do token: sequencias de letras viram "a", sequencias de digitos viram "9", demais caracteres sao mantidos.
     * Ex.: "12.345.678/0001-90" -> "9.9.9/9-9"; "Maio/2026" -> "a/9"; "30/06/2026" -> "9/9/9".
     */
    static String formato(String texto) {
        StringBuilder sb = new StringBuilder();
        char anterior = 0;
        for (char c : texto.toCharArray()) {
            char classe = Character.isLetter(c) ? 'a' : Character.isDigit(c) ? '9' : c;
            if ((classe == 'a' || classe == '9') && classe == anterior) {
                continue;
            }
            sb.append(classe);
            anterior = classe;
        }
        return sb.toString();
    }

    public ResultadoExtracao extrair(CampoLayout campoLayout, List<TokenBruto> tokensDocumento) {
        List<AncoraToken> ancoras = campoLayout.getAncoras();
        if (ancoras.isEmpty() || tokensDocumento.isEmpty()) {
            return ResultadoExtracao.vazio();
        }

        List<AncoraToken> antes = ancoras.stream()
                .filter(a -> a.getPosicaoRelativa() < 0)
                .sorted(Comparator.comparingInt(a -> -a.getPosicaoRelativa()))
                .toList();
        List<AncoraToken> depois = ancoras.stream()
                .filter(a -> a.getPosicaoRelativa() > 0)
                .sorted(Comparator.comparingInt(AncoraToken::getPosicaoRelativa))
                .toList();

        Map<Integer, String> textoNormalizadoPorIndice = new HashMap<>();
        for (TokenBruto t : tokensDocumento) {
            textoNormalizadoPorIndice.put(t.getIndice(), TemplateMatchingService.normalizar(t.getTexto()));
        }

        String formatoEsperado = campoLayout.getTokenTexto() != null ? formato(campoLayout.getTokenTexto()) : null;

        double melhorScore = 0.0;
        List<TokenBruto> melhores = new ArrayList<>();

        for (TokenBruto candidato : tokensDocumento) {
            if (formatoEsperado != null && !formato(candidato.getTexto()).equals(formatoEsperado)) {
                continue;
            }
            double[] ladoAntes = pontuarLado(antes, candidato.getIndice(), textoNormalizadoPorIndice);
            double[] ladoDepois = pontuarLado(depois, candidato.getIndice(), textoNormalizadoPorIndice);

            double pesoTotal = ladoAntes[1] + ladoDepois[1];
            double combinado = pesoTotal == 0 ? 0 : (ladoAntes[0] + ladoDepois[0]) / pesoTotal;
            double melhorLado = Math.max(razao(ladoAntes), razao(ladoDepois));
            double score = (combinado + melhorLado) / 2;

            if (score <= 0) {
                continue;
            }
            if (score > melhorScore) {
                melhorScore = score;
                melhores.clear();
                melhores.add(candidato);
            } else if (score == melhorScore) {
                melhores.add(candidato);
            }
        }

        if (melhores.isEmpty() || melhorScore < limiar) {
            return ResultadoExtracao.vazio();
        }

        BigDecimal scoreArredondado = BigDecimal.valueOf(melhorScore).setScale(3, RoundingMode.HALF_UP);

        long valoresDistintos = melhores.stream().map(TokenBruto::getTexto).distinct().count();
        if (valoresDistintos > 1) {
            return new ResultadoExtracao(null, scoreArredondado, true);
        }
        return new ResultadoExtracao(melhores.get(0).getTexto(), scoreArredondado, false);
    }

    /**
     * Pontua um lado (ancoras ja ordenadas da mais proxima para a mais distante do campo). Peso decrescente pela ordem
     * de proximidade: a ancora mais proxima pesa 1, a segunda 1/2, a terceira 1/3.
     *
     * @return {peso obtido, peso total do lado}
     */
    private double[] pontuarLado(List<AncoraToken> ancorasLado, int indiceCandidato, Map<Integer, String> textoPorIndice) {
        double obtido = 0;
        double total = 0;
        for (int i = 0; i < ancorasLado.size(); i++) {
            AncoraToken ancora = ancorasLado.get(i);
            double peso = 1.0 / (i + 1);
            total += peso;
            String encontrado = textoPorIndice.get(indiceCandidato + ancora.getPosicaoRelativa());
            if (encontrado != null && encontrado.equals(TemplateMatchingService.normalizar(ancora.getTexto()))) {
                obtido += peso;
            }
        }
        return new double[]{obtido, total};
    }

    private static double razao(double[] lado) {
        return lado[1] == 0 ? 0 : lado[0] / lado[1];
    }
}
