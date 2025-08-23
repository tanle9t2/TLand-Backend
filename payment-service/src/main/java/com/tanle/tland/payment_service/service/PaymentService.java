package com.tanle.tland.payment_service.service;

import com.tanle.tland.payment_service.PaymentRepo;
import com.tanle.tland.payment_service.entity.Payment;
import com.tanle.tland.payment_service.entity.PaymentStatus;
import com.tanle.tland.payment_service.request.InitPaymentRequest;
import com.tanle.tland.payment_service.response.InitPaymentResponse;
import com.tanle.tland.payment_service.response.IpnResponse;
import com.tanle.tland.payment_service.utils.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RequiredArgsConstructor
public class PaymentService {


    private final PaymentRepo paymentRepository;

    public static final String VERSION = "2.1.0";
    public static final String COMMAND = "pay";
    public static final String ORDER_TYPE = "190000";
    public static final long DEFAULT_MULTIPLIER = 100L;

    @Value("${payment.vnpay.tmn-code}")
    private String tmnCode;

    @Value("${payment.vnpay.timeout}")
    private Integer paymentTimeout; //minutes

    public InitPaymentResponse init(InitPaymentRequest request) {
        var amount = request.getAmount() * DEFAULT_MULTIPLIER;  // 1. amount * 100
        var txnRef = request.getTxnRef();                       // 2. registerId
        var returnUrl = VNPayUtils.buildReturnUrl(txnRef);                 // 3. FE redirect by returnUrl

        var vnCalendar = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        var createdDate = DateUtils.formatVnTime(vnCalendar);
        vnCalendar.add(Calendar.MINUTE, paymentTimeout);
        var expiredDate = DateUtils.formatVnTime(vnCalendar);    // 4. expiredDate for secure

        var ipAddress = request.getIpAddress();
        var orderInfo = VNPayUtils.buildPaymentDetail(txnRef);

        Map<String, String> params = new HashMap<>();

        params.put(VNPayParams.VERSION, VERSION);
        params.put(VNPayParams.COMMAND, COMMAND);

        params.put(VNPayParams.TMN_CODE, tmnCode);
        params.put(VNPayParams.AMOUNT, String.valueOf(amount));
        params.put(VNPayParams.CURRENCY, CurrencyUtils.VND.getValue());

        params.put(VNPayParams.TXN_REF, txnRef);
        params.put(VNPayParams.RETURN_URL, returnUrl);

        params.put(VNPayParams.CREATED_DATE, createdDate);
        params.put(VNPayParams.EXPIRE_DATE, expiredDate);

        params.put(VNPayParams.IP_ADDRESS, ipAddress);

        params.put(VNPayParams.LOCALE, LocaleUtils.VIETNAM.getCode());

        params.put(VNPayParams.ORDER_INFO, txnRef);
        params.put(VNPayParams.ORDER_TYPE, ORDER_TYPE);

        var initPaymentUrl = VNPayUtils.buildInitPaymentUrl(params);
        return InitPaymentResponse.builder()
                .vnpUrl(initPaymentUrl).build();
    }

    @Transactional
    public IpnResponse process(Map<String, String> params) {
        if (!VNPayUtils.verifyIpn(params)) {
            return VnIpnResponseUtils.SIGNATURE_FAILED;
        }
        String vnpResponseCode = params.get("vnp_ResponseCode");
        String registerId = params.get("vnp_TxnRef");
        Double amount = Double.parseDouble(params.get("vnp_Amount")) / 100;
//        Optional<Register> optionalRegister = registerRepository.findById(registerId);
//
//        if (optionalRegister.isEmpty())
//            return VnIpnResponseUtils.ORDER_NOT_FOUND;


        if (vnpResponseCode.equals("00")) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            LocalDateTime dateTime = LocalDateTime.parse(params.get("vnp_PayDate"), formatter);

//            Register register = optionalRegister.get();
            Payment payment = Payment
                    .builder()
                    .amount(amount)
                    .status(PaymentStatus.SUCCESS)
                    .createdAt(dateTime)
//                    .register(register)
                    .build();
            paymentRepository.save(payment);
            return VnIpnResponseUtils.SUCCESS;
        } else {

        }
        return VnIpnResponseUtils.UNKNOWN_ERROR;
    }
}
















