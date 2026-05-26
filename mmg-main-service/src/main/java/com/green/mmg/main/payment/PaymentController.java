package com.green.mmg.main.payment;

import com.green.mmg.common.dto.ResultResponse;
import com.green.mmg.common.model.UserPrincipal;
import com.green.mmg.main.order.OrderService;
import com.green.mmg.main.order.model.OrderCancelReq;
import com.green.mmg.main.payment.model.PaymentConfirmReq;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final OrderService orderService;

    @PostMapping("/confirm")
    public ResponseEntity<?> confirmPayment(@RequestBody PaymentConfirmReq req) throws Exception {
        paymentService.confirmPayment(req);
        return ResponseEntity.ok(Map.of("result", "success"));
    }

    @PostMapping("/{orderId}/refund")
    public ResultResponse<Void> refundPayment(@AuthenticationPrincipal UserPrincipal principal,
                                                @PathVariable long orderId,
                                                @RequestBody OrderCancelReq req) {
        orderService.cancelOrder(principal.getSignedUserNo(), orderId, req);
        return new ResultResponse<>("환불 완료", null);
    }
}