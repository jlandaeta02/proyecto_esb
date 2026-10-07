// Archivo: ResponseServicioX.java
package com.esb.dto;

public record ResponseServicioX(
    String status,
    String transactionId,
    String message
) {}