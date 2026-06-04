package com.green.mmg.main.internal;


import com.green.mmg.common.dto.ResultResponse;
import com.green.mmg.main.address.UserAddressRepository;
import com.green.mmg.main.cart.CartDetailRepository;
import com.green.mmg.main.cart.CartRepository;
import com.green.mmg.main.internal.dto.OwnerWithdrawCheckRes;
import com.green.mmg.main.notification.NotificationRepository;
import com.green.mmg.main.order.OrderRepository;
import com.green.mmg.main.store.LikedStoreRepository;
import com.green.mmg.main.store.StoreMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/internal/user")
@RequiredArgsConstructor
public class InternalUserCleanupController {
    private final UserAddressRepository userAddressRepository;
    private final LikedStoreRepository likedStoreRepository;
    private final CartRepository cartRepository;
    private final CartDetailRepository cartDetailRepository;
    private final NotificationRepository notificationRepository;
    private final OrderRepository orderRepository;
    private final StoreMapper storeMapper;

    @Transactional
    @PostMapping("/{userNo}/withdraw-cleanup")
    public ResultResponse<Void> cleanupWithdrawUser(@PathVariable Long userNo) {
        cartRepository.findFirstByUserNoOrderByCartIdDesc(userNo).ifPresent(cart -> {
            cartDetailRepository.deleteByCartId(cart.getCartId());
            cartRepository.delete(cart);
        });

        userAddressRepository.deleteByUserNo(userNo);
        likedStoreRepository.deleteByUserNo(userNo);
        notificationRepository.deleteByUserNo(userNo);

        return new ResultResponse<>("회원탈퇴 데이터 정리 완료", null);
    }

    @Transactional(readOnly = true)
    @GetMapping("/{userNo}/active-orders/exists")
    public ResultResponse<Boolean> hasActiveOrders(@PathVariable Long userNo){
        long count = orderRepository.countPaidActiveOrdersByUserNo(userNo, List.of(2, 6));
        return new ResultResponse<>("진행 중 주문 확인 완료", count > 0);
    }

    @Transactional(readOnly = true)
    @GetMapping("/{ownerNo}/owner-withdraw-check")
    public ResultResponse<OwnerWithdrawCheckRes> checkOwnerWithdraw(@PathVariable Long ownerNo) {
        List<Long> storeIds = storeMapper.findStoreIdsByOwnerNo(ownerNo);
        boolean hasActiveStore = storeMapper.countOpenStoresByOwnerNo(ownerNo) > 0;
        boolean hasActiveOrders = !storeIds.isEmpty()
                && orderRepository.countPaidActiveOrdersByStoreIdIn(storeIds, List.of(2, 6)) > 0;

        return new ResultResponse<>("사장 탈퇴 가능 여부 확인 완료",
                new OwnerWithdrawCheckRes(hasActiveStore, hasActiveOrders, storeIds));
    }

}
