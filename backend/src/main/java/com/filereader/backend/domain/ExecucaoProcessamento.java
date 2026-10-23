package com.filereader.backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "execucao_processamento")
@Getter
@Setter
@NoArgsConstructor
public class ExecucaoProcessamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servico_id")
    private Servico servico;

    @Column(name = "nome_arquivo_original", nullable = false)
    private String nomeArquivoOriginal;

    @Column(name = "caminho_armazenamento", nullable = false)
    private String caminhoArmazenamento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusProcessamento status;

    @Column(name = "competencia_extraida")
    private String competenciaExtraida;

    @Column(name = "cnpj_extraido")
    private String cnpjExtraido;

    @Column(name = "score_extracao_competencia", precision = 4, scale = 3)
    private BigDecimal scoreExtracaoCompetencia;

    @Column(name = "score_extracao_cnpj", precision = 4, scale = 3)
    private BigDecimal scoreExtracaoCnpj;

    /** JSON serializado com diagnostico por servico avaliado (termos faltantes, termos de negacao encontrados, candidatos ambiguos). */
    @Column(columnDefinition = "TEXT")
    private String diagnostico;

    @Column(name = "processado_em", nullable = false, updatable = false)
    private Instant processadoEm;

    @PrePersist
    void aoCriar() {
        processadoEm = Instant.now();
    }
}
