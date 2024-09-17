package com.sla.matercard.inbound.service;

import com.sla.matercard.inbound.Repository.InvoiceRepository;
import com.sla.matercard.inbound.Utility.AppConfig;
import com.sla.matercard.inbound.http.HttpHandler;
import com.sla.matercard.inbound.model.Invoice;
import com.sla.matercard.inbound.model.Invoices;
import com.sla.matercard.inbound.model.PayInvoice;
import com.sla.matercard.inbound.security.MasterCardSecurity;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;

import java.util.*;

/**
 * @author samwel.wafula
 * Created on 18/06/2024
 * Time 10:11
 * Project Transfast
 */
@Service
@Slf4j
public class InvoiceServiceImpl implements InvoiceService {
    private final HttpHandler httpHandler;
    private final AppConfig appConfig;
    private final MasterCardSecurity masterCardSecurity;
    private final InvoiceRepository invoiceRepository;
    private final BridgeService bridgeService;

    public InvoiceServiceImpl(HttpHandler httpHandler, AppConfig appConfig, MasterCardSecurity masterCardSecurity,
                              InvoiceRepository invoiceRepository, @Lazy BridgeService bridgeService) {
        this.httpHandler = httpHandler;
        this.appConfig = appConfig;
        this.masterCardSecurity = masterCardSecurity;
        this.invoiceRepository = invoiceRepository;
        this.bridgeService = bridgeService;
    }

