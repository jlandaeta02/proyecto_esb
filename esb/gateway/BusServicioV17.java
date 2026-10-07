package com.esb.gateway;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import com.esb.dto.SecurityHeaders;
import com.esb.security.SecurityService;

import com.esb.dto.TransferenciaResponse;
import com.esb.service.TransferenciaService;

public class BusServicioV17 {

    // En IBM i V7R2, al correr localmente se puede usar el driver de IBM Toolbox o el nativo
    private static final String URL_DB2 = "jdbc:as400://localhost/JLANDAETA1;naming=system";
    private static final String USUARIO = "JLANDAETA";
    private static final String CLAVE = "lcarlos,2465";
    private static final int PUERTO = 8090;

    public static void main(String[] args) {
        // Pool dinámico de hilos optimizado para Java 17
        ExecutorService executor = Executors.newCachedThreadPool();

        try (ServerSocket server = new ServerSocket(PUERTO)) {
            System.out.println("Servicio POST (JSON Body) iniciado en el puerto " + PUERTO + " (Java 17 - V7R2)...");

            ServiceGateway gateway = new ServiceGateway();
            SecurityService securityService = new SecurityService();

            while (!Thread.currentThread().isInterrupted()) {
                Socket cliente = server.accept();
                executor.submit(() -> manejarCliente(cliente));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
    }

    private static void manejarCliente(Socket cliente) {
        try (cliente;
             BufferedReader in = new BufferedReader(new InputStreamReader(cliente.getInputStream(), StandardCharsets.UTF_8));
             OutputStream out = cliente.getOutputStream()) {

            cliente.setSoTimeout(10000);

            // 1. Leer primera línea de la petición HTTP
            String lineaPeticion = in.readLine();
            if (lineaPeticion == null) return;
            //System.out.println("Paso 0 " +  lineaPeticion);  

            // Descartar petición favicon.ico
            if (lineaPeticion.contains("/favicon.ico")) {
                String respuestaFavicon = "HTTP/1.1 204 No Content\r\nConnection: close\r\n\r\n";
                out.write(respuestaFavicon.getBytes(StandardCharsets.UTF_8));
                out.flush();
                return;
            }

            // 2. Leer las cabeceras HTTP y obtener Content-Length
            int contentLength = 0;
            String lineaCabecera;
            while ((lineaCabecera = in.readLine()) != null && !lineaCabecera.isEmpty()) {
                if (lineaCabecera.toLowerCase().startsWith("content-length:")) {
                    contentLength = Integer.parseInt(lineaCabecera.substring(15).trim());
                }
            }
            //System.out.println("Paso 1 " +  lineaCabecera);  

            String respuestaJson;

            // 3. Procesar solo peticiones POST con contenido
            if (lineaPeticion.startsWith("POST") && contentLength > 0) {
                char[] bufferBody = new char[contentLength];
                int leidos = 0;
                while (leidos < contentLength) {
                    int r = in.read(bufferBody, leidos, contentLength - leidos);
                    if (r == -1) break;
                    leidos += r;
                }


                System.out.println("=== INICIANDO PROCESAMIENTO DE TRANSFERENCIA ===");

                // 2. Instanciar el servicio
                TransferenciaService servicio = new TransferenciaService();

                String cuerpoJsonStr = new String(bufferBody);
                System.out.println(cuerpoJsonStr);    
                // 3. Ejecutar el procesamiento (deserialización y lógica de negocio)
                TransferenciaResponse respuesta = servicio.procesarJSON(cuerpoJsonStr);

                // 4. Mostrar el resultado devuelto
                System.out.println("\n=== RESULTADO DE LA OPERACION ===");
                System.out.println("Status:        " + respuesta.status());
                System.out.println("Codigo:        " + respuesta.responseCode());
                System.out.println("Mensaje:       " + respuesta.message());

                if (respuesta.data() != null) {
                    System.out.println("Txn ID:        " + respuesta.data().transactionId());
                    System.out.println("Ref. DB2:      " + respuesta.data().referenciaDB2());
                    System.out.println("Fecha Proceso: " + respuesta.data().fechaProceso());
                }
                //System.out.println("Paso 3 " +  cuerpoJsonStr);  

/*                 //System.out.println("Paso 2 antes de convertirlo ");  
                String cuerpoJsonStr = new String(bufferBody);
                System.out.println("Paso 3 " +  cuerpoJsonStr);  
                // Extracción nativa de parámetros JSON
                String ano = extraerValorJson(cuerpoJsonStr, "ano");
                String mes = extraerValorJson(cuerpoJsonStr, "mes");
                String dia = extraerValorJson(cuerpoJsonStr, "dia");
                System.out.println("Paso 4: Year = " +  ano + ", Mes = " + mes + ", dia = " + dia ); 
                
                String serviceName = extraerValorJson(cuerpoJsonStr, "serviceName"); //
                String transactionId = extraerValorJson(cuerpoJsonStr, "transactionId"); //
                String serviceName = extraerValorJson(cuerpoJsonStr, "serviceName"); //
                String timestamp = extraerValorJson(cuerpoJsonStr, "timestamp"); //
                String channel = extraerValorJson(cuerpoJsonStr, "channel"); //
                String timestamp = extraerValorJson(cuerpoJsonStr, "timestamp"); //
                String apiKey = extraerValorJson(cuerpoJsonStr, "apikey"); //"AK_PROD_8f9a2b4c1d";
                String nonce = extraerValorJson(cuerpoJsonStr, "nonce"); //"AK_PROD_8f9a2b4c1d";
                String signature = extraerValorJson(cuerpoJsonStr, "signature"); //"AK_PROD_8f9a2b4c1d";
                String secretKey = extraerValorJson(cuerpoJsonStr, "secretkey"); //"ClaveSecretaCliente_2026";
                //String secretKey = "ClaveMaestraSistemaIBM";
                //String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
                String payload = "{\"idCliente\":1000,\"monto\":150.50}";
  */              
/*
{
  "header": {
    "serviceName": "TRANSFERENCIA_CUENTAS",
    "version": "1.0",
    "transactionId": "TRX-20260918-8849201",
    "timestamp": 1726679548,
    "channel": "APP_MOVIL",
    "security": {
      "apiKey": "AK_PROD_8f9a2b4c1d",
      "nonce": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
      "signature": "c808b0a9415c4d623253b7c8fb23fb394a11be0cb1d5e3f4db303b7a111c82f2"
    }
  },
  "data": {
    "cuentaOrigen": "01020111001234567890",
    "cuentaDestino": "01020222009876543210",
    "monto": 450.00,
    "moneda": "USD",
    "concepto": "Pago de servicios profesionales",
    "ordenante": {
      "tipoIdentificacion": "V",
      "numeroIdentificacion": "18990123",
      "nombre": "Jose Landaeta"
    },
    "beneficiario": {
      "tipoIdentificacion": "J",
      "numeroIdentificacion": "301234560",
      "nombre": "Servicios Tech C.A."
    }
  }
}
*/


                // Abrir conexión por petición para evitar hilos bloqueados en DB2
                try (Connection conn = obtenerConexionDB2()) {
                    //respuestaJson = consultarDb2AJson(conn, ano, mes, dia);
                }

            } else {
                respuestaJson = "[{\"error\": \"Solo se aceptan peticiones POST con JSON en el cuerpo\"}]";
            }

 /*           String jsonLegible = formatJson(respuestaJson);
            byte[] contenidoBytes = jsonLegible.getBytes(StandardCharsets.UTF_8);

            String cabeceras = "HTTP/1.1 200 OK\r\n" +
                               "Content-Type: application/json; charset=UTF-8\r\n" +
                               "Content-Length: " + contenidoBytes.length + "\r\n" +
                               "Connection: close\r\n\r\n";

            out.write(cabeceras.getBytes(StandardCharsets.UTF_8));
            out.write(contenidoBytes);
            out.flush();
*/
        } catch (Exception e) {
            System.out.println("Error procesando peticion: " + e.getMessage());
        }
    }

    private static Connection obtenerConexionDB2() throws SQLException, ClassNotFoundException {
        // Driver compatible con IBM i V7R2 (JT400)
        //Class.forName("com.ibm.as400.access.AS400JDBCDriver");
        String jdbcUrl = "jdbc:db2://localhost"; 
        //return DriverManager.getConnection(URL_DB2, USUARIO, CLAVE);
        return DriverManager.getConnection(jdbcUrl);
    }

    private static String consultarDb2AJson(Connection conn, String ano, String mes, String dia) {
        StringBuilder jsonBuilder = new StringBuilder("[");
        String biblioteca = "JLANDAETA1";
        String tabla = "TASAPF";

        // Sentencia preparada para evitar inyección SQL
        StringBuilder sql = new StringBuilder("SELECT * FROM ").append(biblioteca).append("/").append(tabla).append(" WHERE 1=1");

        if (ano != null && !ano.isBlank()) {
            sql.append(" AND TANO = ?");
        }
        if (mes != null && !mes.isBlank()) {
            sql.append(" AND TMES = ?");
        }
        if (dia != null && !dia.isBlank()) {
            sql.append(" AND TDIA = ?");
        }

        try (PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            int paramIndex = 1;
            if (ano != null && !ano.isBlank()) {
                pstmt.setString(paramIndex++, ano);
            }
            if (mes != null && !mes.isBlank()) {
                pstmt.setString(paramIndex++, mes);
            }
            if (dia != null && !dia.isBlank()) {
                pstmt.setString(paramIndex++, dia);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnas = metaData.getColumnCount();
                boolean primerRegistro = true;

                while (rs.next()) {
                    if (!primerRegistro) jsonBuilder.append(",");
                    jsonBuilder.append("{");

                    for (int i = 1; i <= columnas; i++) {
                        String nombreCol = metaData.getColumnName(i).toLowerCase();
                        Object valor = rs.getObject(i);
                        String valorStr = (valor != null) ? valor.toString().trim() : "";

                        jsonBuilder.append("\"").append(nombreCol).append("\":\"")
                                   .append(escaparJson(valorStr)).append("\"");

                        if (i < columnas) jsonBuilder.append(",");
                    }

                    jsonBuilder.append("}");
                    primerRegistro = false;
                }
            }
        } catch (SQLException e) {
            return "[{\"error\": \"" + e.getMessage().replace("\"", "'") + "\"}]";
        }

        jsonBuilder.append("]");
        return jsonBuilder.toString();
    }

    private static String extraerValorJson(String json, String clave) {
        if (json == null || json.isBlank()) return null;
        String patron = "\"" + clave + "\"";
        int idx = json.indexOf(patron);
        if (idx == -1) return null;

        int dosPuntos = json.indexOf(":", idx + patron.length());
        if (dosPuntos == -1) return null;

        int inicio = dosPuntos + 1;
        while (inicio < json.length() && Character.isWhitespace(json.charAt(inicio))) {
            inicio++;
        }

        if (inicio >= json.length()) return null;

        if (json.charAt(inicio) == '"') {
            int fin = json.indexOf('"', inicio + 1);
            if (fin != -1) return json.substring(inicio + 1, fin);
        } else {
            int fin = inicio;
            while (fin < json.length() && (Character.isLetterOrDigit(json.charAt(fin)) || json.charAt(fin) == '.')) {
                fin++;
            }
            return json.substring(inicio, fin);
        }
        return null;
    }

    private static String escaparJson(String texto) {
        if (texto == null) return "";
        return texto.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\b", "\\b")
                    .replace("\f", "\\f")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }

    public static String formatJson(String jsonString) {
        if (jsonString == null || jsonString.isBlank()) return "";
        StringBuilder result = new StringBuilder();
        int indentLevel = 0;
        boolean inQuotes = false;

        for (int i = 0; i < jsonString.length(); i++) {
            char ch = jsonString.charAt(i);
            switch (ch) {
                case '"' -> {
                    if (i > 0 && jsonString.charAt(i - 1) == '\\') {
                        result.append(ch);
                    } else {
                        inQuotes = !inQuotes;
                        result.append(ch);
                    }
                }
                case '{', '[' -> {
                    result.append(ch);
                    if (!inQuotes) {
                        result.append("\n");
                        indentLevel++;
                        appendIndent(result, indentLevel);
                    }
                }
                case '}', ']' -> {
                    if (!inQuotes) {
                        result.append("\n");
                        indentLevel--;
                        appendIndent(result, indentLevel);
                    }
                    result.append(ch);
                }
                case ',' -> {
                    result.append(ch);
                    if (!inQuotes) {
                        result.append("\n");
                        appendIndent(result, indentLevel);
                    }
                }
                case ':' -> {
                    if (!inQuotes) result.append(": ");
                    else result.append(ch);
                }
                default -> result.append(ch);
            }
        }
        return result.toString();
    }

    private static void appendIndent(StringBuilder sb, int count) {
        sb.append("  ".repeat(Math.max(0, count)));
    }
}