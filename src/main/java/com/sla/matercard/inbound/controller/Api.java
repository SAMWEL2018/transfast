package com.sla.matercard.inbound.controller;

import com.sla.matercard.inbound.service.InvoiceServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Joseph Kibe
 * Created on July 29, 2024.
 * Time 2:59 PM
 */

@RestController
@Slf4j
public class Api {

    private final InvoiceServiceImpl invoiceService;

    public Api(InvoiceServiceImpl invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping(value = "/api/v1/transaction/status/{tfPin}")
    public ResponseEntity<Object> getTransactionFromMastercard(@PathVariable("tfPin") String tfPin) {

        JsonNode node = invoiceService.getInvoiceStatusFromMastercard(tfPin);
        if (node != null) {
            return ResponseEntity.ok(node);
        } else {
            return ResponseEntity.badRequest().body("Not found");
        }
    }

    @GetMapping(value = "/api/v1/transaction/status/{tfPin}/{modeOfPayment}")
    public ResponseEntity<Object> getTransactionFromMastercard(@PathVariable("tfPin") String tfPin,
                                                               @PathVariable("modeOfPayment") String modeOfPayment) {

        JsonNode node = invoiceService.getInvoiceStatusFromMastercard(tfPin, modeOfPayment);
        if (node != null) {
            return ResponseEntity.ok(node);
        } else {
            return ResponseEntity.badRequest().body("Not found");
        }
    }

    @GetMapping(value = "/api/v1/transaction/complaint/{tfpin}")
    public ResponseEntity<Object> getTransactionComplaint(@PathVariable("tfpin") String tfPin) {
        try {
            return ResponseEntity.status(200).body(invoiceService.getComplaints(tfPin));
        } catch (Exception e) {
            log.error("Error occurred in getting complaints {}", e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping(value = "/api/v1/transaction/complaints/{tfpin}/{modeOfPayment}")
    public ResponseEntity<?> getTransactionFailed(@PathVariable("tfpin") String tfPin, @PathVariable("modeOfPayment") String modeOfPayment) {
        try {
            return ResponseEntity.status(200).body(invoiceService.getComplaints(tfPin, modeOfPayment));
        } catch (Exception e) {
            log.error("Error occurred in getting complaints {}", e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

}
