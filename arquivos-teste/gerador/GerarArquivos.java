import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Gera os 4 arquivos de teste (A, B, C, D).
 *
 * A e B: DARF (layout do Sicalc Web). A ordem em que o texto e desenhado reproduz a ordem de extracao
 * de um DARF real (valores antes dos rotulos, rotulos do topo desenhados no meio do fluxo etc.).
 * C e D: DAS do Simples Nacional. Desenhado em ordem visual, com rotulos acima dos valores na pagina 1
 * e um extrato em formato "Rotulo: valor" na pagina 2.
 *
 * Uso: java -cp <pdfbox, pdfbox-io, fontbox, commons-logging> GerarArquivos.java <pasta-destino>
 */
public class GerarArquivos {

    record LinhaComposicao(String codigo, String denominacao, String principal, String multa, String juros, String total) {
    }

    record Darf(String arquivo, String cnpj, String razaoSocial, String periodoApuracao, String vencimento,
                String numeroDocumento, String codigoReceita, String denominacao, String trimestre, String quota,
                String principal, String multa, String juros, String total, String emissao, String hora,
                String[] codigoBarras, String observacao) {
    }

    record Das(String arquivo, String cnpj, String razaoSocial, String periodoExtenso, String periodoNumerico,
               String vencimento, String numeroDocumento, String total, List<LinhaComposicao> composicao,
               String totalPrincipal, String totalMulta, String totalJuros, String observacao,
               String receitaBrutaPa, String rbt12, String atividade, String aliquota, String emissao,
               String hora, String[] codigoBarras) {
    }

    private static final PDType1Font NORMAL = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDType1Font NEGRITO = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    public static void main(String[] args) throws IOException {
        File destino = new File(args.length > 0 ? args[0] : ".");
        destino.mkdirs();

        Darf a = new Darf("A_DARF_modelo.pdf",
                cnpj("11222333"), "METALURGICA VALE DO ITAJAI LTDA",
                "30/06/2026", "31/07/2026", "07.01.26188.5843064-4",
                "2089", "IRPJ - LUCRO PRESUMIDO", "2º Trimestre/2026", "1ª",
                "18.427,35", "", "", "18.427,35", "07/07/2026", "09:39:06",
                new String[]{"85810000184", "4", "27350385262", "3", "12070126188", "0", "58430644920", "2"},
                "Darf emitido pelo Sicalc Web");

        Darf b = new Darf("B_DARF_teste.pdf",
                cnpj("45987123"), "ALFA COMERCIO DE PRODUTOS ALIMENTICIOS E BEBIDAS EIRELI",
                "31/03/2026", "30/06/2026", "07.01.26151.0098127-7",
                "2089", "IRPJ - LUCRO PRESUMIDO", "1º Trimestre/2026", "3ª",
                "4.902,18", "980,43", "147,07", "6.029,68", "22/06/2026", "16:05:44",
                new String[]{"85830000060", "1", "29680385262", "9", "12070126151", "4", "00981277391", "6"},
                "Darf emitido pelo Sicalc Web");

        Das c = new Das("C_DAS_modelo.pdf",
                cnpj("23456789"), "PADARIA PAO DOURADO LTDA",
                "Maio/2026", "05/2026", "22/06/2026", "07.20.26142.3301948-2", "6.441,42",
                List.of(
                        new LinhaComposicao("1001", "IRPJ - SIMPLES NACIONAL", "354,28", "-", "-", "354,28"),
                        new LinhaComposicao("1002", "CSLL - SIMPLES NACIONAL", "225,45", "-", "-", "225,45"),
                        new LinhaComposicao("1004", "COFINS - SIMPLES NACIONAL", "819,22", "-", "-", "819,22"),
                        new LinhaComposicao("1005", "PIS - SIMPLES NACIONAL", "177,53", "-", "-", "177,53"),
                        new LinhaComposicao("1006", "INSS - SIMPLES NACIONAL", "2.680,63", "-", "-", "2.680,63"),
                        new LinhaComposicao("1007", "ICMS - SIMPLES NACIONAL", "2.184,31", "-", "-", "2.184,31")),
                "6.441,42", "0,00", "0,00",
                "Receita bruta do período informada no PGDAS-D.",
                "85.430,12", "912.345,67", "Revenda de mercadorias, exceto para o exterior - Anexo I",
                "7,54%", "12/06/2026", "14:22:51",
                new String[]{"85800000064", "4", "41420328262", "7", "20261423301", "6", "94820000001", "3"});

        Das d = new Das("D_DAS_teste.pdf",
                cnpj("38111222"), "TECH SOLUCOES EM SOFTWARE E CONSULTORIA LTDA",
                "Abril/2026", "04/2026", "20/05/2026", "07.20.26117.8812345-0", "9.584,67",
                List.of(
                        new LinhaComposicao("1001", "IRPJ - SIMPLES NACIONAL", "321,05", "64,21", "9,63", "394,89"),
                        new LinhaComposicao("1002", "CSLL - SIMPLES NACIONAL", "274,98", "55,00", "8,25", "338,23"),
                        new LinhaComposicao("1004", "COFINS - SIMPLES NACIONAL", "1.014,62", "202,92", "30,44", "1.247,98"),
                        new LinhaComposicao("1005", "PIS - SIMPLES NACIONAL", "220,13", "44,03", "6,60", "270,76"),
                        new LinhaComposicao("1006", "INSS - SIMPLES NACIONAL", "3.303,28", "660,66", "99,10", "4.063,04"),
                        new LinhaComposicao("1010", "ISS - SIMPLES NACIONAL", "2.658,35", "531,67", "79,75", "3.269,77"),
                        new LinhaComposicao("1011", "ISS - SIMPLES NACIONAL - MUNICIPIO DIVERSO", "-", "-", "-", "-")),
                "7.792,41", "1.558,49", "233,77",
                "Documento emitido após o vencimento. Multa e juros calculados até a data de pagamento.",
                "131.904,55", "1.402.877,10", "Prestação de serviços de tecnologia da informação - Anexo III",
                "5,91%", "15/05/2026", "08:47:13",
                new String[]{"85870000095", "8", "83110328262", "0", "20261178812", "1", "34500000002", "5"});

        gerarDarf(a, destino);
        gerarDarf(b, destino);
        gerarDas(c, destino);
        gerarDas(d, destino);

        for (Object o : List.of(a, b, c, d)) {
            System.out.println(o);
        }
    }

