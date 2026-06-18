package org.example.service;

import org.example.entity.DailyRate;
import org.example.entity.Jewelry;
import org.example.repository.DailyRateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class DailyRateService {
    @Autowired
    private DailyRateRepository dailyRateRepository;

    public List<DailyRate> getAllRates() {
        return dailyRateRepository.findAll();
    }

    public Optional<DailyRate> getTodayRate(Jewelry.MetalType type) {
        return dailyRateRepository.findByMetalTypeAndRateDate(type, LocalDate.now());
    }

    @Transactional
    public DailyRate saveRate(DailyRate rate) {
        if (rate.getRateDate() == null) {
            rate.setRateDate(LocalDate.now());
        }
        Optional<DailyRate> existing = dailyRateRepository.findByMetalTypeAndRateDate(rate.getMetalType(), rate.getRateDate());
        if (existing.isPresent()) {
            DailyRate existingRate = existing.get();
            existingRate.setPricePerGram(rate.getPricePerGram());
            return dailyRateRepository.save(existingRate);
        }
        return dailyRateRepository.save(rate);
    }
}
