package com.green.mmg.main.owner;


import com.green.mmg.main.owner.model.*;
import com.green.mmg.common.dto.ResultResponse;
import com.green.mmg.common.model.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/owner")
@RequiredArgsConstructor
public class OwnerController {

    private final OwnerService ownerService;

    @Value("${file.upload.menu-path:C:/uploads/menu/}")
    private String menuUploadPath;

    @Value("${file.upload.store-path:C:/uploads/store/}")
    private String storeUploadPath;

    // ========== 가게 관련 ==========

    @PostMapping("/store")
    public ResultResponse<Void> postStore(@RequestBody OwnerStoreRegReq dto){
        log.info("가게 등록 요청 데이터: {}", dto);
        ownerService.registerStore(dto);
        return new ResultResponse<>("가게 등록 성공", null);
    }

    @PutMapping("/store")
    public ResultResponse<Void> updatedStore(@RequestBody OwnerStoreUpdateReq dto){
        log.info("가게 기본 정보 수정: {}", dto);
        ownerService.updateStore(dto);
        return new ResultResponse<>("기본정보 수정 완료", null);
    }

    @PutMapping("/store/status")
    public ResultResponse<OwnerStoreRes> updateStoreStatus(@RequestBody OwnerStoreUpdateStatusReq dto){
        log.info("가게 운영관리 수정: {}", dto);
        OwnerStoreRes updatedStore = ownerService.updateStoreStatus(dto);
        return new ResultResponse<>("운영정보 업데이트 완료", null);
    }

    @DeleteMapping("/store/{store_id}")
    public ResultResponse<Void> deleteStore(@PathVariable Long store_id){
        log.info("가게 삭제 요청: store_id = {}", store_id);
        ownerService.deleteStore(store_id);
        return new ResultResponse<>("가게 삭제 성공", null);
    }

    // 내 가게 목록 조회 (여러 가게 지원)
    @GetMapping("/store")
    public ResultResponse<List<OwnerStoreRes>> getMyStores(@AuthenticationPrincipal UserPrincipal principal) {
        List<OwnerStoreRes> stores = ownerService.getMyStores(principal.getSignedUserNo());
        return new ResultResponse<>("가게 조회 성공", stores);
    }

    // 가게 이미지 업로드
    @PostMapping("/store/image")
    public ResultResponse<String> uploadStoreImage(@RequestParam("file") MultipartFile file) throws IOException {
        String imageUrl = ownerService.uploadImage(file, storeUploadPath, "/uploads/store/");
        return new ResultResponse<>("가게 이미지 업로드 성공", imageUrl);
    }

    // ========== 주문 관련 ==========

    @GetMapping("/order")
    public ResultResponse<List<OwnerOrderRes>> getOrders(
            @RequestParam Long store_id,
            @RequestParam(required = false) Integer state,
            @RequestParam(required = false) String date) {
        log.info("주문 조회 요청: store_id = {}, state = {}, date = {}", store_id, state, date);
        List<OwnerOrderRes> list = ownerService.getOrders(store_id, state, date);
        return new ResultResponse<>(String.format("%d건의 주문을 조회합니다.", list.size()), list);
    }

    @PutMapping("/order/{order_id}")
    public ResultResponse<Void> putOrderState(@PathVariable Long order_id,
                                              @RequestBody OwnerOrderStateReq req){
        log.info("주문 상태 요청: order_id = {}, state = {}", order_id, req.getOrderState());
        req.setOrderId(order_id);
        ownerService.updateOrderState(req);
        return new ResultResponse<>("주문 상태 수정 성공", null);
    }

    @DeleteMapping("/order/{order_id}")
    public ResultResponse<Void> deleteOrder(@PathVariable Long order_id){
        log.info("주문 삭제 요청: order_id = {}", order_id);
        ownerService.deleteOrder(order_id);
        return new ResultResponse<>("주문 삭제 성공", null);
    }

    // ========== 메뉴 관련 ==========

    @PostMapping("/menu")
    public ResultResponse<OwnerMenuRes> registerMenu(@AuthenticationPrincipal UserPrincipal principal,
                                                     @RequestBody OwnerMenuRegReq dto){
        OwnerMenuRes result = ownerService.registerMenu(principal.getSignedUserNo(), dto);
        return new ResultResponse<>("메뉴가 등록 되었습니다", result);
    }