    // ---------------------------------------------------------------- DARF

    private static void gerarDarf(Darf v, File destino) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage pagina = new PDPage(PDRectangle.A4);
            doc.addPage(pagina);
            try (PDPageContentStream cs = new PDPageContentStream(doc, pagina)) {
                // Molduras (nao geram texto)
                caixa(cs, 30, 600, 535, 220);
                linha(cs, 30, 760, 565, 760);
                linha(cs, 30, 725, 565, 725);
                linha(cs, 30, 690, 565, 690);
                linha(cs, 30, 655, 565, 655);
                linha(cs, 320, 600, 320, 725);
                caixa(cs, 30, 470, 535, 120);
                caixa(cs, 30, 250, 535, 150);

                // A ordem abaixo reproduz a ordem de extracao de um DARF real do Sicalc.
                texto(cs, NEGRITO, 13, 40, 800, "Documento de Arrecadação");
                texto(cs, NEGRITO, 13, 40, 784, "de Receitas Federais");
                texto(cs, NORMAL, 10, 40, 735, v.cnpj());
                texto(cs, NORMAL, 10, 170, 735, v.razaoSocial());
                texto(cs, NORMAL, 7, 40, 715, "Período de Apuração");
                texto(cs, NORMAL, 7, 170, 715, "Data de Vencimento");
                texto(cs, NORMAL, 7, 330, 715, "Número do Documento");
                texto(cs, NORMAL, 10, 330, 700, v.numeroDocumento());
                texto(cs, NORMAL, 7, 40, 680, "Pagar este documento até");
                texto(cs, NEGRITO, 10, 40, 665, v.vencimento());
                texto(cs, NORMAL, 7, 40, 645, "Observações");
                texto(cs, NORMAL, 8, 40, 630, v.observacao());
                texto(cs, NORMAL, 7, 330, 645, "Valor Total do Documento");
                texto(cs, NEGRITO, 12, 330, 625, v.total());
                texto(cs, NORMAL, 7, 40, 750, "CNPJ");
                texto(cs, NORMAL, 7, 170, 750, "Razão Social");
                texto(cs, NORMAL, 10, 40, 700, v.periodoApuracao());
                texto(cs, NORMAL, 10, 170, 700, v.vencimento());

                texto(cs, NORMAL, 7, 40, 562, "Código");
                texto(cs, NORMAL, 7, 330, 562, "Principal");
                texto(cs, NORMAL, 7, 90, 562, "Denominação");
                texto(cs, NORMAL, 7, 520, 562, "Total");
                texto(cs, NORMAL, 7, 395, 562, "Multa");
                texto(cs, NORMAL, 7, 460, 562, "Juros");
                texto(cs, NEGRITO, 9, 40, 578, "Composição do Documento de Arrecadação");
                texto(cs, NORMAL, 8, 40, 545, v.codigoReceita());
                texto(cs, NORMAL, 8, 90, 545, v.denominacao());
                texto(cs, NORMAL, 8, 330, 545, v.principal());
                if (!v.multa().isEmpty()) {
                    texto(cs, NORMAL, 8, 395, 545, v.multa());
                    texto(cs, NORMAL, 8, 460, 545, v.juros());
                }
                texto(cs, NORMAL, 8, 520, 545, v.total());
                texto(cs, NORMAL, 8, 40, 532, "01");
                texto(cs, NORMAL, 8, 90, 532, v.denominacao());
                texto(cs, NORMAL, 8, 90, 520, "PA " + v.trimestre() + " " + v.quota() + " Quota Vencimento " + v.vencimento());
                texto(cs, NEGRITO, 8, 90, 500, "Totais");
                texto(cs, NEGRITO, 8, 330, 500, v.principal());
                if (!v.multa().isEmpty()) {
                    texto(cs, NEGRITO, 8, 395, 500, v.multa());
                    texto(cs, NEGRITO, 8, 460, 500, v.juros());
                }
                texto(cs, NEGRITO, 8, 520, 500, v.total());
                texto(cs, NORMAL, 6, 40, 478, "SENDA (Versão:1.5.4) " + v.emissao() + " " + v.hora());
                texto(cs, NORMAL, 6, 520, 478, "Página: 1 / 2");

                String[] cb = v.codigoBarras();
                texto(cs, NORMAL, 9, 40, 440, cb[0] + " " + cb[1] + " " + cb[2] + " " + cb[3] + " " + cb[4] + " "
                        + cb[5] + " " + cb[6] + " " + cb[7]);
                texto(cs, NORMAL, 7, 430, 420, "AUTENTICAÇÃO MECÂNICA");

                texto(cs, NEGRITO, 9, 40, 385, "Documento de Arrecadação de Receitas Federais");
                texto(cs, NORMAL, 8, 40, 360, cb[0] + " " + cb[1] + " " + cb[2] + " " + cb[3] + " " + cb[4] + " " + cb[5]);
                texto(cs, NORMAL, 8, 330, 360, v.cnpj());
                texto(cs, NORMAL, 8, 330, 345, "Número: " + v.numeroDocumento());
                texto(cs, NORMAL, 8, 330, 330, "Pagar até: " + v.vencimento());
                texto(cs, NORMAL, 8, 330, 315, "Valor: " + v.total());
                texto(cs, NORMAL, 8, 40, 345, cb[6] + " " + cb[7]);
                texto(cs, NORMAL, 7, 290, 360, "CNPJ:");
                texto(cs, NEGRITO, 9, 40, 270, "Pague com o PIX");
                texto(cs, NORMAL, 7, 40, 258, "Leia o QR Code no aplicativo do seu banco.");
            }

