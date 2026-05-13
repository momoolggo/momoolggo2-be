package com.green.mmg.main.payment;

import com.green.mmg.main.cart.CartDetailRepository;
import com.green.mmg.main.cart.CartRepository;
import com.green.mmg.main.order.OrderRepository;
import com.green.mmg.main.order.model.Orders;
import com.green.mmg.main.payment.model.PaymentConfirmReq;
import com.green.mmg.main.payment.model.PaymentEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Phase 3-C-3: Cart/Order 외부 호출 정리 — JPA Repository 위임으로 전환.
 *
 * <p>제거된 MyBatis 호출:
 * <ul>
 *   <li>orderMapper.findByOrderId → orderRepository.findById</li>
 *   <li>orderMapper.findUserNoByOrderId → order.getUserNo()</li>
 *   <li>orderMapper.updateState → dirty checking (영속 entity setter)</li>
 *   <li>cartMapper.findCartEntityByUserNo → cartRepository.findByUserNo</li>
 *   <li>cartMapper.deleteAllCartItems → cartDetailRepository.deleteByCartId</li>
 *   <li>cartMapper.deleteCart → cartRepository.delete</li>
 * </ul>
 *
 * <p>잔존: paymentMapper.existsByOrderId (orders.pay_state 검사 — 의미상 Order 도메인이지만
 * SQL 그대로 유지, Phase 3-D 또는 Phase 5에서 정리 검토)</p>
 *
 * <p>Toss 호출 (HttpURLConnection)은 Phase 5 TossPaymentClient 추출 + RestTemplate/WebClient 전환 예정.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentMapper paymentMapper;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartDetailRepository cartDetailRepository;

    @Value("${toss.secret-key}")
    private String secretKey;

    @Transactional
    public void confirmPayment(PaymentConfirmReq req) throws Exception {
        // 1) 주문 검증 (존재/금액/중복결제)
        Long orderId = Long.parseLong(req.getOrderId());
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 주문입니다."));

        Integer orderAmount = order.getAmount();
        if (orderAmount == null || orderAmount != req.getAmount()) {
            throw new RuntimeException("결제 금액이 일치하지 않습니다.");
        }

        if (paymentMapper.existsByOrderId(orderId)) {
            throw new RuntimeException("이미 결제된 주문입니다.");
        }

        // 2) 토스 결제 검증 (외부 호출) — 실패 시 즉시 throw, 이후 로컬 변경 0
        callTossConfirm(req);

        // 3) 결제 row 저장
        PaymentEntity payment = new PaymentEntity();
        payment.setOrderId(orderId);
        payment.setPaymentKey(req.getPaymentKey());
        payment.setAmount(req.getAmount());
        payment.setPayState(req.getPayState());
        paymentRepository.save(payment);

        // 4) 주문 상태 = 결제완료 (dirty checking)
        order.setPayState(2);

        // 5) 장바구니 정리 — 결제 완료 후 비움 (앞 단계 모두 성공한 뒤에만 도달)
        Long userNo = order.getUserNo();
        if (userNo != null) {
            cartRepository.findByUserNo(userNo).ifPresent(cart -> {
                cartDetailRepository.deleteByCartId(cart.getCartId());
                cartRepository.delete(cart);
            });
        }
    }

    /**
     * 토스 결제 확인 호출. 200 외 응답은 RuntimeException(응답 message).
     * 단위 테스트에서 Spy로 stub 가능하도록 protected.
     * Phase 5 예정: TossPaymentClient 컴포넌트 추출 + RestTemplate/WebClient 전환 + timeout.
     */
    protected JSONObject callTossConfirm(PaymentConfirmReq req) throws Exception {
        JSONObject requestBody = new JSONObject();
        requestBody.put("paymentKey", req.getPaymentKey());
        requestBody.put("orderId",    req.getOrderId());
        requestBody.put("amount",     req.getAmount());
        log.info("토스 요청 바디: {}", requestBody.toJSONString());

        String encoded = Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));

        URL url = new URL("https://api.tosspayments.com/v1/payments/confirm");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Authorization", "Basic " + encoded);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(requestBody.toString().getBytes(StandardCharsets.UTF_8));
        }

        int code = connection.getResponseCode();
        log.info("토스 응답 코드: {}", code);

        InputStream responseStream = code == 200
                ? connection.getInputStream()
                : connection.getErrorStream();

        JSONObject response;
        try (Reader reader = new InputStreamReader(responseStream, StandardCharsets.UTF_8)) {
            response = (JSONObject) new JSONParser().parse(reader);
        }
        log.info("토스 응답 바디: {}", response.toJSONString());

        if (code != 200) {
            throw new RuntimeException((String) response.get("message"));
        }
        return response;
    }
}
