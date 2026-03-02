package com.laptophub.backend.model;

public enum OrderStatus {
    PENDIENTE_PAGO("pendiente_pago"),
    PROCESANDO("procesando"),
    ENVIADO("enviado"),
    ENTREGADO("entregado"),
    CANCELADO("cancelado"),
    EXPIRADO("expirado");
    
    private final String value;
    
    OrderStatus(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
}
