package org.example.repository;

import org.example.entity.DailyRate;
import org.example.entity.Jewelry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DailyRateRepository extends JpaRepository<DailyRate, Long> {
    Optional<DailyRate> findByMetalTypeAndRateDate(Jewelry.MetalType metalType, LocalDate rateDate);
}
