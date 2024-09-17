package com.sla.matercard.inbound.model;

import lombok.*;

/**
 * @author samwel.wafula
 * Created on 24/06/2024
 * Time 14:56
 * Project Transfast
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PayInvoice {

    private String TfPin;
    private String IdNumber;
    private String IdType;
    private String IdExpirationDate;
    private String IdDateOfIssue;
    private String ReceiverAddress;
    private String ReceiverCountryIsoCode;
    private String ReceiverStateId;
    private String ReceiverCityId;
    private String ReceiverPhoneMobile;
    private String ReceiverDoB;
    private String ReceiverGender;
    private String FormOfPaymentId;
    private String ProofOfAddressCollected;
    private String KYCVerified;
}
