package org.example.service;

import org.example.entity.Jewelry;
import org.example.repository.JewelryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class JewelryService {
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
        return jewelryRepository.save(jewelry);
    }

    @Transactional
    public Jewelry updateJewelry(Long id, Jewelry jewelryDetails) {
        Jewelry jewelry = jewelryRepository.findById(id).orElse(null);
        if (jewelry != null) {
            jewelry.setName(jewelryDetails.getName());
            jewelry.setCompanyName(jewelryDetails.getCompanyName());
            jewelry.setType(jewelryDetails.getType());
            jewelry.setWeight(jewelryDetails.getWeight());
            jewelry.setStock(jewelryDetails.getStock());
            jewelry.setMakingPercent(jewelryDetails.getMakingPercent());
            return jewelryRepository.save(jewelry);
        }
        return null;
    }

    @Transactional
    public void deleteJewelry(Long id) {
        jewelryRepository.deleteById(id);
    }

    @Transactional
    public Jewelry addStock(Long id, Integer quantity) {
        Jewelry jewelry = jewelryRepository.findById(id).orElse(null);
        if (jewelry != null) {
            jewelry.setStock(jewelry.getStock() + quantity);
            return jewelryRepository.save(jewelry);
        }
        return null;
    }
}
