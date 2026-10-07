// Archivo: SecurityHeaders.java
package com.esb.dto;

public record SecurityHeaders(
    String apiKey,
    String timestamp,
    String signature
) {}
