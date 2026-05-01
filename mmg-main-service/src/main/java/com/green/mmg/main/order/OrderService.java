package com.green.mmg.main.order;

import com.green.mmg.common.exception.BusinessException;
import com.green.mmg.common.feign.AuthFeignClient;
import com.green.mmg.main.address.UserAddressRepository;
import com.green.mmg.main.cart.CartMapper;
import com.green.mmg.main.cart.CartRepository;
import com.green.mmg.main.cart.model.Cart;
import com.green.mmg.main.cart.model.CartItemRes;
import com.green.mmg.main.order.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Phase 3-C-1: Order/OrderDetail 단순 CRUD JPA 전환 + 하이브리드.
 *
 * <p>전환:
 * <ul>
 *   <li>insertOrder → orderRepository.save (Orders는 Persistable — manual ID assign)</li>
 *   <li>insertOrderDetail → orderDetailRepository.save (반복)</li>
 *   <li>deleteOrder(WHERE pay_state=1) → @Modifying @Query</li>
 *   <li>maxHistoryPage → countByUserNo</li>
 *   <li>findItemsByOrderId → @Query JPQL constructor expression (OrderItemDto)</li>
 * </ul>
 * 잔존:
 * <ul>
 *   <li>findOrdersByUserId / orderHistoryDetail / calSumOrder: 복잡 SQL — 영구 MyBatis</li>
 *   <li>findDefaultAddress: 외부 도메인(address) — Phase 3-D Address Repository 신설 시 정리</li>
 *   <li>cartMapper.findCartItems: Cart JOIN — 영구 MyBatis (Phase 3-B-3 정책)</li>
 *   <li>cartMapper.findCartEntityByUserNo: CartRepository.findByUserNo 위임 (Phase 3-C-3 정리됨)</li>
 *   <li>cartMapper.findStoreNameByStoreId: Store 도메인 — Phase 3-D 정리 예정</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final CartMapper cartMapper;          // findCartItems (JOIN) + findStoreNameByStoreId 잔존
    private final CartRepository cartRepository;  // Phase 3-C-3: findCartEntityByUserNo 위임
    private final UserAddressRepository userAddressRepository;  // Phase 3-D-B: findDefaultAddress 위임
    private final AuthFeignClient authFeignClient;

    private static final int DELIVERY_FEE = 1500;

    // 주문 화면 초기 데이터 조회
    @Transactional(readOnly = true)
    public OrderInfoRes getOrderInfo(Long userNo) {
        Cart cart = cartRepository.findByUserNo(userNo)
                .orElseThrow(() -> new RuntimeException("장바구니가 비어있습니다."));

        List<CartItemRes> items = cartMapper.findCartItems(cart.getCartId());
        if (items.isEmpty()) throw new RuntimeException("장바구니가 비어있습니다.");

        String storeName = cartMapper.findStoreNameByStoreId(cart.getStoreId());

        // Phase 3-Backfill-A-4: Feign null 처리 (StoreService.storeOneGet 패턴 전파)
        com.green.mmg.common.dto.feign.UserBriefDto user = authFeignClient.getUser(userNo);
        if (user == null) {
            throw new BusinessException("사용자 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
        }
        String tel = user.getTel();
        OrderAddressInfo addr = userAddressRepository.findFirstDefaultByUserNo(userNo).orElse(null);

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

    @Transactional
    public long placeOrder(Long userNo, OrderReqDto dto) {
        Cart cart = cartRepository.findByUserNo(userNo)
                .orElseThrow(() -> new RuntimeException("장바구니가 비어있습니다."));

        List<CartItemRes> items = cartMapper.findCartItems(cart.getCartId());
        if (items.isEmpty()) throw new RuntimeException("장바구니가 비어있습니다.");

        OrderAddressInfo addr = userAddressRepository.findFirstDefaultByUserNo(userNo).orElse(null);

        int menuTotal = items.stream()
                .mapToInt(i -> i.getPrice() * i.getQuantity())
                .sum();
        int totalAmount = menuTotal + DELIVERY_FEE;

        // 기존 패턴 유지: serverOrderId + timestamp 결합 큰 숫자 ID
        long uniqueId = Long.parseLong("39" + System.currentTimeMillis());

        Orders order = new Orders();
        order.setOrderId(uniqueId);
        order.setUserNo(userNo);
        order.setStoreId(cart.getStoreId());
        order.setRequest(dto.getRequest());
        order.setRiderRequest(dto.getRiderRequest());
        order.setAddress(addr != null ? addr.getAddress() : "");
        order.setAddressDetail(addr != null ? addr.getAddressDetail() : "");
        order.setDeliveryFee(DELIVERY_FEE);
        order.setAmount(totalAmount);
        order.setPayState(dto.getPayState());
        orderRepository.saveAndFlush(order);

        for (CartItemRes item : items) {
            OrderDetail detail = new OrderDetail();
            detail.setOrderId(uniqueId);
            detail.setMenuId(item.getMenuId());
            detail.setQuantity(item.getQuantity());
            detail.setMenuName(item.getMenuName());
            detail.setMenuPrice(item.getPrice());
            orderDetailRepository.save(detail);
        }

        // 가게 누적 주문 수(store.order_count) 갱신 — 같은 트랜잭션 내 처리
        orderMapper.calSumOrder(cart.getStoreId());
        return uniqueId;
    }

    @Transactional
    public int deleteOrder(long callerUserNo, long orderId) {
        // 삭제 전 order 조회: storeId 확보 + 소유자 검증 (Phase 3-Backfill-A-1: 보안)
        // 미존재 orderId는 기존 동작 유지 (return 0 → "삭제실패") — 응답 스펙 동결
        Orders order = orderRepository.findById(orderId).orElse(null);
        if (order == null) return 0;
        if (!Objects.equals(order.getUserNo(), callerUserNo)) {
            throw new BusinessException("본인 주문만 삭제할 수 있습니다.", HttpStatus.FORBIDDEN);
        }
        Long storeId = order.getStoreId();
        int affected = orderRepository.deleteByOrderIdAndPayStateUnpaid(orderId);
        if (affected > 0 && storeId != null) {
            orderMapper.calSumOrder(storeId);
        }
        return affected;
    }

    @Transactional(readOnly = true)
    public List<OrderHistoryDto> getOrderHistory(long callerUserNo, OrderHistoryReq req) {
        // Phase 3-Backfill-A-3: req.userId 위조 방지 (옵션 B — 명시적 403 throw)
        if (req.getUserId() != callerUserNo) {
            throw new BusinessException("본인 주문 내역만 조회 가능합니다.", HttpStatus.FORBIDDEN);
        }
        // 복잡 SQL (DATE_FORMAT + 서브쿼리 hasReview) — MyBatis 영구 잔존
        List<OrderHistoryDto> orders = orderMapper.findOrdersByUserId(req);

        // 메뉴 목록만 JPA constructor expression으로
        for (OrderHistoryDto order : orders) {
            order.setItems(orderDetailRepository.findItemsByOrderId(order.getOrderId()));
        }
        return orders;
    }

    @Transactional(readOnly = true)
    public OrderHistoryDto orderHistoryDetail(long callerUserNo, long orderId) {
        // Phase 3-Backfill-A-3: 본인 주문 검증
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("주문을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        if (!Objects.equals(order.getUserNo(), callerUserNo)) {
            throw new BusinessException("본인 주문만 조회 가능합니다.", HttpStatus.FORBIDDEN);
        }
        OrderHistoryDto result = orderMapper.orderHistoryDetail(orderId);  // 복잡 DATE_FORMAT — 잔존
        result.setItems(orderDetailRepository.findItemsByOrderId(orderId));
        return result;
    }

    @Transactional(readOnly = true)
    public int maxHistoryPage(long callerUserNo, long userId) {
        // Phase 3-Backfill-A-3: path userId 위조 방지 (옵션 B)
        if (userId != callerUserNo) {
            throw new BusinessException("본인 주문 내역만 조회 가능합니다.", HttpStatus.FORBIDDEN);
        }
        // 응답 동결: 기존 OrderMapper.maxHistoryPage가 int 반환 → 동일 타입 유지
        return (int) orderRepository.countByUserNo(userId);
    }
}
