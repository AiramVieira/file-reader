package com.filereader.backend.service;

import com.filereader.backend.domain.CampoLayout;
import com.filereader.backend.domain.DocumentoBase;
import com.filereader.backend.domain.Servico;
import com.filereader.backend.domain.TipoCampo;
import com.filereader.backend.domain.Token;
import com.filereader.backend.parser.DocumentoParseado;
import com.filereader.backend.parser.FileParser;
import com.filereader.backend.parser.FileParserResolver;
import com.filereader.backend.parser.TokenBruto;
import com.filereader.backend.repository.CampoLayoutRepository;
import com.filereader.backend.repository.DocumentoBaseRepository;
import com.filereader.backend.repository.ServicoRepository;
import com.filereader.backend.web.dto.LayoutDTO;
import com.filereader.backend.web.dto.MarcacaoDTO;
import com.filereader.backend.web.dto.ResumoLayoutDTO;
import com.filereader.backend.web.dto.ServicoDTO;
import com.filereader.backend.web.dto.TokenDTO;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Persiste e recarrega a marcacao de campos (Competencia, CNPJ, Termos de baixa/negacao) feita na tela 1. */
@Service
public class LayoutMappingService {

    private final ServicoRepository servicoRepository;
    private final DocumentoBaseRepository documentoBaseRepository;
    private final CampoLayoutRepository campoLayoutRepository;
    private final FileStorageService fileStorageService;
    private final FileParserResolver fileParserResolver;
    private final int tokensAntes;
    private final int tokensDepois;
    private final int distanciaMaxima;

    public LayoutMappingService(ServicoRepository servicoRepository,
                                 DocumentoBaseRepository documentoBaseRepository,
                                 CampoLayoutRepository campoLayoutRepository,
                                 FileStorageService fileStorageService,
                                 FileParserResolver fileParserResolver,
                                 @Value("${filereader.anchor.tokens-before}") int tokensAntes,
                                 @Value("${filereader.anchor.tokens-after}") int tokensDepois,
                                 @Value("${filereader.anchor.max-distance}") int distanciaMaxima) {
        this.servicoRepository = servicoRepository;
        this.documentoBaseRepository = documentoBaseRepository;
        this.campoLayoutRepository = campoLayoutRepository;
        this.fileStorageService = fileStorageService;
        this.fileParserResolver = fileParserResolver;
        this.tokensAntes = tokensAntes;
        this.tokensDepois = tokensDepois;
        this.distanciaMaxima = distanciaMaxima;
    }

    @Transactional
    public LayoutDTO uploadArquivoBase(Long servicoId, MultipartFile arquivo) {
        Servico servico = buscarServico(servicoId);
        ArquivoArmazenado armazenado = fileStorageService.armazenar(arquivo);
        FileParser parser = fileParserResolver.resolver(arquivo);

        DocumentoParseado parseado;
        try {
            parseado = parser.parse(new ByteArrayInputStream(armazenado.conteudo()));
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao parsear arquivo-modelo", e);
        }

        documentoBaseRepository.findByServicoId(servicoId).ifPresent(existente -> {
            campoLayoutRepository.deleteByServicoId(servicoId);
            documentoBaseRepository.delete(existente);
            documentoBaseRepository.flush();
        });

        DocumentoBase documentoBase = new DocumentoBase();
        documentoBase.setServico(servico);
        documentoBase.setNomeArquivoOriginal(armazenado.nomeOriginal());
        documentoBase.setCaminhoArmazenamento(armazenado.caminho());
        documentoBase.setHashSha256(armazenado.hash());
        documentoBase.setTamanhoBytes(armazenado.tamanho());

        for (TokenBruto bruto : parseado.getTokens()) {
            Token token = new Token(bruto.getIndice(), bruto.getTexto(), bruto.getEspacoApos());
            token.setPagina(bruto.getPagina());
            token.setLinha(bruto.getLinha());
            token.setColuna(bruto.getColuna());
            token.setDocumentoBase(documentoBase);
            documentoBase.getTokens().add(token);
        }

        documentoBaseRepository.save(documentoBase);
        return montarLayoutDTO(servico, documentoBase, List.of());
    }

