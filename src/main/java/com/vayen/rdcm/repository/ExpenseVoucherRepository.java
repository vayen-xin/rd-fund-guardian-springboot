package com.vayen.rdcm.repository;

import com.vayen.rdcm.entity.ExpenseVoucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseVoucherRepository extends JpaRepository<ExpenseVoucher, Long> {
    
    List<ExpenseVoucher> findBySettlementId(Long settlementId);
    
    List<ExpenseVoucher> findByExpenseType(String expenseType);
}
