package com.sla.matercard.inbound.service;

import com.sla.matercard.inbound.Utility.AppConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * @author samwel.wafula
 * Created on 18/06/2024
 * Time 10:38
 * Project Transfast
 */
@RequiredArgsConstructor
@Service
@Slf4j
@EnableScheduling
public class SchedulerService {
    private final AppConfig appConfig;
    private final InvoiceService invoiceService;
    private final BridgeService bridgeService;

    @Scheduled(fixedDelayString = "${app.fetch_payable_rate}")
    public void initGetInvoiceToProcess() {
        if (appConfig.isAutoPullInvoice()) {
            try {
                invoiceService.processTransactions();
            } catch (Exception e) {
                log.error("error in downloading Invoice {}", e.getMessage(), e);
            }

        }
    }

    @Scheduled(fixedDelayString ="${app.mastercard_cleanup_rate}")
    public void sendFinalStatusToPartner() {
        if (appConfig.isAutoSendFinalStatus()) {
            try {
                invoiceService.payInvoice();
                invoiceService.updatePartnerFailedStatus();
            } catch (Exception e) {
                log.error("Exception in sending Final status {}", e.getMessage(), e);

            }
        }
    }

    @Scheduled(fixedDelayString ="${app.mastercard_cleanup_rate}")
    public void SendLockedStatusToMastercard() {
        if (appConfig.isScheduledLockEnabled()) {
            invoiceService.sendLockStatus();
        }
    }

    @Scheduled(fixedDelayString = "20000")
    public void queryTransactionStatusFromBridge() {
        if (appConfig.isAutoQueryFromBridge()) {
            bridgeService.queryTransactionStatusFromBridge();
        }

    }

}
