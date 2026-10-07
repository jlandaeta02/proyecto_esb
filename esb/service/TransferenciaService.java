package com.esb.service;

// Importamos los DTOs definidos en el paquete dto
import com.esb.dto.TransferenciaRequest;
import com.esb.dto.TransferenciaResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TransferenciaService {

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Recibe el String JSON, lo deserializa y procesa los datos.
     */
    public TransferenciaResponse procesarJSON(String jsonInput) {
        try {
            // 1. DESERIALIZACIÓN: Transforma el String JSON a los Records de Java
            TransferenciaRequest request = mapper.readValue(jsonInput, TransferenciaRequest.class);

            // 2. LECTURA Y EXTRACCIÓN DE DATOS
            String transactionId = request.header().transactionId();
            String cuentaOrigen = request.data().cuentaOrigen();
            String cuentaDestino = request.data().cuentaDestino();
            double monto = request.data().monto();
            
            String ordenanteIdentificacion = request.data().ordenante().tipoIdentificacion() + 
                                             request.data().ordenante().numeroIdentificacion();
            
            String beneficiarioIdentificacion = request.data().beneficiario().tipoIdentificacion() + 
                                                request.data().beneficiario().numeroIdentificacion();

            // 3. AQUÍ INVOCAS A TU STORED PROCEDURE O PROGRAMA RPGLE EN DB2
            // String refDB2 = ejecutarProcedimientoDB2(request);
            String refDB2 = "REF-DB2-99887766"; 

            // 4. RETORNO DE RESPUESTA
            return new TransferenciaResponse(
                "SUCCESS",
                "200",
                "Transferencia procesada exitosamente",
                new TransferenciaResponse.ResponseData(transactionId, refDB2, "2026-09-21")
            );

        } catch (Exception e) {
            e.printStackTrace();
            return new TransferenciaResponse(
                "ERROR",
                "500",
                "Error al procesar el JSON: " + e.getMessage(),
                null
            );
        }
    }
}