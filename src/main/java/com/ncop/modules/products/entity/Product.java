package com.ncop.modules.products.entity;

import com.ncop.modules.products.enums.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "products")
public class Product {

    @Id
    private String id;

    private String productCode; // Always equals MongoDB document _id

    private String brandName; // e.g. "Nourish-Paraxil"

    private String category; // e.g. "Analgesics & Antipyretics", "Antibiotics"

    private String therapeuticClass; // e.g. "NSAID", "Cephalosporin"

    private String dosageForm; // Level 1 (e.g. "Syrup")

    private String dosageVariant; // Level 2 (e.g. "Sugar Free Syrup")

    private List<ProductIngredient> ingredients = new ArrayList<>();

    private String composition; // Auto-computed formula: "Paracetamol 500mg + Diclofenac 50mg BP Sugar Free Syrup"

    private String packaging; // e.g. "10x10 Alu-Alu Blister", "100ml Amber Glass Bottle"

    private Long moq; // Minimum order quantity e.g. 5000

    private Double unitPrice; // e.g. 1.25

    private String currency = "USD"; // USD, EUR, GBP, INR

    private String shelfLife; // e.g. "24 Months", "36 Months"

    private String storageCondition; // e.g. "Store below 25°C in a dry place. Protect from light."

    private String description;

    private ProductStatus status = ProductStatus.ACTIVE;

    private List<ProductDocument> documents = new ArrayList<>();

    @CreatedDate
    private Instant createdOn = Instant.now();

    @LastModifiedDate
    private Instant lastUpdatedOn = Instant.now();
}
