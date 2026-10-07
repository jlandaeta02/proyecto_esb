package com.bank.esb.services.dto;

import org.json.JSONObject;

public class EsbResponse {
    private String transactionId;
    private String responseCode;
    private String responseMessage;
    private JSONObject payload;

    public EsbResponse() {}

    public EsbResponse(String responseCode, String responseMessage) {
        this.responseCode = responseCode;
        this.responseMessage = responseMessage;
        this.payload = new JSONObject();
    }

    // CONSTRUCTOR REQUERIDO POR CLIENTTASK.JAVA (Línea 134)
    public EsbResponse(String responseCode, String responseMessage, JSONObject payload) {
        this.responseCode = responseCode;
        this.responseMessage = responseMessage;
        this.payload = payload != null ? payload : new JSONObject();
    }

    public EsbResponse(String transactionId, String responseCode, String responseMessage, JSONObject payload) {
        this.transactionId = transactionId;
        this.responseCode = responseCode;
        this.responseMessage = responseMessage;
        this.payload = payload != null ? payload : new JSONObject();
    }

    // --- MÉTODOS ESTÁTICOS DE FÁBRICA (FACTORY METHODS) ---

    public static EsbResponse error(String code, String message) {
        return new EsbResponse(code, message);
    }

    public static EsbResponse error(String transactionId, String code, String message) {
        EsbResponse resp = new EsbResponse(code, message);
        resp.setTransactionId(transactionId);
        return resp;
    }

    public static EsbResponse success(String transactionId, JSONObject payload) {
        return new EsbResponse(transactionId, "200", "SUCCESS", payload);
    }

    public static EsbResponse success(JSONObject payload) {
        return new EsbResponse(null, "200", "SUCCESS", payload);
    }

    // --- GETTERS Y SETTERS ---

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(String responseCode) {
        this.responseCode = responseCode;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public void setResponseMessage(String responseMessage) {
        this.responseMessage = responseMessage;
    }

    public JSONObject getPayload() {
        return payload;
    }

    public void setPayload(JSONObject payload) {
        this.payload = payload;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        if (this.transactionId != null) {
            json.put("transactionId", this.transactionId);
        }
        json.put("responseCode", this.responseCode != null ? this.responseCode : "500");
        json.put("responseMessage", this.responseMessage != null ? this.responseMessage : "Error desconocido");
        json.put("payload", this.payload != null ? this.payload : new JSONObject());
        return json;
    }
}