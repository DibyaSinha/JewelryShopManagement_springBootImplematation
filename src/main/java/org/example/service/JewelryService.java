package org.example.service;

import org.example.entity.Jewelry;
import org.example.repository.JewelryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class JewelryService {
    private static final Logger logger = LoggerFactory.getLogger(JewelryService.class);

    @Autowired
    private JewelryRepository jewelryRepository;

    public List<Jewelry> getAllJewelry() {
        return jewelryRepository.findAll();
    }

    public Jewelry getJewelryById(Long id) {
        return jewelryRepository.findById(id).orElse(null);
    }

    @Transactional
    public Jewelry addJewelry(Jewelry jewelry) {
        if (jewelry.getVersion() == null) {
            jewelry.setVersion(0L);
        }
        Jewelry saved = jewelryRepository.save(jewelry);
        logger.info("Jewelry design saved in catalog: '{}' (ID: {}, Stock: {})", saved.getName(), saved.getId(), saved.getStock());
        return saved;
    }

    @Transactional
    public Jewelry updateJewelry(Long id, Jewelry jewelryDetails) {
        Jewelry jewelry = jewelryRepository.findById(id).orElse(null);
        if (jewelry != null) {
            if (jewelry.getVersion() == null) {
                jewelry.setVersion(0L);
            }
            jewelry.setName(jewelryDetails.getName());
            jewelry.setCompanyName(jewelryDetails.getCompanyName());
            jewelry.setType(jewelryDetails.getType());
            jewelry.setWeight(jewelryDetails.getWeight());
            jewelry.setStock(jewelryDetails.getStock());
            jewelry.setMakingPercent(jewelryDetails.getMakingPercent());
            Jewelry saved = jewelryRepository.save(jewelry);
            logger.info("Jewelry design details updated: ID {}", saved.getId());
            return saved;
        }
        return null;
    }

    @Transactional
    public void deleteJewelry(Long id) {
        jewelryRepository.deleteById(id);
        logger.info("Jewelry design removed from catalog: ID {}", id);
    }

    @Transactional
    public Jewelry addStock(Long id, Integer quantity) {
        Jewelry jewelry = jewelryRepository.findById(id).orElse(null);
        if (jewelry != null) {
            if (jewelry.getVersion() == null) {
                jewelry.setVersion(0L);
            }
            jewelry.setStock(jewelry.getStock() + quantity);
            Jewelry saved = jewelryRepository.save(jewelry);
            logger.info("Stock updated: Added {} units to '{}' (ID: {}). Total stock: {}", quantity, saved.getName(), saved.getId(), saved.getStock());
            return saved;
        }
        return null;
    }
}
