package com.sla.matercard.inbound.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;


@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BridgeRequest {

    @JsonProperty("partner_id")
    private String partnerId;
    @JsonProperty("transaction_date")
    private String transactionDate;
    @JsonProperty("transaction_ref")
    private String transactionRef;
    @JsonProperty("transaction_type")
    private String transactionType;
    @JsonProperty("collection_branch")
    private String collectionBranch;
    @JsonProperty("payment_mode_name")
    private String paymentModeName;
    @JsonProperty("payment_mode_id")
    private String paymentModeId;

    @JsonProperty("send_amount")
    private String sendAmount;
    @JsonProperty("sender_address")
    private String senderAddress;
    @JsonProperty("sender_city")
    private String senderCity;
    @JsonProperty("sender_country_code")
    private String senderCountryCode;
    @JsonProperty("sender_currency_code")
    private String senderCurrencyCode;
    @JsonProperty("sender_dob")
    private String senderDob;
    @JsonProperty("sender_full_name")
    private String senderFullName;
    @JsonProperty("sender_id_expiry_date")
    private String senderIdExpiryDate;
    @JsonProperty("sender_id_issue_date")
    private String senderIdIssueDate;
    @JsonProperty("sender_id_number")
    private String senderIdNumber;
    @JsonProperty("sender_id_place_of_issue")
    private String senderIdPlaceOfIssue;
    @JsonProperty("sender_id_type")
    private String senderIdType;
    @JsonProperty("sender_mobile")
    private String senderMobile;
    @JsonProperty("sender_nationality")
    private String senderNationality;
    @JsonProperty("sender_type")
    private String senderType;
    @JsonProperty("sender_source_of_funds")
    private String senderSourceOfFunds;
    @JsonProperty("sender_state_province")
    private String senderStateProvince;

    @JsonProperty("receiver_state_province")
    private String receiverStateProvince;
    @JsonProperty("receiver_nationality")
    private String receiverNationality;
    @JsonProperty("receiver_account")
    private String receiverAccount;
    @JsonProperty("receiver_address")
    private String receiverAddress;
    @JsonProperty("receiver_amount")
    private String receiverAmount;
    @JsonProperty("receiver_bank")
    private String receiverBank;
    @JsonProperty("receiver_bank_code")
    private String receiverBankCode;
    @JsonProperty("receiver_branch")
    private String receiverBranch;
    @JsonProperty("receiver_branch_code")
    private String receiverBranchCode;
    @JsonProperty("receiver_city")
    private String receiverCity;
    @JsonProperty("receiver_city_id")
    private String receiverCityId;
    @JsonProperty("receiver_country_code")
    private String receiverCountryCode;
    @JsonProperty("receiver_currency_code")
    private String receiverCurrencyCode;
    @JsonProperty("receiver_email")
    private String receiverEmail;
    @JsonProperty("receiver_full_name")
    private String receiverFullName;
    @JsonProperty("receiver_id_number")
    private String receiverIdNumber;
    @JsonProperty("receiver_id_type")
    private String receiverIdType;
    @JsonProperty("receiver_mobile")
    private String receiverMobile;
    @JsonProperty("receiver_swiftcode")
    private String receiverSwiftcode;
    @JsonProperty("receiver_type")
    private String receiverType;

    @JsonProperty("mobile_operator")
    private String mobileOperator;
    @JsonProperty("commission_amount")
    private String commissionAmount;
    @JsonProperty("exchange_rate")
    private String exchangeRate;

    @JsonProperty("channel")
    private String channel;
    @JsonProperty("remarks")
    private String remarks;
    @JsonProperty("callbacks")
    private String callbacks;
    @JsonProperty("hash")
    private String hash;


}
