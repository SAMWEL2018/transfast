package com.sla.matercard.inbound.security;
import com.sla.matercard.inbound.Utility.AppConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import javax.crypto.Cipher;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;


@RequiredArgsConstructor
@Configuration
public class MasterCardSecurity {
    private final AppConfig appConfig;


    public String getAuthorizationToken(String apiUser, String apiPassword, String branchId)  {

        RSAPublicKey publicKey = getPublicKeyFromModulusExponent(appConfig.getRsaModulus(), appConfig.getRsaExponent());
        //PublicKey publicKeyGen = GetPublicKey(appConfig.getRsaModulus(), appConfig.getRsaExponent());

        String token = "" +
                "<Authentication>" +
                "<Id>" + appConfig.getSystemId() + "</Id>" +
                "<UserName>" + encryptData(apiUser, publicKey) + "</UserName>" +
                "<Password>" + encryptData(apiPassword, publicKey) + "</Password>" +
                "<BranchId>" + encryptData(branchId, publicKey) + "</BranchId>" +
                "</Authentication>";

        return Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));

    }

    public String encryptData(String data, RSAPublicKey publicKey) {

        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            return Base64.getEncoder().encodeToString(cipher.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public RSAPublicKey getPublicKeyFromModulusExponent(String modulus, String exponent) {

        try {
            RSAPublicKeySpec rsaPubKey = new RSAPublicKeySpec(
                    new BigInteger(1, Base64.getDecoder().decode(modulus)),
                    new BigInteger(1, Base64.getDecoder().decode(exponent))
            );

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) keyFactory.generatePublic(rsaPubKey);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


}
