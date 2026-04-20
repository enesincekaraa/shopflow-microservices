package com.shopflow.productservice.model;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Ürün adı boş olamaz")
    private String name;

    private String description;

    @NotNull
    @Positive(message = "Fiyat pozitif olmalı")
    @Column(nullable = false)
    private BigDecimal price;

    @NotNull
    @Column(nullable = false)
    private Integer stock;

    public void decreaseStock(int quantity) {
        if (this.stock<quantity){
            throw new IllegalStateException("Yetersiz stok");
        }
        this.stock -= quantity;
    }

}
