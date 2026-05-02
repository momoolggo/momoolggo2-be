package com.green.mmg.main.owner;


import com.green.mmg.common.dto.feign.UserBriefDto;
import com.green.mmg.common.exception.BusinessException;
import com.green.mmg.common.feign.AuthFeignClient;
import com.green.mmg.main.owner.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class OwnerService {

    private final OwnerMapper ownerMapper;
    private final AuthFeignClient authFeignClient;   // Phase 4-A: cross-schema customerName/tel 합성

    // ========== 이미지 업로드 (공통) ==========

    public String uploadImage(MultipartFile file, String uploadPath, String urlPrefix) {
        try {
            if (file.isEmpty()) {
                throw new IllegalArgumentException("파일이 비어있습니다.");
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new IllegalArgumentException("이미지 파일만 업로드 가능합니다.");
            }

            File dir = new File(uploadPath);
            if (!dir.exists()) dir.mkdirs();

            String originalName = file.getOriginalFilename();
            String fileName = UUID.randomUUID() + "_" + originalName;
            File savedFile = new File(uploadPath + fileName);

            Thumbnails.of(file.getInputStream())
                    .size(800, 600)
                    .outputQuality(0.8)
                    .toFile(savedFile);

            log.info("이미지 저장 완료: {}", savedFile.getAbsolutePath());
            return urlPrefix + fileName;

        } catch (IOException e) {
            throw new RuntimeException("이미지 업로드 실패: " + e.getMessage());
        }
    }

    // ========== 가게 관련 (D-bis 그룹 ㄱ: 권한 분기 + dto.userId 위조 방지) ==========

    @Transactional
    public void registerStore(long callerOwnerNo, OwnerStoreRegReq dto){
        // dto.userId 위조 방지: 옵션 B — 불일치 시 403 throw (강제 덮어쓰기 대신 명시적 거부)
        if (!Objects.equals(dto.getUserId(), callerOwnerNo)) {
            throw new BusinessException("자신의 계정으로만 가게를 등록할 수 있습니다.", HttpStatus.FORBIDDEN);
        }
        log.info("가게 등록 로직 시작: {}", dto.getStoreName());
        int result = ownerMapper.registerStore(dto);
        if (result == 0) {
            throw new RuntimeException("가게 등록 실패");
        }
        ownerMapper.registerStoreCategory(dto.getUserId(), dto.getCategoryId());
        ownerMapper.registerDefaultMenuCategory(dto.getUserId());
    }

    @Transactional
    public void updateStore(long callerOwnerNo, OwnerStoreUpdateReq dto){
        // OwnerStoreUpdateReq.storeId는 String 타입 — Long으로 변환 후 권한 검증
        // (타입 정리는 tech-debt.md, 프론트 협의 필요)
        long storeId;
        try {
            storeId = Long.parseLong(dto.getStoreId());
        } catch (NumberFormatException e) {
            throw new BusinessException("storeId 형식이 잘못되었습니다.", HttpStatus.BAD_REQUEST);
        }
        verifyStoreOwner(callerOwnerNo, storeId);
        int result = ownerMapper.updateStore(dto);
        if (result == 0){
            throw new RuntimeException("가게 정보 수정 실패: 해당 가게를 찾을 수 없음");
        }
    }

    @Transactional
    public OwnerStoreRes updateStoreStatus(long callerOwnerNo, OwnerStoreUpdateStatusReq dto){
        verifyStoreOwner(callerOwnerNo, dto.getStoreId());
        ownerMapper.updateStoreStatus(dto);
        return ownerMapper.getStoreById(dto.getStoreId());
    }

    @Transactional
    public void deleteStore(long callerOwnerNo, Long store_id){
        verifyStoreOwner(callerOwnerNo, store_id);
        int result = ownerMapper.deleteStore(store_id);
        if (result == 0){
            throw new RuntimeException("삭제할 가게를 찾을 수 없습니다.");
        }
    }

    // 내 가게 1개 조회 (매출 조회 등 내부용) — ownerNo는 callerOwnerNo와 동일 (본인 자원만 조회)
    @Transactional(readOnly = true)
    public OwnerStoreRes getMyStore(long callerOwnerNo) {
        return ownerMapper.getMyStore(callerOwnerNo);
    }

    // 내 가게 목록 조회 (여러 가게 지원)
    @Transactional(readOnly = true)
    public List<OwnerStoreRes> getMyStores(long callerOwnerNo) {
        return ownerMapper.getMyStores(callerOwnerNo);
    }

    // ========== 주문 관련 (D-bis 그룹 ㄴ: 권한 분기 추가) ==========

    @Transactional(readOnly = true)
    public List<OwnerOrderRes> getOrders(long callerOwnerNo, Long storeId, Integer state, String date) {
        verifyStoreOwner(callerOwnerNo, storeId);
        List<OwnerOrderRes> orders = ownerMapper.getOrders(storeId, state, date);
        if (orders.isEmpty()) return orders;

        // Phase 4-A: customerName/tel을 Feign batch로 합성 (N+1 회피)
        List<Long> userNos = orders.stream()
                .map(OwnerOrderRes::getUserNo)
                .distinct().collect(Collectors.toList());
        Map<Long, UserBriefDto> userMap = authFeignClient.getUsers(userNos).stream()
                .collect(Collectors.toMap(UserBriefDto::getUserNo, u -> u));

        orders.forEach(o -> {
            UserBriefDto u = userMap.get(o.getUserNo());
            if (u != null) {
                o.setCustomerName(u.getName());
                o.setTel(u.getTel());
            }
        });
        return orders;
    }

    @Transactional
    public void updateOrderState(long callerOwnerNo, OwnerOrderStateReq req){
        verifyOrderOwner(callerOwnerNo, req.getOrderId());
        int result = ownerMapper.updateOrderState(req);
        if (result == 0){
            throw new RuntimeException("주문 상태 변경 실패: 주문을 찾을 수 없습니다.");
        }
    }

    @Transactional
    public void deleteOrder(long callerOwnerNo, Long orderId){
        verifyOrderOwner(callerOwnerNo, orderId);
        ownerMapper.deleteOrderDetail(orderId);
        ownerMapper.deleteOrder(orderId);
    }

    // ========== 메뉴 관련 (D-bis 그룹 ㄷ: 권한 분기 추가) ==========

    @Transactional
    public OwnerMenuRes registerMenu(long callerOwnerNo, OwnerMenuRegReq dto){
        verifyStoreOwner(callerOwnerNo, dto.getStoreId());
        ownerMapper.registerMenu(dto);
        return ownerMapper.getMenuById(dto.getMenuId());
    }

    @Transactional
    public OwnerMenuRes updateMenu(long callerOwnerNo, OwnerMenuUpdateReq dto){
        verifyMenuOwner(callerOwnerNo, dto.getMenuId());
        int result = ownerMapper.updateMenu(dto);
        if (result == 0) {
            throw new RuntimeException("메뉴 수정 실패: 해당 메뉴를 찾을 수 없음");
        }
        return ownerMapper.getMenuById(dto.getMenuId());
    }

    @Transactional
    public Long deleteMenu(long callerOwnerNo, Long menuId){
        verifyMenuOwner(callerOwnerNo, menuId);
        int result = ownerMapper.deleteMenu(menuId);
        if (result == 0) {
            throw new RuntimeException("메뉴 삭제 실패: 해당 메뉴를 찾을 수 없음");
        }
        return menuId;
    }

    @Transactional(readOnly = true)
    public List<OwnerMenuRes> getMenusByStoreId(long callerOwnerNo, Long storeId) {
        verifyStoreOwner(callerOwnerNo, storeId);
        return ownerMapper.getMenusByStoreId(storeId);
    }

    // ========== 매출 관련 (D-bis 그룹 ㄹ: 권한 분기 추가) ==========

    @Transactional(readOnly = true)
    public OwnerSalesStatsRes getSalesStats(long callerOwnerNo, long storeId, String period) {
        verifyStoreOwner(callerOwnerNo, storeId);
        return ownerMapper.getSalesStats(storeId, period);
    }

    @Transactional(readOnly = true)
    public List<OwnerSalesRankingRes> getSalesRanking(long callerOwnerNo, long storeId, String period) {
        verifyStoreOwner(callerOwnerNo, storeId);
        return ownerMapper.getSalesRanking(storeId, period);
    }

    // ========== Phase 2-Backfill-D-bis: 권한 검증 헬퍼 (private) ==========

    /** store가 callerOwnerNo 소유인지. 미존재 → NOT_FOUND, 타인 소유 → FORBIDDEN. */
    private void verifyStoreOwner(long callerOwnerNo, long storeId) {
        Long ownerId = ownerMapper.findStoreOwnerByStoreId(storeId);
        if (ownerId == null) {
            throw new BusinessException("가게를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
        }
        if (!Objects.equals(ownerId, callerOwnerNo)) {
            throw new BusinessException("본인 가게만 접근 가능합니다.", HttpStatus.FORBIDDEN);
        }
    }

    /** order의 store가 callerOwnerNo 소유인지. */
    private void verifyOrderOwner(long callerOwnerNo, long orderId) {
        Long ownerId = ownerMapper.findStoreOwnerByOrderId(orderId);
        if (ownerId == null) {
            throw new BusinessException("주문을 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
        }
        if (!Objects.equals(ownerId, callerOwnerNo)) {
            throw new BusinessException("본인 가게의 주문만 접근 가능합니다.", HttpStatus.FORBIDDEN);
        }
    }

    /** menu의 store가 callerOwnerNo 소유인지. */
    private void verifyMenuOwner(long callerOwnerNo, long menuId) {
        Long ownerId = ownerMapper.findStoreOwnerByMenuId(menuId);
        if (ownerId == null) {
            throw new BusinessException("메뉴를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
        }
        if (!Objects.equals(ownerId, callerOwnerNo)) {
            throw new BusinessException("본인 가게의 메뉴만 접근 가능합니다.", HttpStatus.FORBIDDEN);
        }
    }

    /** category의 store가 callerOwnerNo 소유인지. */
    private void verifyCategoryOwner(long callerOwnerNo, long categoryId) {
        Long ownerId = ownerMapper.findStoreOwnerByCategoryId(categoryId);
        if (ownerId == null) {
            throw new BusinessException("카테고리를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
        }
        if (!Objects.equals(ownerId, callerOwnerNo)) {
            throw new BusinessException("본인 가게의 카테고리만 접근 가능합니다.", HttpStatus.FORBIDDEN);
        }
    }

    // ========== 카테고리 관련 (D-bis 그룹 ㅁ: 권한 분기 추가) ==========

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCategoriesByStoreId(long callerOwnerNo, Long storeId) {
        verifyStoreOwner(callerOwnerNo, storeId);
        return ownerMapper.getCategoriesByStoreId(storeId);
    }

    @Transactional
    public void addCategory(long callerOwnerNo, Long storeId, String category) {
        verifyStoreOwner(callerOwnerNo, storeId);
        ownerMapper.addCategory(storeId, category);
    }

    @Transactional
    public void updateCategory(long callerOwnerNo, Long categoryId, String category) {
        verifyCategoryOwner(callerOwnerNo, categoryId);
        ownerMapper.updateCategory(categoryId, category);
    }

    @Transactional
    public void deleteCategory(long callerOwnerNo, Long categoryId) {
        verifyCategoryOwner(callerOwnerNo, categoryId);
        ownerMapper.deleteCategory(categoryId);
    }
}