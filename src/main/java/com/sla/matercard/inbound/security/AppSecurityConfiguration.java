package com.sla.matercard.inbound.security;

import com.sla.matercard.inbound.Utility.AppConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;

/**
 * @author samwel.wafula
 * Created on 11/03/2024
 * Time 09:41
 * Project moneytrans
 */
@RequiredArgsConstructor
@Component
@Slf4j
public class AppSecurityConfiguration {

    private final AppConfig config;

    public String encryptTransactionValidationHash(String trnRef, String partnerId, String trnType, String amount, String trnDate) {
        try {

            // {trnRef}:{partnerId}:{trnType}:{amount}:{trnDate}
            String message = trnRef + ":" + partnerId + ":" + trnType + ":" + amount + ":" + trnDate;

            byte[] bytKey = config.getBridgeHashKey().getBytes(StandardCharsets.UTF_8);
            String algorithm = "HmacSHA1";
            String base64Message = Base64.getEncoder().encodeToString(message.getBytes());
            byte[] bytMsg = base64Message.getBytes(StandardCharsets.UTF_8);

            Mac mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(bytKey, algorithm));

            byte[] result = mac.doFinal(bytMsg);
            HexFormat hexFormat = HexFormat.of();

            return hexFormat.formatHex(result).toLowerCase();

        } catch (Exception e) {
            log.error(e.getLocalizedMessage());
        }

        return "";
    }
}
