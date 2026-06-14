package org.example.service;

import org.example.exception.ProductNotFoundException;
import org.example.model.Jewelry;
import org.example.repository.impl.JewelryRepositoryImpl;
import org.example.repository.interfaces.JewelryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class JewelryService {
    private static final Logger logger = LoggerFactory.getLogger(JewelryService.class);
    private JewelryRepository repo = new JewelryRepositoryImpl();

    public void addJewelry(String name, String companyName, String type, double weight, int stock, double making) {
        Jewelry jewelry = new Jewelry(0, name, companyName, type, weight, stock, making);
        repo.save(jewelry);
        logger.info("New jewelry design added: {}", name);
    }

    public List<Jewelry> getAll() {
        return repo.findAll();
    }

    public Jewelry getById(long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Jewelry not found with ID: " + id));
    }

    public void addStock(long id, int qty) {
        Jewelry j = getById(id);
        int newStock = j.getStock() + qty;
        repo.updateStock(id, newStock, null);
        logger.info("Stock updated for jewelry ID: {}. New stock: {}", id, newStock);
    }
}
