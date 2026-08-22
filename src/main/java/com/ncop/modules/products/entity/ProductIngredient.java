package com.ncop.modules.products.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductIngredient {
    private String api;          // e.g. Paracetamol, Diclofenac Potassium
    private String strength;     // e.g. "500", "50", "125"
    private String unit;         // e.g. "mg", "mcg", "g", "IU", "% w/v", "% w/w", "ml"
    private String pharmacopeia; // e.g. "BP", "USP", "IP", "EP", "Ph.Eur", "In-House"
}
