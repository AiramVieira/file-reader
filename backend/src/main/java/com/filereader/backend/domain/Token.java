package com.filereader.backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "token")
@Getter
@Setter
@NoArgsConstructor
public class Token {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documento_base_id", nullable = false)
    private DocumentoBase documentoBase;

    @Column(nullable = false)
    private int indice;

    @Column(nullable = false, length = 1000)
    private String texto;

    /** Fragmento de espaco/quebra de linha que segue o token, usado para reconstruir o preview visual. */
    @Column(name = "espaco_apos", nullable = false, length = 50)
    private String espacoApos = " ";

    private Integer pagina;
    private Integer linha;
    private Integer coluna;

    public Token(int indice, String texto, String espacoApos) {
        this.indice = indice;
        this.texto = texto;
        this.espacoApos = espacoApos;
    }
}
