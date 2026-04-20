package com.shopflow.customerservice.repository;

import com.shopflow.customerservice.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByEmail(String email);  // email ile müşteri bul
    boolean existsByEmail(String email);           // email kayıtlı mı?
}