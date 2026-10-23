package com.filereader.backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "campo_layout")
@Getter
@Setter
@NoArgsConstructor
public class CampoLayout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servico_id", nullable = false)
    private Servico servico;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoCampo tipo;

    /** Indice do token marcado no arquivo-modelo; nulo para termos de negacao, que sao digitados. */
    @Column(name = "token_indice")
    private Integer tokenIndice;

    /** Ultimo token de um termo marcado como intervalo (varias palavras); nulo quando a marcacao e de um unico token. */
    @Column(name = "token_indice_fim")
    private Integer tokenIndiceFim;

    /** Snapshot do texto do token no momento da marcacao (auditoria/exibicao, independe do token original mudar). */
    @Column(name = "token_texto", nullable = false, length = 1000)
    private String tokenTexto;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @OneToMany(mappedBy = "campoLayout", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AncoraToken> ancoras = new ArrayList<>();

    @PrePersist
    void aoCriar() {
        criadoEm = Instant.now();
    }

    public void adicionarAncora(int posicaoRelativa, String texto) {
        AncoraToken ancora = new AncoraToken(posicaoRelativa, texto);
        ancora.setCampoLayout(this);
        this.ancoras.add(ancora);
    }
}
