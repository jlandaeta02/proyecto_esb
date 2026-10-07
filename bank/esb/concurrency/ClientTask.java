package com.bank.esb.concurrency;

import com.bank.esb.db.BankingOperationsDAO;
import com.bank.esb.router.ServiceRouter;
import com.bank.esb.security.SecurityService;
import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.SSLSocket;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Tarea encargada de procesar de forma individual y concurrente
 * cada petición bancaria recibida a través del Socket TLS.
 */
public class ClientTask implements Runnable {

    private final Socket clientSocket;

    public ClientTask(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        String clientIp = clientSocket.getInetAddress().getHostAddress();

        try (
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8))
        ) {
            // 1. Handshake SSL/TLS
            if (clientSocket instanceof SSLSocket) {
                SSLSocket sslSocket = (SSLSocket) clientSocket;
                sslSocket.startHandshake();
            }

            clientSocket.setSoTimeout(15000);

            // 2. Filtrar encabezados HTTP si existen (ej. POST / HTTP/1.1)
            String line = "";  //reader.readLine();
            // Si la trama comienza con protocolo HTTP, descartamos todas las cabeceras hasta la línea en blanco
            StringBuilder rawPayload = new StringBuilder();
            boolean isHttp = false;
            int contentLength = -1;

            // 1. Lectura de flujo TCP e inspección HTTP
            while ((line = reader.readLine()) != null) {
                String trimmedLine = line.trim();
                //System.err.println("trimmedLine 1 : " + trimmedLine);
                if (trimmedLine.startsWith("POST ") || trimmedLine.startsWith("GET ") || trimmedLine.startsWith("PUT ")) {
                    //System.err.println("Http :" + isHttp);
                    isHttp = true;
                    continue;
                }

                if (isHttp && trimmedLine.toLowerCase().startsWith("content-length:")) {
                    try {
                        contentLength = Integer.parseInt(trimmedLine.substring(15).trim());
                    } catch (NumberFormatException ignored) {}
                    continue;
                }

                if (isHttp && trimmedLine.isEmpty()) {
                    break;
                }

                if (!isHttp) {
                    if (trimmedLine.startsWith("{") || trimmedLine.startsWith("[")) {
                        rawPayload.append(trimmedLine);
                        //System.err.println("trimmedLine 2 : " + trimmedLine);
                        //break;
                    }
                }
            }

            // 2. Extraer cuerpo si fue HTTP
            if (isHttp) {
                if (contentLength > 0) {
                    char[] bodyBuffer = new char[contentLength];
                    int bytesRead = reader.read(bodyBuffer, 0, contentLength);
                    if (bytesRead > 0) {
                        rawPayload.append(new String(bodyBuffer, 0, bytesRead));
                    }
                } else {
                    line = reader.readLine();
                    if (line != null) {
                        rawPayload.append(line.trim());
                    }
                }
            }

            String jsonText = rawPayload.toString().trim();            
            //System.err.println("input line (JSON payload): " + jsonText);

            if (jsonText == null || jsonText.trim().isEmpty()) {
                sendErrorResponse(writer, "400", "Cuerpo JSON vacío o no proporcionado.");
                return;
            }

            //System.err.println("processMessage");
            // 3. Procesar y autenticar la petición JSON
            JSONObject response = processMessage(jsonText , clientIp);

             //System.err.println("WRITE RESPONSE");
             //System.err.println(response.toString());

             sendHttpResponse(writer, "200 OK", response);
            // 4. Enviar respuesta final
            //writer.write(response.toString());
            //writer.newLine();
            //writer.flush();

        } catch (java.net.SocketTimeoutException e) {
            System.err.println("[ESB ClientTask] Timeout de lectura alcanzado para el cliente: " + clientIp);
        } catch (Exception e) {
            System.err.println("[ESB ClientTask] Error procesando la conexión de " + clientIp + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeSocket();
        }
    }

    /**
     * Valida la estructura del JSON con bloques 'header' y 'data',
     * la autenticación por API Key y la firma HMAC antes de derivar
     * la petición al enrutador de servicios.
     */
    private JSONObject processMessage(String rawJson, String clientIp) {
        JSONObject response = new JSONObject();

        try {
            JSONObject request = new JSONObject(rawJson);

            // 1. Validación de bloques de primer nivel
            if (!request.has("header") || !request.has("data")) {
                response.put("code", "400");
                response.put("message", "Estructura de petición inválida. Campos requeridos: 'header' y 'data'.");
                return response;
            }

            JSONObject header = request.getJSONObject("header");
            JSONObject data = request.getJSONObject("data");

            // 2. Validación de campos dentro de 'header' y 'security'
            if (!header.has("serviceName") || !header.has("security")) {
                response.put("code", "400");
                response.put("message", "Estructura de 'header' inválida. Campos requeridos: 'serviceName' y 'security'.");
                return response;
            }

            JSONObject security = header.getJSONObject("security");
            if (!security.has("apiKey") || !security.has("signature")) {
                response.put("code", "400");
                response.put("message", "Estructura de 'security' inválida. Campos requeridos: 'apiKey' y 'signature'.");
                return response;
            }

            // Extracción de datos
            String apiKey = security.getString("apiKey");
            String signature = security.getString("signature");
            String serviceName = header.getString("serviceName");

            // 3. Validar la existencia de la API Key y recuperar el Secret desde la DB
            String encryptedSecret = BankingOperationsDAO.getClientSecretByApiKey(apiKey);
            if (encryptedSecret == null) {
                response.put("code", "401");
                response.put("message", "Autenticación fallida: API Key no autorizada.");
                return response;
            }
            encryptedSecret.trim();
            //System.err.println("encryptedSecret : " + encryptedSecret);       
            //System.err.println("Longitud : " + encryptedSecret.length());       
            // 4. Descifrar el Secret del cliente usando la clave maestra AES del servidor
            //String clientSecret = SecurityService.decryptClientSecret(encryptedSecret);
            String clientSecret = BankingOperationsDAO.DesencriptaDAO(apiKey);
            //String clientSecret = encryptedSecret;  
            //System.err.println("clientSecret : " + clientSecret);       
            //System.err.println(data.toString());
            // 5. Verificar la integridad del payload 'data' mediante la firma HMAC-SHA256
            // Se pasa data.toString() que contiene los parámetros de la transacción (cuentaOrigen, monto, etc.)
            boolean isSignatureValid = SecurityService.verifySignature(data.toString(), clientSecret, signature);
            if (!isSignatureValid) {
                response.put("code", "403");
                response.put("message", "Acceso denegado: Firma HMAC no coincide o el payload fue alterado.");
                return response;
            }

            // 6. Enrutamiento al servicio bancario solicitado pasando el objeto 'data'
            response = ServiceRouter.route(serviceName, data);

        } catch (JSONException e) {
            response.put("code", "400");
            response.put("message", "Formato JSON malformado: " + e.getMessage());
        } catch (Exception e) {
            response.put("code", "500");
            response.put("message", "Error interno procesando la transacción en el servidor: " + e.getMessage());
        }

        return response;
    }


