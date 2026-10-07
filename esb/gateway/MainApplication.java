package com.esb.gateway;

import com.esb.dto.SecurityHeaders;
import com.esb.security.SecurityService;

public class MainApplication {

    public static void main(String[] args) {
        try {
            ServiceGateway gateway = new ServiceGateway();
            SecurityService securityService = new SecurityService();

            // 1. Datos de la Petición
            String apiKey = "AK_PROD_8f9a2b4c1d";
            String secretKey = "ClaveSecretaCliente_2026";
            //String secretKey = "ClaveMaestraSistemaIBM";
            String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
            String payload = "{\"idCliente\":1000,\"monto\":150.50}";

            // 2. Generar la Firma HMAC-SHA256 REAL sobre (Timestamp + Payload)
            String stringToSign = timestamp + payload;
            String validSignature = securityService.calculateHMAC(stringToSign, secretKey);

            // 3. Crear Headers con la Firma Válida
            SecurityHeaders headers = new SecurityHeaders(
                apiKey,
                timestamp,
                validSignature
            );

            // 4. Ejecutar en el Gateway
            ServiceGateway.GatewayResult result = gateway.processRequest(
                headers, 
                payload, 
                "/api/v1/ServicioX"
            );

            // Imprimir Resultados
            System.out.println("HTTP Code: " + result.statusCode());
            System.out.println("Response:  " + result.jsonResponseBody());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}