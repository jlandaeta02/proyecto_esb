package com.bank.esb.services.impl;

import com.bank.esb.services.dto.EsbRequest;
import com.bank.esb.services.dto.EsbResponse;
import com.bank.esb.services.AbstractBankingService;
import org.json.JSONObject;

public class PagoService extends AbstractBankingService {

    @Override
    public String getServiceName() {
        return "PAGO";
    }

    @Override
    protected String validatePayload(JSONObject payload) {
        if (!payload.has("monto") || payload.optDouble("monto", 0) <= 0) {
            return "El campo 'monto' es requerido y debe ser mayor a 0.";
        }
        if (!payload.has("cuentaOrigen") || payload.optString("cuentaOrigen").isEmpty()) {
            return "El campo 'cuentaOrigen' es requerido.";
        }
        if (!payload.has("cuentaDestino") || payload.optString("cuentaDestino").isEmpty()) {
            return "El campo 'cuentaDestino' es requerido.";
        }
        return null; // Payload válido
    }

    @Override
    protected EsbResponse executeBusinessLogic(EsbRequest request) {
        JSONObject payload = request.getPayload();
        
        JSONObject resultPayload = new JSONObject();
        resultPayload.put("referencia", "REF-" + System.currentTimeMillis());
        resultPayload.put("montoProcesado", payload.getDouble("monto"));
        resultPayload.put("estatus", "COMPLETADO");

        return EsbResponse.success(request.getTransactionId(), resultPayload);
    }
}