package com.filereader.backend.service;

import com.filereader.backend.domain.CampoLayout;
import com.filereader.backend.domain.TipoCampo;
import com.filereader.backend.parser.TokenBruto;
import com.filereader.backend.parser.Tokenizer;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AnchorMatchingServiceTest {

    private final AnchorMatchingService service = new AnchorMatchingService(0.7);

    /** Simula a marcacao feita na tela: escolhe as ancoras no texto do modelo a partir do token marcado. */
    private CampoLayout marcar(String textoModelo, String valorMarcado) {
        List<TokenBruto> tokens = Tokenizer.tokenizar(textoModelo);
        Map<Integer, String> textoPorIndice = new HashMap<>();
        tokens.forEach(t -> textoPorIndice.put(t.getIndice(), t.getTexto()));
        int indice = tokens.stream().filter(t -> t.getTexto().equals(valorMarcado)).findFirst().orElseThrow().getIndice();

        CampoLayout campo = new CampoLayout();
        campo.setTipo(TipoCampo.CNPJ);
        campo.setTokenIndice(indice);
        campo.setTokenTexto(valorMarcado);
        AnchorMatchingService.selecionarAncoras(textoPorIndice, indice, 3, 3, 10).forEach(campo::adicionarAncora);
        return campo;
    }

    @Test
    void deveExtrairValorQuandoAncorasBatemExatamente() {
        CampoLayout campo = marcar("Empresa ACME CNPJ: 12.345.678/0001-90 Competencia: Janeiro/2026", "12.345.678/0001-90");

        var resultado = service.extrair(campo, Tokenizer.tokenizar("Empresa XPTO CNPJ: 98.765.432/0001-10 Competencia: Fevereiro/2026"));

        assertThat(resultado.valor()).isEqualTo("98.765.432/0001-10");
        assertThat(resultado.ambiguo()).isFalse();
    }

    @Test
    void naoDeveExtrairQuandoContextoNaoApareceNoDocumento() {
        CampoLayout campo = marcar("Empresa ACME CNPJ: 12.345.678/0001-90 Competencia: Janeiro/2026", "12.345.678/0001-90");

        var resultado = service.extrair(campo, Tokenizer.tokenizar("Documento 98.765.432/0001-10 sem nenhuma referencia relevante"));

        assertThat(resultado.valor()).isNull();
    }

    @Test
    void deveExtrairQuandoApenasUmLadoBateEOutroEDadoVariavel() {
        // Depois do CNPJ vem a razao social, que muda de documento para documento.
        CampoLayout campo = marcar("de Receitas Federais 11.222.333/0001-81 METALURGICA VALE DO ITAJAI LTDA Periodo",
                "11.222.333/0001-81");

        var resultado = service.extrair(campo, Tokenizer.tokenizar(
                "de Receitas Federais 45.987.123/0001-04 ALFA COMERCIO DE PRODUTOS ALIMENTICIOS EIRELI Periodo"));

        assertThat(resultado.valor()).isEqualTo("45.987.123/0001-04");
    }

    @Test
    void deveIgnorarDadosVariaveisAoEscolherAncoras() {
        // Logo depois da competencia vem a data de vencimento e o numero do documento.
        CampoLayout campo = marcar("Numero do Documento Maio/2026 22/06/2026 07.20.26142.3301948-2 Pagar este documento",
                "Maio/2026");

        assertThat(campo.getAncoras()).extracting("posicaoRelativa", "texto").containsExactlyInAnyOrder(
                org.assertj.core.groups.Tuple.tuple(-1, "Documento"),
                org.assertj.core.groups.Tuple.tuple(-2, "do"),
                org.assertj.core.groups.Tuple.tuple(-3, "Numero"),
                org.assertj.core.groups.Tuple.tuple(3, "Pagar"),
                org.assertj.core.groups.Tuple.tuple(4, "este"),
                org.assertj.core.groups.Tuple.tuple(5, "documento"));

        var resultado = service.extrair(campo, Tokenizer.tokenizar(
                "Numero do Documento Abril/2026 20/05/2026 07.20.26117.8812345-0 Pagar este documento"));

        assertThat(resultado.valor()).isEqualTo("Abril/2026");
        assertThat(resultado.score()).isEqualByComparingTo("1.000");
    }

    @Test
    void deveConsiderarApenasCandidatosComOMesmoFormatoDoValorMarcado() {
        CampoLayout campo = marcar("CNPJ: 12.345.678/0001-90 Fim", "12.345.678/0001-90");

        // O primeiro "CNPJ:" e seguido de texto sem formato de CNPJ; so o segundo e candidato.
        var resultado = service.extrair(campo, Tokenizer.tokenizar("CNPJ: nao-informado Fim ... CNPJ: 98.765.432/0001-10 Fim"));

        assertThat(resultado.valor()).isEqualTo("98.765.432/0001-10");
        assertThat(resultado.ambiguo()).isFalse();
    }

    @Test
    void deveRetornarAmbiguoQuandoDoisValoresDiferentesTemOMesmoContexto() {
        CampoLayout campo = marcar("CNPJ: 12.345.678/0001-90 Fim", "12.345.678/0001-90");

        var resultado = service.extrair(campo, Tokenizer.tokenizar("CNPJ: 11.111.111/0001-11 Fim CNPJ: 22.222.222/0001-22 Fim"));

        assertThat(resultado.ambiguo()).isTrue();
        assertThat(resultado.valor()).isNull();
    }

    @Test
    void naoDeveSerAmbiguoQuandoOMesmoValorApareceComOMesmoContexto() {
        CampoLayout campo = marcar("CNPJ: 12.345.678/0001-90 Fim", "12.345.678/0001-90");

        var resultado = service.extrair(campo, Tokenizer.tokenizar("CNPJ: 98.765.432/0001-10 Fim CNPJ: 98.765.432/0001-10 Fim"));

        assertThat(resultado.valor()).isEqualTo("98.765.432/0001-10");
        assertThat(resultado.ambiguo()).isFalse();
    }

    @Test
    void formatoDeveAgruparLetrasEDigitos() {
        assertThat(AnchorMatchingService.formato("12.345.678/0001-90")).isEqualTo("9.9.9/9-9");
        assertThat(AnchorMatchingService.formato("Maio/2026")).isEqualTo(AnchorMatchingService.formato("Março/2026"));
        assertThat(AnchorMatchingService.formato("30/06/2026")).isEqualTo("9/9/9");
        assertThat(AnchorMatchingService.formato("18.427,35")).isNotEqualTo(AnchorMatchingService.formato("30/06/2026"));
    }
}
