package com.bank.esb.router;

import com.bank.esb.db.BankingOperationsDAO;
import org.json.JSONObject;

import java.math.BigDecimal;

/**
 * Enrutador central del ESB.
 * Mapea el nombre del servicio a la operación de base de datos o lógica de negocio.
 */
public class ServiceRouter {

/**
 *   Enrutar las transacciones de acuerdo a su naturaleza financiera. 
 *  */    
    public static JSONObject route(String serviceName, JSONObject payload) {
        JSONObject response = new JSONObject();

        if (serviceName == null || serviceName.trim().isEmpty()) {
            response.put("code", "400");
            response.put("message", "El nombre del servicio es obligatorio.");
            return response;
        }

        switch (serviceName.toUpperCase()) {
            case "CONSULTA_SALDO":
                return handleConsultaSaldo(payload);

            case "TRANSFERENCIA", "TRANSFERENCIAINMEDIATA":
                System.err.println("Transferencia inmediate");
                return handleTransferencia(payload);

            default:
                response.put("code", "404");
                response.put("message", "Servicio no encontrado o no soportado: " + serviceName);
                return response;
        }
    }

    /**
     *  Consulta de saldo financiero 
     */

    private static JSONObject handleConsultaSaldo(JSONObject payload) {
        JSONObject response = new JSONObject();

        if (!payload.has("cuentaOrigen") || payload.getString("cuentaOrigen").trim().isEmpty()) {
            response.put("code", "400");
            response.put("message", "El campo 'cuenta' es requerido para este servicio.");
            return response;
        }

        String cuenta = payload.getString("cuentaOrigen").trim();
        return BankingOperationsDAO.getAccountBalance(cuenta);
    }

    /**
    * Transferencia entre cuentas
    */
    private static JSONObject handleTransferencia(JSONObject payload) {
        JSONObject response = new JSONObject();

        if (!payload.has("cuentaOrigen") || !payload.has("cuentaDestino") || !payload.has("monto")) {
            response.put("code", "400");
            response.put("message", "Faltan parámetros requeridos: cuentaOrigen, cuentaDestino, monto.");
            return response;
        }

        String cuentaOrigen = payload.getString("cuentaOrigen").trim();
        String cuentaDestino = payload.getString("cuentaDestino").trim();
        double montoDouble = payload.getDouble("monto");

        if (montoDouble <= 0) {
            response.put("code", "400");
            response.put("message", "El monto de la transferencia debe ser mayor a cero.");
            return response;
        }

        BigDecimal monto = BigDecimal.valueOf(montoDouble);
        return BankingOperationsDAO.executeTransfer(cuentaOrigen, cuentaDestino, monto);
    }

  /**
   * Otras transacciones 
   *  */  
}