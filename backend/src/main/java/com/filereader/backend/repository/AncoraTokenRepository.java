package com.filereader.backend.repository;

import com.filereader.backend.domain.AncoraToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AncoraTokenRepository extends JpaRepository<AncoraToken, Long> {
}
