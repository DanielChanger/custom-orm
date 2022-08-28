package com.example.model;

import com.example.annotation.Column;
import com.example.annotation.Id;
import com.example.annotation.Table;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Table("PRODUCT")
@Setter
@ToString
public class Product {

    @Id
    @Column("ID")
    private Long id;
    @Column("NAME")
    private String name;
    @Column("PRICE")
    private BigDecimal price;
}