    /**
     * Process of transaction processing
     * 1. Download Invoices (UNPAID)
     * 2. Check Invoice Status to ensure it's okay to process
     * 3. Lock the invoice on the Mastercard side to prevent other partners from processing the invoice
     * 4. Push the transaction to the Gateway (global API) for termination
     */
    @Override
    public void processTransactions() {
        try {

            // Step 1
            if (appConfig.isDownloadInvoicesEnabled()) {
                downloadAndSaveNewInvoices();
            }
            // The below functionality is to get new transactions, check their status and then push to bridge
            if (appConfig.isAutoProcessDownloadedInvoiceEnabled()) {
                List<Invoice> invoiceList = invoiceRepository.getTransactionsByStatusAndPartnerProcessToProcess("NEW", "NEW");
                log.info("Invoice to process and push {}", invoiceList.size());
                List<Invoice> validInvoices = new ArrayList<>();
                for (Invoice invoice : invoiceList) {
                    //Step 2 and 3
                    if (validateNotPaidAndLockInvoice(invoice) != null) {
                        validInvoices.add(invoice);
                    }
                    log.info("Number of Valid Invoices for Processing after Lock : {} ", validInvoices.size());
                }
                // The below functionality is to get transactions that status have already been checked and have not yet been pushed to Bridge, also gets pushed to bridge
                // This situation may occur when a transaction is set to UPLOADED and yet not in Bridge, upon querying the status from Bridge it will be set back to NEW TO AS to pe repushed
                List<Invoice> invoiceListToPush = invoiceRepository.getTransactionsByStatusAndPartnerProcessToProcess("LOCKED", "NEW");
                for (Invoice invoice : invoiceListToPush) {
                    //pushTransactionToBridge
                    bridgeService.pushTransactionToBridge(invoice);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public Invoice validateNotPaidAndLockInvoice(Invoice invoice) {
        try {
            String partnerStatus = getInvoiceStatusFromMastercard(invoice);

            if (partnerStatus != null && partnerStatus.equalsIgnoreCase("NOTPAID")) {
                invoiceRepository.updatePartnerProcessStatus(invoice.getTfPin(), "NOTPAID");
                if (appConfig.isSendLockEnabled()) {
                    if (lockTransaction(invoice)) {
                        return invoice;
                    }
                }
            } else {
                invoiceRepository.updateTrnStatusAndPartnerStatus(invoice.getTfPin(), "COMPLETED", partnerStatus);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public List<Invoice> downloadAndSaveNewInvoices() {
        try {
            List<Invoice> invoices = downloadedInvoices();
            if (!invoices.isEmpty()) {
                log.info("Downloaded invoices: {}", new ObjectMapper().writeValueAsString(invoices));
                for (Invoice invoice : invoices) {
                    try {
                        Optional<Invoice> v = invoiceRepository.findById(invoice.getTfPin());
                        if (v.isEmpty()) {
                            log.info("NEW transaction {}", invoice.getTfPin());
                            invoice.setTransactionStatus("NEW");
                            invoice.setPartnerProcess("NEW");
                            invoiceRepository.save(invoice);
                        } else {
                            log.error("Transaction already exists");
                        }
                    } catch (Exception e) {
                        log.error("Error Occurred on processing {}", e.getMessage());
                    }
                }
            }
            return invoices;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return new ArrayList<>();
    }

    @Override
    public List<Invoice> downloadedInvoices() {
        String[] paymentModes = appConfig.getPaymentMode().split(",");
        return downloadInvoices(paymentModes);
    }

    @Override
    public List<Invoice> downloadInvoices(String[] paymentModes) {

        List<Invoice> invoices = new ArrayList<>();
        log.info("Download Invoice for Payment Modes {}", paymentModes);
        for (String paymentMode : paymentModes) {
            try {
                if (paymentMode.equals("G") || paymentMode.equals("C") || paymentMode.equals("8")) {
                    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
                    params.add("ModeOfPayment", paymentMode);
                    //params.add("IncludeDownloadedInvoices", String.valueOf(true));
                    String token = token(paymentMode);
                    JsonNode data = httpHandler.sendSyncCallWithoutBody(appConfig.getMasterCardEndpoint() + "api/invoice/downloadedinvoices", HttpMethod.PUT, params, token);
                    Invoices invoice = new ObjectMapper().convertValue(data, new TypeReference<>() {
                    });
                    if (invoice != null) {
                        invoices.addAll(invoice.getInvoices());
                    }
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }

        log.info("Invoices length : {}", invoices.size());
        return invoices;

    }

    /**
     * Input format("07/18/2024","07/19/2024");
     *
     * @param startDate => MM/dd/YYYY
     * @param endDate   => MM/dd/YYYY
     * @return list of paid invoices
     */
    @Override
    public List<Invoice> getPaidInvoices(String startDate, String endDate) {
        String[] paymentModes = appConfig.getPaymentMode().split(",");
        List<Invoice> invoices = new ArrayList<>();
        try {
            for (String paymentMode : paymentModes) {
                if (paymentMode.equals("G") || paymentMode.equals("C")) {
                    log.info("Download Invoice for Payment Mode {}", paymentMode);
                    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
                    params.add("StartDate", startDate);
                    params.add("EndDate", endDate);
                    log.info("Params {}", params);
                    String token = token(paymentMode);
                    JsonNode data = httpHandler.sendSyncCallWithoutBody(appConfig.getMasterCardEndpoint() + "api/invoice/paidinvoices", HttpMethod.GET, params, token);
                    Invoices invoice = new ObjectMapper().convertValue(data, new TypeReference<>() {
                    });
                    invoices.addAll(invoice.getInvoices());
                }
            }
            log.info("Invoices length : {} Data: {} ", invoices.size(), new ObjectMapper().writeValueAsString(invoices));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return invoices;
    }

    public String getInvoiceStatusFromMastercard(Invoice invoice) {
        try {
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("tfpin", invoice.getTfPin());
            JsonNode jsonNode = getInvoiceStatusFromMastercard(invoice.getTfPin(), invoice.getPaymentModeId());
            return jsonNode.get("Status").asText();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    public JsonNode getInvoiceStatusFromMastercard(String tfPin) {
        try {
            Invoice invoice = invoiceRepository.getInvoiceByRef(tfPin);
            if (invoice != null) {
                return getInvoiceStatusFromMastercard(tfPin, invoice.getPaymentModeId());
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
        return null;
    }

    public JsonNode getInvoiceStatusFromMastercard(String tfPin, String paymentModeId) {
        try {
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("tfpin", tfPin);

            MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            headers.add("Authorization", token(paymentModeId));

            log.info("Invoice Valid for Processing Status check TfPin: {}, PaymentMode {}", tfPin, paymentModeId);
            JsonNode jsonNode = httpHandler.sendSyncCallWithoutBody(HttpMethod.GET, appConfig.getMasterCardEndpoint() + "api/invoice/invoicestatus", params, headers);
            log.info("Invoice status Response {}", jsonNode);
            log.info("Invoice Valid for Processing Status check TfPin: {} Response: {} ", tfPin, jsonNode.asText());
            return jsonNode;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    @Override
    public void payInvoice() {

        List<Invoice> completedTransactions = invoiceRepository.getTransactionsByStatusAndPartnerProcessToProcess("LOCKED", "COMPLETED");
        if (!completedTransactions.isEmpty()) {
            for (Invoice invoice : completedTransactions) {
                payInvoice(invoice);
            }
        } else {
            log.info("-----No transaction to send complete status------");
        }
    }

    public String token(String paymentModeId) {
        String token = "";
        switch (paymentModeId) {
            case "G", "8" -> {
                token = masterCardSecurity.getAuthorizationToken(appConfig.getMobileApiUser(), appConfig.getMobileApiPassword(), appConfig.getMobileBranchId());
            }
            case "C" -> {
                token = masterCardSecurity.getAuthorizationToken(appConfig.getBankApiUser(), appConfig.getBankApiPassword(), appConfig.getBankBranchId());
            }
        }
        return token;
    }

    @Override
    public void updatePartnerFailedStatus() {

        List<Invoice> failedTransactions = invoiceRepository.getTransactionsByStatusAndPartnerProcessToProcess("LOCKED", "FAILED");
        if (!failedTransactions.isEmpty()) {
            for (Invoice v : failedTransactions) {
                updatePartnerFailedStatus(v);
            }

        } else {
            log.info("-------No transaction to send Failed status-----");
        }
    }

    public void sendLockStatus() {
        List<Invoice> lockedTransactions = invoiceRepository.getTransactionsByStatusAndPartnerProcessToProcess("NOTPAID", "NEW");
        if (!lockedTransactions.isEmpty()) {
            for (Invoice v : lockedTransactions) {
                lockTransaction(v);
            }
        } else {
            log.info("-------------No transaction found to send lock status----------");

        }
    }

    public boolean lockTransaction(Invoice invoice) {
        try {
            Map<String, String> data = new LinkedHashMap<>();
            data.put("ErrorCode", "ER051");
            data.put("Description", "In process to be deposited");
            data.put("TfPin", invoice.getTfPin());

            log.info("Lock object {}", new ObjectMapper().writeValueAsString(data));
            ClientResponse clientResponse = httpHandler.sendSynchronousInvoiceLock(appConfig.getMasterCardEndpoint() + "api/customercare/complaints", HttpMethod.POST, token(invoice.getPaymentModeId()), data);
            log.info("Lock response {} {}", invoice.getTfPin(), clientResponse.statusCode());
            if (clientResponse.statusCode().value() == 201 || clientResponse.statusCode().value() == 200) {
                invoiceRepository.updatePartnerProcessStatus(invoice.getTfPin(), "LOCKED");
                return true;
            } else {
                JsonNode jsonNode = clientResponse.bodyToMono(JsonNode.class).block();
                log.info("Transaction {} not Locked. Response:  {} status Code {}", invoice.getTfPin(), jsonNode, clientResponse.statusCode().value());
            }
        } catch (Exception e) {
            log.error("Error Locking transaction {}", e.getMessage());
        }
        return false;

    }

    public void updatePartnerFailedStatus(String ref) {
        Invoice v = invoiceRepository.getInvoiceByRef(ref);
        updatePartnerFailedStatus(v);

    }

    public void updatePartnerFailedStatus(Invoice v) {
        try {
            Map<String, String> data = new LinkedHashMap<>();
            if (v.getIncidentCode().equals("ERO26")) {
                data.put("ErrorCode", v.getIncidentCode());
                data.put("Description", "Invalid Beneficiary Name");
            } else {
                data.put("ErrorCode", "ER0102");
                data.put("Description", "Transaction Failed  or rejected");
            }
            data.put("TfPin", v.getTfPin());
            log.info("Failed Trans {} Request body {}", v.getTfPin(), new ObjectMapper().writeValueAsString(data));

            Mono<ClientResponse> response = httpHandler.sendAsyncRequest(appConfig.getMasterCardEndpoint() + "api/customercare/complaints", HttpMethod.POST, token(v.getPaymentModeId()), data);
            response.subscribe(clientResponse -> {
                        log.info("HTTP response code  {} on Failed invoice Body {}", clientResponse.statusCode(),
                                clientResponse.toEntity(String.class));
                        if (clientResponse.statusCode().is2xxSuccessful()) {
                            log.info("Updating transaction to UNLOCKED transaction ref {}", v.getTfPin());
                            invoiceRepository.updatePartnerProcessStatus(v.getTfPin(), "UNLOCKED");
                        }
                    },
                    err -> {
                        log.info("Transaction {} Failed status not send", v.getTfPin());
                        log.error(err.getMessage(), err);
                    },
                    () -> log.info("Completed Posting your failed trans")
            );
        } catch (Exception e) {
            log.error("Error in sending Failed request {}", e.getMessage());
        }

    }

    public JsonNode getComplaints(String ref) {
        Invoice invoice = invoiceRepository.getInvoiceByRef(ref);
        if (invoice != null) {
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("tfpin", invoice.getTfPin());
            JsonNode jsonNode = httpHandler.sendSyncRequest(appConfig.getMasterCardEndpoint() + "api/customercare/complaints", HttpMethod.GET, token(invoice.getPaymentModeId()), params);
            log.info("Complain get req {}", jsonNode);
            return jsonNode;
        } else {
            log.error("Invoice {} does not exist ", ref);
            return null;

        }
    }

    public JsonNode getComplaints(String ref, String modeOfPayment) {


        try {
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("tfpin", ref);
            JsonNode jsonNode = httpHandler.sendSyncRequest(appConfig.getMasterCardEndpoint() + "api/customercare/complaints", HttpMethod.GET, token(modeOfPayment), params);
            log.info("Complain get req {}", jsonNode);
            return jsonNode;
        } catch (Exception e) {
            log.error("Error Occurred in get status of failed transaction {}", e.getMessage());
            return null;
        }


    }


    public void sendFinalStatus(String transactionStatus, String tsnRef) {
        log.info("sending final status {} for {}", transactionStatus, tsnRef);
        if (transactionStatus != null) {
            switch (transactionStatus) {
                case "FAILED" -> updatePartnerFailedStatus(tsnRef);
                case "COMPLETED" -> payInvoice(tsnRef);
            }
        }
    }

    public void payInvoice(String tsnRef) {
        Invoice invoice = invoiceRepository.getInvoiceByRef(tsnRef);
        payInvoice(invoice);
    }

    public void payInvoice(Invoice invoice) {
        try {
            PayInvoice payInvoice = PayInvoice.builder()
                    .TfPin(invoice.getTfPin())
                    .IdNumber(invoice.getReceiverIdNumber())
                    .IdType(Optional.ofNullable(invoice.getReceiverIDType()).orElse("172"))
                    .IdExpirationDate("2030-12-31T00:00:00+03:00")
                    .IdDateOfIssue("2018-01-31T00:00:00+03:00")
                    .ReceiverAddress(Optional.ofNullable(invoice.getReceiverAddress()).orElse("UPESI"))
                    .ReceiverCountryIsoCode(Optional.ofNullable(invoice.getReceiverCountryIsoCode()).orElse("KE"))
                    .ReceiverStateId(Optional.ofNullable(invoice.getReceiverStateId()).orElse("NAIROBI"))
                    .ReceiverCityId(Optional.ofNullable(invoice.getReceiverCityId()).orElse("61359"))
                    .ReceiverPhoneMobile(Optional.of(invoice.getReceiverPhoneMobile()).orElse(""))
                    .ReceiverDoB("01-01-1990")
                    .ReceiverGender("M")
                    .FormOfPaymentId(invoice.getPaymentModeId().equals("G") ? appConfig.getMobileFormOfPayment() : appConfig.getBankFormOfPayment())
                    .ProofOfAddressCollected("TRUE")
                    .KYCVerified("TRUE")
                    .build();

            log.info("Pay Invoice {}", new ObjectMapper().writeValueAsString(payInvoice));
            Mono<ClientResponse> response = httpHandler.sendAsyncRequest(appConfig.getMasterCardEndpoint() + "api/invoice/payinvoice", HttpMethod.PUT,
                    token(invoice.getPaymentModeId()), payInvoice);
            log.info("Pay invoice {} response {}", invoice.getTfPin(), new ObjectMapper().writeValueAsString(response));

            response.subscribe(clientResponse -> {
                        log.info("HTTP response code  {} on Pay invoice ", clientResponse.statusCode());
                        if (clientResponse.statusCode().is2xxSuccessful()) {
                            log.info("Updating transaction to UNLOCKED transaction ref {}", invoice.getTfPin());
                            invoiceRepository.updatePartnerProcessStatus(invoice.getTfPin(), "UNLOCKED");
                        }
                    },
                    err -> log.error(err.getMessage(), err),
                    () -> log.info("Completed Posting your")
            );
        } catch (Exception e) {
            log.error("Error occurred on Sending PayInvoice status {}", e.getMessage());
        }
    }
}