    /**
     * Marca um token (ou um intervalo de tokens) do arquivo-modelo.
     * <p>
     * Competencia e CNPJ aceitam um unico token. Termos de baixa/negacao aceitam um intervalo: as palavras sao unidas
     * com "_" em um unico termo (ex.: "Pagamento_de_boleto"), que so casa quando as palavras aparecem em sequencia.
     */
    @Transactional
    public LayoutDTO marcarCampo(Long servicoId, int tokenIndice, Integer tokenIndiceFim, TipoCampo tipo) {
        Servico servico = buscarServico(servicoId);
        DocumentoBase documentoBase = buscarDocumentoBase(servicoId);

        int inicio = tokenIndiceFim == null ? tokenIndice : Math.min(tokenIndice, tokenIndiceFim);
        int fim = tokenIndiceFim == null ? tokenIndice : Math.max(tokenIndice, tokenIndiceFim);
        boolean intervalo = fim > inicio;
        if (intervalo && tipo.isSelecaoUnica()) {
            throw new IllegalArgumentException("Competência e CNPJ aceitam apenas uma palavra");
        }
        Integer fimGravado = intervalo ? fim : null;

        List<Token> tokens = documentoBase.getTokens();
        Map<Integer, String> textoPorIndice = new HashMap<>();
        for (Token t : tokens) {
            textoPorIndice.put(t.getIndice(), t.getTexto());
        }
        List<String> palavras = new ArrayList<>();
        for (int i = inicio; i <= fim; i++) {
            String texto = textoPorIndice.get(i);
            if (texto == null) {
                throw new IllegalArgumentException("Token nao encontrado: " + i);
            }
            palavras.add(texto);
        }

        List<CampoLayout> camposAtuais = campoLayoutRepository.findByServicoId(servicoId);

        if (tipo.isSelecaoUnica()) {
            camposAtuais.stream()
                    .filter(c -> c.getTipo() == tipo)
                    .forEach(campoLayoutRepository::delete);
            campoLayoutRepository.flush();
        } else {
            boolean jaMarcado = camposAtuais.stream()
                    .anyMatch(c -> c.getTipo() == tipo && Objects.equals(c.getTokenIndice(), inicio)
                            && Objects.equals(c.getTokenIndiceFim(), fimGravado));
            if (jaMarcado) {
                return montarLayoutDTO(servico, documentoBase, campoLayoutRepository.findByServicoId(servicoId));
            }
        }

        CampoLayout campo = new CampoLayout();
        campo.setServico(servico);
        campo.setTipo(tipo);
        campo.setTokenIndice(inicio);
        campo.setTokenIndiceFim(fimGravado);
        campo.setTokenTexto(String.join("_", palavras));

        if (tipo.isSelecaoUnica()) {
            AnchorMatchingService.selecionarAncoras(textoPorIndice, inicio, tokensAntes, tokensDepois, distanciaMaxima)
                    .forEach(campo::adicionarAncora);
        }

        campoLayoutRepository.save(campo);
        return montarLayoutDTO(servico, documentoBase, campoLayoutRepository.findByServicoId(servicoId));
    }

    /** Desmarca a marcacao de um unico token (para intervalos, use {@link #removerMarcacao}). */
    @Transactional
    public LayoutDTO desmarcarCampo(Long servicoId, int tokenIndice, TipoCampo tipo) {
        Servico servico = buscarServico(servicoId);
        DocumentoBase documentoBase = buscarDocumentoBase(servicoId);

        campoLayoutRepository.findByServicoId(servicoId).stream()
                .filter(c -> c.getTipo() == tipo && Objects.equals(c.getTokenIndice(), tokenIndice)
                        && c.getTokenIndiceFim() == null)
                .forEach(campoLayoutRepository::delete);

        return montarLayoutDTO(servico, documentoBase, campoLayoutRepository.findByServicoId(servicoId));
    }

    /** Remove qualquer marcacao (token unico, intervalo ou termo digitado) pelo id. */
    @Transactional
    public LayoutDTO removerMarcacao(Long servicoId, Long campoId) {
        Servico servico = buscarServico(servicoId);
        DocumentoBase documentoBase = buscarDocumentoBase(servicoId);

        campoLayoutRepository.findByServicoId(servicoId).stream()
                .filter(c -> c.getId().equals(campoId))
                .forEach(campoLayoutRepository::delete);

        return montarLayoutDTO(servico, documentoBase, campoLayoutRepository.findByServicoId(servicoId));
    }

    /** Termo de negacao digitado. Varias palavras viram um unico termo unido por "_" (ex.: "Receitas_Federais"). */
    @Transactional
    public LayoutDTO adicionarTermoNegacao(Long servicoId, String texto) {
        Servico servico = buscarServico(servicoId);
        DocumentoBase documentoBase = buscarDocumentoBase(servicoId);
        String termo = texto.trim().replaceAll("\\s+", "_");
        if (termo.isEmpty()) {
            throw new IllegalArgumentException("Informe o termo de negação");
        }

        List<CampoLayout> camposAtuais = campoLayoutRepository.findByServicoId(servicoId);
        boolean jaExiste = camposAtuais.stream()
                .anyMatch(c -> c.getTipo() == TipoCampo.TERMO_NEGACAO
                        && TemplateMatchingService.normalizar(c.getTokenTexto()).equals(TemplateMatchingService.normalizar(termo)));
        if (!jaExiste) {
            CampoLayout campo = new CampoLayout();
            campo.setServico(servico);
            campo.setTipo(TipoCampo.TERMO_NEGACAO);
            campo.setTokenTexto(termo);
            campoLayoutRepository.save(campo);
        }
        return montarLayoutDTO(servico, documentoBase, campoLayoutRepository.findByServicoId(servicoId));
    }

