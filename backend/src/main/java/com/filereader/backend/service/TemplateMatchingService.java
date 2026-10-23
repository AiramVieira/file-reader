package com.filereader.backend.service;

import com.filereader.backend.domain.CampoLayout;
import com.filereader.backend.domain.Servico;
import com.filereader.backend.domain.StatusProcessamento;
import com.filereader.backend.domain.TipoCampo;
import com.filereader.backend.parser.TokenBruto;
import com.filereader.backend.repository.CampoLayoutRepository;
import com.filereader.backend.repository.ServicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Identifica qual Servico corresponde a um documento novo: todos os termos de baixa cadastrados devem
 * estar presentes no documento, e nenhum termo de negacao pode estar presente. Sem score parcial nessa etapa.
 */
@Service
public class TemplateMatchingService {

    private static final Pattern PONTUACAO_NAS_PONTAS = Pattern.compile("^[^\\p{L}\\p{N}]+|[^\\p{L}\\p{N}]+$");

    private final ServicoRepository servicoRepository;
    private final CampoLayoutRepository campoLayoutRepository;

    public TemplateMatchingService(ServicoRepository servicoRepository, CampoLayoutRepository campoLayoutRepository) {
        this.servicoRepository = servicoRepository;
        this.campoLayoutRepository = campoLayoutRepository;
    }

    public record DiagnosticoServico(Servico servico, List<String> termosBaixaFaltantes,
                                      List<String> termosNegacaoEncontrados, boolean elegivel) {
    }

    public record ResultadoMatch(StatusProcessamento status, Servico servicoIdentificado,
                                  List<DiagnosticoServico> diagnostico) {
    }

    @Transactional(readOnly = true)
    public ResultadoMatch identificarServico(List<TokenBruto> tokensDocumento) {
        List<String> tokensNormalizados = normalizarTokensParaTermos(tokensDocumento.stream().map(TokenBruto::getTexto).toList());

        List<Servico> candidatos = servicoRepository.findByAtivoTrue();
        List<DiagnosticoServico> diagnostico = new ArrayList<>();
        List<Servico> elegiveis = new ArrayList<>();

        for (Servico servico : candidatos) {
            List<CampoLayout> campos = campoLayoutRepository.findByServicoId(servico.getId());
            List<String> termosBaixa = extrairTextos(campos, TipoCampo.TERMO_BAIXA);
            List<String> termosNegacao = extrairTextos(campos, TipoCampo.TERMO_NEGACAO);

            List<String> faltantes = termosBaixa.stream()
                    .filter(termo -> !contemTermo(tokensNormalizados, termo))
                    .toList();
            List<String> negacoesEncontradas = termosNegacao.stream()
                    .filter(termo -> contemTermo(tokensNormalizados, termo))
                    .toList();

            boolean elegivel = !termosBaixa.isEmpty() && faltantes.isEmpty() && negacoesEncontradas.isEmpty();
            diagnostico.add(new DiagnosticoServico(servico, faltantes, negacoesEncontradas, elegivel));
            if (elegivel) {
                elegiveis.add(servico);
            }
        }

        if (elegiveis.size() == 1) {
            return new ResultadoMatch(StatusProcessamento.MATCH, elegiveis.get(0), diagnostico);
        }
        if (elegiveis.size() > 1) {
            return new ResultadoMatch(StatusProcessamento.AMBIGUO, null, diagnostico);
        }
        return new ResultadoMatch(StatusProcessamento.NO_MATCH, null, diagnostico);
    }

    private List<String> extrairTextos(List<CampoLayout> campos, TipoCampo tipo) {
        return campos.stream().filter(c -> c.getTipo() == tipo).map(CampoLayout::getTokenTexto).toList();
    }

    /** Normalizacao usada em toda comparacao de texto: minusculas, sem acento, espacos colapsados. */
    public static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }

    /** Normaliza os tokens de um documento para comparacao com termos (ver {@link #normalizarPalavraDeTermo}). */
    public static List<String> normalizarTokensParaTermos(List<String> textos) {
        return textos.stream().map(TemplateMatchingService::normalizarPalavraDeTermo).toList();
    }

    /**
     * Verifica se o termo aparece no documento. Termos com varias palavras unidas por "_" (ex.: "Pagamento_de_boleto")
     * exigem as palavras em sequencia; o token literal com "_" tambem conta. Maiusculas, acentos e pontuacao nas
     * pontas de cada palavra sao ignorados.
     */
    public static boolean contemTermo(List<String> tokensNormalizados, String termo) {
        if (termo != null && termo.contains("_") && tokensNormalizados.contains(normalizarPalavraDeTermo(termo))) {
            return true;
        }
        List<String> palavras = Arrays.stream(normalizar(termo).split("[\\s_]+"))
                .filter(p -> !p.isEmpty())
                .map(TemplateMatchingService::normalizarPalavraDeTermo)
                .toList();
        if (palavras.isEmpty()) {
            return false;
        }
        for (int i = 0; i + palavras.size() <= tokensNormalizados.size(); i++) {
            if (tokensNormalizados.subList(i, i + palavras.size()).equals(palavras)) {
                return true;
            }
        }
        return false;
    }

    /** Normaliza e remove pontuacao das pontas ("Nacional," -> "nacional"); tokens so de pontuacao sao mantidos. */
    private static String normalizarPalavraDeTermo(String texto) {
        String normalizado = normalizar(texto);
        String semPontas = PONTUACAO_NAS_PONTAS.matcher(normalizado).replaceAll("");
        return semPontas.isEmpty() ? normalizado : semPontas;
    }
}
