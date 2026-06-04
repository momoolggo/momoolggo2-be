package com.green.mmg.main.cart;

import com.green.mmg.common.exception.BusinessException;
import com.green.mmg.main.cart.model.*;
import com.green.mmg.main.owner.MenuOptionCategoryRepository;
import com.green.mmg.main.owner.MenuOptionRepository;
import com.green.mmg.main.owner.entity.MenuOption;
import com.green.mmg.main.owner.entity.MenuOptionCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Phase 3-B-3: 하이브리드 영구 공존 핵심 검증 도메인.
 * Phase 2-Backfill-D Step D-3: cartItem 소유자 검증 추가 (보안).
 *
 * <p>모든 메서드는 호출자 userNo (JWT principal에서 추출)를 받고 본인 자원만 접근 가능.
 * 다른 사용자 카트/cartItem 접근 시도는 {@link BusinessException} FORBIDDEN.</p>
 *
 * <p>cartItem 소유자 검증: cartItemId로 CartDetail 조회 → cart의 userNo 비교.</p>
 */
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartMapper cartMapper;                  // MyBatis (JOIN 잔존)
    private final CartRepository cartRepository;          // JPA
    private final CartDetailRepository cartDetailRepository;  // JPA
    private final MenuOptionCategoryRepository menuOptionCategoryRepository;
    private final MenuOptionRepository menuOptionRepository;

    @Transactional
    public CartListRes getCart(long callerUserNo, Long userNo) {
        verifyOwner(callerUserNo, userNo);
        Cart cart = findActiveCart(userNo);
        if (cart == null) return null;

        // 가게명 + 아이템 목록은 MyBatis JOIN 잔존
        String storeName = cartMapper.findStoreNameByStoreId(cart.getStoreId());
        List<CartItemRes> items = cartMapper.findCartItems(cart.getCartId());

        CartListRes response = new CartListRes();
        response.setCartId(cart.getCartId());
        response.setStoreId(cart.getStoreId());
        response.setStoreName(storeName);
        response.setItems(items);

        int totalCount = items.stream()
                                .mapToInt(CartItemRes::getQuantity)
                                .sum();
        response.setCartCount(totalCount);

        return response;
    }

    @Transactional
    public void addToCart(long callerUserNo, CartAddRequestDto dto) {
        verifyOwner(callerUserNo, dto.getUserNo());
        Long storeId = cartMapper.findStoreIdByMenuId(dto.getMenuId());  // MyBatis JOIN
        if (storeId == null) throw new RuntimeException("존재하지 않는 메뉴입니다.");
        CartOptionSnapshot optionSnapshot = buildOptionSnapshot(dto);

        Cart existCart = findActiveCart(dto.getUserNo());

        if (existCart == null) {
            // 새 카트 생성: save 후 MyBatis 후속 호출 가시화 위해 saveAndFlush
            Cart newCart = new Cart();
            newCart.setUserNo(dto.getUserNo());
            newCart.setStoreId(storeId);
            cartRepository.saveAndFlush(newCart);

            CartDetail detail = newDetail(newCart.getCartId(), dto.getMenuId(), dto.getQuantity(), optionSnapshot);
            cartDetailRepository.saveAndFlush(detail);

        } else if (existCart.getStoreId().equals(storeId)) {
            Optional<CartDetail> existItem =
                    cartDetailRepository.findByCartIdAndMenuIdAndOptionSignature(
                            existCart.getCartId(),
                            dto.getMenuId(),
                            optionSnapshot.signature()
                    );

            if (existItem.isPresent()) {
                // 같은 메뉴 — 수량 합산 (dirty checking)
                CartDetail item = existItem.get();
                item.setQuantity(item.getQuantity() + dto.getQuantity());
                cartDetailRepository.saveAndFlush(item);
            } else {
                CartDetail detail = newDetail(existCart.getCartId(), dto.getMenuId(), dto.getQuantity(), optionSnapshot);
                cartDetailRepository.saveAndFlush(detail);
            }

        } else {
            throw new DifferentStoreException("다른 매장의 메뉴가 장바구니에 있습니다.");
        }
    }

    @Transactional
    public void clearAndAddToCart(long callerUserNo, CartAddRequestDto dto) {
        verifyOwner(callerUserNo, dto.getUserNo());
        Long storeId = cartMapper.findStoreIdByMenuId(dto.getMenuId());
        if (storeId == null) throw new RuntimeException("존재하지 않는 메뉴입니다.");
        CartOptionSnapshot optionSnapshot = buildOptionSnapshot(dto);

        Cart existingCart = findActiveCart(dto.getUserNo());
        if (existingCart != null) {
            cartDetailRepository.deleteByCartId(existingCart.getCartId());
            cartRepository.delete(existingCart);
            cartRepository.flush();
        }

        Cart newCart = new Cart();
        newCart.setUserNo(dto.getUserNo());
        newCart.setStoreId(storeId);
        cartRepository.saveAndFlush(newCart);

        CartDetail detail = newDetail(newCart.getCartId(), dto.getMenuId(), dto.getQuantity(), optionSnapshot);
        cartDetailRepository.saveAndFlush(detail);
    }

    @Transactional
    public void updateCartItem(long callerUserNo, Long cartItemId, int quantity) {
        CartDetail item = cartDetailRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("장바구니 아이템 없음"));
        verifyCartItemOwner(callerUserNo, item.getCartId());
        item.setQuantity(quantity);  // dirty checking
    }

    @Transactional
    public void updateCartItemOptions(long callerUserNo, Long cartItemId, CartAddRequestDto dto) {
        CartDetail item = cartDetailRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("장바구니 아이템 없음"));
        verifyCartItemOwner(callerUserNo, item.getCartId());

        dto.setMenuId(item.getMenuId());
        CartOptionSnapshot optionSnapshot = buildOptionSnapshot(dto);
        if (dto.getQuantity() > 0) {
            item.setQuantity(dto.getQuantity());
        }
        item.setOptionSignature(optionSnapshot.signature());
        item.setOptionSummary(optionSnapshot.summary());
        item.setOptionPrice(optionSnapshot.optionPrice());
    }

    @Transactional
    public void deleteCartItem(long callerUserNo, Long cartItemId) {
        CartDetail item = cartDetailRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("장바구니 아이템 없음"));
        verifyCartItemOwner(callerUserNo, item.getCartId());
        Long cartId = item.getCartId();
        cartDetailRepository.delete(item);
        cartDetailRepository.flush();

        long remain = cartDetailRepository.countByCartId(cartId);
        if (remain == 0) {
            cartRepository.deleteById(cartId);
        }
    }

    @Transactional
    public void clearCart(long callerUserNo, Long userNo) {
        verifyOwner(callerUserNo, userNo);
        Cart cart = findActiveCart(userNo);
        if (cart != null) {
            cartDetailRepository.deleteByCartId(cart.getCartId());
            cartRepository.delete(cart);
        }
    }

    /** URL/dto의 targetUserNo가 호출자와 일치하는지 동결 검증 (자기 카트만) */
    private void verifyOwner(long callerUserNo, Long targetUserNo) {
        if (!Objects.equals(targetUserNo, callerUserNo)) {
            throw new BusinessException("본인 장바구니만 접근 가능합니다.", HttpStatus.FORBIDDEN);
        }
    }

    private Cart findActiveCart(Long userNo) {
        List<Cart> carts = cartRepository.findAllByUserNoOrderByCartIdDesc(userNo);
        if (carts.isEmpty()) {
            return null;
        }

        Cart activeCart = carts.get(0);
        for (int i = 1; i < carts.size(); i++) {
            Cart duplicateCart = carts.get(i);
            cartDetailRepository.deleteByCartId(duplicateCart.getCartId());
            cartRepository.delete(duplicateCart);
        }
        return activeCart;
    }

    /** cartItemId의 cart 소유자가 호출자인지 동결 검증 (cartId → Cart.userNo 조회) */
    private void verifyCartItemOwner(long callerUserNo, Long cartId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new BusinessException("장바구니를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        if (!Objects.equals(cart.getUserNo(), callerUserNo)) {
            throw new BusinessException("본인 장바구니 아이템만 접근 가능합니다.", HttpStatus.FORBIDDEN);
        }
    }

    private CartOptionSnapshot buildOptionSnapshot(CartAddRequestDto dto) {
        List<MenuOptionCategory> categories = menuOptionCategoryRepository.findByMenuId(dto.getMenuId());
        if (categories == null) {
            categories = List.of();
        }
        List<CartAddRequestDto.CartOptionRequestDto> selectedOptions =
                dto.getSelectedOptions() == null ? List.of() : dto.getSelectedOptions();

        Map<Long, List<Long>> selectedOptionIdsByCategory = selectedOptions.stream()
                .filter(option -> option.getOptionCategoryNo() != null && option.getOptionId() != null)
                .collect(Collectors.groupingBy(
                        CartAddRequestDto.CartOptionRequestDto::getOptionCategoryNo,
                        Collectors.mapping(CartAddRequestDto.CartOptionRequestDto::getOptionId, Collectors.toList())
                ));

        List<MenuOption> selectedOptionsForSnapshot = new ArrayList<>();

        for (MenuOptionCategory category : categories) {
            List<Long> selectedOptionIds = selectedOptionIdsByCategory.getOrDefault(
                    category.getOptionCategoryNo(),
                    List.of()
            );

            if (Boolean.TRUE.equals(category.getIsRequired()) && selectedOptionIds.isEmpty()) {
                throw new BusinessException(
                        category.getOptionCategoryName() + " 옵션을 선택해 주세요.",
                        HttpStatus.BAD_REQUEST
                );
            }

            int maxSelect = category.getMaxSelect() == null ? 1 : category.getMaxSelect();
            if (selectedOptionIds.size() > maxSelect) {
                throw new BusinessException(
                        category.getOptionCategoryName() + " 옵션은 최대 " + maxSelect + "개까지 선택할 수 있습니다.",
                        HttpStatus.BAD_REQUEST
                );
            }

            List<MenuOption> categoryOptions = menuOptionRepository.findByOptionCategoryNo(category.getOptionCategoryNo());
            Map<Long, MenuOption> optionMap = categoryOptions.stream()
                    .collect(Collectors.toMap(MenuOption::getOptionId, Function.identity()));

            for (Long optionId : selectedOptionIds) {
                MenuOption option = optionMap.get(optionId);
                if (option == null) {
                    throw new BusinessException("선택할 수 없는 옵션입니다.", HttpStatus.BAD_REQUEST);
                }
                if ("Y".equalsIgnoreCase(option.getSoldOut())) {
                    throw new BusinessException(option.getName() + " 옵션은 품절입니다.", HttpStatus.BAD_REQUEST);
                }
                selectedOptionsForSnapshot.add(option);
            }
        }

        Set<Long> validCategoryNos = categories.stream()
                .map(MenuOptionCategory::getOptionCategoryNo)
                .collect(Collectors.toSet());
        if (!selectedOptionIdsByCategory.keySet().stream().allMatch(validCategoryNos::contains)) {
            throw new BusinessException("선택할 수 없는 옵션입니다.", HttpStatus.BAD_REQUEST);
        }

        selectedOptionsForSnapshot.sort(Comparator.comparing(MenuOption::getOptionId));

        String signature = selectedOptionsForSnapshot.stream()
                .map(option -> String.valueOf(option.getOptionId()))
                .collect(Collectors.joining(","));
        String summary = selectedOptionsForSnapshot.stream()
                .map(option -> option.getPrice() == null || option.getPrice() == 0
                        ? option.getName()
                        : option.getName() + "(+" + option.getPrice() + "원)")
                .collect(Collectors.joining(", "));
        int optionPrice = selectedOptionsForSnapshot.stream()
                .mapToInt(option -> option.getPrice() == null ? 0 : option.getPrice())
                .sum();

        return new CartOptionSnapshot(signature, summary, optionPrice);
    }

    private static CartDetail newDetail(Long cartId, Long menuId, int quantity, CartOptionSnapshot optionSnapshot) {
        CartDetail d = new CartDetail();
        d.setCartId(cartId);
        d.setMenuId(menuId);
        d.setQuantity(quantity);
        d.setOptionSignature(optionSnapshot.signature());
        d.setOptionSummary(optionSnapshot.summary());
        d.setOptionPrice(optionSnapshot.optionPrice());
        return d;
    }

    private record CartOptionSnapshot(String signature, String summary, int optionPrice) {
    }
}
