package com.sla.matercard.inbound.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity
@Table(name = "tbl_transactions")
public class Invoice {

    @CreationTimestamp
    private Timestamp createdOn;
    @CreationTimestamp
    @Column(name = "datetime_modified")
    private Timestamp datetimeModified;
    @Column(columnDefinition = "varchar(255) default 'NEW'", name = "transaction_status")
    private String transactionStatus;
    @Column(name = "incident_code")
    private String incidentCode;
    @Column(name = "incident_description")
    private String incidentDesc;
    @Column(name = "switch_response")
    private String switchResponse;
    @Column(name = "partner_process")
    private String partnerProcess;
    @JsonProperty("BankName")
    private String bankName;
    @JsonProperty("AgentBranchId")
    private String agentBranchId;
    private String receiverSecondLastNameUnicode;
    @JsonProperty("ReceiverPhoneMobile")
    private String receiverPhoneMobile;
    @JsonProperty("SenderStateId")
    private String senderStateId;
    @JsonProperty("PaymentModeName")
    private String paymentModeName;
    @JsonProperty("ReceiverCountryIsoCode")
    private String receiverCountryIsoCode;
    @JsonProperty("SenderPhoneMobile")
    private String senderPhoneMobile;
    @JsonProperty("CommAmountLocal")
    private String commAmountLocal;
    @JsonProperty("SenderCountryName")
    private String senderCountryName;
    @JsonProperty("SenderDateofBirth")
    private String senderDoB;
    @JsonProperty("SenderIDTypeDesc")
    private String senderIDTypeDesc;
    @JsonProperty("SenderIDType")
    private String senderIdType;
    @JsonProperty("SenderIDNumber")
    private String senderIDNumber;
    @JsonProperty("SenderIDDateExpiry")
    private String senderIdDateExpiry;
    @JsonProperty("SenderIsIndividual")
    private String senderIsIndividual;
    //    @JsonProperty("SenderId")
//    private String senderId;
    @JsonProperty("SourceOfFunds")
    private String senderSourceOfFunds;
    @JsonProperty("ReceiverIDType")
    private String receiverIDType;
    @JsonProperty("ReceiverIsIndividual")
    private String receiverIsIndividual;
    @JsonProperty("ReceiverIdNumber")
    private String receiverIdNumber;
    @JsonProperty("ReceiverStateName")
    private String receiverStateName;
    private String senderMiddleNameUnicode;
    @JsonProperty("Status")
    private String status;
    @JsonProperty("ReceiverAddress")
    private String receiverAddress;
    @JsonProperty("ReceiverLastName")
    private String receiverLastName;
    private String senderPhoneWork;
    @JsonProperty("StatusName")
    private String statusName;
    private String receiverLastNameUnicode;
    private String receiverPhoneWork;
    private String receiverAddressUnicode;
    @JsonProperty("SenderNationalityIsoCode")
    private String senderNationalityIsoCode;
    @JsonProperty("ReceiverFirstName")
    private String receiverFirstName;
    @JsonProperty("ReceiveAmount")
    private String receiveAmount;
    @JsonProperty("SenderStateName")
    private String senderStateName;
    private String receiverSecondLastName;
    @JsonProperty("PayoutBranchId")
    private String payoutBranchId;
    @JsonProperty("SendAmount")
    private String sendAmount;
    @JsonProperty("ReceiverMiddleName")
    private String receiverMiddleName;
    @JsonProperty("ReceiverFullName")
    private String receiverFullName;
    @JsonProperty("ExchangeRate")
    private String exchangeRate;
    @JsonProperty("SenderNationalityName")
    private String senderNationalityName;
    private String senderSecondLastNameUnicode;
    @JsonProperty("SenderAddress")
    private String senderAddress;
    private String senderPhoneHome;
    @JsonProperty("ReceiveCurrencyIsoCode")
    private String receiveCurrencyIsoCode;
    @JsonProperty("PaymentModeId")
    private String paymentModeId;
    @JsonProperty("SenderFullName")
    private String senderFullName;
    @JsonProperty("SenderCountryIsoCode")
    private String senderCountryIsoCode;
    @JsonProperty("SenderAddressUnicode")
    private String senderAddressUnicode;
    @JsonProperty("ReceiverDOB")
    private String receiverDOB;
    @JsonProperty("ReceiverCityId")
    private String receiverCityId;
    @JsonProperty("SenderCityId")
    private String senderCityId;
    @JsonProperty("ReceiverFullNameUnicode")
    private String receiverFullNameUnicode;
    @JsonProperty("ReceiverCityName")
    private String receiverCityName;
    @JsonProperty("SenderCityName")
    private String senderCityName;
//    @JsonProperty("SenderStateName")
//    private String senderStateProvince;



    @JsonProperty("TransactionDate")
    private String transactionDate;
    @JsonProperty("ReceiverStateId")
    private String receiverStateId;
    @JsonProperty("ReceiverNationalityIsoCode")
    private String receiverNationalityIsoCode;
    private String senderFullNameUnicode;
    @JsonProperty("CommAmountForeign")
    private String commAmountForeign;
    private String receiverFirstNameUnicode;
    private String bankAddress;
    private String receiverMiddleNameUnicode;
    private String receiverPhoneHome;
    @JsonProperty("ExpirationDateid")
    private String expirationDateId;
    @JsonProperty("ReceiverNationalityName")
    private String receiverNationalityName;
    @Id
    @Column(name = "tfPin")
    @JsonProperty("TfPin")
    private String tfPin;
    @JsonProperty("AccountNumber")
    private String accountNumber;
    @JsonProperty("PayeeBankID")
    private String payeeBankID;
    private String senderFirstNameUnicode;
    @JsonProperty("BankBranch")
    private String bankBranch;
    private String senderLastNameUnicode;
    @JsonProperty("ReceiverCountryName")
    private String receiverCountryName;
    @JsonProperty("PurposeOfRemittanceDesc")
    private String PurposeOfRemittanceDesc;
}