    @Transactional
    public LayoutDTO removerTermoNegacao(Long servicoId, String texto) {
        Servico servico = buscarServico(servicoId);
        DocumentoBase documentoBase = buscarDocumentoBase(servicoId);

        campoLayoutRepository.findByServicoId(servicoId).stream()
                .filter(c -> c.getTipo() == TipoCampo.TERMO_NEGACAO && c.getTokenTexto().equals(texto))
                .forEach(campoLayoutRepository::delete);

        return montarLayoutDTO(servico, documentoBase, campoLayoutRepository.findByServicoId(servicoId));
    }

    @Transactional(readOnly = true)
    public LayoutDTO obterLayout(Long servicoId) {
        Servico servico = buscarServico(servicoId);
        return documentoBaseRepository.findByServicoId(servicoId)
                .map(documentoBase -> montarLayoutDTO(servico, documentoBase, campoLayoutRepository.findByServicoId(servicoId)))
                .orElseGet(() -> LayoutDTO.semArquivo(ServicoDTO.of(servico)));
    }

    private Servico buscarServico(Long id) {
        return servicoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Servico nao encontrado: " + id));
    }

    private DocumentoBase buscarDocumentoBase(Long servicoId) {
        return documentoBaseRepository.findByServicoId(servicoId)
                .orElseThrow(() -> new IllegalStateException("Servico ainda nao possui arquivo-modelo enviado"));
    }

    private LayoutDTO montarLayoutDTO(Servico servico, DocumentoBase documentoBase, List<CampoLayout> campos) {
        Map<Integer, List<TipoCampo>> tiposPorTokenIndice = new HashMap<>();
        for (CampoLayout campo : campos) {
            if (campo.getTokenIndice() == null) {
                continue;
            }
            int fim = campo.getTokenIndiceFim() != null ? campo.getTokenIndiceFim() : campo.getTokenIndice();
            for (int i = campo.getTokenIndice(); i <= fim; i++) {
                List<TipoCampo> tipos = tiposPorTokenIndice.computeIfAbsent(i, k -> new ArrayList<>());
                if (!tipos.contains(campo.getTipo())) {
                    tipos.add(campo.getTipo());
                }
            }
        }

        List<TokenDTO> tokenDTOs = documentoBase.getTokens().stream()
                .sorted(Comparator.comparingInt(Token::getIndice))
                .map(t -> new TokenDTO(t.getIndice(), t.getTexto(), t.getEspacoApos(), t.getPagina(), t.getLinha(),
                        t.getColuna(), tiposPorTokenIndice.getOrDefault(t.getIndice(), List.of())))
                .toList();

        String competencia = campos.stream().filter(c -> c.getTipo() == TipoCampo.COMPETENCIA)
                .map(CampoLayout::getTokenTexto).findFirst().orElse(null);
        String cnpj = campos.stream().filter(c -> c.getTipo() == TipoCampo.CNPJ)
                .map(CampoLayout::getTokenTexto).findFirst().orElse(null);
        List<String> termosBaixa = campos.stream().filter(c -> c.getTipo() == TipoCampo.TERMO_BAIXA)
                .map(CampoLayout::getTokenTexto).toList();
        List<String> termosNegacao = campos.stream().filter(c -> c.getTipo() == TipoCampo.TERMO_NEGACAO)
                .map(CampoLayout::getTokenTexto).toList();

        // Termo de negacao presente no proprio modelo: documentos iguais ao modelo nao serao reconhecidos (aviso na tela).
        List<String> tokensModelo = TemplateMatchingService.normalizarTokensParaTermos(
                documentoBase.getTokens().stream().map(Token::getTexto).toList());
        List<MarcacaoDTO> marcacoes = campos.stream()
                .sorted(Comparator.comparing(CampoLayout::getId))
                .map(c -> new MarcacaoDTO(c.getId(), c.getTipo(), c.getTokenIndice(), c.getTokenIndiceFim(),
                        c.getTokenTexto(),
                        c.getTipo() == TipoCampo.TERMO_NEGACAO && TemplateMatchingService.contemTermo(tokensModelo, c.getTokenTexto())))
                .toList();

        ResumoLayoutDTO resumo = new ResumoLayoutDTO(competencia, cnpj, termosBaixa, termosNegacao);
        return new LayoutDTO(ServicoDTO.of(servico), true, documentoBase.getNomeArquivoOriginal(), tokenDTOs, resumo,
                marcacoes);
    }
}
