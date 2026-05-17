package org.anta.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.anta.client.OrderClient;
import org.anta.dto.response.CreateMomoResponse;
import org.anta.dto.response.PaymentStatusResponse;
import org.anta.entity.*;
import org.anta.repository.PaymentLogRepository;
import org.anta.repository.PaymentRepository;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
@Slf4j
public class PaymentService {

    @Inject
    PaymentRepository paymentRepository;

    @Inject
    PaymentLogRepository logRepository;

    @Inject
    MomoService momoService;

    @Inject
    @RestClient
    OrderClient orderClient;

    @ConfigProperty(name = "momo.check-status-url", defaultValue = "https://api.momo.vn/check-payment-status")
    String momoCheckStatusUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public Payment createAndCommitPaymentRecord(Payment p) {
        return paymentRepository.save(p);
    }

    @Transactional
    public CreateMomoResponse createPaymentAndRequestMomo(Long orderId,
                                                          Long userId,
                                                          Long amount,
                                                          Map<String, String> extra) {
        // Determine finalAmount: prefer order-service's authoritative total
        Long finalAmount = amount; // incoming
        try {
            if (orderId != null) {
                Map<String, Object> body = orderClient.getOrderById(orderId);

                if (body != null) {
                    // thích ứng với cấu trúc payload trả về của order-service (kiểm tra keys)
                    Object serverTotalObj = body.getOrDefault("total", body.get("amount"));

                    if (serverTotalObj instanceof Number) {
                        Long serverTotal = ((Number) serverTotalObj).longValue();
                        if (serverTotal != null && serverTotal > 0) {
                            finalAmount = serverTotal;
                            log.info("Using server order total for orderId={} : {}", orderId, finalAmount);
                        }
                    } else if (serverTotalObj instanceof String) {
                        try {
                            finalAmount = Long.valueOf((String) serverTotalObj);
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch order total from order-service for orderId={}, falling back to requested amount. err={}", orderId, e.toString());
        }

        String requestId = java.util.UUID.randomUUID().toString();

        Payment p = Payment.builder()
                .orderId(orderId)
                .userId(userId)
                .requestId(requestId)
                .amount(finalAmount)   // dùng finalAmount đã xác định
                .currency("VND")
                .status("PENDING")
                .extraData(extra == null ? null : toJson(extra))
                .build();

        p = createAndCommitPaymentRecord(p);

        String partnerOrderId = (orderId != null ? orderId.toString() : "") + "-" + requestId;
        p.setPartnerOrderId(partnerOrderId);
        paymentRepository.save(p);

        CreateMomoResponse resp;
        try {
            resp = momoService.createQRForPayment(p.getRequestId(), p.getAmount(), partnerOrderId);

            if (resp == null) {
                p.setStatus("FAILED");
                paymentRepository.save(p);
                log.error("Momo returned null for paymentId={}", p.getId());
                throw new RuntimeException("Momo returned null");
            }

            Integer resultCode = null;
            try {
                resultCode = resp.getResultCode();
            } catch (Exception ignored) {
            }

            if (resultCode != null && resultCode != 0) {
                // Nếu provider báo duplicate trong message, CỐ GẮNG lấy payment cũ trả về
                String msg = resp.getMessage() != null ? resp.getMessage().toLowerCase() : "";
                if (msg.contains("trùng") || msg.contains("duplicate") || msg.contains("already")) {
                    // tìm payment đã tạo trước đó cho orderId
                    Optional<Payment> maybeExisting = paymentRepository.findTopByOrderIdOrderByCreatedAtDesc(orderId);
                    if (maybeExisting.isPresent()) {
                        Payment existing = maybeExisting.get();
                        if (existing.getPayUrl() != null) {
                            CreateMomoResponse existingResp = new CreateMomoResponse();
                            existingResp.setOrderId(existing.getPartnerOrderId());
                            existingResp.setPayUrl(existing.getPayUrl());
                            existingResp.setMessage("DUPLICATE_HANDLED_RETURN_EXISTING");
                            existingResp.setResultCode(0);
                            return existingResp;
                        }
                    }
                }

                p.setStatus("FAILED");
                paymentRepository.save(p);
                log.error("Momo create failed: resultCode={}, message={}, resp={}", resultCode, resp.getMessage(), resp);
                throw new RuntimeException("Momo create failed: " + resp.getMessage());
            }

            p.setPartnerOrderId(resp.getOrderId());
            p.setPayUrl(resp.getPayUrl());
            p.setStatus("PENDING");
            paymentRepository.save(p);

            logRepository.save(PaymentLog.builder()
                    .paymentId(p.getId())
                    .eventType("MOMO_REQUEST_SENT")
                    .payload(toJson(resp))
                    .build());

            return resp;

        } catch (Exception ex) {
            // nếu exception từ provider chứa thông tin duplicate: cố gắng tìm payment đã tồn tại trả về
            String errMsg = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
            if (errMsg.contains("trùng") || errMsg.contains("duplicate") || errMsg.contains("already")) {
                Optional<Payment> maybeExisting = paymentRepository.findTopByOrderIdOrderByCreatedAtDesc(orderId);
                if (maybeExisting.isPresent()) {
                    Payment existing = maybeExisting.get();
                    if (existing.getPayUrl() != null) {
                        CreateMomoResponse existingResp = CreateMomoResponse.builder()
                                .orderId(existing.getPartnerOrderId())
                                .requestId(existing.getRequestId())
                                .amount(existing.getAmount())
                                .payUrl(existing.getPayUrl())
                                .resultCode(0)
                                .message("DUPLICATE_EXCEPTION_HANDLED_RETURN_EXISTING")
                                .build();
                        return existingResp;
                    }
                }
            }

            try {
                p.setStatus("FAILED");
                paymentRepository.save(p);
            } catch (Exception e) {
                log.warn("Failed to mark payment failed for id={}", p.getId(), e);
            }

            log.error("Error when creating payment with Momo for paymentId={}", p.getId(), ex);
            throw new RuntimeException("Payment provider error: " + ex.getMessage(), ex);
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return null;
        }
    }

    @Transactional
    public void handlePaymentSuccess(Payment payment) {
        payment.setStatus("SUCCESS");
        // updatedAt do DB tự update, nhưng set cũng không sao
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        try {
            orderClient.markOrderPaid(payment.getOrderId());
        } catch (Exception ex) {
            log.warn("Failed to notify order-service for PAID orderId={}", payment.getOrderId(), ex);
        }
    }

    @Transactional
    public void handlePaymentFailed(Payment payment) {
        payment.setStatus("FAILED");
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        try {
            orderClient.markOrderPaymentFailed(payment.getOrderId());
        } catch (Exception ex) {
            log.warn("Failed to notify order-service for PAYMENT_FAILED orderId={}", payment.getOrderId(), ex);
        }
    }

    @Transactional
    public PaymentStatusResponse checkMomoStatus(String requestId) {
        Payment payment = paymentRepository
                .findByRequestId(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found for requestId=" + requestId));

        return PaymentStatusResponse.from(payment);
    }

    public boolean checkPaymentStatus(String orderId, String resultCode) {
        // Sử dụng MoMo API hoặc thông tin trả về từ MoMo để kiểm tra trạng thái thanh toán
        String url = momoCheckStatusUrl
                + "?orderId=" + encode(orderId)
                + "&resultCode=" + encode(resultCode);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Map<String, String> responseBody = objectMapper.readValue(response.body(), Map.class);
                // Kiểm tra trạng thái thanh toán
                return "SUCCESS".equals(responseBody.get("paymentStatus"));
            } else {
                return false;
            }
        } catch (Exception e) {
            throw new RuntimeException("Cannot check payment status: " + e.getMessage(), e);
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}