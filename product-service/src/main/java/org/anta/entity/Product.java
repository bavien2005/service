package org.anta.entity;

import org.anta.util.JsonListConverter;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String brand;

    private String description;

    @Column(precision = 12, scale = 2)
    private BigDecimal price;

    private Long categoryId;

    @Column(name = "total_stock")
    private Integer totalStock;

    @Convert(converter = JsonListConverter.class)
    @Column(columnDefinition = "json")
    private List<String> images = new ArrayList<>();

    @Column(name = "created_at", updatable = false, insertable = false,
            columnDefinition = "timestamp default current_timestamp")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductVariant> variants = new ArrayList<>();

    @PrePersist @PreUpdate
    public void ensureImages() {
        if (images == null){
            images = new ArrayList<>();
        }
    }
}
