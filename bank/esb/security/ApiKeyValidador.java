package com.bank.esb.security;

import java.util.regex.Pattern;

/**
 * Validador de formato, sintaxis y estructura para API Keys en el ESB.
 */
public class ApiKeyValidador {

    // Formato esperado: Prefijo del Banco + Identificador único (Ej: BK-CLIENT-8891A3B)
    private static final Pattern API_KEY_PATTERN = Pattern.compile("^[A-Z0-9]{2,6}-[A-Z0-9_-]{8,32}$", Pattern.CASE_INSENSITIVE);

    /**
     * Evalúa si una API Key cumple con la sintaxis, longitud y formato permitidos.
     */
    public static boolean isValidFormat(String apiKey) {
        if (apiKey == null) {
            return false;
        }
        
        String trimmedKey = apiKey.trim();

        if (trimmedKey.isEmpty()) {
            return false;
        }

        // Validar longitud mínima y máxima
        if (trimmedKey.length() < 10 || trimmedKey.length() > 40) {
            return false;
        }

        // Evaluar expresión regular
        return API_KEY_PATTERN.matcher(trimmedKey).matches();
    }

    /**
     * Sanitiza la API Key de entrada removiendo caracteres no deseados antes del query SQL.
     */
    public static String sanitize(String apiKey) {
        if (apiKey == null) {
            return "";
        }
        return apiKey.trim();
    }
}
