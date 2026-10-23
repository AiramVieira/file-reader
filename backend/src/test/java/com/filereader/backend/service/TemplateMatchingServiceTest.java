package com.filereader.backend.service;

import com.filereader.backend.domain.CampoLayout;
import com.filereader.backend.domain.Servico;
import com.filereader.backend.domain.StatusProcessamento;
import com.filereader.backend.domain.TipoCampo;
import com.filereader.backend.parser.TokenBruto;
import com.filereader.backend.parser.Tokenizer;
import com.filereader.backend.repository.CampoLayoutRepository;
import com.filereader.backend.repository.ServicoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TemplateMatchingServiceTest {

    @Mock
    private ServicoRepository servicoRepository;
    @Mock
    private CampoLayoutRepository campoLayoutRepository;

    private TemplateMatchingService service;
    private Servico servicoBoleto;

    @BeforeEach
    void setUp() {
        service = new TemplateMatchingService(servicoRepository, campoLayoutRepository);
        servicoBoleto = new Servico();
        servicoBoleto.setId(1L);
        servicoBoleto.setNome("Boleto Quitado");
    }

    private CampoLayout campo(TipoCampo tipo, String texto) {
        CampoLayout campo = new CampoLayout();
        campo.setTipo(tipo);
        campo.setTokenTexto(texto);
        return campo;
    }

    @Test
    void deveIdentificarServicoQuandoTodosOsTermosDeBaixaEstaoPresentesENenhumDeNegacao() {
        when(servicoRepository.findByAtivoTrue()).thenReturn(List.of(servicoBoleto));
        when(campoLayoutRepository.findByServicoId(1L)).thenReturn(List.of(
                campo(TipoCampo.TERMO_BAIXA, "quitacao"),
                campo(TipoCampo.TERMO_BAIXA, "integral"),
                campo(TipoCampo.TERMO_NEGACAO, "cancelado")
        ));

        List<TokenBruto> tokensDoc = Tokenizer.tokenizar("Quitacao Integral confirmada, sem cancelamento.");

        TemplateMatchingService.ResultadoMatch resultado = service.identificarServico(tokensDoc);

        assertThat(resultado.status()).isEqualTo(StatusProcessamento.MATCH);
        assertThat(resultado.servicoIdentificado()).isEqualTo(servicoBoleto);
    }

    @Test
    void naoDeveIdentificarServicoQuandoFaltaUmTermoDeBaixa() {
        when(servicoRepository.findByAtivoTrue()).thenReturn(List.of(servicoBoleto));
        when(campoLayoutRepository.findByServicoId(1L)).thenReturn(List.of(
                campo(TipoCampo.TERMO_BAIXA, "quitacao"),
                campo(TipoCampo.TERMO_BAIXA, "integral")
        ));

        List<TokenBruto> tokensDoc = Tokenizer.tokenizar("Quitacao parcial recebida.");

        TemplateMatchingService.ResultadoMatch resultado = service.identificarServico(tokensDoc);

        assertThat(resultado.status()).isEqualTo(StatusProcessamento.NO_MATCH);
        assertThat(resultado.diagnostico()).hasSize(1);
        assertThat(resultado.diagnostico().get(0).termosBaixaFaltantes()).containsExactly("integral");
    }

    @Test
    void naoDeveIdentificarServicoQuandoTermoDeNegacaoEstaPresente() {
        when(servicoRepository.findByAtivoTrue()).thenReturn(List.of(servicoBoleto));
        when(campoLayoutRepository.findByServicoId(1L)).thenReturn(List.of(
                campo(TipoCampo.TERMO_BAIXA, "quitacao"),
                campo(TipoCampo.TERMO_NEGACAO, "cancelado")
        ));

        List<TokenBruto> tokensDoc = Tokenizer.tokenizar("Recebemos a quitacao porem o pedido foi cancelado");

        TemplateMatchingService.ResultadoMatch resultado = service.identificarServico(tokensDoc);

        assertThat(resultado.status()).isEqualTo(StatusProcessamento.NO_MATCH);
        assertThat(resultado.diagnostico().get(0).termosNegacaoEncontrados()).containsExactly("cancelado");
    }

    @Test
    void deveReconhecerTermoDeNegacaoComVariasPalavrasEmSequencia() {
        when(servicoRepository.findByAtivoTrue()).thenReturn(List.of(servicoBoleto));
        when(campoLayoutRepository.findByServicoId(1L)).thenReturn(List.of(
                campo(TipoCampo.TERMO_BAIXA, "quitacao"),
                campo(TipoCampo.TERMO_NEGACAO, "Receitas Federais")
        ));

        List<TokenBruto> comExpressao = Tokenizer.tokenizar("Quitacao de Receitas Federais");
        List<TokenBruto> palavrasSeparadas = Tokenizer.tokenizar("Quitacao: receitas municipais e taxas federais");

        assertThat(service.identificarServico(comExpressao).status()).isEqualTo(StatusProcessamento.NO_MATCH);
        assertThat(service.identificarServico(palavrasSeparadas).status()).isEqualTo(StatusProcessamento.MATCH);
    }

    @Test
    void termoUnidoPorUnderlineExigePalavrasEmSequenciaEPalavrasSoltasPodemEstarEmQualquerLugar() {
        when(servicoRepository.findByAtivoTrue()).thenReturn(List.of(servicoBoleto));
        when(campoLayoutRepository.findByServicoId(1L)).thenReturn(List.of(
                campo(TipoCampo.TERMO_BAIXA, "comprovante"),
                campo(TipoCampo.TERMO_NEGACAO, "Pagamento_de_boleto")
        ));

        List<TokenBruto> emSequencia = Tokenizer.tokenizar("Comprovante: pagamento de boleto efetuado");
        List<TokenBruto> espalhadas = Tokenizer.tokenizar("Comprovante de pagamento via PIX; boleto cancelado");

        assertThat(service.identificarServico(emSequencia).status()).isEqualTo(StatusProcessamento.NO_MATCH);
        assertThat(service.identificarServico(espalhadas).status()).isEqualTo(StatusProcessamento.MATCH);
    }

    @Test
    void deveIgnorarPontuacaoNasPontasAoCompararTermos() {
        when(servicoRepository.findByAtivoTrue()).thenReturn(List.of(servicoBoleto));
        when(campoLayoutRepository.findByServicoId(1L)).thenReturn(List.of(
                campo(TipoCampo.TERMO_BAIXA, "Nacional,"),
                campo(TipoCampo.TERMO_NEGACAO, "Sicalc")
        ));

        List<TokenBruto> tokensDoc = Tokenizer.tokenizar("Simples Nacional. Emitido pelo (Sicalc).");

        TemplateMatchingService.ResultadoMatch resultado = service.identificarServico(tokensDoc);

        assertThat(resultado.diagnostico().get(0).termosBaixaFaltantes()).isEmpty();
        assertThat(resultado.diagnostico().get(0).termosNegacaoEncontrados()).containsExactly("Sicalc");
    }

    @Test
    void deveRetornarAmbiguoQuandoMaisDeUmServicoElegivel() {
        Servico outroServico = new Servico();
        outroServico.setId(2L);
        outroServico.setNome("Outro Servico");

        when(servicoRepository.findByAtivoTrue()).thenReturn(List.of(servicoBoleto, outroServico));
        when(campoLayoutRepository.findByServicoId(1L)).thenReturn(List.of(campo(TipoCampo.TERMO_BAIXA, "quitacao")));
        when(campoLayoutRepository.findByServicoId(2L)).thenReturn(List.of(campo(TipoCampo.TERMO_BAIXA, "quitacao")));

        List<TokenBruto> tokensDoc = Tokenizer.tokenizar("Quitacao efetuada com sucesso.");

        TemplateMatchingService.ResultadoMatch resultado = service.identificarServico(tokensDoc);

        assertThat(resultado.status()).isEqualTo(StatusProcessamento.AMBIGUO);
        assertThat(resultado.servicoIdentificado()).isNull();
    }
}
