package org.example.repository;

import org.example.entity.Jewelry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JewelryRepository extends JpaRepository<Jewelry, Long> {
    List<Jewelry> findByType(Jewelry.MetalType type);
}
