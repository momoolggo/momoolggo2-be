package com.green.mmg.main.order;

import com.green.mmg.main.order.model.OrderHistoryDto;
import com.green.mmg.main.order.model.OrderHistoryReq;
import com.green.mmg.main.order.model.OrderInfoRes;
import com.green.mmg.main.order.model.OrderReqDto;
import com.green.mmg.common.dto.ResultResponse;
import com.green.mmg.common.model.UserPrincipal;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // 주문 화면 초기 데이터 조회
    @GetMapping
    public ResponseEntity<?> getOrderInfo(
            @AuthenticationPrincipal UserPrincipal principal) {
        OrderInfoRes res = orderService.getOrderInfo(principal.getSignedUserNo());
        return ResponseEntity.ok(Map.of("resultData", res));
    }

    // 주문 확정 — calSumOrder는 OrderService.placeOrder 내부에서 호출 (트랜잭션 일체화)
    @PostMapping
    public ResponseEntity<?> placeOrder(@AuthenticationPrincipal UserPrincipal principal,
            @RequestBody OrderReqDto dto) {
        long orderId = orderService.placeOrder(principal.getSignedUserNo(), dto);
        return ResponseEntity.ok(Map.of("result", "success","orderId", orderId));
    }

    // 삭제 — calSumOrder는 OrderService.deleteOrder 내부에서 처리 (storeId 사전 확보 후 호출)
    @DeleteMapping("/{id}")
    public ResultResponse<?> deleteOrder(@AuthenticationPrincipal UserPrincipal principal,
                                         @PathVariable long id){
        int result = orderService.deleteOrder(principal.getSignedUserNo(), id);
        return new ResultResponse<>(result==1 ? "삭제성공": "삭제실패", null);
    }

    //주문내역
    @GetMapping("/history")
    public ResponseEntity<List<OrderHistoryDto>> getOrderHistory(@ModelAttribute OrderHistoryReq req) {
        return ResponseEntity.ok(orderService.getOrderHistory(req));
    }

    //주문상세
    @GetMapping("/history/{id}")
    public ResponseEntity<OrderHistoryDto> orderHistoryDetail(@PathVariable long id){
        return ResponseEntity.ok(orderService.orderHistoryDetail(id));
    }
    //주무내역 맥스페이지
    @GetMapping("/history/max/{id}")
    public ResultResponse<?> maxHistoryPage(@PathVariable long id){
        int result = orderService.maxHistoryPage(id);
        System.out.println(result);
        return new ResultResponse<>("조회성공",result);
    }

}
