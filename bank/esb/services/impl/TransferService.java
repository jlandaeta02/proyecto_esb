package com.bank.esb.services.impl;

import com.bank.esb.services.AbstractBankingService;
import com.bank.esb.services.dto.EsbRequest;
import com.bank.esb.services.dto.EsbResponse;
import org.json.JSONObject;

public class TransferService extends AbstractBankingService {

    @Override
    public String getServiceName() {
        return "TransferService";
    }

    @Override
    protected String validatePayload(JSONObject payload) {
        if (!payload.has("cuentaOrigen")) return "El campo 'cuentaOrigen' es requerido.";
        if (!payload.has("cuentaDestino")) return "El campo 'cuentaDestino' es requerido.";
        if (!payload.has("monto") || payload.getDouble("monto") <= 0) return "Monto inválido.";
        return null;
    }

    @Override
    protected EsbResponse executeBusinessLogic(EsbRequest request) throws Exception {
        JSONObject payload = request.getPayload();
        
        String origen = payload.getString("cuentaOrigen");
        String destino = payload.getString("cuentaDestino");
        double monto = payload.getDouble("monto");

        // Lógica de transferencia en DB2 / Stored Procedure
        
        JSONObject resPayload = new JSONObject();
        resPayload.put("status", "SUCCESS");
        resPayload.put("referencia", "TRF-" + System.currentTimeMillis());
        resPayload.put("montoTransferido", monto);

        EsbResponse response = new EsbResponse();
        response.setTransactionId(request.getTransactionId());
        response.setResponseCode("200");
        response.setResponseMessage("Transferencia realizada con éxito.");
        response.setPayload(resPayload);

        return response;
    }
}