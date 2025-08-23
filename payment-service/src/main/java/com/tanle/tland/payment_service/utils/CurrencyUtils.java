package com.tanle.tland.payment_service.utils;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CurrencyUtils {

    USD("USD"),
    VND("VND");

    private final String value;
}
