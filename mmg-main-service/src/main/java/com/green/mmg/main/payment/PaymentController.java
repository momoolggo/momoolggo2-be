package com.green.mmg.main.payment;

import com.green.mmg.main.payment.model.PaymentConfirmReq;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/confirm")
    public ResponseEntity<?> confirmPayment(@RequestBody PaymentConfirmReq req) throws Exception {
        paymentService.confirmPayment(req);
        return ResponseEntity.ok(Map.of("result", "success"));
    }
}