/*    private JSONObject processMessage(String rawJson, String clientIp) {
        JSONObject response = new JSONObject();

        try {
            JSONObject request = new JSONObject(rawJson);
            System.err.println("processMessage ENTRO");
            System.err.println("apiKey : " + request.getString("apiKey"));
            System.err.println("signature : " + request.getString("signature"));
            System.err.println("serviceName : " + request.getString("service"));
            System.err.println("rawPayload : " + request.get("payload").toString());

            // Validación de campos estructurales obligatorios
            if (!request.has("apiKey") || !request.has("signature") || !request.has("serviceName") || !request.has("payload")) {
                response.put("code", "400");
                response.put("message", "Estructura de petición inválida. Campos requeridos: apiKey, signature, service, payload.");
                return response;
            }
            System.err.println("processMessage 2");
            String apiKey = request.getString("apiKey");
            String signature = request.getString("signature");
            String serviceName = request.getString("service");
            
            // Extraer el string crudo del payload para evitar deformaciones en la firma HMAC
            String rawPayload = request.get("payload").toString();
            JSONObject payloadObj = request.getJSONObject("payload");

            // 1. Validar la existencia de la API Key y recuperar el Secret
            String encryptedSecret = BankingOperationsDAO.getClientSecretByApiKey(apiKey);
            if (encryptedSecret == null) {
                response.put("code", "401");
                response.put("message", "Autenticación fallida: API Key no autorizada.");
                return response;
            }

            // 2. Descifrar el Secret del cliente
            String clientSecret = SecurityService.decryptClientSecret(encryptedSecret);

            // 3. Verificar la integridad del payload mediante HMAC-SHA256
            boolean isSignatureValid = SecurityService.verifySignature(rawPayload, clientSecret, signature);
            if (!isSignatureValid) {
                response.put("code", "403");
                response.put("message", "Acceso denegado: Firma HMAC no coincide o el payload fue alterado.");
                return response;
            }

            // 4. Enrutamiento al servicio bancario solicitado
            response = ServiceRouter.route(serviceName, payloadObj);

        } catch (JSONException e) {
            response.put("code", "400");
            response.put("message", "Formato JSON malformado: " + e.getMessage());
        } catch (Exception e) {
            response.put("code", "500");
            response.put("message", "Error interno procesando la transacción en el servidor: " + e.getMessage());
        }

        return response;
    }
*/
 /*   private void sendErrorResponse(BufferedWriter writer, String code, String message) {
        try {
            JSONObject err = new JSONObject();
            err.put("code", code);
            err.put("message", message);
            writer.write(err.toString());
            writer.newLine();
            writer.flush();
        } catch (Exception ignored) {
        }
    }
*/
    private void sendErrorResponse(BufferedWriter writer, String code, String message) {
    try {
        JSONObject err = new JSONObject();
        err.put("code", code);
        err.put("message", message);
        
        String jsonResponse = err.toString();
        byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);

        // Si es error 400, enviamos el status HTTP adecuado
        String httpStatus = code.equals("400") ? "HTTP/1.1 400 Bad Request\r\n" : "HTTP/1.1 500 Internal Server Error\r\n";

        writer.write(httpStatus);
        writer.write("Content-Type: application/json; charset=UTF-8\r\n");
        writer.write("Content-Length: " + responseBytes.length + "\r\n");
        writer.write("Connection: close\r\n");
        writer.write("\r\n");
        writer.write(jsonResponse);
        writer.flush();
    } catch (Exception ignored) {
        // Fallo de red secundario al responder
    }
}

/**
     * Emite una respuesta HTTP/1.1 limpia y compatible con Postman.
     */
    private void sendHttpResponse(BufferedWriter writer, String status, JSONObject jsonBody) {
        try {
            String jsonStr = jsonBody.toString();
            byte[] bodyBytes = jsonStr.getBytes(StandardCharsets.UTF_8);

            writer.write("HTTP/1.1 " + status + "\r\n");
            writer.write("Content-Type: application/json; charset=UTF-8\r\n");
            writer.write("Content-Length: " + bodyBytes.length + "\r\n");
            writer.write("Connection: close\r\n");
            writer.write("\r\n"); // Línea en blanco obligatoria que separa los encabezados del cuerpo
            writer.write(jsonStr);
            writer.flush();
        } catch (Exception e) {
            System.err.println("[ESB ClientTask] Error enviando respuesta HTTP: " + e.getMessage());
        }
    }

    private void closeSocket() {
        if (clientSocket != null && !clientSocket.isClosed()) {
            try {
                clientSocket.close();
            } catch (Exception e) {
                System.err.println("[ESB ClientTask] Error cerrando el socket del cliente: " + e.getMessage());
            }
        }
    }
}
