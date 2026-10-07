package com.bank.esb.security;

import com.bank.esb.config.AppConfig;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * Servicio Criptográfico Central del ESB.
 * Proporciona utilidades para cifrado/descifrado AES-256 y cálculo/verificación HMAC-SHA256.
 */
public class SecurityService {

    private static final String AES_ALGORITHM = "AES/ECB/PKCS5Padding";
    private static final String HMAC_SHA256 = "HmacSHA256";

    /**
     * Descifra la clave secreta de un cliente almacenada en DB2 utilizando la Clave Maestra del Servidor.
     */
    public static String decryptClientSecret(String encryptedSecret) throws Exception {
        //System.err.println("encryptedSecret : " + encryptedSecret);
        String cleanBase64 = encryptedSecret.trim().replaceAll("[\\r\\n]", "");
        System.err.println("Longitud cleanBase64 : " + cleanBase64.length());
        String masterKey = AppConfig.getInstance().getProperty("esb.security.master-key", "32ByteStringMasterKey2026Bank!");
        masterKey = AppConfig.getInstance().getSystemMasterKey();
        //System.err.println("masterKey : " + masterKey);
        masterKey = "ClaveMaestraSistemaIBM";
        return decryptAES(cleanBase64, masterKey);
    }
    

    /**
     * Cifra un texto plano utilizando el algoritmo AES-256.
     */
    public static String encryptAES(String plainText, String key) throws Exception {
        byte[] keyBytes = prepareKey(key);
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");

        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    /**
     * Descifra un texto encriptado en Base64 utilizando el algoritmo AES-256.
     */
    public static String decryptAES(String encryptedText, String key) throws Exception {
        byte[] keyBytes = prepareKey(key);
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");

        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);

        byte[] decodedBytes = Base64.getDecoder().decode(encryptedText);
        byte[] decryptedBytes = cipher.doFinal(decodedBytes);

        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

/*     public static String decryptAES(String cleanBase64, String masterKey) throws Exception {
        // 1. Asegurar que la clave tenga una longitud válida (256 bits / 32 bytes) usando SHA-256
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = sha.digest(masterKey.getBytes(StandardCharsets.UTF_8));
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");

        // 2. Configurar el cifrador en modo AES/ECB/PKCS5Padding
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);

        // 3. Decodificar la cadena Base64 a bytes
        byte[] encryptedBytes = Base64.getDecoder().decode(cleanBase64);

        // 4. Desencriptar y convertir el resultado a texto plano
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
}*/
    /**
     * Calcula la firma HMAC-SHA256 de una cadena (payload JSON) utilizando el Secret del cliente.
     */
    public static String calculateHMAC(String data, String secret) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
        Mac mac = Mac.getInstance(HMAC_SHA256);
        mac.init(secretKey);

        byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        //System.out.println('[' + rawHmac.toString() + ']');
        return Base64.getEncoder().encodeToString(rawHmac);
    }

    /**
     * Verifica en tiempo constante si la firma HMAC provista por el cliente coincide con la calculada sobre el payload.
     */
    public static boolean verifySignature(String payload, String clientSecret, String providedSignature) {
        //System.err.println("payload : " + payload);
        //System.err.println("clientSecret : " + clientSecret);
        //System.err.println("providedSignature : " + providedSignature);
        if (payload == null || clientSecret == null || providedSignature == null) {
            return false;
        }

        try {
            //System.err.println("payload : " + payload);
            String expectedSignature = calculateHMAC(payload, clientSecret);
            //System.err.println("expectedSignature : " + expectedSignature);
            //System.err.println("providedSignature : " + providedSignature);
            // Comparación de tiempo constante para prevenir ataques de temporización (Timing Attacks)
            return MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    providedSignature.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            System.err.println("[SecurityService] Error al verificar firma HMAC: " + e.getMessage());
            return false;
        }
    }
/**
     * Valida la integridad del payload comparando la firma enviada con la generada
     * usando la clave secreta del cliente (que ya viene descifrada desde el DAO).
     */
 /*   public static boolean verifySignature(String payload, String clientSecret, String signature) {
        if (payload == null || clientSecret == null || signature == null) {
            return false;
        }

        try {
            Mac sha256HMAC = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKey = new SecretKeySpec(
                    clientSecret.trim().getBytes(StandardCharsets.UTF_8), 
                    HMAC_SHA256
            );
            sha256HMAC.init(secretKey);

            byte[] hashBytes = sha256HMAC.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            
            // Convierte el Hash a formato Hexadecimal de 64 caracteres
            String calculatedSignature = bytesToHex(hashBytes);

            // Compara ignorando mayúsculas/minúsculas
            return calculatedSignature.equalsIgnoreCase(signature.trim());

        } catch (Exception e) {
            System.err.println("[SecurityService] Error al verificar firma HMAC: " + e.getMessage());
            return false;
        }
    }
*/
    /**
     * Helper para convertir los bytes del Digest HMAC a String Hexadecimal.
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    
    /**
     * Asegura que la clave posea una longitud fija de 256 bits (32 bytes) aplicando SHA-256.
     */
    private static byte[] prepareKey(String key) throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        return sha.digest(key.getBytes(StandardCharsets.UTF_8));
    }
}
