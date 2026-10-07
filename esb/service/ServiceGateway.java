package com.esb.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.esb.dto.ErrorResponse;
import com.esb.dto.SecurityHeaders;
import com.esb.security.SecurityService;
import com.esb.service.ServicioXProvider;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ServiceGateway {

    private final SecurityService securityService = new SecurityService();
    private final ServicioXProvider servicioXProvider = new ServicioXProvider();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public record GatewayResult(int statusCode, String jsonResponseBody) {}

    public GatewayResult processRequest(SecurityHeaders headers, String payloadJson, String endpointUri) {
        try {
            // STEP A: Timestamp
            if (!securityService.isValidTimestamp(headers.timestamp())) {
                return new GatewayResult(401, objectMapper.writeValueAsString(
                    new ErrorResponse(401, "Solicitud expirada (Replay Attack detectado)")
                ));
            }
            // 2. Obtener Secret Key Dinámicamente desde DB2
            String secretKey = getSecretKeyFromDB2(headers.apiKey());
            System.out.println("HEADERS.apikey : " + headers.apiKey());
           
            if (secretKey == null) {
                return new GatewayResult(403, objectMapper.writeValueAsString(
                    new ErrorResponse(403, "API Key invalida o no registrada")
                ));
            }
            
            // STEP B: Autenticación / Permisos DB2
            /*String secretKey = getSecretKeyFromDB2(headers.apiKey(), endpointUri);
            if (secretKey == null) {
                return new GatewayResult(403, objectMapper.writeValueAsString(
                    new ErrorResponse(403, "Cliente no autorizado o sin acceso al recurso")
                ));
            }*/

            // STEP C: HMAC
            String stringToSign = headers.timestamp() + payloadJson;
            String calculatedSignature = securityService.calculateHMAC(stringToSign, secretKey);
            System.out.println("Firma enviada :   " + headers.signature());
            System.out.println("Firma calculada : " + calculatedSignature);
            if (!securityService.compareSignatures(calculatedSignature, headers.signature())) {
                return new GatewayResult(401, objectMapper.writeValueAsString(
                    new ErrorResponse(401, "Firma HMAC invalida")
                ));
            }

            // STEP D: Ejecución del Servicio
            String serviceResponseJson = servicioXProvider.execute(payloadJson);
            return new GatewayResult(200, serviceResponseJson);

        } catch (Exception e) {
            return new GatewayResult(500, "{\"code\": 500, \"message\": \"Error interno en Gateway\"}");
        }
    }

    /*private String getSecretKeyFromDB2(String apiKey, String endpoint) {
        if ("AK_PROD_8f9a2b4c1d".equals(apiKey) && "/api/v1/ServicioX".equals(endpoint)) {
            return "MiClaveSecretaSuperSegura2026";
        }
        return null;
    }*/

    /**
     * Consulta la tabla JLANDAETA1.API_KEYS en DB2 y desencripta la clave secreta.
     */
    private String getSecretKeyFromDB2(String apiKey) {
        // En IBM i / PASE, "jdbc:default:connection" o la conexión local utiliza las credenciales del Job
        String jdbcUrl = "jdbc:db2://localhost"; 
        
        String sql = """
            SELECT VARCHAR(DECRYPT_CHAR(SECRET_KEY_ENC, CAST(? AS VARCHAR(128) CCSID 37)), 256) AS SECRET_KEY 
            FROM JLANDAETA1.API_KEYS 
            WHERE API_KEY = ?
        """;
        //return DriverManager.getConnection(URL_DB2, USUARIO, CLAVE);
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
        //try (Connection conn = DriverManager.getConnection(URL_DB2, USUARIO, CLAVE);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            //stmt.setString(1, SYSTEM_MASTER_KEY);
            stmt.setString(1,"ClaveMaestraSistemaIBM");
            stmt.setString(2, apiKey);

            try (ResultSet rs = stmt.executeQuery()) {
                System.err.println(rs);
                if (rs.next()) {
                    return rs.getString("SECRET_KEY");
                }
            }
        } catch (Exception e) {
            System.err.println("Error consultando DB2: " + e.getMessage());
        }

        return null; // Si no existe la API Key o falla la DB
    }
}

