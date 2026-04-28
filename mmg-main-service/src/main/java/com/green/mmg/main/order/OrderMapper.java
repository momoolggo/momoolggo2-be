package com.green.mmg.main.order;

import com.green.mmg.main.order.model.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderMapper {
    long findUserNoByOrderId(long orderId);
    int deleteOrder(long id);
    //주문의 state 변경
    int updateState(OrderState orderstate);

    // Phase 4-A: findTelByUserNo는 AuthFeignClient.getUser().getTel()로 대체됨

    Orders findByOrderId(Long orderId);
    // 기본 배송지 조회
    OrderAddressInfo findDefaultAddress(@Param("userNo") Long userNo);

    // 주문 INSERT
    void insertOrder(@Param("uniqueId") Long uniqueId,
                     @Param("userNo")    Long    userNo,
                     @Param("storeId")   Long    storeId,
                     @Param("request")   String  request,
                     @Param("riderRequest")  String  riderRequest,
                     @Param("address")   String  address,
                     @Param("addressDetail") String addressDetail,
                     @Param("deliveryFee")   Integer deliveryFee,
                     @Param("amount")    Integer amount,
                     @Param("payState")  Integer payState);


    // 주문 상세 INSERT
    void insertOrderDetail(@Param("uniqueId")    Long    uniqueId,
                           @Param("menuId")     Long    menuId,
                           @Param("quantity")   Integer quantity,
                           @Param("menuName")   String  menuName,
                           @Param("menuPrice")  Integer price);
    List<OrderHistoryDto> findOrdersByUserId(OrderHistoryReq req);           // 주문 목록
    List<OrderHistoryDto.OrderItemDto> findItemsByOrderId(Long orderId); // 메뉴 목록
    OrderHistoryDto orderHistoryDetail(long id);
    int maxHistoryPage(long id);


    int calSumOrder(long id);
}