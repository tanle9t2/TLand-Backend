package com.tanle.tland.payment_service.service.impl;

import com.tanle.tland.payment_service.repository.PaymentRepo;
import com.tanle.tland.payment_service.entity.Payment;
import com.tanle.tland.payment_service.entity.PaymentStatus;
import com.tanle.tland.payment_service.entity.PurposeType;
import com.tanle.tland.payment_service.entity.TransactionType;
import com.tanle.tland.payment_service.event.PaymentEvent;
import com.tanle.tland.payment_service.grpc.PaymentServiceGrpc;
import com.tanle.tland.payment_service.grpc.PaymentUrlRequest;
import com.tanle.tland.payment_service.grpc.PaymentUrlResponse;
import com.tanle.tland.payment_service.kafka.PaymentPublisher;
import com.tanle.tland.payment_service.request.PaymentRequest;
import com.tanle.tland.payment_service.response.InitPaymentResponse;
import com.tanle.tland.payment_service.response.IpnResponse;
import com.tanle.tland.payment_service.service.PaymentService;
import com.tanle.tland.payment_service.utils.*;
import io.grpc.stub.StreamObserver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.tanle.tland.payment_service.utils.AppConstant.PRICE_CREATED;

@RequiredArgsConstructor
@GrpcService
@Slf4j
public class PaymentServiceImpl extends PaymentServiceGrpc.PaymentServiceImplBase implements PaymentService {


    private final PaymentRepo paymentRepository;
    private final PaymentPublisher paymentPublisher;

    public static final String VERSION = "2.1.0";
    public static final String COMMAND = "pay";
    public static final String ORDER_TYPE = "190000";
    public static final long DEFAULT_MULTIPLIER = 100L;

    @Value("${payment.vnpay.tmn-code}")
    private String tmnCode;

    @Value("${payment.vnpay.timeout}")
    private Integer paymentTimeout;

