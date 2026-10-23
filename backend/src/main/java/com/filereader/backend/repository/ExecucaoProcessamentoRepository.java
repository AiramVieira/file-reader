package com.filereader.backend.repository;

import com.filereader.backend.domain.ExecucaoProcessamento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExecucaoProcessamentoRepository extends JpaRepository<ExecucaoProcessamento, Long> {
}
