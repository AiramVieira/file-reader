package com.filereader.backend.repository;

import com.filereader.backend.domain.CampoLayout;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CampoLayoutRepository extends JpaRepository<CampoLayout, Long> {

    List<CampoLayout> findByServicoId(Long servicoId);

    void deleteByServicoId(Long servicoId);
}
