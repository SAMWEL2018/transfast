package com.sla.matercard.inbound;

import com.sla.matercard.inbound.service.InvoiceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TransfastApplicationTests {

    @Autowired
    private InvoiceService invoiceService;

    @Test
    void contextLoads() {
    }


    void downloadInvoices(){

        String[] types = {"G", "C"};
        invoiceService.downloadInvoices(types);

    }
}
