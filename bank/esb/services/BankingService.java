package com.bank.esb.services;

import com.bank.esb.services.dto.EsbRequest;
import com.bank.esb.services.dto.EsbResponse;

public interface BankingService {
    String getServiceName();
    EsbResponse process(EsbRequest request);
}