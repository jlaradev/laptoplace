package com.laptophub.backend.model;

public enum PaymentStatus {
    PENDIENTE("pendiente"),
    COMPLETADO("completado"),
    FALLIDO("fallido"),
    EXPIRADO("expirado");
    
    private final String value;
    
    PaymentStatus(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
}
