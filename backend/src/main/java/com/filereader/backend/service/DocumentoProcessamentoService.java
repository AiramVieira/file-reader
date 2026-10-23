package com.filereader.backend.service;

import tools.jackson.databind.ObjectMapper;
import com.filereader.backend.domain.CampoLayout;
import com.filereader.backend.domain.ExecucaoProcessamento;
import com.filereader.backend.domain.Servico;
import com.filereader.backend.domain.StatusProcessamento;
import com.filereader.backend.domain.TipoCampo;
import com.filereader.backend.parser.DocumentoParseado;
import com.filereader.backend.parser.FileParser;
import com.filereader.backend.parser.FileParserResolver;
import com.filereader.backend.repository.CampoLayoutRepository;
import com.filereader.backend.repository.ExecucaoProcessamentoRepository;
import com.filereader.backend.web.dto.DiagnosticoServicoDTO;
import com.filereader.backend.web.dto.ResultadoProcessamentoDTO;
import com.filereader.backend.web.dto.ServicoDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** Orquestra o fluxo da tela 2: tokeniza o documento novo, identifica o Servico e extrai Competencia/CNPJ. */
@Service
public class DocumentoProcessamentoService {

    private final FileStorageService fileStorageService;
    private final FileParserResolver fileParserResolver;
    private final TemplateMatchingService templateMatchingService;
    private final AnchorMatchingService anchorMatchingService;
    private final CampoLayoutRepository campoLayoutRepository;
    private final ExecucaoProcessamentoRepository execucaoProcessamentoRepository;
    private final ObjectMapper objectMapper;

    public DocumentoProcessamentoService(FileStorageService fileStorageService,
                                          FileParserResolver fileParserResolver,
                                          TemplateMatchingService templateMatchingService,
                                          AnchorMatchingService anchorMatchingService,
                                          CampoLayoutRepository campoLayoutRepository,
                                          ExecucaoProcessamentoRepository execucaoProcessamentoRepository,
                                          ObjectMapper objectMapper) {
        this.fileStorageService = fileStorageService;
        this.fileParserResolver = fileParserResolver;
        this.templateMatchingService = templateMatchingService;
        this.anchorMatchingService = anchorMatchingService;
        this.campoLayoutRepository = campoLayoutRepository;
        this.execucaoProcessamentoRepository = execucaoProcessamentoRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ResultadoProcessamentoDTO processar(MultipartFile arquivo) {
        ArquivoArmazenado armazenado = fileStorageService.armazenar(arquivo);
        FileParser parser = fileParserResolver.resolver(arquivo);

        DocumentoParseado parseado;
        try {
            parseado = parser.parse(new ByteArrayInputStream(armazenado.conteudo()));
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao parsear documento", e);
        }

        TemplateMatchingService.ResultadoMatch resultadoMatch =
                templateMatchingService.identificarServico(parseado.getTokens());

        List<DiagnosticoServicoDTO> diagnosticoDTO = resultadoMatch.diagnostico().stream()
                .map(d -> new DiagnosticoServicoDTO(d.servico().getId(), d.servico().getNome(),
                        d.termosBaixaFaltantes(), d.termosNegacaoEncontrados(), d.elegivel()))
                .toList();

        ExecucaoProcessamento execucao = new ExecucaoProcessamento();
        execucao.setNomeArquivoOriginal(armazenado.nomeOriginal());
        execucao.setCaminhoArmazenamento(armazenado.caminho());
        execucao.setStatus(resultadoMatch.status());
        execucao.setDiagnostico(serializarDiagnostico(diagnosticoDTO));

        String competencia = null;
        String cnpj = null;
        BigDecimal scoreCompetencia = null;
        BigDecimal scoreCnpj = null;
        ServicoDTO servicoDTO = null;

        if (resultadoMatch.status() == StatusProcessamento.MATCH) {
            Servico servico = resultadoMatch.servicoIdentificado();
            execucao.setServico(servico);
            servicoDTO = ServicoDTO.of(servico);

            List<CampoLayout> campos = campoLayoutRepository.findByServicoId(servico.getId());
            Optional<CampoLayout> campoCompetencia = campos.stream()
                    .filter(c -> c.getTipo() == TipoCampo.COMPETENCIA).findFirst();
            Optional<CampoLayout> campoCnpj = campos.stream()
                    .filter(c -> c.getTipo() == TipoCampo.CNPJ).findFirst();

            if (campoCompetencia.isPresent()) {
                AnchorMatchingService.ResultadoExtracao resultado =
                        anchorMatchingService.extrair(campoCompetencia.get(), parseado.getTokens());
                competencia = resultado.valor();
                scoreCompetencia = resultado.score();
            }
            if (campoCnpj.isPresent()) {
                AnchorMatchingService.ResultadoExtracao resultado =
                        anchorMatchingService.extrair(campoCnpj.get(), parseado.getTokens());
                cnpj = resultado.valor();
                scoreCnpj = resultado.score();
            }
        }

        execucao.setCompetenciaExtraida(competencia);
        execucao.setCnpjExtraido(cnpj);
        execucao.setScoreExtracaoCompetencia(scoreCompetencia);
        execucao.setScoreExtracaoCnpj(scoreCnpj);
        execucaoProcessamentoRepository.save(execucao);

        return new ResultadoProcessamentoDTO(resultadoMatch.status(), servicoDTO, competencia, scoreCompetencia,
                cnpj, scoreCnpj, diagnosticoDTO);
    }

    private String serializarDiagnostico(List<DiagnosticoServicoDTO> diagnostico) {
        try {
            return objectMapper.writeValueAsString(diagnostico);
        } catch (Exception e) {
            return "[]";
        }
    }
}
