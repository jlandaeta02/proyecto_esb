package com.bank.esb.services.impl;

import com.bank.esb.services.BankingService;
import com.bank.esb.services.dto.EsbRequest;
import com.bank.esb.services.dto.EsbResponse;

public class SaldoService implements BankingService {

    @Override
    public String getServiceName() {
        return "SaldoService";
    }

    @Override
    public EsbResponse process(EsbRequest request) {
        // Implementación directa del procesamiento
        EsbResponse response = new EsbResponse();
        // Lógica de procesamiento de saldo...
        return response;
    }
}