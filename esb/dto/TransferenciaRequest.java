package com.esb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TransferenciaRequest(
    @JsonProperty("header") Header header,
    @JsonProperty("data") Data data
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Header(
        @JsonProperty("serviceName") String serviceName,
        @JsonProperty("version") String version,
        @JsonProperty("transactionId") String transactionId,
        @JsonProperty("timestamp") long timestamp,
        @JsonProperty("channel") String channel,
        @JsonProperty("security") Security security
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Security(
        @JsonProperty("apiKey") String apiKey,
        @JsonProperty("nonce") String nonce,
        @JsonProperty("signature") String signature
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(
        @JsonProperty("cuentaOrigen") String cuentaOrigen,
        @JsonProperty("cuentaDestino") String cuentaDestino,
        @JsonProperty("monto") double monto,
        @JsonProperty("moneda") String moneda,
        @JsonProperty("concepto") String concepto,
        @JsonProperty("ordenante") Persona ordenante,
        @JsonProperty("beneficiario") Persona beneficiario
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Persona(
        @JsonProperty("tipoIdentificacion") String tipoIdentificacion,
        @JsonProperty("numeroIdentificacion") String numeroIdentificacion,
        @JsonProperty("nombre") String nombre
    ) {}
}
