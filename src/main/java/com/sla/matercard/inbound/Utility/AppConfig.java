package com.sla.matercard.inbound.Utility;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * @author samwel.wafula
 * Created on 18/06/2024
 * Time 09:55
 * Project Transfast
 */
@Getter
@Setter
@Configuration
public class AppConfig {

    @Value("${app.SYSTEM_ID}")
    private String systemId;
    @Value("${app.mobile_branch_id}")
    private String mobileBranchId;
    @Value("${app.RSA_EXPONENT}")
    private String rsaExponent;
    @Value("${app.RSA_MODULUS}")
    private String rsaModulus;
    @Value("${app.mobile_api_user}")
    private String mobileApiUser;
    @Value("${app.mobile_api_password}")
    private String mobileApiPassword;
    @Value("${app.mobile_form_of_payment}")
    private String mobileFormOfPayment;

    @Value("${app.bank_api_user}")
    private String bankApiUser;
    @Value("${app.bank_api_password}")
    private String bankApiPassword;
    @Value("${app.bank_form_of_payment}")
    private String bankFormOfPayment;
    @Value("${app.bank_branch_id}")
    private String bankBranchId;
    @Value("${app.MASTERCARD_ENDPOINT}")
    private String masterCardEndpoint;
    @Value("${bridge.endpoint}")
    private String bridgeEndpoint;
    @Value("${bridge.api_key}")
    private String bridgeApiKey;
    @Value("${bridge.api_password}")
    private String bridgeApiPassword;
    @Value("${bridge.hash_key}")
    private String bridgeHashKey;
    @Value("${app.auto_push_bridge}")
    private boolean isAutoPushToBridge;
    @Value("${app.auto_query_bridge}")
    private boolean isAutoQueryFromBridge;
    @Value("${app.auto_download_invoices}")
    private boolean isDownloadInvoicesEnabled;
    @Value("${app.auto_pull_invoice}")
    private boolean isAutoPullInvoice;
    @Value("${app.auto_process_downloaded_invoice}")
    private boolean isAutoProcessDownloadedInvoiceEnabled;
    @Value("${app.auto_push_final_status}")
    private boolean isAutoSendFinalStatus;
    @Value("${app.application_name}")
    private String applicationName;
    @Value("${app.auto_send_lock}")
    private boolean isSendLockEnabled;
    @Value("${app.auto_send_scheduled_lock}")
    private boolean isScheduledLockEnabled;
    @Value("${app.payment_mode}")
    private String paymentMode;
    @Value("${app.send_to_tech_ops}")
    private String sendTechOps;
    @Value("${app.send_to_ops}")
    private String sendOps;
    @Value("${app.smtp_host}")
    private String smtpHost;
    @Value("${app.username}")
    private String smtpUsername;
    @Value("${app.password}")
    private String smtpPassword;

}
