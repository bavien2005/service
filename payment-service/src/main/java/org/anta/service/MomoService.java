package org.anta.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.anta.client.MomoAPI;
import org.anta.dto.request.CreateMomoRequest;
import org.anta.dto.response.CreateMomoResponse;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@ApplicationScoped
@Slf4j
public class MomoService {

    @ConfigProperty(name = "momo.partner-code")
    String PARTNER_CODE;

    @ConfigProperty(name = "momo.access-key")
    String ACCESS_KEY;

    @ConfigProperty(name = "momo.secret-key")
    String SECRET_KEY;

    @ConfigProperty(name = "momo.return-url")
    String REDIRECT_URL;

    @ConfigProperty(name = "momo.ipn-url")
    String IPN_URL;

    @ConfigProperty(name = "momo.request-type", defaultValue = "captureWallet")
    String REQUEST_TYPE;

    @Inject
    @RestClient
    MomoAPI momoAPI;

    public CreateMomoResponse createQRForPayment(String requestId,
                                                 Long amount,
                                                 String partnerOrderIdInput) {

        // partnerOrderIdInput is expected to be already prepared by caller (e.g. "10-<requestId>")
        String partnerOrderId = partnerOrderIdInput;
        if (partnerOrderId == null || partnerOrderId.isBlank()) {
            // if caller didn't supply a partnerOrderId, fallback to unique id
            partnerOrderId = UUID.randomUUID().toString();
        }

        String orderInfo = "Payment for order: " + partnerOrderId;
        String extraData = "";

        String rawSignature = String.format(
                "accessKey=%s&amount=%s&extraData=%s&ipnUrl=%s&orderId=%s&orderInfo=%s&partnerCode=%s&redirectUrl=%s&requestId=%s&requestType=%s",
                ACCESS_KEY,
                amount,
                extraData,
                IPN_URL,
                partnerOrderId,
                orderInfo,
                PARTNER_CODE,
                REDIRECT_URL,
                requestId,
                REQUEST_TYPE
        );

        log.info(" [CREATE] Raw string before signing:\n{}", rawSignature);

        String prettySignnature;
        try {
            prettySignnature = signHmacSHA256(rawSignature, SECRET_KEY);
            log.info("Signature: {}", prettySignnature);
        } catch (Exception e) {
            log.error("Error while signing HMAC SHA256: {}", e.getMessage(), e);
            throw new RuntimeException("Error creating signature for Momo request", e);
        }

        CreateMomoRequest createMomoRequest = CreateMomoRequest.builder()
                .partnerCode(PARTNER_CODE)
                .requestType(REQUEST_TYPE)
                .ipnUrl(IPN_URL)
                .orderId(partnerOrderId)
                .amount(amount)
                .orderInfo(orderInfo)
                .requestId(requestId)
                .redirectUrl(REDIRECT_URL)
                .lang("vi")
                .extraData(extraData)
                .signature(prettySignnature)
                .accessKey(ACCESS_KEY)
                .build();

        // **Call MoMo once and return the single response**
        CreateMomoResponse resp = momoAPI.createMomoQR(createMomoRequest);
        log.info("MoMo resp: orderId={}, requestId={}, payUrl={}, deeplink={}, qrCodeUrl={}, resultCode={}, message={}",
                resp.getOrderId(), resp.getRequestId(), resp.getPayUrl(), resp.getDeeplink(), resp.getQrCodeUrl(),
                resp.getResultCode(), resp.getMessage());
        return resp;
    }

    // HMAC SHA256 signing method
    // truyền dử liệu và key vào để mã hóa giữa client và server
    // dữ liệu truyền đi sẽ được mã hóa và chỉ có server mới giải mã được
    // đảm bảo tính toàn vẹn và bảo mật của dữ liệu không cho phép bên thứ 3 can thiệp
    private String signHmacSHA256(String data, String key) throws Exception {

        Mac hmacSHA256 = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "" +
                "HmacSHA256");
        hmacSHA256.init(secretKeySpec);
        byte[] hash = hmacSHA256.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();

        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }

        return hexString.toString();
    }
}