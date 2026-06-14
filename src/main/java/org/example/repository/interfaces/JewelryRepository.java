package org.example.repository.interfaces;

import org.example.model.Jewelry;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface JewelryRepository {
    void save(Jewelry jewelry);
    void updateStock(long id, int newStock, Connection conn);
    Optional<Jewelry> findById(long id);
    List<Jewelry> findAll();
}
