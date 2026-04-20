package com.shopflow.customerservice.service;

import com.shopflow.customerservice.dto.CustomerRequest;
import com.shopflow.customerservice.dto.CustomerResponse;
import com.shopflow.customerservice.exception.DuplicateResourceException;
import com.shopflow.customerservice.exception.ResourceNotFoundException;
import com.shopflow.customerservice.model.Customer;
import com.shopflow.customerservice.repository.CustomerRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor

public class CustomerService {
    private final CustomerRepository customerRepository;

    public CustomerResponse createCustomer(@Valid CustomerRequest req) {
        if (customerRepository.existsByEmail(req.getEmail())) {
            throw new DuplicateResourceException("Bu email zaten kayıtlı: " + req.getEmail());
        }
        Customer customer = new Customer();
        customer.setName(req.getName());
        customer.setEmail(req.getEmail());
        customer.setPhone(req.getPhone());
        Customer saved = customerRepository.save(customer);
        return toResponse(saved);

    }
    public List<CustomerResponse> getAllCustomers() {
       return customerRepository.findAll().stream()
               .map(this::toResponse)
               .collect(Collectors.toList());
    }

    public CustomerResponse getCustomerById(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Müşteri bulunamadı: " + id));
        return toResponse(customer);
    }

    private CustomerResponse toResponse(Customer c) {
        return new CustomerResponse(
                c.getId(),
                c.getName(),
                c.getEmail(),
                c.getPhone()
        );
    }
}
