// Archivo: ErrorResponse.java
package com.esb.dto;

public record ErrorResponse(
    int code,
    String message
) {}
