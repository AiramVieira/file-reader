package com.filereader.backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ancora_token")
@Getter
@Setter
@NoArgsConstructor
public class AncoraToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campo_layout_id", nullable = false)
    private CampoLayout campoLayout;

    /** Deslocamento em relacao ao token do campo: -1, -2, -3 (antes) ou +1, +2, +3 (depois). */
    @Column(name = "posicao_relativa", nullable = false)
    private int posicaoRelativa;

    @Column(nullable = false, length = 1000)
    private String texto;

    public AncoraToken(int posicaoRelativa, String texto) {
        this.posicaoRelativa = posicaoRelativa;
        this.texto = texto;
    }
}
