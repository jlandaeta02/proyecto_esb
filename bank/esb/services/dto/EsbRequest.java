package com.bank.esb.services.dto;

import org.json.JSONException;
import org.json.JSONObject;

public class EsbRequest {

    private String serviceName;
    private String transactionId;
    private JSONObject payload;

    public EsbRequest() {
        this.payload = new JSONObject();
    }

    public EsbRequest(String serviceName, JSONObject payload) {
        this.serviceName = serviceName;
        this.payload = (payload != null) ? payload : new JSONObject();
    }

    public EsbRequest(String serviceName, String transactionId, JSONObject payload) {
        this.serviceName = serviceName;
        this.transactionId = transactionId;
        this.payload = (payload != null) ? payload : new JSONObject();
    }

    public static EsbRequest fromJson(JSONObject json) throws JSONException {
        EsbRequest request = new EsbRequest();

        if (json.has("header") && !json.isNull("header")) {
            JSONObject header = json.getJSONObject("header");
            if (header.has("serviceName")) {
                request.setServiceName(header.getString("serviceName"));
            }
            if (header.has("transactionId")) {
                request.setTransactionId(header.getString("transactionId"));
            }
        }

        if (json.has("data") && !json.isNull("data")) {
            request.setPayload(json.getJSONObject("data"));
        } else {
            request.setPayload(json);
        }

        return request;
    }

    public static EsbRequest fromJson(String jsonStr) throws JSONException {
        return fromJson(new JSONObject(jsonStr));
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public JSONObject getPayload() {
        return payload;
    }

    public void setPayload(JSONObject payload) {
        this.payload = payload;
    }
}
