package com.bank.esb.services;

import com.bank.esb.services.dto.EsbRequest;
import com.bank.esb.services.dto.EsbResponse;
import org.json.JSONObject;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Clase base abstracta para todos los servicios bancarios del ESB.
 * Proporciona un flujo estandarizado de ejecución: validación, logging,
 * manejo de excepciones y construcción de respuestas JSON.
 */
public abstract class AbstractBankingService implements BankingService {

    protected static final Logger logger = Logger.getLogger(AbstractBankingService.class.getName());

    /**
     * Método plantilla (Template Method) que orquesta el ciclo de vida de la solicitud.
     */
    @Override
    public EsbResponse process(EsbRequest request) {
        long startTime = System.currentTimeMillis();
        String serviceName = this.getServiceName();
        
        logger.info("[" + serviceName + "] Inicio de procesamiento para TRX: " + request.getTransactionId());

        try {
            // 1. Validación común de parámetros
            if (request == null || request.getPayload() == null) {
                return buildErrorResponse(request, "400", "Payload o petición nula.");
            }

            // 2. Validación específica de la implementación
            String validationError = validatePayload(request.getPayload());
            if (validationError != null) {
                logger.warning("[" + serviceName + "] Error de validación: " + validationError);
                return buildErrorResponse(request, "400", "Validación fallida: " + validationError);
            }

            // 3. Ejecución de la lógica de negocio concreta
            EsbResponse response = executeBusinessLogic(request);

            long duration = System.currentTimeMillis() - startTime;
            logger.info("[" + serviceName + "] Procesado exitosamente en " + duration + " ms.");
            
            return response;

        } catch (Exception e) {
            logger.log(Level.SEVERE, "[" + serviceName + "] Error no controlado en la transacción " + request.getTransactionId(), e);
            return buildErrorResponse(request, "500", "Error interno en el servicio " + serviceName + ": " + e.getMessage());
        }
    }

    /**
     * Devuelve el nombre identificador del servicio para logs y auditoría.
     */
    public abstract String getServiceName();

    /**
     * Método para que cada servicio valide los campos obligatorios de su JSON de entrada.
     * @return null si todo está correcto, o un String con el mensaje de error si falla.
     */
    protected abstract String validatePayload(JSONObject payload);

    /**
     * Ejecución de la lógica bancaria específica (consultas a DB2, spools, llamadas JDBC, etc.).
     */
    protected abstract EsbResponse executeBusinessLogic(EsbRequest request) throws Exception;

    /**
     * Helper para construir respuestas de error uniformes.
     */
    protected EsbResponse buildErrorResponse(EsbRequest request, String code, String message) {
        EsbResponse response = new EsbResponse();
        if (request != null) {
            response.setTransactionId(request.getTransactionId());
        }
        response.setResponseCode(code);
        response.setResponseMessage(message);
        
        JSONObject errPayload = new JSONObject();
        errPayload.put("status", "ERROR");
        errPayload.put("code", code);
        errPayload.put("detail", message);
        response.setPayload(errPayload);
        
        return response;
    }
}