    @PutMapping("/menu")
    public ResultResponse<OwnerMenuRes> updateMenu(@AuthenticationPrincipal UserPrincipal principal,
                                                   @RequestBody OwnerMenuUpdateReq dto){
        OwnerMenuRes updateMenu = ownerService.updateMenu(principal.getSignedUserNo(), dto);
        return new ResultResponse<>("메뉴가 수정되었습니다.", updateMenu);
    }

    @DeleteMapping("/menu/{menu_id}")
    public ResultResponse<Long> deleteMenu(@AuthenticationPrincipal UserPrincipal principal,
                                           @PathVariable("menu_id") Long menuId){
        Long deleteId = ownerService.deleteMenu(principal.getSignedUserNo(), menuId);
        return new ResultResponse<>("메뉴가 삭제되었습니다.", deleteId);
    }

    @GetMapping("/menu")
    public ResultResponse<List<OwnerMenuRes>> getMenus(@AuthenticationPrincipal UserPrincipal principal,
                                                       @RequestParam Long storeId) {
        List<OwnerMenuRes> list = ownerService.getMenusByStoreId(principal.getSignedUserNo(), storeId);
        return new ResultResponse<>("메뉴 조회 성공", list);
    }

    // 메뉴 이미지 업로드 (로컬 파일 저장 → URL 경로 반환)
    @PostMapping("/menu/image")
    public ResultResponse<String> uploadMenuImage(@RequestParam("file") MultipartFile file) throws IOException {
        String imageUrl = ownerService.uploadImage(file, menuUploadPath, "/uploads/menu/");
        return new ResultResponse<>("이미지 업로드 성공", imageUrl);
    }

    // ========== 카테고리 관련 ==========

    @GetMapping("/category")
    public ResultResponse<List<Map<String, Object>>> getCategories(@AuthenticationPrincipal UserPrincipal principal,
                                                                   @RequestParam Long storeId) {
        return new ResultResponse<>("카테고리 조회 성공",
                ownerService.getCategoriesByStoreId(principal.getSignedUserNo(), storeId));
    }

    @PostMapping("/category")
    public ResultResponse<Void> addCategory(@AuthenticationPrincipal UserPrincipal principal,
                                            @RequestBody Map<String, Object> body) {
        ownerService.addCategory(principal.getSignedUserNo(),
                Long.valueOf(body.get("storeId").toString()), body.get("category").toString());
        return new ResultResponse<>("카테고리 추가 성공", null);
    }

    @PutMapping("/category")
    public ResultResponse<Void> updateCategory(@AuthenticationPrincipal UserPrincipal principal,
                                               @RequestBody Map<String, Object> body) {
        ownerService.updateCategory(principal.getSignedUserNo(),
                Long.valueOf(body.get("categoryId").toString()), body.get("category").toString());
        return new ResultResponse<>("카테고리 수정 성공", null);
    }

    @DeleteMapping("/category/{categoryId}")
    public ResultResponse<Void> deleteCategory(@AuthenticationPrincipal UserPrincipal principal,
                                               @PathVariable Long categoryId) {
        ownerService.deleteCategory(principal.getSignedUserNo(), categoryId);
        return new ResultResponse<>("카테고리 삭제 성공", null);
    }

    // ========== 매출 관련 ==========

    @GetMapping("/sales/stats")
    public ResultResponse<OwnerSalesStatsRes> getSalesStats(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam Long storeId,
            @RequestParam String period) {
        OwnerSalesStatsRes stats = ownerService.getSalesStats(principal.getSignedUserNo(), storeId, period);
        return new ResultResponse<>("매출 통계 조회 성공", stats);
    }

    // ranking도 동일하게
    @GetMapping("/sales/ranking")
    public ResultResponse<List<OwnerSalesRankingRes>> getSalesRanking(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam Long storeId,
            @RequestParam String period) {
        List<OwnerSalesRankingRes> ranking = ownerService.getSalesRanking(principal.getSignedUserNo(), storeId, period);
        return new ResultResponse<>("매출 순위 조회 성공", ranking);
    }
}