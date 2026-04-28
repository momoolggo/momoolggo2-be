package com.green.mmg.main.order;

import com.green.mmg.common.feign.AuthFeignClient;
import com.green.mmg.main.cart.CartMapper;
import com.green.mmg.main.cart.model.Cart;
import com.green.mmg.main.cart.model.CartItemRes;
import com.green.mmg.main.order.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final CartMapper  cartMapper;
    private final AuthFeignClient authFeignClient;   // Phase 4-A: tel을 auth에서 fetch

    private static final int DELIVERY_FEE = 1500;

    // 주문 화면 초기 데이터 조회
    public OrderInfoRes getOrderInfo(Long userNo) {
        // 1. 장바구니 조회
        Cart cart = cartMapper.findCartEntityByUserNo(userNo);
        if (cart == null) throw new RuntimeException("장바구니가 비어있습니다.");

        List<CartItemRes> items = cartMapper.findCartItems(cart.getCartId());
        if (items.isEmpty()) throw new RuntimeException("장바구니가 비어있습니다.");

        // 2. 가게명 조회
        String storeName = cartMapper.findStoreNameByStoreId(cart.getStoreId());

        // 3. 유저 정보 조회 (Phase 4-A: tel은 auth-service Feign, address는 main 자체 — Phase 1-B-3.5 후)
        String tel = authFeignClient.getUser(userNo).getTel();
        OrderAddressInfo addr = orderMapper.findDefaultAddress(userNo);

        // 4. 금액 계산 (DB 가격 기준 - 프론트 금액 신뢰 안 함)
        int menuTotal = items.stream()
                .mapToInt(i -> i.getPrice() * i.getQuantity())
                .sum();

        OrderInfoRes res = new OrderInfoRes();
        res.setStoreName(storeName);
        res.setTel(tel);
        res.setAddress(addr != null ? addr.getAddress() : "");
        res.setAddressDetail(addr != null ? addr.getAddressDetail() : "");
        res.setItems(items);
        res.setMenuTotal(menuTotal);
        res.setDeliveryFee(DELIVERY_FEE);
        res.setTotalAmount(menuTotal + DELIVERY_FEE);
        return res;
    }

    // 주문 확정
    @Transactional
    public long placeOrder(Long userNo, OrderReqDto dto) {
        // 1. 장바구니 조회
        Cart cart = cartMapper.findCartEntityByUserNo(userNo);
        if (cart == null) throw new RuntimeException("장바구니가 비어있습니다.");

        List<CartItemRes> items = cartMapper.findCartItems(cart.getCartId());
        if (items.isEmpty()) throw new RuntimeException("장바구니가 비어있습니다.");

        // 2. 배송지 조회 (addressId로 검증)
        OrderAddressInfo addr = orderMapper.findDefaultAddress(userNo);

        // 3. 금액 DB에서 재계산 (보안 - 프론트 금액 절대 신뢰 안 함)
        int menuTotal = items.stream()
                .mapToInt(i -> i.getPrice() * i.getQuantity())
                .sum();
        int totalAmount = menuTotal + DELIVERY_FEE;

        // 4. 주문 INSERT
        String serverOrderId = "39";
        String combined = serverOrderId + System.currentTimeMillis();

// 다시 숫자로 변환
        long uniqueId = Long.parseLong(combined);
        orderMapper.insertOrder(
                uniqueId,
                userNo,
                cart.getStoreId(),
                dto.getRequest(),
                dto.getRiderRequest(),
                addr != null ? addr.getAddress() : "",
                addr != null ? addr.getAddressDetail() : "",
                DELIVERY_FEE,
                totalAmount,
                dto.getPayState()
        );

        // 5. 주문 상세 INSERT
        for (CartItemRes item : items) {
            orderMapper.insertOrderDetail(
                    uniqueId,
                    item.getMenuId(),
                    item.getQuantity(),
                    item.getMenuName(),
                    item.getPrice()
            );
        }
    return uniqueId ;}

    //주문내역삭제
    public int deleteOrder(long id){
        return orderMapper.deleteOrder(id);
    }


    //주문내역조회
     public List<OrderHistoryDto> getOrderHistory(OrderHistoryReq req){
         // 1) 주문 목록 조회
         List<OrderHistoryDto> orders = orderMapper.findOrdersByUserId(req);

         // 2) 각 주문마다 메뉴 리스트 조회 후 세팅
         for (OrderHistoryDto order : orders) {
             List<OrderHistoryDto.OrderItemDto> items =
                     orderMapper.findItemsByOrderId(order.getOrderId());
             order.setItems(items);
         }
         return orders;
     }

     public OrderHistoryDto orderHistoryDetail(long id){
        OrderHistoryDto result =orderMapper.orderHistoryDetail(id);
        result.setItems(orderMapper.findItemsByOrderId(id));
        return result;
     }

     public int maxHistoryPage(long id){
        return orderMapper.maxHistoryPage(id);
     }
     // 가게 주문수 계산
     public int calSumOrder(long id){
        return orderMapper.calSumOrder(id);
     }
}
