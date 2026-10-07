package com.esb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TransferenciaResponse(
    @JsonProperty("status") String status,          // "SUCCESS" o "ERROR"
    @JsonProperty("responseCode") String responseCode,  // "200", "401", "500"
    @JsonProperty("message") String message,
    @JsonProperty("data") ResponseData data
) {
    public record ResponseData(
        @JsonProperty("transactionId") String transactionId,
        @JsonProperty("referenciaDB2") String referenciaDB2,
        @JsonProperty("fechaProceso") String fechaProceso
    ) {}
}

