package com.green.mmg.main.order;

import com.green.mmg.common.exception.BusinessException;
import com.green.mmg.main.cart.CartDetailRepository;
import com.green.mmg.main.cart.model.CartDetail;
import com.green.mmg.main.coupon.CouponService;
import com.green.mmg.main.feign.AuthFeignClient;
import com.green.mmg.main.address.UserAddressRepository;
import com.green.mmg.main.cart.CartMapper;
import com.green.mmg.main.cart.CartRepository;
import com.green.mmg.main.cart.model.Cart;
import com.green.mmg.main.cart.model.CartItemRes;
import com.green.mmg.main.internal.dto.DeliveryCompleteRes;
import com.green.mmg.main.internal.dto.DeliveryStatusUpdateRes;
import com.green.mmg.main.internal.dto.InternalSettlementOrderListRes;
import com.green.mmg.main.internal.dto.InternalSettlementOrderRes;
import com.green.mmg.main.notification.NotificationService;
import com.green.mmg.main.notification.model.NotificationCreateReq;
import com.green.mmg.main.order.model.*;
import com.green.mmg.main.payment.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
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
    private final OrderDeliverySseService orderDeliverySseService;
    private final NotificationService notificationService;
    private final PaymentService paymentService;
    private final CouponService couponService;

    private static final int ORDER_STATE_WAITING = 1;
    private static final int ORDER_STATE_CANCELED = 2;

    private static final int PAY_STATE_UNPAID = 1;
    private static final int PAY_STATE_PAID = 2;
    private static final int PAY_STATE_REFUNDED = 3;

    private static final int DELIVERY_FEE = 1500;
    // 2026-05-25 9건 트랙 — 거리 기반 동적 배달팁 (rider DeliveryService와 일관: base 1500 + ceil(km)*1000)
    private static final int DELIVERY_BASE_FEE = 1500;
    private static final int FEE_PER_KM = 1000;
    private static final double EARTH_RADIUS_M = 6_371_000.0;
    @org.springframework.beans.factory.annotation.Autowired
    private com.green.mmg.main.owner.OwnerOrderSseService ownerOrderSseService;

    /** 좌표 기반 배달팁 — 좌표 NULL시 base만(1500). */
    private int computeDeliveryFee(Double storeLat, Double storeLng, Double dLat, Double dLng) {
        if (storeLat == null || storeLng == null || dLat == null || dLng == null) return DELIVERY_BASE_FEE;
        double phi1 = Math.toRadians(storeLat);
        double phi2 = Math.toRadians(dLat);
        double dPhi = Math.toRadians(dLat - storeLat);
        double dLambda = Math.toRadians(dLng - storeLng);
        double a = Math.sin(dPhi/2)*Math.sin(dPhi/2) + Math.cos(phi1)*Math.cos(phi2)*Math.sin(dLambda/2)*Math.sin(dLambda/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distanceM = EARTH_RADIUS_M * c;
        if (distanceM <= 0) return DELIVERY_BASE_FEE;
        int km = (int) Math.ceil(distanceM / 1000.0);
        return DELIVERY_BASE_FEE + km * FEE_PER_KM;
    }

    /** 가게 좌표 조회 + computeDeliveryFee 호출 helper. */
    private int calcDeliveryFee(Long storeId, OrderAddressInfo addr) {
        java.util.Map<String, Object> sc = orderMapper.findStoreCoord(storeId);
        if (sc == null) return DELIVERY_BASE_FEE;
        Object la = sc.get("storeLat"), ln = sc.get("storeLng");
        Double sLat = la instanceof Number ? ((Number) la).doubleValue() : null;
        Double sLng = ln instanceof Number ? ((Number) ln).doubleValue() : null;
        Double dLat = addr != null ? addr.getLatitude() : null;
        Double dLng = addr != null ? addr.getLongitude() : null;
        return computeDeliveryFee(sLat, sLng, dLat, dLng);
    }

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
                .mapToInt(i -> i.getUnitPrice() * i.getQuantity())
                .sum();

        OrderInfoRes res = new OrderInfoRes();
        res.setStoreName(storeName);
        res.setTel(tel);
        res.setAddress(addr != null ? addr.getAddress() : "");
        res.setAddressDetail(addr != null ? addr.getAddressDetail() : "");
        res.setItems(items);
        res.setMenuTotal(menuTotal);
        // 2026-05-25 9건 트랙 — 거리 기반 동적 배달팁
        int deliveryFee = calcDeliveryFee(cart.getStoreId(), addr);
        res.setDeliveryFee(deliveryFee);
        res.setTotalAmount(menuTotal + deliveryFee);
        return res;
    }

    @Transactional
    public OrderCreateRes placeOrder(Long userNo, OrderReqDto dto) {
        Cart cart = cartRepository.findByUserNo(userNo)
                .orElseThrow(() -> new RuntimeException("장바구니가 비어있습니다."));

        List<CartItemRes> items = cartMapper.findCartItems(cart.getCartId());
        if (items.isEmpty()) throw new RuntimeException("장바구니가 비어있습니다.");

        OrderAddressInfo addr = userAddressRepository.findFirstDefaultByUserNo(userNo).orElse(null);

        int menuTotal = items.stream()
                .mapToInt(i -> i.getUnitPrice() * i.getQuantity())
                .sum();
        // 2026-05-25 9건 트랙 — 거리 기반 동적 배달팁
        int deliveryFee = calcDeliveryFee(cart.getStoreId(), addr);

        // 기존 패턴 유지: serverOrderId + timestamp 결합 큰 숫자 ID
        long uniqueId = Long.parseLong("39" + System.currentTimeMillis());

        int discountedMenuTotal = couponService.applyCouponToOrder(
                userNo,
                uniqueId,
                dto.getCouponId(),
                dto.getCouponListId(),
                menuTotal
        );
        int totalAmount = discountedMenuTotal + deliveryFee;

        Orders order = new Orders();
        order.setOrderId(uniqueId);
        order.setUserNo(userNo);
        order.setStoreId(cart.getStoreId());
        order.setRequest(dto.getRequest());
        order.setRiderRequest(dto.getRiderRequest());
        order.setAddress(addr != null ? addr.getAddress() : "");
        order.setAddressDetail(addr != null ? addr.getAddressDetail() : "");
        order.setDeliveryFee(deliveryFee);
        order.setAmount(totalAmount);
        order.setPayState(dto.getPayState());
        order.setOrderState(ORDER_STATE_WAITING);
        order.setDeliveryState(1);
        order.setEcoSelected(Boolean.TRUE.equals(dto.getEcoSelected()));
        orderRepository.saveAndFlush(order);

        for (CartItemRes item : items) {
            OrderDetail detail = new OrderDetail();
            detail.setOrderId(uniqueId);
            detail.setMenuId(item.getMenuId());
            detail.setQuantity(item.getQuantity());
            detail.setMenuName(item.getMenuName());
            detail.setMenuPrice(item.getUnitPrice());
            detail.setOptionSummary(item.getOptionSummary());
            orderDetailRepository.save(detail);
        }

        // 가게 누적 주문 수(store.order_count) 갱신 — 같은 트랜잭션 내 처리
        orderMapper.calSumOrder(cart.getStoreId());

        return new OrderCreateRes(uniqueId, totalAmount);
    }


    // 주문 취소

    @Transactional
    public void cancelOrder(long callUserNo, long orderId, OrderCancelReq req) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("주문을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        if (!Objects.equals(order.getUserNo(), callUserNo)) {
            throw new BusinessException("본인 주문만 취소할 수 있습니다.", HttpStatus.FORBIDDEN);
        }

        if (!Objects.equals(order.getOrderState(), ORDER_STATE_WAITING)) {
            throw new BusinessException("가게가 주문을 수락한 이후에는 취소할 수 없습니다.", HttpStatus.BAD_REQUEST);
        }

        if (req == null || req.getReason() == null || req.getReason().isBlank()) {
            throw new BusinessException("취소 사유를 선택해 주세요.", HttpStatus.BAD_REQUEST);
        }

        Integer payState = order.getPayState();

        if (Objects.equals(payState, PAY_STATE_REFUNDED)) {
            throw new BusinessException("이미 환불된 주문입니다.", HttpStatus.BAD_REQUEST);
        }

        boolean refunded = false;
        if (Objects.equals(payState, PAY_STATE_PAID)) {
            paymentService.refundPaidOrder(order, req.getReason());
            refunded = true;
        } else if (payState == null || Objects.equals(payState, PAY_STATE_UNPAID)) {
            // 결제 전 주문은 환불 없이 주문 상태만 취소 처리
        } else {
            throw new BusinessException("취소할 수 없는 결제 상태입니다.", HttpStatus.BAD_REQUEST);
        }

        order.setOrderState(ORDER_STATE_CANCELED);

        if(refunded) {
            sendOrderCancelRefundedNotification(order, "주문이 취소되었습니다. 결제 취소는 결제수단에 따라 추가적으로 시일이 소요될 수 있습니다.");
        } else {
            sendOrderCancelledNotification(order, "주문이 취소되었습니다.");
        }

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
        return (int) orderRepository.countByUserNoAndPayState(userId, PAY_STATE_PAID);
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
            cartDetail.setOptionSummary(orderDetail.getOptionSummary());
            cartDetail.setOptionSignature("");
            cartDetail.setOptionPrice(0);
            cartDetailRepository.save(cartDetail);
        }
    }

    @Transactional(readOnly = true)
    public InternalSettlementOrderListRes getSettlementOrders(Long storeId,
                                                              LocalDate startDate,
                                                              LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();

        List<InternalSettlementOrderRes> orders =
                orderMapper.findSettlementOrderDetails(storeId, start, end);

        long totalSales =
                orderMapper.sumSettlementSales(storeId, start, end);

        return new InternalSettlementOrderListRes(
                storeId,
                startDate,
                endDate,
                totalSales,
                orders
        );
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

        // 2026-05-23 자잘 에러 트랙 #7 — 사장-라이더 배차 정합 (delivery → order_state forward 동기화)
        // delivery 1(ASSIGNED 등) → order_state=4 / delivery 2(PICKED_UP/DELIVERING) → 5
        // WAITING_ASSIGN(reject) keep / completeDelivery에서 delivery 3 → order_state=6 별도 처리
        if (!"WAITING_ASSIGN".equals(deliveryStatus)) {
            Integer mappedOrderState = null;
            if (newState == 1) mappedOrderState = 4;
            else if (newState == 2) mappedOrderState = 5;
            if (mappedOrderState != null
                    && (order.getOrderState() == null || mappedOrderState > order.getOrderState())) {
                order.setOrderState(mappedOrderState);
            }
        }

        OrderDeliveryStatusRes status = new OrderDeliveryStatusRes(
                order.getOrderId(),
                order.getOrderState(),
                getOrderStatusText(order.getOrderState()),
                newState,
                deliveryStateText(newState)
        );

        orderDeliverySseService.sendDeliveryStatus(orderId, status);

        // 2026-05-25 9건 트랙 — 라이더 배차 수락/픽업/배달 완료 시 사장 OrderList 자동 갱신
        if (ownerOrderSseService != null) {
            ownerOrderSseService.sendOrderStateChanged(order.getStoreId(), java.util.Map.of(
                    "orderId", orderId,
                    "orderState", order.getOrderState(),
                    "deliveryState", newState
            ));
        }

        if(!Objects.equals(previousState, newState)) {
            sendOrderStatusNotification(order, status);
        }


        return new DeliveryStatusUpdateRes(orderId, previousState, newState);
    }

    /**
     * rider → main 배달 완료 처리 (interfaces.md §2.2).
     * delivery_state=3 (DELIVERED 종결, ADR-004) + order_state=6 (배달완료, CLAUDE.md §7).
     * completedAt body 인자는 수신 후 무시 (Q-A8.e-1 (나), orders.completed_at 컬럼 부재 — tech-debt).
     */
    @Transactional
    public DeliveryCompleteRes completeDelivery(Long orderId, LocalDateTime completedAt, String deliveredPhotoUrl) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("주문을 찾을 수 없습니다: " + orderId, HttpStatus.NOT_FOUND));

        if (completedAt != null) {
            log.info("배달 완료 알림 수신 (completedAt={}, orderId={})", completedAt, orderId);
        }

        order.setDeliveryState(3);
        order.setOrderState(6);
        // 2026-05-25 9건 트랙 #7 — 배달 완료 사진 URL 박제 (자잘 에러 트랙 #9-B 일관)
        if (deliveredPhotoUrl != null && !deliveredPhotoUrl.isBlank()) {
            order.setDeliveredPhotoUrl(deliveredPhotoUrl);
        }

        OrderDeliveryStatusRes status = new OrderDeliveryStatusRes(
                order.getOrderId(),
                order.getOrderState(),
                getOrderStatusText(order.getOrderState()),
                3,
                deliveryStateText(3)
        );

        orderDeliverySseService.sendDeliveryStatus(orderId, status);
        // 2026-05-25 9건 트랙 — 배달 완료 시 사장 OrderList 자동 갱신
        if (ownerOrderSseService != null) {
            ownerOrderSseService.sendOrderStateChanged(order.getStoreId(), java.util.Map.of(
                    "orderId", orderId,
                    "orderState", 6,
                    "deliveryState", 3
            ));
        }
        sendOrderStatusNotification(order, status);

        return new DeliveryCompleteRes(orderId, 3);
    }

    private void sendOrderStatusNotification(Orders order, OrderDeliveryStatusRes status) {
        notificationService.createNotification(new NotificationCreateReq(
                order.getUserNo(),
                "DELIVERY_STATUS",
                "배달 상태 변경",
                "주문이 " + status.getDeliveryStateText() + " 상태로 변경되었습니다.",
                "/mypage/orders/" + order.getOrderId()
        ));
    }

    //주문 취소 및 환불 알림
    private void sendOrderCancelRefundedNotification(Orders order, String message) {
        notificationService.createNotification(new NotificationCreateReq(
                order.getUserNo(),
                "ORDER_CANCEL_REFUNDED",
                "주문 취소 및 환불 접수 완료",
                message,
                "/order/history/" + order.getOrderId()
        ));
    }

    private void sendOrderCancelledNotification(Orders order, String message){
        notificationService.createNotification(new NotificationCreateReq(
                order.getUserNo(),
                "ORDER_CANCELLED",
                "주문 취소 완료",
                message,
                "/order/history/" + order.getOrderId()
        ));
    }

    //배달 현황 SSE 알림
    @Transactional(readOnly = true)
    public OrderDeliveryStatusRes getDeliveryStatus(long callUserNo, Long orderId) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(()-> new BusinessException("주문을 찾을 수 없습니다", HttpStatus.NOT_FOUND));

        if(!Objects.equals(order.getUserNo(), callUserNo)) {
            throw new BusinessException("본인 주문만 조회 가능합니다", HttpStatus.FORBIDDEN);
        }

        Integer orderState = order.getOrderState();
        Integer deliveryState = order.getDeliveryState() == null ? 1 : order.getDeliveryState();

        return new OrderDeliveryStatusRes(
                order.getOrderId(),
                orderState,
                getOrderStatusText(orderState),
                deliveryState,
                deliveryStateText(deliveryState)
        );
    }

    private String getOrderStatusText(Integer orderState) {
        if(orderState == null) return "상태 없음";

        return switch (orderState) {
            case 1 -> "주문 대기";
            case 2 -> "주문 취소";
            case 3 -> "조리 중";
            case 4 -> "배차 완료";
            case 5 -> "배달 중";
            case 6 -> "배달 완료";
            default -> "상태 없음";
        };
    }

    private String deliveryStateText(Integer deliveryState){
        if(deliveryState == null) return "배달 준비 중";

        return switch (deliveryState) {
            case 1 -> "배달 준비 중";
            case 2-> "배달 중";
            case 3-> "배달 완료";
            default -> "배달 준비 중";
        };
    }

    public SseEmitter subscribeDeliveryStatus(long callUserNo, Long orderId) {
        getDeliveryStatus(callUserNo, orderId);
        return orderDeliverySseService.subscribe(orderId);
    }
}
