package com.green.mmg.main.payment;

import com.green.mmg.main.cart.CartMapper;
import com.green.mmg.main.order.OrderMapper;
import com.green.mmg.main.order.model.OrderState;
import com.green.mmg.main.order.model.Orders;
import com.green.mmg.main.payment.model.PaymentConfirmReq;
import com.green.mmg.main.payment.model
        .PaymentEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import com.green.mmg.main.cart.model.Cart;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j  // ✅ 추가
public class PaymentService {

    private final PaymentMapper paymentMapper;
    private final PaymentRepository paymentRepository;  // Phase 3-B: insertPayment → save()
    private final CartMapper cartMapper;
    private final OrderMapper orderMapper;

    @Value("${toss.secret-key}")
    private String secretKey;
    @Transactional
    public void confirmPayment(PaymentConfirmReq req) throws Exception {

        // ✅ 1. orderId로 주문 조회
        Long orderId = Long.parseLong(req.getOrderId());
        Orders order = orderMapper.findByOrderId(orderId);

        if (order == null) {
            throw new RuntimeException("존재하지 않는 주문입니다.");
        }

        // ✅ 2. 요청한 금액이 실제 주문 금액과 같은지 검증
        if (order.getAmount() != req.getAmount()) {
            throw new RuntimeException("결제 금액이 일치하지 않습니다.");
        }

        // ✅ 3. 이미 결제된 주문인지 검증
        boolean alreadyPaid = paymentMapper.existsByOrderId(orderId);
        if (alreadyPaid) {
            throw new RuntimeException("이미 결제된 주문입니다.");
        }
        JSONObject requestBody = new JSONObject();
        requestBody.put("paymentKey", req.getPaymentKey());
        requestBody.put("orderId",    req.getOrderId());
        requestBody.put("amount",     req.getAmount());

        log.info("토스 요청 바디: {}", requestBody.toJSONString()); // ✅ 추가

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
        log.info("토스 응답 코드: {}", code); // ✅ 추가

        InputStream responseStream = code == 200
                ? connection.getInputStream()
                : connection.getErrorStream();

        JSONParser parser = new JSONParser();
        JSONObject response;
        try (Reader reader = new InputStreamReader(responseStream, StandardCharsets.UTF_8)) {
            response = (JSONObject) parser.parse(reader);
        }

        log.info("토스 응답 바디: {}", response.toJSONString()); // ✅ 추가

        if (code == 200) {
            Long userNo = orderMapper.findUserNoByOrderId(orderId);

            if (userNo != null) {
                Cart cart = cartMapper.findCartEntityByUserNo(userNo);
                if (cart != null) {
                    cartMapper.deleteAllCartItems(cart.getCartId());
                    cartMapper.deleteCart(cart.getCartId());
                }
            }
        }

        if (code != 200) {
            throw new RuntimeException((String) response.get("message"));
        }
        OrderState orderState= new OrderState();
        orderState.setState(2);
        orderState.setOrderId(orderId);
        PaymentEntity payment = new PaymentEntity();
        orderMapper.updateState(orderState);
        payment.setOrderId(orderId);  // Phase 3-B: String → Long 타입 정합 (PaymentEntity.orderId: Long)
        payment.setPaymentKey(req.getPaymentKey());
        payment.setAmount(req.getAmount());
        payment.setPayState(req.getPayState());
        paymentRepository.save(payment);  // Phase 3-B: MyBatis insertPayment → JPA save (paymentTime은 DB DEFAULT)
    }
}