    public void getPaymentUrl(PaymentUrlRequest request, StreamObserver<PaymentUrlResponse> responseObserver) {
        var amount = request.getAmount() * DEFAULT_MULTIPLIER;
        var txnRef = request.getTxnRef();
        var ipAddress = request.getIpAddress();

        log.info("Request info - txnRef: {}, amount: {}, ip: {}", txnRef, amount, ipAddress);

        var returnUrl = VNPayUtils.buildReturnUrl(txnRef);
        log.info("Return URL: {}", returnUrl);

        var zone = ZoneId.of("Asia/Ho_Chi_Minh");
        var now = ZonedDateTime.now(zone);

        var createdDate = DateUtils.formatVnTime(now);
        var expiredDate = DateUtils.formatVnTime(now.plusMinutes(paymentTimeout));

        log.info("Time info - now(VN): {}", now);
        log.info("CreatedDate: {}, ExpiredDate: {}", createdDate, expiredDate);

        var utcNow = ZonedDateTime.now(ZoneOffset.UTC);
        log.info("UTC time: {}", utcNow);

        var orderInfo = VNPayUtils.buildPaymentDetail(
                txnRef,
                PurposeType.valueOf(request.getPurposeType()),
                TransactionType.valueOf(request.getTransactionType())
        );

        log.info("Order info: {}", orderInfo);
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

        params.put(VNPayParams.ORDER_INFO, orderInfo);
        params.put(VNPayParams.ORDER_TYPE, ORDER_TYPE);

        log.info("VNPay params: {}", params);

        var initPaymentUrl = VNPayUtils.buildInitPaymentUrl(params);

        log.info("VNPay URL generated: {}", initPaymentUrl);

        PaymentUrlResponse response = PaymentUrlResponse.newBuilder()
                .setVnpUrl(initPaymentUrl)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Transactional
    public IpnResponse process(Map<String, String> params) {
        if (!VNPayUtils.verifyIpn(params)) {
            return VnIpnResponseUtils.SIGNATURE_FAILED;
        }
        String vnpResponseCode = params.get(VNPayParams.RESPONSE_CODE);
        String transactionRef = params.get(VNPayParams.TRANSACTION_NO);

        //[0]: purposeId, [1] purposeType, [2] transactionType
        String[] otherInfo = params.get(VNPayParams.ORDER_INFO).split("\\|");
        Double amount = Double.parseDouble(params.get(VNPayParams.AMOUNT)) / 100;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        LocalDateTime dateTime = LocalDateTime.parse(params.get(VNPayParams.PAY_DATE), formatter);

        if (vnpResponseCode.equals("00")) {
            Payment payment = Payment
                    .builder()
                    .amount(amount)
                    .status(PaymentStatus.SUCCESS)
                    .purposeId(otherInfo[0])
                    .purposeType(PurposeType.valueOf(otherInfo[1]))
                    .transactionType(TransactionType.valueOf(otherInfo[2]))
                    .createdAt(dateTime)
                    .transactionRef(transactionRef)
                    .build();
            paymentRepository.save(payment);

            PaymentRequest paymentRequest = PaymentRequest.builder()
                    .amount(amount)
                    .purposeId(otherInfo[0])
                    .purposeType(otherInfo[1])
                    .build();
            PaymentEvent paymentEvent = PaymentEvent.builder()
                    .eventId(UUID.randomUUID())
                    .paymentStatus(PaymentStatus.SUCCESS)
                    .eventDate(new Date())
                    .paymentRequestDto(paymentRequest)
                    .build();

            paymentPublisher.publishMessage(paymentEvent);

            return VnIpnResponseUtils.SUCCESS;
        }

        Payment payment = Payment
                .builder()
                .amount(amount)
                .status(PaymentStatus.FAILED)
                .purposeId(otherInfo[0])
                .purposeType(PurposeType.valueOf(otherInfo[1]))
                .transactionType(TransactionType.valueOf(otherInfo[2]))
                .createdAt(dateTime)
                .transactionRef(transactionRef)
                .build();

        PaymentRequest paymentRequest = PaymentRequest.builder()
                .amount(amount)
                .purposeId(otherInfo[0])
                .purposeType(otherInfo[1])
                .build();
        PaymentEvent paymentEvent = PaymentEvent.builder()
                .eventId(UUID.randomUUID())
                .paymentStatus(PaymentStatus.FAILED)
                .eventDate(new Date())
                .paymentRequestDto(paymentRequest)
                .build();

        paymentPublisher.publishMessage(paymentEvent);
        paymentRepository.save(payment);

        return VnIpnResponseUtils.UNKNOWN_ERROR;
    }

    @Override
    public InitPaymentResponse getPaymentUrl(com.tanle.tland.payment_service.request.PaymentUrlRequest request,
                                             HttpServletRequest servletRequest) {
        var amount = PRICE_CREATED * DEFAULT_MULTIPLIER;
        var txnRef = request.getTxnRef();
        var ipAddress = VNPayUtils.getIpAddress(servletRequest);

        log.info("Request info - txnRef: {}, amount: {}, ip: {}", txnRef, amount, ipAddress);

        var returnUrl = VNPayUtils.buildReturnUrl(txnRef);
        log.info("Return URL: {}", returnUrl);

        var zone = ZoneId.of("Asia/Ho_Chi_Minh");
        var now = ZonedDateTime.now(zone);

        var createdDate = DateUtils.formatVnTime(now);
        var expiredDate = DateUtils.formatVnTime(now.plusMinutes(paymentTimeout));

        log.info("Time info - now(VN): {}", now);
        log.info("CreatedDate: {}, ExpiredDate: {}", createdDate, expiredDate);

        var utcNow = ZonedDateTime.now(ZoneOffset.UTC);
        log.info("UTC time: {}", utcNow);

        var orderInfo = VNPayUtils.buildPaymentDetail(
                txnRef,
                request.getPurposeType(),
                request.getTransactionType()
        );

        log.info("Order info: {}", orderInfo);
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

        params.put(VNPayParams.ORDER_INFO, orderInfo);
        params.put(VNPayParams.ORDER_TYPE, ORDER_TYPE);

        log.info("VNPay params: {}", params);

        var initPaymentUrl = VNPayUtils.buildInitPaymentUrl(params);

        log.info("VNPay URL generated: {}", initPaymentUrl);

        return InitPaymentResponse.builder()
                .vnpUrl(initPaymentUrl)
                .build();
    }
}
















