package com.bank.esb.router;

import com.bank.esb.services.dto.EsbRequest;
import com.bank.esb.services.dto.EsbResponse;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Utilidad especializada en el procesamiento, análisis sintáctico y conversión 
 * de las tramas de mensajes JSON recibidas y enviadas sobre el Socket TLS.
 */
public class EsbMessageParser {

    /**
     * Parsea una cadena de texto proveniente de la red y la convierte a un objeto EsbRequest.
     *
     * @param rawMessage Trama cruda recibida en UTF-8.
     * @return Instancia poblada de EsbRequest.
     * @throws JSONException Si la trama está malformada o no se puede interpretar como JSON.
     */
    public static EsbRequest parseRequest(String rawMessage) throws JSONException {
        if (rawMessage == null || rawMessage.trim().isEmpty()) {
            throw new JSONException("La trama del mensaje recibida está vacía.");
        }

        String sanitizedMessage = rawMessage.trim();

        // Validar delimitadores básicos de JSON
        if (!sanitizedMessage.startsWith("{") || !sanitizedMessage.endsWith("}")) {
            throw new JSONException("La trama de red no cumple con la estructura JSON básica (debe iniciar con '{' y finalizar con '}').");
        }

        return EsbRequest.fromJson(sanitizedMessage);
    }

    /**
     * Serializa un objeto EsbResponse a una cadena de texto terminada en salto de línea
     * lista para ser enviada a través del OutputStream del socket.
     *
     * @param response Objeto de respuesta del ESB.
     * @return Trama JSON serializada en formato String.
     */
    public static String formatResponse(EsbResponse response) {
        if (response == null) {
            return EsbResponse.error("500", "Respuesta interna nula.").toString();
        }
        return response.toString();
    }

    /**
     * Intenta extraer de forma segura un objeto JSONObject desde una cadena sin lanzar excepciones hacia la capa superior.
     *
     * @param rawJson Cadena JSON a evaluar.
     * @return JSONObject parseado o null si es inválido.
     */
    public static JSONObject safeParseJson(String rawJson) {
        if (rawJson == null || rawJson.trim().isEmpty()) {
            return null;
        }
        try {
            return new JSONObject(rawJson.trim());
        } catch (JSONException e) {
            System.err.println("[EsbMessageParser] Error analizando trama JSON no estructurada: " + e.getMessage());
            return null;
        }
    }
}
