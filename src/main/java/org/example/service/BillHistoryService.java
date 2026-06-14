package org.example.service;

import org.example.model.Bill;
import org.example.repository.impl.BillRepositoryImpl;
import org.example.repository.interfaces.BillRepository;

import java.util.List;

public class BillHistoryService {
    private BillRepository repo = new BillRepositoryImpl();

    public List<Bill> getAllBills() {
        return repo.findAll();
    }

    public Bill getBillById(long billId) {
        return repo.findById(billId).orElse(null);
    }
}
