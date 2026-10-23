package com.filereader.backend.repository;

import com.filereader.backend.domain.DocumentoBase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DocumentoBaseRepository extends JpaRepository<DocumentoBase, Long> {

    Optional<DocumentoBase> findByServicoId(Long servicoId);
}
