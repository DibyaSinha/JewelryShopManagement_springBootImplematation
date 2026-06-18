package org.example.repository;

import org.example.entity.Bill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {
    
    @Query("SELECT b.seller.username as seller, SUM(b.grandTotal) as total " +
           "FROM Bill b WHERE CAST(b.billDate AS date) = :date GROUP BY b.seller.username")
    List<Object[]> findSalesBySellerForDate(@Param("date") LocalDate date);

    List<Bill> findAllByOrderByBillDateDesc();

    @Query("SELECT SUM(b.grandTotal) FROM Bill b")
    Double getTotalRevenue();

    @Query("SELECT SUM(b.grandTotal) FROM Bill b WHERE b.billDate >= :start AND b.billDate <= :end")
    Double getTotalRevenueBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT FUNCTION('DATE_FORMAT', b.billDate, '%Y-%m') as month, SUM(b.grandTotal) FROM Bill b GROUP BY month ORDER BY month DESC")
    List<Object[]> getMonthlySalesBreakdown();
}