            PDPage instrucoes = new PDPage(PDRectangle.A4);
            doc.addPage(instrucoes);
            try (PDPageContentStream cs = new PDPageContentStream(doc, instrucoes)) {
                int y = 800;
                texto(cs, NEGRITO, 12, 40, y, "Instruções para pagamento do DARF");
                y -= 25;
                for (String l : List.of(
                        "1. O pagamento pode ser efetuado em qualquer agência bancária, nos canais digitais dos bancos",
                        "   conveniados à Receita Federal ou por meio do PIX, até a data de vencimento indicada.",
                        "2. Após o vencimento, utilize o Sicalc Web para recalcular os acréscimos legais (multa e juros)",
                        "   e emitir um novo documento. Não rasure este documento.",
                        "3. Este documento não se destina ao recolhimento de tributos apurados no Simples Nacional,",
                        "   que devem ser pagos exclusivamente por meio do DAS gerado no portal do Simples Nacional.",
                        "4. Guarde o comprovante de pagamento pelo prazo de 5 (cinco) anos.",
                        "5. Em caso de dúvidas sobre o código de receita, consulte a tabela de códigos de arrecadação",
                        "   disponível no sítio da Receita Federal do Brasil.")) {
                    texto(cs, NORMAL, 9, 40, y, l);
                    y -= 15;
                }
                texto(cs, NORMAL, 6, 40, 40, "SENDA (Versão:1.5.4) " + v.emissao() + " " + v.hora());
                texto(cs, NORMAL, 6, 520, 40, "Página: 2 / 2");
            }
            doc.save(new File(destino, v.arquivo()));
        }
    }

    // ---------------------------------------------------------------- DAS

    private static void gerarDas(Das v, File destino) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage pagina = new PDPage(PDRectangle.A4);
            doc.addPage(pagina);
            try (PDPageContentStream cs = new PDPageContentStream(doc, pagina)) {
                caixa(cs, 30, 610, 535, 210);
                linha(cs, 30, 765, 565, 765);
                linha(cs, 30, 730, 565, 730);
                linha(cs, 30, 695, 565, 695);
                linha(cs, 30, 655, 565, 655);

                texto(cs, NEGRITO, 13, 40, 800, "Documento de Arrecadação");
                texto(cs, NEGRITO, 13, 40, 784, "do Simples Nacional");
                texto(cs, NORMAL, 7, 400, 800, "Simples Nacional");
                texto(cs, NORMAL, 7, 400, 790, "Regime Especial Unificado de Arrecadação");

                texto(cs, NORMAL, 7, 40, 755, "CNPJ");
                texto(cs, NORMAL, 7, 170, 755, "Razão Social");
                texto(cs, NORMAL, 10, 40, 740, v.cnpj());
                texto(cs, NORMAL, 10, 170, 740, v.razaoSocial());

                texto(cs, NORMAL, 7, 40, 720, "Período de Apuração");
                texto(cs, NORMAL, 7, 170, 720, "Data de Vencimento");
                texto(cs, NORMAL, 7, 330, 720, "Número do Documento");
                texto(cs, NORMAL, 10, 40, 705, v.periodoExtenso());
                texto(cs, NORMAL, 10, 170, 705, v.vencimento());
                texto(cs, NORMAL, 10, 330, 705, v.numeroDocumento());

                texto(cs, NORMAL, 7, 40, 685, "Pagar este documento até");
                texto(cs, NEGRITO, 10, 40, 670, v.vencimento());
                texto(cs, NORMAL, 7, 330, 685, "Valor Total do Documento");
                texto(cs, NEGRITO, 12, 330, 668, v.total());

                texto(cs, NORMAL, 7, 40, 645, "Observações");
                texto(cs, NORMAL, 8, 40, 630, v.observacao());

                texto(cs, NEGRITO, 9, 40, 585, "Composição do Documento de Arrecadação");
                texto(cs, NORMAL, 7, 40, 570, "Código");
                texto(cs, NORMAL, 7, 80, 570, "Denominação");
                texto(cs, NORMAL, 7, 330, 570, "Principal");
                texto(cs, NORMAL, 7, 395, 570, "Multa");
                texto(cs, NORMAL, 7, 460, 570, "Juros");
                texto(cs, NORMAL, 7, 520, 570, "Total");
                int y = 555;
                for (LinhaComposicao l : v.composicao()) {
                    texto(cs, NORMAL, 8, 40, y, l.codigo());
                    texto(cs, NORMAL, 8, 80, y, l.denominacao());
                    texto(cs, NORMAL, 8, 330, y, l.principal());
                    texto(cs, NORMAL, 8, 395, y, l.multa());
                    texto(cs, NORMAL, 8, 460, y, l.juros());
                    texto(cs, NORMAL, 8, 520, y, l.total());
                    y -= 13;
                }
                y -= 5;
                texto(cs, NEGRITO, 8, 80, y, "Totais");
                texto(cs, NEGRITO, 8, 330, y, v.totalPrincipal());
                texto(cs, NEGRITO, 8, 395, y, v.totalMulta());
                texto(cs, NEGRITO, 8, 460, y, v.totalJuros());
                texto(cs, NEGRITO, 8, 520, y, v.total());

                String[] cb = v.codigoBarras();
                y -= 40;
                texto(cs, NORMAL, 9, 40, y, String.join(" ", cb));
                texto(cs, NORMAL, 7, 430, y - 18, "AUTENTICAÇÃO MECÂNICA");
                texto(cs, NEGRITO, 9, 40, y - 50, "Pague com o PIX");
                texto(cs, NORMAL, 7, 40, y - 62, "Leia o QR Code no aplicativo do seu banco.");
                texto(cs, NORMAL, 6, 40, 40, "Gerado pelo PGDAS-D em " + v.emissao() + " às " + v.hora());
                texto(cs, NORMAL, 6, 520, 40, "Página: 1 / 2");
            }

            PDPage extrato = new PDPage(PDRectangle.A4);
            doc.addPage(extrato);
            try (PDPageContentStream cs = new PDPageContentStream(doc, extrato)) {
                int y = 800;
                texto(cs, NEGRITO, 12, 40, y, "Extrato do Simples Nacional - Demonstrativo do Período");
                y -= 30;
                for (String l : List.of(
                        "CNPJ Estabelecimento: " + v.cnpj(),
                        "Razão Social: " + v.razaoSocial(),
                        "Período de Apuração (PA): " + v.periodoNumerico(),
                        "Receita Bruta do PA (RPA) - Competência: " + v.receitaBrutaPa(),
                        "Receita bruta acumulada nos 12 meses anteriores ao PA (RBT12): " + v.rbt12(),
                        "Atividade: " + v.atividade(),
                        "Alíquota efetiva: " + v.aliquota(),
                        "Valor do débito declarado: " + v.totalPrincipal())) {
                    texto(cs, NORMAL, 9, 40, y, l);
                    y -= 16;
                }
                y -= 14;
                texto(cs, NEGRITO, 10, 40, y, "Informações importantes");
                y -= 18;
                for (String l : List.of(
                        "- Débitos não incluídos no Simples Nacional devem ser recolhidos em DARF próprio, emitido pelo Sicalc.",
                        "- O pagamento após o vencimento sujeita o contribuinte a multa de mora de 0,33% ao dia,",
                        "  limitada a 20%, e juros equivalentes à taxa Selic.",
                        "- A falta de pagamento pode resultar na exclusão do regime e inscrição em Dívida Ativa da União.")) {
                    texto(cs, NORMAL, 8, 40, y, l);
                    y -= 14;
                }
                texto(cs, NORMAL, 6, 40, 40, "Gerado pelo PGDAS-D em " + v.emissao() + " às " + v.hora());
                texto(cs, NORMAL, 6, 520, 40, "Página: 2 / 2");
            }
            doc.save(new File(destino, v.arquivo()));
        }
    }

    // ---------------------------------------------------------------- utilitarios

    private static void texto(PDPageContentStream cs, PDType1Font fonte, float tamanho, float x, float y, String texto)
            throws IOException {
        cs.beginText();
        cs.setFont(fonte, tamanho);
        cs.newLineAtOffset(x, y);
        cs.showText(texto);
        cs.endText();
    }

    private static void caixa(PDPageContentStream cs, float x, float y, float largura, float altura) throws IOException {
        cs.setLineWidth(0.6f);
        cs.addRect(x, y, largura, altura);
        cs.stroke();
    }

    private static void linha(PDPageContentStream cs, float x1, float y1, float x2, float y2) throws IOException {
        cs.setLineWidth(0.4f);
        cs.moveTo(x1, y1);
        cs.lineTo(x2, y2);
        cs.stroke();
    }

    /** Monta um CNPJ (matriz 0001) com digitos verificadores validos a partir dos 8 digitos da raiz. */
    private static String cnpj(String raiz) {
        String base = raiz + "0001";
        int dv1 = dv(base, new int[]{5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});
        int dv2 = dv(base + dv1, new int[]{6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});
        String n = base + dv1 + dv2;
        return n.substring(0, 2) + "." + n.substring(2, 5) + "." + n.substring(5, 8) + "/" + n.substring(8, 12) + "-" + n.substring(12);
    }

    private static int dv(String numeros, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            soma += (numeros.charAt(i) - '0') * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
