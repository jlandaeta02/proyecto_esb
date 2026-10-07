package com.esb.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.esb.dto.RequestServicioX;
import com.esb.dto.ResponseServicioX;

public class ServicioXProvider {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String execute(String payloadJson) throws Exception {
        // Deserializar JSON de entrada
        RequestServicioX request = objectMapper.readValue(payloadJson, RequestServicioX.class);
        
        // Lógica de Negocio (Ej: Ejecución de programa RPGLE o SQL en DB2)
        ResponseServicioX response = new ResponseServicioX(
            "SUCCESS", 
            "TX-998822", 
            "Procesado exitosamente para el cliente " + request.idCliente()
        );

        // Retornar respuesta formateada a JSON
        return objectMapper.writeValueAsString(response);
    }
}