package com.sla.matercard.inbound.service;

import com.sla.matercard.inbound.model.Invoice;

import java.util.List;

/**
 * @author samwel.wafula
 * Created on 19/06/2024
 * Time 16:30
 * Project Transfast
 */
public interface InvoiceService {


    List<Invoice> downloadAndSaveNewInvoices();

    Invoice validateNotPaidAndLockInvoice(Invoice invoice);

    List<Invoice> downloadedInvoices();

    List<Invoice> getPaidInvoices(String startDate, String endDate);

    List<Invoice> downloadInvoices(String[] paymentModes);

    //boolean payInvoice(String tfPin, String paymentMode) throws Exception;

    void processTransactions();

    void updatePartnerFailedStatus();

    void payInvoice();

    void sendLockStatus();
}
