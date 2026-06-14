package org.example.repository.interfaces;

import org.example.model.DailyRate;
import java.util.List;
import java.util.Optional;

public interface DailyRateRepository {
    void save(DailyRate rate);
    Optional<DailyRate> getTodayRate(String metalType);
    List<DailyRate> findAll();
}
