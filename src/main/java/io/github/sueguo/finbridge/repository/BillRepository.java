package io.github.sueguo.finbridge.repository;

import io.github.sueguo.finbridge.model.entity.Bill;
import io.github.sueguo.finbridge.model.entity.BillStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {
    List<Bill> findByStatus(BillStatus status);
    Optional<Bill> findBySourceBankAndBillMonth(String sourceBank, String billMonth);
    boolean existsBySourceBankAndBillMonth(String sourceBank, String billMonth);
}