package com.green.mmg.main.order;

import com.green.mmg.common.exception.BusinessException;
import com.green.mmg.main.cart.CartDetailRepository;
import com.green.mmg.main.cart.model.CartDetail;
import com.green.mmg.main.feign.AuthFeignClient;
import com.green.mmg.main.address.UserAddressRepository;
import com.green.mmg.main.cart.CartMapper;
import com.green.mmg.main.cart.CartRepository;
import com.green.mmg.main.cart.model.Cart;
import com.green.mmg.main.cart.model.CartItemRes;
import com.green.mmg.main.internal.dto.DeliveryCompleteRes;
import com.green.mmg.main.internal.dto.DeliveryStatusUpdateRes;
import com.green.mmg.main.order.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    // ADR-004 line 90-98 매핑 표 — rider DeliveryStatus(String) → orders.delivery_state(int).
    // 신규 enum 도입 X (영역 분리 + case-#34 양 schema 일관성 검증 일관). 매핑 박제 그대로.
    private static final Map<String, Integer> DELIVERY_STATE_MAP = Map.of(
            "WAITING_ASSIGN", 1,
            "ASSIGNED", 1,
            "ARRIVED_AT_STORE", 1,
            "AWAITING_PICKUP", 1,
            "PICKED_UP", 2,
            "DELIVERING", 2,
            "DELIVERED", 3
    );

    private final OrderMapper orderMapper;
    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final CartMapper cartMapper;          // findCartItems (JOIN) + findStoreNameByStoreId 잔존
    private final CartRepository cartRepository;  // Phase 3-C-3: findCartEntityByUserNo 위임
    private final CartDetailRepository cartDetailRepository;
    private final UserAddressRepository userAddressRepository;  // Phase 3-D-B: findDefaultAddress 위임
    private final OrderStatusLogRepository orderStatusLogRepository;
    private final AuthFeignClient authFeignClient;

    private static final int ORDER_STATE_WAITING = 1;
    private static final int ORDER_STATE_CANCELED = 2;

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
        com.green.mmg.common.dto.feign.UserBriefDto user = authFeignClient.getUserInfo(userNo).getResultData();
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
        order.setOrderState(ORDER_STATE_WAITING);
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

    // 주문 취소

    @Transactional
    public void cancelOrder(long callUserNo,long orderId, OrderCancelReq req) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("주문을 찾을 수 없습니다", HttpStatus.NOT_FOUND));

        if(!Objects.equals(order.getUserNo(), callUserNo)) {
            throw new BusinessException("본인 주문만 취소할 수 있습니다", HttpStatus.FORBIDDEN);
        }

        if(!Objects.equals(order.getOrderState(), ORDER_STATE_WAITING)) {
            throw new BusinessException("가게가 주문을 수락한 이후에는 취소할 수 없습니다", HttpStatus.BAD_REQUEST);
        }

        if(req == null || req.getReason() == null || req.getReason().isBlank()) {
            throw new BusinessException("취소 사유를 선택해 주세요", HttpStatus.BAD_REQUEST);
        }
        order.setOrderState(ORDER_STATE_CANCELED);

        OrderStatusLog log = new OrderStatusLog();
        log.setOrderId(orderId);
        log.setBeforeState(ORDER_STATE_WAITING);
        log.setAfterState(ORDER_STATE_CANCELED);
        log.setChangedByType("USER");
        log.setChangedByNo(callUserNo);
        log.setMemo(req.getReason());

        orderStatusLogRepository.save(log);
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
        if (!Objects.equals(req.getUserId(), callerUserNo)) {
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
        if (!Objects.equals(userId, callerUserNo)) {
            throw new BusinessException("본인 주문 내역만 조회 가능합니다.", HttpStatus.FORBIDDEN);
        }
        // 응답 동결: 기존 OrderMapper.maxHistoryPage가 int 반환 → 동일 타입 유지
        return (int) orderRepository.countByUserNo(userId);
    }

    // 주문 취소 건 재주문(장바구니 다시 담기)
    @Transactional
    public void reorder(long callUserNo, long orderId) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("주문을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        if(!Objects.equals(order.getUserNo(), callUserNo)) {
            throw new BusinessException("본인 주문만 재주문할 수 있습니다.", HttpStatus.FORBIDDEN);
        }
        if(!Objects.equals(order.getOrderState(), ORDER_STATE_CANCELED)) {
            throw new BusinessException("취소된 주문만 재주문할 수 있습니다.", HttpStatus.BAD_REQUEST);
        }

        List<OrderDetail> orderDetails = orderDetailRepository.findAllByOrderId(orderId);

        if(orderDetails.isEmpty()) {
            throw new BusinessException("재주문할 수 없습니다.", HttpStatus.NOT_FOUND);
        }
        cartRepository.findByUserNo(callUserNo).ifPresent(cart -> {
            cartDetailRepository.deleteByCartId(cart.getCartId());
            cartRepository.delete(cart);
            cartRepository.flush();
        });

        Cart newCart = new Cart();
        newCart.setUserNo(callUserNo);
        newCart.setStoreId(order.getStoreId());
        cartRepository.saveAndFlush(newCart);

        for(OrderDetail orderDetail : orderDetails) {
            CartDetail cartDetail = new CartDetail();
            cartDetail.setCartId(newCart.getCartId());
            cartDetail.setMenuId(orderDetail.getMenuId());
            cartDetail.setQuantity(orderDetail.getQuantity());
            cartDetailRepository.save(cartDetail);
        }
    }

    /**
     * rider → main 배달 상태 변경 알림 (interfaces.md §2.1, ADR-004 매핑).
     * 트랜잭션: 단일 UPDATE — orderRepository.findById 후 setDeliveryState, JPA dirty checking으로 commit 시점 자동 UPDATE.
     */
    @Transactional
    public DeliveryStatusUpdateRes updateDeliveryStatus(Long orderId, String deliveryStatus) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("주문을 찾을 수 없습니다: " + orderId, HttpStatus.NOT_FOUND));

        Integer newState = DELIVERY_STATE_MAP.get(deliveryStatus);
        if (newState == null) {
            throw new BusinessException("유효하지 않은 배달 상태입니다: " + deliveryStatus, HttpStatus.BAD_REQUEST);
        }

        Integer previousState = order.getDeliveryState();
        order.setDeliveryState(newState);

        return new DeliveryStatusUpdateRes(orderId, previousState, newState);
    }

    /**
     * rider → main 배달 완료 처리 (interfaces.md §2.2).
     * delivery_state=3 (DELIVERED 종결, ADR-004) + order_state=6 (배달완료, CLAUDE.md §7).
     * completedAt body 인자는 수신 후 무시 (Q-A8.e-1 (나), orders.completed_at 컬럼 부재 — tech-debt).
     */
    @Transactional
    public DeliveryCompleteRes completeDelivery(Long orderId, LocalDateTime completedAt) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("주문을 찾을 수 없습니다: " + orderId, HttpStatus.NOT_FOUND));

        if (completedAt != null) {
            log.info("배달 완료 알림 수신 (completedAt={}, orderId={}) — orders.completed_at 컬럼 부재로 무시", completedAt, orderId);
        }

        order.setDeliveryState(3);
        order.setOrderState(6);

        return new DeliveryCompleteRes(orderId, 3);
    }
}
