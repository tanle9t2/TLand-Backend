package com.tanle.tland.payment_service.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

@Component
public class VNPayUtils {

    @Value(value = "${payment.vnpay.secret-key}")
    public static String secretKey;

    @Value(value = "${payment.vnpay.init-payment-url}")
    public static String initPaymentPrefixUrl;

    @Value(value = "${payment.vnpay.return-url}")
    public static String returnUrlFormat;

    public static Boolean verifyIpn(Map<String, String> params) {
        var reqSecureHash = params.get(VNPayParams.SECURE_HASH);
        params.remove(VNPayParams.SECURE_HASH);
        params.remove(VNPayParams.SECURE_HASH_TYPE);
        var hashPayload = new StringBuilder();
        var fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);

        var itr = fieldNames.iterator();
        while (itr.hasNext()) {
            var fieldName = itr.next();
            var fieldValue = params.get(fieldName);
            if ((fieldValue != null) && (!fieldValue.isEmpty())) {
                //Build hash data
                hashPayload.append(fieldName);
                hashPayload.append("=");
                hashPayload.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));

                if (itr.hasNext()) {
                    hashPayload.append("&");
                }
            }
        }

        var secureHash = hmacSHA512(secretKey ,hashPayload.toString());
        return secureHash.equals(reqSecureHash);
    }

    public static String buildInitPaymentUrl(Map<String, String> params) {
        var hashPayload = new StringBuilder();
        var query = new StringBuilder();
        var fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);   // 1. Sort field names

        var itr = fieldNames.iterator();
        while (itr.hasNext()) {
            var fieldName = itr.next();
            var fieldValue = params.get(fieldName);
            if ((fieldValue != null) && (!fieldValue.isEmpty())) {
                // 2.1. Build hash data
                hashPayload.append(fieldName);
                hashPayload.append("=");
                hashPayload.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));

                // 2.2. Build query
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII));
                query.append("=");
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));

                if (itr.hasNext()) {
                    query.append("&");
                    hashPayload.append("&");
                }
            }
        }

        // 3. Build secureHash
        var secureHash = hmacSHA512(secretKey , String.valueOf(hashPayload));

        // 4. Finalize query
        query.append("&vnp_SecureHash=");
        query.append(secureHash);

        return initPaymentPrefixUrl + "?" + query;
    }

    public static String buildReturnUrl(String txnRef) {
        return String.format(returnUrlFormat);
    }

    public static String buildPaymentDetail(String orderId) {
        return String.format("Payment for order(s): %s", "x");
    }

    public static String hmacSHA512(final String key, final String data) {
        try {

            if (key == null || data == null) {
                throw new NullPointerException();
            }
            final Mac hmac512 = Mac.getInstance("HmacSHA512");
            byte[] hmacKeyBytes = key.getBytes();
            final SecretKeySpec secretKey = new SecretKeySpec(hmacKeyBytes, "HmacSHA512");
            hmac512.init(secretKey);
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            byte[] result = hmac512.doFinal(dataBytes);
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();

        } catch (Exception ex) {
            return "";
        }
    }
    @Value("${payment.vnpay.secret-key}")
    public void setSecretKey(String secretKey) {
        VNPayUtils.secretKey = secretKey;
    }

    @Value("${payment.vnpay.init-payment-url}")
    public void setInitPaymentPrefixUrl(String initPaymentPrefixUrl) {
        VNPayUtils.initPaymentPrefixUrl = initPaymentPrefixUrl;
    }

    @Value("${payment.vnpay.return-url}")
    public void setReturnUrlFormat(String returnUrlFormat) {
        VNPayUtils.returnUrlFormat = returnUrlFormat;
    }
}
