package com.filereader.backend.repository;

import com.filereader.backend.domain.Token;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TokenRepository extends JpaRepository<Token, Long> {

    List<Token> findByDocumentoBaseIdOrderByIndiceAsc(Long documentoBaseId);
}
