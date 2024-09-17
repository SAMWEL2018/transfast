package com.sla.matercard.inbound.service;

import com.sla.matercard.inbound.Repository.InvoiceRepository;
import com.sla.matercard.inbound.Utility.AppConfig;
import com.sla.matercard.inbound.http.HttpHandler;
import com.sla.matercard.inbound.model.BridgeRequest;
import com.sla.matercard.inbound.model.Invoice;
import com.sla.matercard.inbound.security.AppSecurityConfiguration;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author samwel.wafula
 * Created on 11/03/2024
 * Time 10:30
 * Project MoneyTrans
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BridgeService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final AppConfig config;
    private final HttpHandler httpHandler;
    private final InvoiceRepository invoiceRepository;
    private final AppSecurityConfiguration appSecurityConfiguration;
    EmailNotification emailNotification;
    @Lazy
    private final InvoiceServiceImpl invoiceService;
    //   private final EmailNotification emailNotification;


    public void pushTransactionToBridge(Invoice transaction) {
        try {
            if (!config.isAutoPushToBridge()) { //Push to bridge allowed or not
                log.info("auto push disabled !!");
                return;
            }

            BridgeRequest request = buildBridgeRequest(transaction);

            Map<String, Object> entries = new HashMap<>();
            entries.put("entries", List.of(request));

            String requestStr = objectMapper.writeValueAsString(entries);
            log.info("Bridge Request: " + requestStr);
            Mono<JsonNode> bridgeReponse = httpHandler.makeBridgeApiCall(
                    HttpMethod.POST,
                    config.getBridgeEndpoint() + "/api/v1/request",
                    requestStr);

            //Future processing
            bridgeReponse.subscribe(res -> {
                        try {
                            String response = objectMapper.writeValueAsString(res);
                            log.info("Bridge response :: " + response);
                            processBridgePostResponse(res, transaction.getTfPin(), transaction.getPaymentModeId());
                        } catch (JsonProcessingException e) {
                            log.error("Error Message : " + e.getMessage());
                            e.getMessage();
                        }
                    },
                    err -> {
                        String er = "Error Posting Tsn : " + transaction.getTfPin() + " to Bridge Error: " + err.getMessage();
                        if (!err.getMessage().contains("Connection refused")) {
                            try {
                                log.error("Posting Error Response :: " + err.getMessage());
                                //To Allow check for duplicate Push
                                processBridgePostResponse(objectMapper.readValue(err.getMessage(), JsonNode.class), transaction.getTfPin(), transaction.getPaymentModeId());
                            } catch (Exception e) {
                                log.error(e.getMessage(), e);
                            }

                            if (!err.getMessage().contains("0003")) { // Ignore duplicate error notification
                                //emailNotification.sendNotification(er);
                                log.info("");
                            }
                        } else {
                            log.error(er);
                        }
                    },
                    () -> log.info("Bridge POST request processed request successfully ")
            );

        } catch (Exception e) {
            log.error("error pushing to bridge {}", e.getMessage());
        }
    }

    public void queryTransactionStatusFromBridge(String ref) {

        if (!config.isAutoQueryFromBridge()) //Query enabled
            return;

        try {
            String queryEndpoint = config.getBridgeEndpoint()+"/api/query/status/" + config.getApplicationName() + "/" + ref;
            //String queryEn

            log.info("Request :: " + queryEndpoint);
            Mono<JsonNode> bridgeResponse = httpHandler.makeBridgeApiCall(
                    HttpMethod.GET,
                    queryEndpoint,
                    null);

            //Future processing
            bridgeResponse.subscribe(res -> {
                        try {
                            String response = objectMapper.writeValueAsString(res);
                            log.info("Bridge Query response  INCIDENT DESC: {} INCIDENT CODE: {} STATUS: {}  ", res.get("incident_description").asText(""), res.get("incident_code").asText(""), res.get("transaction_status").asText(""));
                            processBridgeQueryResponse(res, ref);

                        } catch (JsonProcessingException e) {
                            log.error("Error occurred on bridge Res {}", e.getMessage());
                        }
                    },
                    err -> {
                        //err.printStackTrace();

                        String er = "Error Querying Tsn : " + ref + " from Bridge Error: " + err.getMessage();
                        log.info(er);
                        //emailNotification.sendNotification(er);
                    },
                    () -> log.info("Bridge QUERY request response processed request successfully ")
            );
        } catch (Exception e) {
            log.error("Error on processing bridge Response ", e.getCause());
            //  emailNotification.sendNotification("Error Querying Transaction From Bridge :: " + ref + " :" + e.getMessage());
        }

    }

    public void queryTransactionStatusFromBridge() {
        List<Invoice> transactionEntities = invoiceRepository.getTransactionsByStatusToProcess("UPLOADED");
        transactionEntities.forEach(transaction -> {
            try {
                log.info("querying {} from bridge", transaction.getTfPin());
                queryTransactionStatusFromBridge(transaction.getTfPin());
            } catch (Exception e) {
                e.getCause();
            }
        });
    }

    private void processBridgeQueryResponse(JsonNode res, String transactionRef) {

        if (res != null) {
            if (!res.has("error")) {
                String incidentCode = res.get("incident_code").asText("");
                String incidentDesc = res.get("incident_description").asText("");
                String switchResponse = res.get("switch_response").asText();
                String queryStatus = res.get("query_status").asText("");
                String ref = res.get("transaction_ref").asText("");
                String status = res.get("transaction_status").asText("");

                if (status.equalsIgnoreCase("FAILED") || status.equalsIgnoreCase("COMPLETED")) {

                    invoiceRepository.updateTransactionFromFinalBridgeResponse(status.toUpperCase(), switchResponse, incidentCode, incidentDesc, ref, new Timestamp(System.currentTimeMillis()));
                    invoiceService.sendFinalStatus(status.toUpperCase(), transactionRef);
                } else {
                    log.info("Transaction {} still pending", transactionRef);

                }
                    /*
                    Add this point we can invoke UpdatePaidOrFailedStatus since we have reached the final status
                     */
            }
        } else { // In case transaction is missing return it to new so that it can be Pushed By Scheduled service
            JsonNode node = res.get("error");
            String code = node.get("code").asText("");
            if (code.equalsIgnoreCase("0076")) {
                invoiceRepository.updateTrnStatus(transactionRef, "LOCKED");
            }
        }
    }


    /**
     * Retry Pushing transaction to bridge
     */

    private void processBridgePostResponse(JsonNode res, String tsnRef, String paymentModeId) {
        if (res != null && res.has("response_code")) {
            String resCode = res.get("response_code").asText("");
            if (resCode.equalsIgnoreCase("200")) {
                String requestNumber = res.get("resource").get("request_number").asText("");
                String ref;
                try {
                    ref = res.get("extra_data").get(0).get(0).get("transaction_ref").asText();
                } catch (Exception e) {
                    ref = res.get("extra_data").get(0).get("transaction_ref").asText();
                }

                log.info("Transaction ref Update : " + ref);
                log.info("request number {}", requestNumber);
                if (ref != null && requestNumber != null) {
                    invoiceRepository.updateTrnStatus(ref, "UPLOADED");
                    log.info("Db updated");
                }
            } else if (resCode.equalsIgnoreCase("400")) {

                JsonNode node = res.get("errors");
                if (node != null && node.isArray()) {
                    String error = node.get(0).get("code").asText("");
                    if (error.contains("0003")) { //Transaction Exists
                        invoiceRepository.updateTrnStatus(tsnRef, "UPLOADED");
                    } else {
                        invoiceRepository.updateTransactionFromFinalBridgeResponse("UPLOAD-FAILED", res.asText(), "05", "Error", tsnRef, new Timestamp(System.currentTimeMillis()));
                        emailNotification.sendNotification("Error occurred on BridgeQuery for trn " + tsnRef + " with error " + node);

                    }
                }
            }
        }
    }


    private BridgeRequest buildBridgeRequest(Invoice trn) {


        String trnDate = strDateTo_yyyyMMdd(trn.getTransactionDate().substring(0, trn.getTransactionDate().length() - 6));
        String senderDob = strDateTo_yyyyMMdd(trn.getSenderDoB().substring(0, trn.getSenderDoB().length() - 6));
        String senderIdDateExpiry = trn.getSenderIdDateExpiry() != null?
                strDateTo_yyyyMMdd(trn.getSenderIdDateExpiry().substring(0, trn.getSenderIdDateExpiry().length() - 6)) : "";
        String partnerId = config.getApplicationName();
        String amount = trn.getReceiveAmount();
        String trnType = transactionType(trn.getPaymentModeId());
        String ref = trn.getTfPin();
        String hash = appSecurityConfiguration.encryptTransactionValidationHash(ref, partnerId, trnType, amount, trnDate);

        //Replace sender contact with receiver is receiver size is less than 10
        return BridgeRequest.builder()
                .partnerId(partnerId)
                .transactionRef(ref)
                .transactionDate(trnDate)
                .collectionBranch("")
                .transactionType(trnType)
                .senderType("P")
                .senderFullName(trn.getSenderFullName())
                .senderAddress(Optional.ofNullable(trn.getSenderAddress()).orElse(trn.getSenderCountryIsoCode()))
                .senderCity(Optional.ofNullable(trn.getSenderCityName()).orElse(""))
                .senderCountryCode(getSenderCountryCode(trn.getSenderCountryIsoCode()))
                .senderCurrencyCode("USD")
                .senderMobile(Optional.ofNullable(trn.getSenderPhoneMobile()).orElse(""))
                .senderDob(Optional.ofNullable(senderDob).orElse("").isEmpty() ? "19950101" : senderDob)
                .senderNationality(Optional.ofNullable(trn.getSenderNationalityIsoCode()).orElse(trn.getSenderCountryIsoCode()))
                .sendAmount(trn.getSendAmount())
                .senderIdType(Optional.ofNullable(trn.getSenderIDTypeDesc()).orElse("Passport"))
                .senderIdNumber(Optional.ofNullable(trn.getSenderIDNumber()).orElse(""))
                .senderIdIssueDate("20220701")
                .senderIdExpiryDate(Optional.ofNullable(senderIdDateExpiry).orElse("20300101"))
                .senderIdPlaceOfIssue("")
                .senderSourceOfFunds(Optional.ofNullable(trn.getSenderSourceOfFunds()).orElse(""))
                .senderStateProvince(Optional.ofNullable(trn.getSenderStateName()).orElse(""))
                .receiverStateProvince(Optional.ofNullable(trn.getReceiverStateName()).orElse(""))
                .receiverNationality(Optional.ofNullable(trn.getReceiverNationalityName()).orElse(""))
                .receiverType("P")
                .receiverCityId(Optional.ofNullable(trn.getReceiverCityId()).orElse(""))
                .receiverFullName(trn.getReceiverFullName())
                .receiverEmail("")
                .receiverCountryCode(Optional.ofNullable(trn.getReceiverCountryIsoCode()).orElse("KEN"))
                .receiverCurrencyCode(Optional.ofNullable(trn.getReceiveCurrencyIsoCode()).orElse("KES"))
                .receiverAmount(trn.getReceiveAmount())
                .receiverCity(Optional.ofNullable(trn.getReceiverCityName()).orElse("NAIROBI"))
                .receiverAddress(Optional.ofNullable(trn.getReceiverAddress()).orElse("Nairobi Kenya"))
                .receiverMobile(Optional.ofNullable(trn.getReceiverPhoneMobile()).orElse(""))
                .mobileOperator("")
                .receiverIdType(Optional.ofNullable(trn.getReceiverIDType()).orElse("0"))
                .receiverIdNumber(Optional.ofNullable(trn.getReceiverIdNumber()).orElse(""))
                .receiverAccount(Optional.ofNullable(trn.getAccountNumber()).orElse(trn.getReceiverPhoneMobile()))
                .receiverBank(Optional.ofNullable(trn.getBankName()).orElse(""))
                .receiverBankCode(Optional.ofNullable(trn.getPayeeBankID()).orElse(""))
                .receiverSwiftcode(Optional.ofNullable(trn.getBankBranch()).orElse(""))
                .receiverBranch(Optional.ofNullable(trn.getPayeeBankID()).orElse(""))
                .receiverBranchCode(Optional.ofNullable(trn.getBankBranch()).orElse(""))
                .exchangeRate(trn.getExchangeRate())
                .commissionAmount(trn.getCommAmountLocal())
                .paymentModeId(trn.getPaymentModeId())
                .paymentModeName(Optional.ofNullable(trn.getPaymentModeName()).orElse(""))
                .channel(transactionChannel(trn.getPaymentModeId()))
                .remarks(Optional.ofNullable(trn.getPurposeOfRemittanceDesc()).orElse("FAMILY_SUPPORT"))
                .callbacks("")
                .hash(hash)
                .build();
    }

    /**
     * Mapping transaction status to partner equivalent
     *
     * @param senderCountryCode bridge transaction status
     * @return GCC remit equivalent status
     */

    private String getSenderCountryCode(String senderCountryCode) {
        switch (senderCountryCode) {
            case "KE", "KEN" -> {
                return "US";
            }
            default -> {
                return senderCountryCode;
            }
        }
    }

    private String transactionChannel(String paymentModeId) {
        switch (paymentModeId) {
            case "G", "8" -> {
                return "MOBILE";
            }
            case "2" -> {
                return "CASH PICKUP";
            }
            case "C" -> {
                return "BANK";
            }
            default -> {
                return paymentModeId;
            }

        }


    }


    /**
     * Maps transaction types to Bridge Type
     *
     * @param transType id GCCREMIT Transaction type
     * @return bridge acceptable transaction type
     */
    private String transactionType(String transType) {
        switch (transType) {
            case "G", "8" -> {
                return "M";
            }
            case "2" -> {
                return "C";
            }
            case "C" -> {
                return "B";
            }

        }
        return transType;
    }

    /**
     * Handling multiple date formats
     *
     * @param date string
     * @return date string of format {yyyyMMdd}
     */
    public String strDateTo_yyyyMMdd(String date) {
        System.out.println("Date to Parse : " + date);
        // 2023-11-04T15:14:33.443
        try {
            String[] formats = {"yyyy-MM-dd'T'HH:mm:ss.SSSZ",
                    "yyyy-MM-dd HH:mm:ss.SSSSSSS",
                    "yyyy-MM-dd'T'HH:mm:ss.SSS",
                    "yyyy-MM-dd HH:mm:ss.SSS",
                    "yyyy-MM-dd",
                    "dd MMM yyyy",
                    "dd-MMM-yyyy",
                    "dd/MM/yyyy"
            };
            Date da = DateUtils.parseDate(date, formats);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            return sdf.format(da);
        } catch (Exception e) {
            log.warn("Date conversion error {} : Date Provided {}", e.getMessage(), date);
        }
        return "";
    }

}
